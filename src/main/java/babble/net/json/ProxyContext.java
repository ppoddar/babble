package babble.net.json;

import babble.net.ExecutionContext;

/**
 * An execution context for a proxy.
 * The context is responsible for the instance to be proxied.
 * 
 * The requests are invoked on the proxied instance.
 * 
 * @author pinaki poddar
 *
 */
public interface ProxyContext extends ExecutionContext {
    /**
     * Gets the proxied instance. 
     * 
     * @return the proxied instance. Can be null only when {@link 
     * ProxyContext#isInitialized()} resturns false.
     */
    public Object getProxiedInstance();
    
    /**
     * Affirms if this context has been initialized.
     * 
     */
    public boolean isInitialized();
}
