package babble.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
//import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.util.ChannelInfo;


/**
 * A client to a server. The client receives server response asynchronously. 
 * <br>
 * The caller {@link #sendRequest(Request, ResponseCallback) 
 * sends} a request to server supplying a {@link ResponseCallback callback}. 
 * The callback is {@link ResponseCallback#onResponse(byte[]) invoked} 
 * whenever server completes the request and sends a response.
 * <br>
 * A <em>synchronous</em> client waits on a network channel until server 
 * responds. Hence, during that waiting period, the network channel is
 * <em>blocked>/em>, no other service can use it.
 * <br>
 * An <em>asynchronous</em> client, such as this one, does not block the
 * network channel. Instead, the client immediately returns (with a void)
 * when user sends a {@link #sendRequest(Request, ResponseCallback) sends}
 * a request to server.
 * 
 * <br>
 * A client runs a <em>i/o thread</em>  that sends requests and receives 
 * response from server.
 * This 'main' thread is different than the thread on which user has called
 * this client. The <em>i/o thread</em> runs continually until it is
 * interrupted externally by a user.
 * 
 * <br>
 * The ' 
 * 
 * @author pinaki poddar
 *
 * @param <R>
 */
public abstract class NioClient<R extends Request, P extends Response> implements Runnable {
    private InetAddress _hostAddress;
    private int _port;

    private Selector _selector;
    private Protocol<R,P> _protocol;
    private final Object _selectorBug = new Object();
    private Channel _channel;

    private final BlockingDeque<Boolean> _connected = 
            new LinkedBlockingDeque<Boolean>();

    private Logger _logger;
    
    /**
     * Create a client supplying the address and port of the server.
     * The client will wait for 1 second to connect to server before giving up.
     * 
     * @param host remote host name to connect
     * @param port port where host listens for request
     * @param daemon if true runs the i/o thread as a daemon thread.
     * If the client runs as a main program, then the i/o thread
     * should be a non-daemon thread. Otherwise, the client will not
     * be alive to receive an asynchronous response after the main
     * program has exited.
     * @throws IOException
     */
    public NioClient(String host, int port, boolean daemon) throws IOException {
        this(InetAddress.getByName(host), port, daemon, 1, TimeUnit.SECONDS);
    }

    
    /**
     * Create a client supplying the address and port of the server.
     * The client will run a daemon thread for network i/o and will wait
     * for 1 second to connect to server before giving up.
     * 
     * @param host remote host name to connect
     * @param port port where host listens for request
     * @param daemon if true runs the i/o thread as a daemon thread.
     * If the client runs as a main program, then the i/o thread
     * should be a non-daemon thread. Otherwise, the client will not
     * be alive to receive an asynchronous response after the main
     * program has exited.
     * @throws IOException
     */
    public NioClient(String host, int port) throws IOException {
        this(InetAddress.getByName(host), port, true, 1, TimeUnit.SECONDS);
    }


    /**
     * Create a client supplying the address and port of the server.
     * <br>
     * <b>Note</b>: This client starts a thread on its own. The thread will
     * run until interrupted to listen for server response.
     * The thread is interrupted if the server can not be connected in
     * 2 seconds. 
     * 
     * @param host remote host address to connect
     * @param port port where host listens for request
     * @param daemon if true runs the i/o thread as a daemon thread.
     * If the client runs as a main program, then the i/o thread
     * should be a non-daemon thread. Otherwise, the client will not
     * be alive to receive an asynchronous response after the main
     * program has exited.
     * @param timeout time limit before client gives up attempt to connect
     * to server.
     * @param unit unit of time to express the timeout value.
     * 
     * @throws IOException if server can not be contacted within given 
     * timeout period.
     */
    public NioClient(InetAddress host, int port, boolean daemon,
            int timeout, TimeUnit unit) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", ""+true);
        String target = host.getHostName() + ':' + port;
        String name = "client->" + target;
        _logger =  LoggerFactory.getLogger(name);
        _hostAddress = host;
        _port = port;

        if (!daemon) _logger.info(name + " is running non-daemon i/o thread");
        Thread ioThread = new Thread(this, name);
        ioThread.setDaemon(daemon); // otherwise program will exit
        ioThread.start();

