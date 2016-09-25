package babble.service;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.json.JSONRPCServer;
import babble.net.json.JSONRoute;
import babble.util.MethodSignature;

/**
 * Wraps a function with a shell to access remotely with HTTP/JSON-RPC protocol.
 * 
 * @author pinaki poddar
 *
 */
public class ServiceMaker {
    private static Logger _logger = LoggerFactory.getLogger("ServiceMaker");
    public ServiceMaker() {
        // TODO Auto-generated constructor stub
    }
    
    public void wrap(Object instance) throws Exception {
        JSONRPCServer server = new JSONRPCServer("json-rpc-server", 10000);
        
        Method[] methods =  instance.getClass().getMethods();
        for (Method m : methods) {
            MethodSignature signature = new MethodSignature(m);
            _logger.debug("creating route for " + signature);
            server.addRoute(new JSONRoute(signature.toString()));
        }
        
    }

}
