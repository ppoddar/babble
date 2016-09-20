package babble.net;

import java.util.concurrent.CompletableFuture.AsynchronousCompletionTask;

/**
 * A route is an operation that accepts a {@link Request request} and
 * generate a {@link Response response}.
 * 
 * @author pinaki poddar
 *
 */
public interface Route<R extends Request, P extends Response<R>>  
 extends AsynchronousCompletionTask {
    
    /**
     * Affirms if this route matches the given request.
     * 
     * 
     * @return true if the request matches this route
     */
    boolean matches(R request);
    
    /**
     * Execute the given request in the given execution context to
     * populate the given response.
     * 
     * @param ctx an execution context. Never null.
     * @param request a request to be executed. Never null.
     * @param response a response to be populated. Never null.
     * 
     * @return response  possibly same as given response, but populated
     * by execution of this method
     * 
     * @throws Exception
     */
    
    P execute(ExecutionContext ctx, R request, P response) 
            throws Exception;
    
}