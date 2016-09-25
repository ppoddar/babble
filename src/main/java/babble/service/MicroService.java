package babble.service;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import babble.net.http.HttpRoute;
import babble.net.http.HttpServer;

/**
 * A micro-service is a HTTP enabled service that delegates any requests
 * to a set of other services. The other services are specified via URI.
 * The asynchronous response from other services are collected by this
 * micro-serivice into a single multi-part response. 
 *  
 * @author pinaki poddar
 *
 */
public class MicroService {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        HttpServer server = new HttpServer(port);
        Document config = getConfiguration(args[1]);
        configure(config, server);
        server.start();
    }

    /**
     * Supply a resource to configure the micro-service.
     * @param configResource
     */
    static Document getConfiguration(String configResource) {
        return null;
    }
    
    static void configure(Document config, HttpServer server) throws Exception {
        NodeList routes = config.getDocumentElement().getElementsByTagName("route");
        for (int i = 0; i < routes.getLength(); i++) {
            Node node = routes.item(i);
            if (! Element.class.isInstance(node)) continue;
            Element routeNode = Element.class.cast(node);
            HttpRoute route = new MicroServiceRoute(routeNode);
            server.addRoute(route);
        }
        
    }

}
