package babble.net;

/**
 * An execution context provides environment variables to a {@link Route
 * route}.
 * 
 * The framework supplies an execution context to a route before {@link 
 * Route#execute(ExecutionContext, Request, Response) invoking} it.
 * 
 * @author pinaki poddar
 *
 */
public interface ExecutionContext {
    /**
     * A name to describe this context.
     */
    String getName();

}
