package babble.net;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.util.ChannelInfo;

/**
 * A server for asynchronous network based request-response processing.
 * The server receives request over a network channel and produces 
 * a response on the same channel. The request is not blocked in the sense
 * that caller will receive the response in a separate thread
 * and hence the calling thread can processed while the request being 
 * processed by the server.
 * 
 * <p>
 * The server employs asynchronous behavior at two levels:
 * <ul>
 * <li>
 * The server listens to incoming requests over a socket. 
 * The server is not blocked on network i/o on listening for incoming 
 * requests but are called by underlying operating system when a request
 * arrives (via {@link Selector#select() selector}). This is
 * the first level of asynchronous behavior.
 * <li>
 * The incoming requests are handed over to a {@link Router dispatcher} 
 * which executes requests in separate request-processing thread instead of 
 * the server thread. This is the second level of asynchronous behavior.
 * <br>
 * 
 * Whenever a request has been processed, the response is posted back
 * to the server and server thread sends the response back to original 
 * remote client.  Hence remote i/o i.e. reading request and writing response 
 * to remote client happens on same 'main' server thread. 
 * </ul>
 * From a client's perspective, it sends a request over socket and server 
 * responds on the same socket whenever the request is ready. 
 * <br>
 * 
 * This server does not interpret the incoming request. A router 
 * defines the behavior to {@link Router#createRequest(SocketChannel, byte[]) 
 * interpret} the incoming request by a definite protocol such as HTTP.
 * 
 * @param R the type of request handled by this server. The type of request
 * is not directly important for this server but for the router used by
 * this server.
 * 
 * @param P the type of response handled by this server. The type of response
 * is not directly important for this server but for the router used by
 * this server.
 * 
 * @author pinaki poddar
 *
 */
