package babble.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.exception.ProtocolException;
import babble.util.ChannelInfo;
import babble.util.Timeout;


/**
 * A client to a server to receive response asynchronously. 
 * 
 * The caller can {@link #sendRequest(Request, ResponseCallback) 
 * send} a request to server supplying a callback. 
 * The callback will be invoked when server completes the
 * request and sends an asynchronous response.
 * <br>
 * A client that receives asynchronous response must wait till server responds.
 * However, a {@link #sendRequest(Request, ResponseCallback) request to server}
 * returns to caller immediately with a void.  
 * This aspect is different than synchronous client that waits for server
 * response before returning to the caller.   
 * <br>
 * Hence a client continues running on a 'main' thread to receive response
 * from server over a network channel. Once a response is received, the
 * client invokes the callback function registered
 * when the request was sent with the asynchronous response received. 
 * 
 * @author pinaki poddar
 *
 * @param <R>
 */
public abstract class NioClient<R extends Request> implements Runnable {
    private InetAddress _hostAddress;
    private int _port;

    private Selector _selector;
    private Object _selectorBug = new Object();
    private SocketChannel _socketChannel;
    private ByteBuffer _readBuffer = ByteBuffer.allocate(8 * 1024);;

    private final BlockingDeque<Boolean> _connected = 
            new LinkedBlockingDeque<Boolean>();

    private Logger _logger;

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
     * @throws IOException
     */
    public NioClient(InetAddress host, int port) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", ""+true);
        String target = host.getHostName() + ':' + port;
        String name = "client->" + target;
        _logger =  LoggerFactory.getLogger(name);
        _hostAddress = host;
        _port = port;

        Thread mainThread = new Thread(this, name);
        mainThread.setDaemon(false); // otehrwise program will exit
        mainThread.start();

        Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
        _logger.info("waiting to connect in " + timeout);
        try {
            if (_connected.pollFirst(timeout.value(), timeout.unit()) == null) {
                throw new IOException("Can not connect to " + target + " in " + timeout);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    public Logger getLogger() {
        return _logger;
    }

    /**
     * Create a client supplying the address and port of the server.
     * 
     * @param host remote host name to connect
     * @param port port where host listens for request
     * @throws IOException
     */
    public NioClient(String host, int port) throws IOException {
        this(InetAddress.getByName(host), port);
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
            _socketChannel = initiateConnection();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (!Thread.interrupted()) {
            try {
                synchronized (_selectorBug) {
                    step();
                    _selectorBug.notifyAll();
                    Thread.yield();
                }
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

    private SocketChannel initiateConnection() throws IOException {
        _logger.info("Contacting " + _hostAddress.getHostName()+':'+_port);
        
        _selector = SelectorProvider.provider().openSelector();
        _socketChannel = SocketChannel.open();
        _socketChannel.configureBlocking(false);
        InetSocketAddress addr = new InetSocketAddress(_hostAddress, _port);
        
        boolean sucess = _socketChannel.connect(addr);
        if (sucess) {
            _logger.info("Connected to " + _hostAddress.getHostName()+':'+_port);
            _connected.offerFirst(sucess);
        }

        _socketChannel.register(_selector, SelectionKey.OP_CONNECT);

        _selector.wakeup();

        return _socketChannel;

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
                    ex.printStackTrace();
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
    public void sendRequest(R request, ResponseCallback cb) throws IOException {
            
        request.setResponseCallback(cb);
        
        
        synchronized (_selectorBug) {
            try {
                _selectorBug.wait();
                _socketChannel.register(_selector, SelectionKey.OP_WRITE, request);
                _selector.wakeup();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }  
        }
    }
    
    /**
     * writes a request on the network channel corresponding to the given key. 
     * The request to be written is attached to the given key. 
     * 
     * @param key a selection key for a network channel
     */
    void writeRequestToChannel(SelectionKey key) {
        try {
            @SuppressWarnings("unchecked")
            R request = (R) key.attachment();
            _logger.debug("send  request to server " + request);
            request.send((SocketChannel)key.channel());

            key.interestOps(SelectionKey.OP_READ);
        } catch (Exception ex) {
            closeChannel(key, true, ex.getMessage());
        }
    }

    /**
     * Gets the name of the remote server this client connects to. 
     */
    public String getHost() {
        try {
            return ((InetSocketAddress)getChannel()
                    .getRemoteAddress())
                    .getHostName();
        } catch (IOException e) {
        }
        return "";
    }
    


    /**
     * Reads raw bytes from the channel corresponding to given key.
     * The response object is attached with the given key. If the
     * response is associated with a callback, then callback is invoked
     * with raw bytes read from the network channel.
     */
    protected void readResponseFromChannel(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        @SuppressWarnings("unchecked")
        R response = (R) key.attachment();
        ResponseCallback cb = response.getResponseCallback();
        try {
            _readBuffer.clear();
            int n = channel.read(_readBuffer);
            _logger.debug("read " + n + " bytes response from " + channel.getRemoteAddress());
            key.interestOps(SelectionKey.OP_CONNECT);
            
            if (n == -1) {
                return;
            }
            byte[] bytes = new byte[n];
            _readBuffer.position(0);
            _readBuffer.get(bytes, 0, n);
            if (cb != null) cb.onResponse(bytes);
            
        } catch (Exception ex) {
            if (cb != null) cb.onError(ex);
        }

    }
    
    /**
     * Gets the port of the remote server this client connects to. 
     */
    public int getPort() {
        try {
            return ((InetSocketAddress)getChannel().getRemoteAddress()).getPort();
        } catch (IOException e) {
        }
        return 0;
    }

    /**
     * gets the channel that connects to the remote server.
     * 
     */
    protected final SocketChannel getChannel() {
        return _socketChannel;
    }


    /**
     * affirms if connected to a remote server.
     */
    public boolean isConnected() {
        return _socketChannel.isConnected();
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