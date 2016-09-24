package babble.net;

import babble.net.http.HttpRequest;
import babble.net.json.JSONRequest;

/**
 * A communication protocol with asynchronous {@link Request} and {@link Response}.
 * A protocol creates concrete types of {@link Request} and {@link Response}
 * that can read and write byte data according to protocol grammar such as
 * {@link HttpRequest HTTP} or {@link JSONRequest JSON-RPC}.
 * 
 * @author pinaki poddar
 *
 * @param <R> type of Request accepted in this protocol
 * @param <P> type of Response generated in this protocol
 */
public interface Protocol<R extends Request, P extends Response> {
    /**
     * Gets a name for this protocol.
     * Protocol name appears in {@link NioServer#getURL() url} of server
     * using this protocol.
     * 
     * @return a non-null protocol name e.g. "http".
     */
    public String getName();
    
    /**
     * A protocol uses a router to match request to a route.
     * 
     * @param server the server that the router would inform when
     * asynchronous response becomes available.
     * 
     * @return a router
     */
    Router<R,P> newRouter(NioServer<R,P> server);
    
    /**
     * Creates  an instance of a protocol-specific request object type.
     * The request knows how to parse and validate raw byte data from a 
     * network channel.
     * 
     * @return a new request
     */
     R newRequest();
    
    /**
     * Creates an instance of a protocol-specific response object from
     * given request.
     * The response knows how to send/format data over a network channel.
     * 
     * @param request the request that originates a response. Never null.
     * 
     * @return response a new response. 
     */
    P newResponse(R request);
    
    /**
     * Error response is generated when request processing
     * fails. Error response is like normal response except it may
     * produce a protocol-specific error details (e.g. stack trace or
     * HTTP status code)
     * 
     * The error response is written on the same network channel.
     *  
     * @param request the request that caused the exception. Can be null.
     * @param ex the exception raised while processing or reading the request
     * from channel.
     * @return an error response 
     */
    P newErrorResponse(R request, Exception ex);


}