public abstract class NioServer<R extends Request,P extends Response> 
    implements Runnable {
    private String _name; 
    private String _hostname;
    private int _port;
    private Selector _selector;
    private final Protocol<R,P> _protocol;
    private final Router<R,P> _router;
    private ExecutionContext _ctx;
    private final Object _selectorBug = new Object();
    
    private Logger _logger;

    /**
     * Creates a server to listen for incoming connection requests on 
     * the given port. The given protocol determines the type of request
     * and response.
     * 
     * @param serverName a name to describe this server.
     * @param port a port to listen for incoming request
     * @param protocol protocol used by this server
     * 
     * @throws IOException
     */
    protected NioServer(String serverName, int port, Protocol<R, P> protocol) {
        final String name = serverName;
        System.setProperty("java.net.preferIPv4Stack", ""+true);
        _name = serverName;
        _logger = LoggerFactory.getLogger(name);
        _port   = port;
        _hostname = determineHostname();
        _ctx = new ExecutionContext() {
            @Override
            public String getName() {
                return _name;
            }
        };
        _protocol = protocol;
        _router = _protocol.newRouter(this);
    }
    
    public final Protocol<R,P> geProtocol() {
        return _protocol;
    }
    
    /**
     * Gets a logical name of this server.
     */
    public String getName() {
        return _name;
    }
    
    /**
     * Gets the host name on which this server is running. 
     */
    String getHostName(){
        return _hostname;
    }
    
    /**
     * Gets the port on which this server listens for incoming request.
     */
    int getPort() {
        return _port;
    }
    
    /**
     * Sets the port to listen for incoming request.
     * 
     * @param port a non-zero positive number.
     * 
     * @exception IllegalArgumentException if port number is negative
     * @exception IllegalStateException if server is running.
     */
     void setPort(int port) {
        if (_selector != null) {
            throw new IllegalStateException("Can not change port for running server");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("invalid port " + port);
        }
        _port = port;
            
    }
    
    
    
    /**
     * Adds given route to this server. A route is invoked it  
     * {@link Route#matches(Request) matches} a request. 
     * A server requires at least one route to function.
     * 
     * @param route a route to be processed
     * 
     * @throws IllegalStateException if no router has been 
     * {@link #setRouter(Router) set}.
     */
    public void addRoute(Route<R,P> route) {
        if (_router == null) 
            throw new IllegalStateException("Router has not been set");
        _router.addRoute(route);
    }

    /**
     * Starts this server. Stars two threads: 'main' server thread runs
     * continuously for network i/o and a 'router' thread that processes
     * the routes.
     * 
     * @exception IOException if listening channel can not be opened 
     * @exception IllegalStateException if no route has been defined
     */
    public void start() throws IllegalStateException, IOException {
        _logger.info("starting " + getName() + " " + getURL());
        if (_router == null) throw new IllegalStateException("no router is set");
        if (!_router.hasRoute()) _logger.warn("***WARNING: no route is defined");
        
        _selector = initSelector();
        
        
        Thread routerThread = new Thread(_router, "Router Thread");
        Thread mainServerThread = new Thread(this, "Main I/O Thread");
        routerThread.setDaemon(true);
        mainServerThread.setDaemon(false);
        mainServerThread.start();
        routerThread.start();        
    }

    /**
     * Runs continually to accept request from and send response to remote
     * clients.
     * <br>
     * Checks via a {@link Selector#select() selector} when an incoming 
     * request arrives or a response is ready to be sent. 
     * <br>
     * If one or more {@link SelectionKey selection key} 
     * registered with the selector are ready for i/o operation,
     * then the attachment with the selected key carries the request or response
     * as the case may be. In this manner, separate request/response queue 
     * are not necessary to be maintained. 
     * The  selection key itself is attached
     * with the data to be operated, the operation i.e. read or write
     * and the socket to use to send or receive the data.
     *  
     */
    public void run() {
        while (!Thread.interrupted()) {
            try {
                step();
                synchronized (_selectorBug) {}
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }        
        
    private void step() throws Exception {    
        
        SelectionKey key = null;
        
        _selector.select();
        Iterator<SelectionKey> selectedKeys = _selector.selectedKeys().iterator();
        while (selectedKeys.hasNext()) {
            key = selectedKeys.next();
            selectedKeys.remove();

            if (!key.isValid()) {
                continue;
            }
            if (key.isAcceptable()) {
                acceptConnectionRequest(key);
            } else if (key.isReadable()) {
                readRequestFromChannelAndProcess(key);
            } else if (key.isWritable()) {
               writeResponseToChannel(key);
            }
         }
        
    }


    /**
     * Queues the given response to be sent to client.
     * Typically a request-processing thread will call this method when
     * response is ready. Queuing the response in this server instead of
     * request-processing thread  sending the response directly
     * to the remote client ensures that  <em>same</em> thread (i.e. the  
     * thread in which this server is running) performs network i/o i.e
     * reads the request and writes the response on the same channel.
     * Same thread reading and writing on the same channel avoids 
     * problem that arise from concurrent i/o operation on underlying socket. 
     * 
     * 
     * @param response the response to be sent to remote client.
     * Can not be null. If null, then the this method becomes a no-op.
     */
    

    
    /**
     * Process request coming over a channel. 
     * The incoming signal is, at first, read as raw bytes. This server
     * works at network level and can not interpret the bytes it reads.
     * The {@link Router router} interprets the request content via its
     * {@link Router#createRequest(SocketChannel, byte[]) and then
     * {@link Router#processRequest(Request) process} the resulting
     * request. 
     * 
     * @param key a readable channel selection key
     * 
     * @throws Exception two primary kinds of error can happen. Firstly,
     * reading the network socket can raise i/o exception. Also the content
     * can not be interpreted.
     * Secondly, the requested operation may fail.
     */
    void readRequestFromChannelAndProcess(SelectionKey key) throws Exception {
        Object attachement = key.attachment();
        if (RequestReceiver.class.isInstance(attachement)) {
            // someone is already handling request from this client
        } else { // new request
            _logger.debug("creating new request receiver for " + attachement);
            RequestReceiver receiver = new RequestReceiver();
            key.attach(receiver);
            receiver._channel = (ByteChannel)key.channel();
            receiver._request = _protocol.newRequest();
            
            Thread requestThread = new Thread(receiver);
            ChannelInfo info = new ChannelInfo(key.channel());
            requestThread.setName("request-" + info);
            requestThread.setDaemon(true);
            requestThread.start();
        }
    }
    
    
    
    public void processResponse(P response) {
        SocketChannel channel = (SocketChannel)response
                .getRequest().getChannel();
        
        _logger.debug("registering key to process response...");
        synchronized (_selectorBug) {
            try {
                _selector.wakeup();
                channel.register(_selector, SelectionKey.OP_WRITE, response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /** Returns the protocol.
     * 
     */
   public Protocol<R,P> getProtocol() {
       return _protocol;
   }

    
    /**
     * Sends response to the channel represented by the given key.
     * The response is attached to the key itself.
     * 
     * @param key
     */
    void writeResponseToChannel(SelectionKey key) {
        Response response = (Response) key.attachment();
        if (response == null) {
            _logger.warn("ignore sending null response");
        }
        SocketChannel socket = (SocketChannel)key.channel();
        _logger.debug("send response to " + new ChannelInfo(socket));
        try {
            response.send(socket);
            if (key.isValid() && key.channel().isOpen()) {
                key.channel().register(_selector, SelectionKey.OP_READ);
            }
        } catch (Exception ex) {
            closeChannel(key, true, ex);
        } finally {
            key.cancel();
        }
    }

    /**
     * closes the channel in case of error.
     * @param key the key on which an operation has failed
     * @param discard whether to close the channel
     * @param reason the exception 
     */
    void closeChannel(SelectionKey key, boolean discard, Exception ex) {
        closeChannel(key, discard, ex.getClass() + ":" + ex.getMessage());
        ex.printStackTrace();
    }
    
    void closeChannel(SelectionKey key, boolean discard, String reason) {
        _logger.warn(reason);
        if (discard) {
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    

    /**
     * Accepts a connection request from a remote client.
     * Registers with the selector to read data.
     * @param key
     * @throws IOException
     */
    private void acceptConnectionRequest(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        _logger.info("accepted connection from " + new ChannelInfo(channel));
        channel.register(_selector, SelectionKey.OP_READ);
    }

    /**
     * Creates and binds to a socket to accept remote connection.
     * @return
     * @throws IOException
     */
    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress isa = new InetSocketAddress(_port);
        serverChannel.socket().bind(isa);

        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        
        _logger.info("ready to accept connection request at " + getURL());

        return socketSelector;
    }
    

    
    String determineHostname() {
        String DEFAULT_ROUTE = "127.0.0.1";
        try {
            InetAddress[] addrs = InetAddress.getAllByName(
                InetAddress.getLocalHost().getHostName());
            for (int i = 0; i < addrs.length; i++) {
                if (!addrs[i].isLoopbackAddress() 
                        && !DEFAULT_ROUTE.equals(addrs[i].getHostName())) {
                  return addrs[i].getHostName();
            }
        }
        } catch (Exception ex) {
            return DEFAULT_ROUTE;
        }
        return DEFAULT_ROUTE;
    }


    /**
     * gets a URL to reach this server. The URL at this network level 
     * server specify <code>[protocol://]host:port</code> where protocol
     * is supplied by the {@link Router#getProtocol() router}, if it is
     * {@link #setRouter(Router) attached}.
     * 
     * @return a base URL for this network server. 
     */
    public String getURL() {
        return getProtocol().getName() + "://" 
             + getHostName() + ":" + getPort();
    }
    
    /**
     * Sets the execution context.
     * Typically this server is its own execution context, but there can
     * be scenarios where the routes execute in different context 
     * that provides additional features/services.
     * 
     * @param ctx an execution context. Must not be null.
     */
    public void setExecutionContext(ExecutionContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Execution context must " 
                    + " not be null");
        }
        _ctx = ctx;
    }
    
    /**
     * Sets an execution context. All routes registered with this server
     * will execute in this given execution context.
     * 
     * @return a context. Never null.
     */
    public ExecutionContext getExecutionContext() {
        return _ctx;
    }
    
    
    public Logger getLogger() {
        return _logger;
    }
    
    
    /**
     * A request receiver reads request from a network channel. The remote
     * client may take long time to write the request bytes n the channel
     * and hence it may take a long time for a request to be read from the
     * channel.
     * Hence, a thread is dedicated to read from the channel.
     * 
     * @author pinaki poddar
     *
     */
    class RequestReceiver implements Runnable {
        private R _request;
        private ByteChannel _channel;
        
        @Override
        public void run() {
            try {
                // this call will block until remote client completes a request
                _request.receive(_channel);
                _router.processRequest(_request);
            } catch (Exception ex) {
                P response = _protocol.newErrorResponse(_request, ex);
                processResponse(response);
            }
        }
    }
}