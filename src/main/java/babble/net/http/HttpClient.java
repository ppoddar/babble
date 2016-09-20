package babble.net.http;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import babble.net.NioClient;

/**
 * A HTTP client that receives asynchronous response from a server.
 * 
 * @author pinaki poddar
 *
 */
public class HttpClient extends NioClient<HttpRequest>  {
    
    /**
     * Initializes a client to contact given host at given port.
     * Initializing a client amounts to starting a thread that runs 
     * until stopped by an external caller.
     * 
     * @param host name or IP address of the server host to contact
     * @param port listening port of the server host
     * @throws IOException if server can not be contacted within a timeout
     */
    public HttpClient(String host, int port) throws IOException {
        super(host, port);       
    }
    
    /**
     * Initializes a client to contact at given URL.
     * Initializing a client amounts to starting a thread that runs 
     * until stopped by an external caller.
     * 
     * @param uri uri of the server host. The URI must specify the host
     * and port of the server.
     * @throws IOException if server can not be contacted within a timeout
     */
    public HttpClient(URI uri) throws IOException {
        super(uri.getHost(), uri.getPort());       
    }
    
    /**
     * Initializes a client to contact at given URL string.
     * Initializing a client amounts to starting a thread that runs 
     * until stopped by an external caller.
     * 
     * @param url location of the server host. The location must specify 
     * the host and port in URL syntax.
     * @throws IOException if server can not be contacted within a timeout
     */
    public HttpClient(String url) throws IOException {
        super(new URL(url).getHost(), new URL(url).getPort());       
    }
}
