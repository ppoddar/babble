package babble.net.http;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.NioServer;
import babble.net.Route;
import babble.net.Router;
import babble.net.exception.ProtocolException;
import babble.net.exception.RoutingException;

/**
 * A router for HTTP request that matches path to an {@link HttpOperation 
 * operation}.
 * 
 * @author pinaki poddar
 *
 */
public class HttpRouter extends Router<HttpRequest,HttpResponse> {
    private List<HttpOperation> _routes = new ArrayList<>();
    
    private static Logger _logger = LoggerFactory.getLogger(HttpRouter.class);

    /**
     * Create a router for a given server.
     * 
     * @param server a server. Must not be null.
     */
    public HttpRouter(NioServer<HttpRequest,HttpResponse> server) {
        super("http", server);
    }

    /**
     * Adds an operation to this router. This router will match request
     * to this operation.
     */
    @Override
    public void addRoute(Route<HttpRequest,HttpResponse> route) { 
        _logger.debug("adding route " + route);
        _routes.add((HttpOperation)route);
        
    }

    @Override
    public HttpRequest createRequest(SocketChannel channel, byte[] data) 
            throws ProtocolException {
        return new HttpRequest(channel, data);
    }

    @Override
    public Route<HttpRequest,HttpResponse> findMatchingRoute(HttpRequest request) {
        if (request == null) return null;
        for (HttpOperation route : _routes) {
            if (route.matches(request)) {
                 return route;  
            }
        }
        throw new RoutingException("no matching route for " + request);
    }
    
    boolean matches(String route, String path) {
        return Pattern.compile(route).matcher(path).matches();
    }

    @Override
    public boolean hasRoute() {
        return !_routes.isEmpty();
    }

    @Override
    public HttpResponse getErrorResponse(HttpRequest request, Exception ex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpResponse newResponse(HttpRequest request) {
        return new HttpResponse(request);
    }

}
