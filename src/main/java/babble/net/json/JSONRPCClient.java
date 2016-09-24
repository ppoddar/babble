package babble.net.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

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
public class JSONRPCClient extends NioClient<JSONRequest,JSONResponse> implements ResponseCallback {
    
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("***MIssing argument");
            System.err.println(JSONRPCClient.class.getSimpleName() 
                    + " - sends request to a server JSON-RPC server");
            System.err.println("Usage: " + JSONRPCClient.class.getName() + " url");
            System.err.println("where");
            System.err.println("\turl URL of JSON-RPC server e.g. json-rpc://localhost:8090");
            System.exit(1);
        }
        SimpleURI uri = new SimpleURI(args[0]);
        JSONRPCClient client = new JSONRPCClient(uri.getHost(), uri.getPort());
        client.readAndSend(System.in);
        
        
        Thread.sleep(10*1000);
        
    }
    
    

    public JSONRPCClient(String host, int port) throws IOException {
        super(host, port, false);
       
    }
    
    private List<byte[]> _accumulatedResponse = new ArrayList<byte[]>();
    @Override
    public void onResponse(byte[] bytes, boolean eos) {
        if (_accumulatedResponse.isEmpty()) {
            getLogger().info("--- server response --- ");
        
        }
        if (eos) {
            int nTotalBytes = 0;
            for (byte[] chunk : _accumulatedResponse) {
                nTotalBytes += chunk.length;
            }
            byte[] allBytes = new byte[nTotalBytes];
            int pos = 0;
            for (byte[] chunk : _accumulatedResponse) {
                System.arraycopy(chunk, 0, allBytes, pos, chunk.length);
                pos += chunk.length;
            }
            
            JSONObject response = new JSONObject(new String(allBytes));
            JSONResponse.printOutput(System.out, response);
        } else {
            _accumulatedResponse.add(bytes);
        }

    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
    
    void readAndSend(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null;
        int iLine = 0;
        while ((line = reader.readLine()) != null) {
            ++iLine;
            line = line.trim();
            if (line.startsWith("#")) continue;
            if (line.length() == 0) continue;
            
            JSONRequest request = new JSONRequest(line);
            request.putProperty(JSONRPC.PROPERTY_ID, iLine);
            getLogger().info("->" + request);
            sendRequest(request, this);
        }
    }


}
