package babble.net.json;

import java.util.ArrayList;
import java.util.List;

import babble.net.NioServer;
import babble.net.Route;
import babble.net.Router;
import babble.net.exception.RoutingException;

public class JSONRPCRouter extends Router<JSONRequest,JSONResponse> {
    private List<JSONRoute> _routes = new ArrayList<JSONRoute>();
    
    public JSONRPCRouter(NioServer<JSONRequest, JSONResponse> server) {
        super(server);
    }

    @Override
    public boolean hasRoute() {
        return !_routes.isEmpty();
    }

    @Override
    public void addRoute(Route<JSONRequest,JSONResponse> op) {
        JSONRoute route = (JSONRoute)op;
        _routes.add(route);
    }

    @Override
    public Route<JSONRequest,JSONResponse> findMatchingRoute(JSONRequest request) 
        throws RoutingException {
        for (JSONRoute route : _routes) {
            if (route.matches(request)) return route;
        }
        throw new RoutingException("No route can handle request " + request)
            .setErrorCode(JSONRPC.ERROR_CODE_INVALID_REQUEST);
        
    }
    
}
