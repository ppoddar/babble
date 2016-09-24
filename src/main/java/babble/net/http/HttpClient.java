package babble.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import babble.net.NioClient;
import babble.net.ResponseCallback;
import babble.util.SimpleURI;

/**
 * A Client using JSON-RPC protocol.
 * 
 * The program accepts a server URI, sends JSON messages reading from standard
 * input.
 * 
 * 
 * @author pinaki poddar
 *
 */
public class HttpClient extends NioClient<HttpRequest,HttpResponse> implements ResponseCallback {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("***Missing argument");
            System.err.println(HttpClient.class.getSimpleName() 
                    + " - sends request to a server JSON-RPC server");
            System.err.println("Usage: " + HttpClient.class.getName() + " url");
            System.err.println("where");
            System.err.println("\turl URL of HTTP server e.g. http://localhost:8090");
            System.exit(1);
        }
        SimpleURI uri = new SimpleURI(args[0]);
        HttpClient client = new HttpClient(uri.getHost(), uri.getPort());
        client.readAndSend(System.in);
        
        
        Thread.sleep(10*1000);
        
    }
    
    

    public HttpClient(String host, int port) throws IOException {
        super(host, port, false);
       
    }
    
    @Override
    public void onResponse(byte[] bytes, boolean eos) {
        System.err.println(new String(bytes));
        if (eos) getLogger().info("------ end of response ------ ");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
    
    void readAndSend(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#")) continue;
            if (line.length() == 0) continue;
            
            HttpRequest request = new HttpRequest(line);
            getLogger().info("send-> " + request);
            sendRequest(request, this);
        }
    }
}
