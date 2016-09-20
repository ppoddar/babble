package babble.net;

import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import babble.net.exception.ProtocolException;
import babble.net.exception.RoutingException;

/**
 * A router introduces a communication protocol over raw network bytes.
 * 
 * Each protocol (e.g. HTTP or JSON-RPC) provides Request-Response
 * types specific to syntax and semantics of a protocol .
 * <br>
 * This abstract facility implements the mechanics to process a request.
 * Each request executes in a separate request processing thread supplied
 * by this router.
 * <br>
 * The response to a request is asynchronous. This router does not block
 * any network channel waiting for the response. Instead it waits on a
 * {@link Future promise} that is fulfilled when a response becomes
 * available.
 * <br>
 * Once the request is complete, the router {@link NioServer#processResponse(Response) 
 * posts} the response back to its owning server which is responsible for
 * sending the response back to originating client. Thus the server
 * is responsible for reading the request and writing the response on
 * a network channel. Router does not perform any network i/o.
 * <br>
 * A router maintains a set of {@link Route routes}. An incoming
 * request is {@link #findMatchingRoute(Request) matched} to a route and
 * then the matched route is executed on a request-processing thread.
 * 
 *
 */
public abstract class Router<R extends Request, P extends Response<R>> 
    implements Runnable, ThreadFactory {
    
    private final NioServer<R,P> _server;
    private final String _protocol;
    
    private final ExecutorCompletionService<P> _threadPool;
    
    private AtomicInteger threadCounter = new AtomicInteger();
    private final Logger _logger;

    /**
     * Creates a router with a given protocol.
     * 
     * @param protocol name of a protocol
     * @param server the server that is responsible for network i/o
     */
     public Router(String protocol, NioServer<R,P> server) {
        _server = server;
        _protocol = protocol;
        _logger = _server.getLogger();
        _threadPool = new ExecutorCompletionService<P>(
                Executors.newCachedThreadPool(this));
        
    }
    
     /** Returns the protocol name e.g. 'http'.
      * 
      */
    public String getProtocol() {
        return _protocol;
    }

    /**
     * Processes an incoming request arriving in a network channel.
     * The request arrives as raw byte data. A protocol-aware router 
     * converts the raw data into a protocol-specific Request.
     * <br>
     * This method matches the request to one of the route
     * and executes the route in a separate thread.
     * 
     * 
     * @param channel the network channel of incoming request
     * @param data the raw bytes to construct a request
     * 
     * Error Handling:
     * The raw data may not be protocol-compliant.
     * The request may not match any route
     * The processing may fail.
     * 
     * This facility handles all kinds of exception by generating a 
     * error as a response.
     * 
     */
    protected void processRequest(SocketChannel channel, byte[] data)  {
        R request = null;
        ExecutionContext ctx = _server.getExecutionContext();
        try {
            request = createRequest(channel, data);
       
            final Route<R,P> route = findMatchingRoute(request);
            _logger.debug("found route " + route + " for request " + request);
            
            executeRoute(route, ctx, request);
        } catch (ProtocolException ex) {
            _server.processResponse(getErrorResponse(request, ex));
        } catch (RoutingException ex) {
            _server.processResponse(getErrorResponse(request, ex));
        }
        
    }
    
    protected void executeRoute(final Route<R,P> route, 
            final ExecutionContext ctx,
            final R request) {
        Callable<P> call = new Callable<P>() {
            @Override
            public P call() throws Exception {
                return route.execute(_server.getExecutionContext(), 
                        request, newResponse(request));
            }
        };
        _threadPool.submit(call);
    }
    
    /**
     * Runs continually until interrupted to wait for completed responses
     * of the request it has spawned. Once a response is ready, this router
     * {@link NioServer#processResponse(Response) informs} the main server via .
     */
    public void run() {
        P response = null;
        while (!Thread.interrupted()) {
            try {
                Future<P> result = _threadPool.take();
                _logger.debug("finished response " + result);
                if (result != null) {
                     response = result.get();
                     if (response != null 
                     && !response.getRequest().isOneWay()) {
                         _server.processResponse(response);
                     }
                }
            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {
                R request = response == null ? null : (R)response.getRequest();
                _server.processResponse(getErrorResponse(request, ex));
            }
        }
    }
    
    /**
     * Affirms if this router has any registered route.
     */
    public abstract boolean hasRoute();
    
    /**
     * Adds given route to this router.
     * @param op a route to be added
     */
    public abstract void addRoute(Route<R,P> op);

    /**
     * Creates a concrete request from given data. The request is bound to
     * given channel. The response the given request is sent to the same
     * channel.
     * <br>
     * A request specifies a grammar or format. A HTTP request, for example,
     * will start with a status line, followed by headers etc. This method
     * is responsible to create such a request from given raw byte data.
     * 
     * @param channel a socket channel where response of the given request
     * would be sent.
     * @param data the raw byte data of the request. Often 
     * {@link NioServer#readRequestFromChannel(java.nio.channels.SelectionKey) read}
     * from a socket channel.
     * 
     * @return a concrete request.
     * 
     * @throws IOException
     */
    
    public abstract R createRequest(SocketChannel channel, byte[] data) 
            throws ProtocolException;
    
    public abstract P newResponse(R request);


    /**
     * Matches the given request to one of the {@link #addRoute(Route)
     * registered} route.
     * 
     * @param request a request to be matched with a route
     * @return the matched route. null if no route matches
     */
    public abstract Route<R,P> findMatchingRoute(R request)
        throws RoutingException;
    
    
    /**
     * Error response is generated when request processing
     * fails. Error response is like normal response except it may
     * produce a protocol-specific error details (e.g. stack trace or
     * HTTP status code)
     * 
     * The error response is written on the same network channel.
     *  
     * @param request the request that caused the exception
     * @param ex the exception raised
     * @return an error response 
     */
    public abstract P getErrorResponse(R request, Exception ex);
    
    
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(_server.getName() 
                + "-request-" + threadCounter.incrementAndGet());
        return t;
    }

    
}
