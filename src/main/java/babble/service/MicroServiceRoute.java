package babble.service;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import babble.net.ExecutionContext;
import babble.net.ResponseCallback;
import babble.net.http.HttpClient;
import babble.net.http.HttpRequest;
import babble.net.http.HttpResponse;
import babble.net.http.HttpRoute;
import babble.net.http.MultipartHttpResponse;

public class MicroServiceRoute extends HttpRoute implements ResponseCallback {
    Map<HttpClient,RequestRewriteRule> _clients;
    ExecutorService _threadPool;
    
    /**
     * Creates a micro-service with given configuration element.
     *  
     * @param config
     */
    public MicroServiceRoute(Element config) throws Exception {
        super(getAttribute(config, "method", "GET"), 
              getAttribute(config, "pattern", ".*"));
        
        NodeList services = config.getElementsByTagName("service");
        _clients = new HashMap<HttpClient, RequestRewriteRule>();
        for (int i = 0; i < services.getLength(); i++) {
            Node node = services.item(i);
            if (! Element.class.isInstance(node)) continue;
            Element serviceNode = Element.class.cast(node);
            URI uri = new URI(serviceNode.getAttribute("uri"));
            HttpClient client = new HttpClient(uri.getHost(), uri.getPort());
            RequestRewriteRule rule = null;
            _clients.put(client,rule);
        }
        
        _threadPool = Executors.newFixedThreadPool(_clients.size());
      
    }

    @Override
    public HttpResponse execute(ExecutionContext ctx, HttpRequest request) throws Exception {
        CountDownLatch latch = new CountDownLatch(_clients.size());
        MultipartHttpResponse response = new MultipartHttpResponse(request);
        int partIndex = 0;
        for (Map.Entry<HttpClient,RequestRewriteRule> e : _clients.entrySet()) {
            partIndex++;
            ResponseCallback cb = new ResponseCallback() {
                int index;
                private ResponseCallback init(int idx) {
                    index = idx;
                    return this;
                }
                @Override
                public void onResponse(byte[] bytes, boolean eos) {
                    if (eos) latch.countDown();
                    try {
                        response.addPart(index, new String(bytes));
                    } catch (IOException ex) {
                        onError(ex);
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    latch.countDown();
                    response.addFailedPart(index, ex);
                }
            }.init(partIndex);
            HttpRequest childRequest = e.getValue().rewrite(request);
            Callable<Void> clientRequest = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    e.getKey().sendRequest(childRequest, cb);
                    return null;
                }
            };
            _threadPool.submit(clientRequest);
        }
        
        latch.await(2, TimeUnit.SECONDS);
        return response;
    }
    
    static String getAttribute(Element e, String attr, String def) {
        return e.hasAttribute(attr) ? e.getAttribute(attr) : def;
    }

    @Override
    public void onResponse(byte[] bytes, boolean eos) {
        
    }

    @Override
    public void onError(Exception ex) {
        // TODO Auto-generated method stub
        
    }
}
