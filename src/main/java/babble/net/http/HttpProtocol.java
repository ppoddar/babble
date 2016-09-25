package babble.net.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import babble.net.NioServer;
import babble.net.Protocol;
import babble.net.Router;

public class HttpProtocol implements Protocol<HttpRequest, HttpResponse>{

    @Override
    public String getName() {
        return "http";
    }

    @Override
    public Router<HttpRequest, HttpResponse> newRouter(NioServer<HttpRequest, HttpResponse> server) {
        return new HttpRouter(server);
    }
    @Override
    public HttpRequest newRequest() {
        return new HttpRequest();
    }

    @Override
    public HttpResponse newResponse(HttpRequest request) {
        return new SinglePartHttpResponse(request);
    }
    
    @Override
    public HttpResponse newErrorResponse(HttpRequest request, Exception ex) {
        HttpResponse response = new SinglePartHttpResponse(request);
        response.addHeader("Content-Type", "text/plain");
        StringWriter stackTrace = new StringWriter();
        
        ex.printStackTrace(new PrintWriter(stackTrace));
        try {
            response.appendBody(ex.getClass().getName()+
                    ":"+ ex.getMessage()+ "\r\n");
            response.appendBody("Server side stack trace:\r\n");
            response.appendBody(stackTrace.toString());
        } catch (IOException ex2) {
            
        }
        return response;
    }



}
