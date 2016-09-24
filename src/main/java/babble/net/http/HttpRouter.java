package babble.net.http;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.NioServer;
import babble.net.Route;
import babble.net.Router;
import babble.net.exception.RoutingException;

/**
 * A router for HTTP request that matches path to an {@link HttpRoute 
 * operation}.
 * 
 * @author pinaki poddar
 *
 */
public class HttpRouter extends Router<HttpRequest,HttpResponse> {
    private List<HttpRoute> _routes = new ArrayList<>();
    
    private static Logger _logger = LoggerFactory.getLogger(HttpRouter.class);

    /**
     * Create a router for a given server.
     * 
     * @param server a server. Must not be null.
     */
    public HttpRouter(NioServer<HttpRequest,HttpResponse> server) {
        super(server);
    }

    /**
     * Adds an operation to this router. This router will match request
     * to this operation.
     */
    @Override
    public void addRoute(Route<HttpRequest,HttpResponse> route) { 
        _logger.debug("adding route " + route);
        _routes.add((HttpRoute)route);
        
    }


    @Override
    public Route<HttpRequest,HttpResponse> findMatchingRoute(HttpRequest request) {
        if (request == null) return null;
        for (HttpRoute route : _routes) {
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


}
