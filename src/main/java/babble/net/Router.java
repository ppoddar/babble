package babble.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import babble.net.exception.RoutingException;

/**
 * A router manages asynchronous request-response processing.
 * A router does not perform network i/o.
 * <br>
 * A router's basic duties are
 * <ul>
 * <li>find a matching route for a given request
 * <li>execute the route
 * <li>inform a server with a response when route has finished execution
 * </ul>
 * 
 * 
 * This abstract facility implements the mechanics to execute a route
 * and inform a server when response becomes available. 
 * The specifics of matching a route
 * to a request is left unspecified for concrete protocol implementation.
 * <br>
 * Each route executes in a separate request processing thread supplied
 * by this router. So if a request performs network i/o, the router thread
 * is not blocked. 
 * <br>
 * Though response to a request is asynchronous, a router does not block
 * on any network channel waiting for the response. Instead it waits on a
 * a {@link Future promise} that is fulfilled when a response 
 * becomes available.
 * <br>
 * Once the request is complete, the router {@link NioServer#processResponse(Response) 
 * informs} the owning server. The server is responsible for
 * sending the response back to originating client. 
 * Thus a router never performs network i/o. 
 * <br>
 *
 */
public abstract class Router<R extends Request, P extends Response> 
    implements Runnable, ThreadFactory {
    
    private final NioServer<R,P> _server;
    private final ExecutorCompletionService<P> _threadPool;
    private final Map<Future<P>, R> _runningTaks = 
            Collections.synchronizedMap(new HashMap<Future<P>, R>());
    private final AtomicInteger threadCounter = new AtomicInteger();
    private final Logger _logger;

    /**
     * Creates a router with a given protocol.
     * 
     * @param protocol name of a protocol
     * @param server the server that is responsible for network i/o
     */
     public Router(NioServer<R,P> server) {
        _server = server;
        _logger = _server.getLogger();
        ExecutorService threads = Executors.newCachedThreadPool(this);
        _threadPool = new ExecutorCompletionService<P>(threads);
    }
    
    /**
     * Runs continually until interrupted to wait for completed responses
     * of the request it has spawned. Once a response is ready, this router
     * {@link NioServer#processResponse(Response) informs} the server.
     */
    public void run() {
        R request  = null;
        P response = null;
        Future<P> promise = null;
        while (!Thread.interrupted()) {
            try {
                promise  = _threadPool.take();
                synchronized (_runningTaks) {
                    request  = _runningTaks.get(promise);
                }
                response = promise.get();
                 _logger.debug("finished response for request " + request);
                 
                 if (response == null || request == null || request.isOneWay()) {
                     response = _server.getProtocol().newResponse(request);
                 } 
                 _server.processResponse(response);
                 
            } catch (InterruptedException ex) {
                break;
            } catch (Exception ex) {  // request processing had failed and
                ex.printStackTrace(); // raised exception on a different thread
                synchronized (_runningTaks) { // that exception is thrown when
                    request = _runningTaks.get(promise); // by promise.get()
                    P errorResponse = _server.getProtocol()
                            .newErrorResponse(request, ex);
                    _server.processResponse(errorResponse);
                }
            } finally {
                _runningTaks.remove(promise);
            }
        }
    }
    
    /**
     * Processes a request.
     * <br>
     * Matches the request to one of the route
     * and executes the route in a separate thread.
     * <br>
     * If the request does not match any route, informs the server with
     * an {@link Protocol#newErrorResponse(Request, Exception) error response}
     * instead of throwing an exception. 
     * 
     * @param request a request to be processed. never null
     * 
     */
    protected void processRequest(R request)  {
        synchronized (_runningTaks) {
            try {
                final Route<R,P> route = findMatchingRoute(request);
                _logger.debug("found route " + route + " for request " + request);
                _logger.debug("executing route " + route + " for request " + request);
                Callable<P> call = new Callable<P>() {
                    @Override
                    public P call() throws Exception {
                        return route.execute(_server.getExecutionContext(), 
                                request);
                    }
                };
                // associates request to promise of a response
                // this allows to find the original request when promise is fulfilled
                _runningTaks.put(_threadPool.submit(call), request);
            } catch (Exception ex) {
                _server.processResponse(_server.getProtocol().newErrorResponse(request, ex));
            }
        }
    }

    /**
     * Crates a daemon thread to process a request
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(_server.getName() 
                + "-request-" + threadCounter.incrementAndGet());
        return t;
    }
    
    // -------------------------------------------------------------------
    // Methods to be implemented by protocol-specific routers
    // -------------------------------------------------------------------
    
    /**
     * Affirms if this router has any registered route.
     */
    public abstract boolean hasRoute();
    
    /**
     * Adds given route to this router.
     * @param route a route to be added
     */
    public abstract void addRoute(Route<R,P> route);

    /**
     * Matches the given request to one of the {@link #addRoute(Route)
     * registered} route.
     * 
     * @param request a request to be matched with a route
     * @return the matched route. 
     * @exception RoutingException if no route matches
     */
    public abstract Route<R,P> findMatchingRoute(R request) throws RoutingException;
    
}
