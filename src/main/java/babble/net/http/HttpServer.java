package babble.net.http;

import babble.net.NioServer;

public class HttpServer extends NioServer<HttpRequest,HttpResponse> {
    private static final int DEFAULT_PORT = 8090;
    
    /**
     * Runs a HTTP server that has no registered operation,
     * but simply starts a server on a port.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        HttpServer server = new HttpServer("HttpServer", port);
        server.setRouter(new HttpRouter(server));
        server.start();
    }
    
    public HttpServer(String serverName, int port) {
        super(serverName, port);
        setRouter(new HttpRouter(this));
    }
    
    public HttpServer() {
        this("HttpServer", DEFAULT_PORT);
    }

    @Override
    public String getURL() {
        return "http://" + getHostName() + ":" + getPort();
    }
}
