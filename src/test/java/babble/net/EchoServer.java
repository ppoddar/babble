package babble.net;


import babble.net.http.HttpRequest;
import babble.net.http.HttpResponse;
import babble.net.http.HttpRoute;
import babble.net.http.HttpServer;
import babble.net.http.SinglePartHttpResponse;

/**
 * A simple HTTP server that responds to all GET request with the URI as response.
 * 
 * @author pinaki poddar
 *
 */
public class EchoServer {
    /**
     * Starts a server.
     * @param args port [server-name]
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String name = args.length > 1 ? args[1] : EchoServer.class.getSimpleName()
                + '-' + port;
        HttpServer server = new HttpServer(name, port);
        server.addRoute(new EchoRoute());
        server.start();
    }
    
    static class EchoRoute extends HttpRoute {
        /**
         * A route to recognize GET request and any path.
         */
        protected EchoRoute() {
            super("GET", ".*");
        }

        /*
         * Responds with the request URI.
         * 
         * @see babble.net.Route#execute(babble.net.ExecutionContext, babble.net.Request, babble.net.Response)
         */
        @Override
        public HttpResponse execute(ExecutionContext ctx, HttpRequest request) throws Exception {
            HttpResponse response = new SinglePartHttpResponse(request);
            response.appendBody(""+request.getURI());
            return response;
        }
    }

}
