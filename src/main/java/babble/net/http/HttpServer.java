package babble.net.http;

import babble.net.NioServer;

public class HttpServer extends NioServer<HttpRequest,HttpResponse> {
    
    /**
     * Runs a HTTP server that has no registered operation,
     * but simply starts a server on a port.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("***Missing argument");
            System.err.println(HttpServer.class.getSimpleName() 
                    + " - runs a HTTP server on this host");
            System.err.println("Usage: " + HttpServer.class.getName() + " port");
            System.err.println("where");
            System.err.println("\tport listen port of HTTP server");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);

        HttpServer server = new HttpServer("HttpServer", port);
        server.addRoute(new DefaultGet());
        
        server.start();
    }
    
    public HttpServer(String serverName, int port) {
        super(serverName, port, new HttpProtocol());
    }
    
    public HttpServer(int port) {
        this("HttpServer", port);
    }

}