        _logger.info("waiting to connect in " + timeout);
        try {
            if (_connected.pollFirst(timeout, unit) == null) {
                ioThread.interrupt();
                throw new IOException("Can not connect to " + target 
                        + " in " + timeout + " " + unit);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    public Logger getLogger() {
        return _logger;
    }



    /**
     * Runs continually until interrupted.
     * 
     * Sends request to server and listens for response from server.
     * <br>
     * Unlike synchronous client, this client has a lifetime that extends
     * that of the request because it has to listen for response that 
     * arrives asynchronous to the request.
     */
    public void run() {
        _logger.debug("starting network event loop...");
        try {
            _channel = initiateConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (!Thread.interrupted()) {
            try {
                step();
                synchronized (_selectorBug) { /*do nothing*/ }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * run once to check i/o status of the network channel.
     * If any network key is ready for i/o, then {@link Request#send(SocketChannel) 
     * sends request}, {@link #readResponseFromChannel(SelectionKey) reads}
     * response or connects.
     * 
     * @throws IOExcepion for i/o errors
     * 
     */
    private void step() throws IOException {
        _selector.select();
        Iterator<SelectionKey> selectedKeys = _selector.selectedKeys().iterator();
        if (!selectedKeys.hasNext()) return;
        // Iterate over the set of keys for which events are available
        while (selectedKeys.hasNext()) {
            SelectionKey key = selectedKeys.next();
            selectedKeys.remove();

            if (!key.isValid()) {
                continue;
            }

            if (key.isReadable()) {
                readResponseFromChannel(key);
            } else if (key.isWritable()) {
                writeRequestToChannel(key);
            } else if (key.isConnectable()) {
                connect(key);
            }
        }
    }

    private Channel initiateConnection() throws IOException {
        _logger.info("Contacting " + _hostAddress.getHostName()+':'+_port);
        
        _selector = SelectorProvider.provider().openSelector();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        InetSocketAddress addr = new InetSocketAddress(_hostAddress, _port);
        
        boolean sucess = socketChannel.connect(addr);
        if (sucess) {
            _logger.info("Connected to " + _hostAddress.getHostName()+':'+_port);
            _connected.offerFirst(sucess);
        }

        socketChannel.register(_selector, SelectionKey.OP_CONNECT);

        _selector.wakeup();

        return socketChannel;

    }

    /**
     * Attempts to finish the connection if not already connected.
     * Does not make repeated attempt (spinning on a non-blocking socket 
     * is useless), or throws exception (it is invoked on an internal thread).
     * If a connection attempt succeeds, then marks a semaphor success,
     * otherwise a separate mechanics will time out on the same semaphor.
     * @param key
     * @throws IOException
     */
    private void connect(SelectionKey key) {
        SocketChannel socket = (SocketChannel) key.channel();
        if (!isConnected()) { 
            try {
                if (socket.finishConnect()) {
                    _logger.info("connected to " + new ChannelInfo(key.channel()));
                    _connected.offerFirst(true);
                }
            } catch (Exception ex) {
            } finally {
                _selector.wakeup();
            }
        }
    }

    /**
     * Schedules given request to be sent to server. The given callback 
     * will be {@link ResponseCallback#onResponse(byte[]) invoked} 
     * whenever the response is received.
     * <br>
     * This call does not block the network channel nor does it poll
     * for a response.
     * 
     * @param request request to be processed.
     * 
     * @param cb callback to be invoked when response is completed. 
     * If null, then no callback response would be invoked.
     * 
     *            
     * @throws IOException i/o errors
     */
    public void sendRequest(Request request, ResponseCallback cb) throws IOException {
        _waitingRequests.put(request, cb);
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6446653
        synchronized (_selectorBug) {
                _selector.wakeup();
                ((SocketChannel)_channel)
                        .register(_selector, SelectionKey.OP_WRITE, request);
                
            
        }
    }
    
    /**
     * Request whose response is yet to arrive
     */
    Map<Request, ResponseCallback> _waitingRequests = 
            new HashMap<Request, ResponseCallback>();
    
    
    /**
     * writes a request on the network channel corresponding to the given key. 
     * The request to be written is attached to the given key. 
     * 
     * @param key a selection key for a network channel
     */
    void writeRequestToChannel(SelectionKey key) {
        try {
            Request request = (Request) key.attachment();
            _logger.debug("send  request to server " + request);
            request.send((SocketChannel)key.channel());

            key.interestOps(SelectionKey.OP_READ);
        } catch (Exception ex) {
            closeChannel(key, true, ex.getMessage());
        }
    }



    /**
     * Reads raw bytes from the channel corresponding to given key.
     * The request object is attached with the given key. 
     * Uses the protocol to create an empty response.
     * Then protocol-specific response reads from the channel.
     * If the request were associated with a callback, then response
     * will invoke callback with the data it reads from network channel.
     */
    protected void readResponseFromChannel(SelectionKey key) {
        @SuppressWarnings("unchecked")
        R request = (R) key.attachment();
        P response = getProtocol().newResponse(request);
        ResponseCallback cb = getResponseCallback(request);
        try {
            response.receive((ByteChannel)key.channel(), cb);
        } catch (Exception ex) {
            if (cb != null) cb.onError(ex);
        } finally {
            key.interestOps(SelectionKey.OP_CONNECT);
            _waitingRequests.remove(request);
        }
    }
    /** Returns the protocol.
     * 
     */
   public Protocol<R,P> getProtocol() {
       return _protocol;
   }

    
    private ResponseCallback getResponseCallback(Request request) {
        return _waitingRequests.get(request);
    }
    
    
    /**
     * gets the channel that connects to the remote server.
     * 
     */
    protected final Channel getChannel() {
        return _channel;
    }


    /**
     * affirms if connected to a remote server.
     */
    public boolean isConnected() {
        return _channel.isOpen();
    }
    
    /**
     * cancel the key and optionally closes the network channel 
     * corresponding to the given key.
     * 
     * @param key selection key for a network channel to cancel
     * @param close if true the corresponding channel is closed
     * @param reason an explanation of why the channel is closed
     */
    private void closeChannel(SelectionKey key, boolean close, String reason) {
        try {
            _logger.warn("WARN: " + reason);
            key.cancel();
            if (close) {
                key.channel().close();
            }
        } catch (Exception ex) {

        }
    }
    
    
    
    static InetAddress determineHostAddress(String host) 
            throws UnknownHostException {
        String DEFAULT_ROUTE = "127.0.0.1";
        if ("localhost".equals(host) || DEFAULT_ROUTE.equals(host)) {
            try {
                InetAddress[] addrs = InetAddress.getAllByName(
                        InetAddress.getLocalHost().getHostName());
                for (int i = 0; i < addrs.length; i++) {
                    if (!addrs[i].isLoopbackAddress()) {
                        return addrs[i];
                    }
                }
            } catch (Exception ex) {
                return InetAddress.getLocalHost();
            }
        }
        return InetAddress.getByName(host);
    }

}