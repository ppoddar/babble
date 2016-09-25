package babble.net.json;

import static babble.net.json.JSONRPC.ERROR_CODE_INTERNAL_ERROR;
import static babble.net.json.JSONRPC.ERROR_CODE_INVALID_REQUEST;

import java.util.List;

import babble.net.ExecutionContext;
import babble.net.Route;
import babble.util.MethodSignature;

/**
 * A route to be executed. The input arguments for execution is supplied
 * by a {@link JSONRequest}, the method to invoke on the proxied instance
 * is supplied by the the {@link JSONRequest#getMethod()} and result or 
 * error after execution is written to a {@link JSONRequest}.
 *  
 * 
 * @author pinaki poddar
 *
 */
public class JSONRoute implements Route<JSONRequest, JSONResponse> {
    private String _methodSignature;
    
    /**
     * for extensions
     */
    protected JSONRoute() {
        
    }
    /**
     * A route that would match if the method signature matches
     *  
     * @param methodSignature must not be be null
     */
    public JSONRoute(String methodSignature) {
        _methodSignature = methodSignature;
    }

    public String getMethodSignature() {
        return _methodSignature;
    }


    @Override
    public boolean matches(JSONRequest request) {
        return _methodSignature.equals(request.getMethod());
    }

    /**
     * Invoke method on a proxied instance. The method on the proxied instance
     * and the input argument(s) are supplied by the request. The result
     * of the method execution is written on the response encoded in JSON.
     * The proxied instance is supplied by execution context.
     */
    @Override
    public JSONResponse execute(ExecutionContext ctx, JSONRequest request) throws Exception {
        ProxyContext proxyContext = ProxyContext.class.cast(ctx);
        JSONResponse response = new JSONResponse(request);
        if (!proxyContext.isInitialized()) {
            response.fail(ERROR_CODE_INVALID_REQUEST, 
                    new IllegalStateException("Proxy is not yet connected"
                          + " to database. Call connect-database first"));
        }
        
        Object proxiedInstance = proxyContext.getProxiedInstance();
        MethodSignature signature = new MethodSignature(
                proxiedInstance.getClass(),
                request.getMethod());
        
        List<String> argumentNames = signature.getArgumentNames();
        Object[] args = new Object[argumentNames.size()];
        for (int i = 0; i < argumentNames.size(); i++) {
            Object value = request.getParameterValue(argumentNames.get(i));
            args[i++] = value;
        }
        
        try {
            Object result = signature.invoke(proxiedInstance, args);
            response.result(result);
        } catch (Exception ex) {
            response.fail(ERROR_CODE_INTERNAL_ERROR, ex);
        }
        return response;
    }
    
    
    public String toString() {
        return _methodSignature;
    }

}
