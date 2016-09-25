package babble.net;

/**
 * An execution context provides environment variables to a {@link Route
 * route}.
 * 
 * A {@linkplain Router} {@link Route#execute(ExecutionContext, Request) 
 * invokes} a route in an execution context.
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
