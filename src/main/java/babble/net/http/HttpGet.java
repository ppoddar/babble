package babble.net.http;

import babble.net.ExecutionContext;
import babble.net.Request;
import babble.net.Response;

/**
 * A HTTP GET operation that does not do anything by default.
 * 
 * A concrete derivation must fill in the execution semantics in
 * {@link #execute(ExecutionContext, Request, Response) execute()} method.
 *  
 * @author pinaki poddar
 *
 */
public class HttpGet extends HttpOperation {

    public HttpGet() {
        super("GET");
    }

    public HttpGet(String pattern) {
        super("GET", pattern);
    }
    
    @Override
    public HttpResponse execute(ExecutionContext ctx, HttpRequest request, 
            HttpResponse response) throws Exception {
        throw new AbstractMethodError();
    }

}
