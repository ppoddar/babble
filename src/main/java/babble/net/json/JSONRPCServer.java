package babble.net.json;

import babble.net.NioServer;

/**
 * A server using TCP/IP as transport and JSON-RPC as application protocol.
 * The transport protocol is provided by the {@link NioServer base class}.
 * <br>
 * The <a href="http://json-rpc.org/wiki/specification">JSON-RPC protocol</a> 
 * is provided by a combination of {@link JSONRPCRouter}, {@link JSONRequest}
 * and {@link JSONResponse}.
 * <br>
 * This server is intended to be embedded, though it can also be run as
 * a standalone application. To embed in a larger application,
 * <pre>
 *  public class MyApplication {
 *  
 *    public static void main(String[] args) {
 *       JSONServer server = new JSONServer("my JSON-RPC server", 8080);
 *       
 *       server.addRoute(new JSONRoute());
 *       
 *       server.start();
 *    }
 *  }
 * </pre>
 * 
 * 
 * @author pinaki poddar
 *
 */
public class JSONRPCServer extends NioServer<JSONRequest,JSONResponse> {
   
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("***Missing argument");
            System.err.println(JSONRPCServer.class.getSimpleName() 
                    + " - runs a JSON-RPC server on this host");
            System.err.println("Usage: " + JSONRPCServer.class.getName() + " port");
            System.err.println("where");
            System.err.println("\tport listen port of JSON-RPC server");
            System.exit(1);
        }
        
        
        int port = Integer.parseInt(args[0]);
        
        JSONRPCServer server = new JSONRPCServer("babble", port);
        
        server.addRoute(new EchoRoute());

        server.start();
    }
    /**
     * Creates a 
     * @param serverName
     * @param port
     */
    public JSONRPCServer(String serverName, int port) {
        super(serverName, port, new JSONRPCProtocol());
        
    }

}
