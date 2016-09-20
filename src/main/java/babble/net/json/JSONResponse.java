package babble.net.json;


import static babble.net.json.JSONRPC.ERROR_CODE;
import static babble.net.json.JSONRPC.ERROR_DATA;
import static babble.net.json.JSONRPC.ERROR_MESSAGE;
import static babble.net.json.JSONRPC.PROPERTY_ERROR;
import static babble.net.json.JSONRPC.PROPERTY_ID;
import static babble.net.json.JSONRPC.PROPERTY_RESULT;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.SocketChannel;

import org.json.JSONArray;
import org.json.JSONObject;

import babble.net.Response;

@SuppressWarnings("serial")
public class JSONResponse extends Response<JSONRequest> {
    private final JSONObject _json;
    
    public JSONResponse(JSONRequest request) {
        super(request);
        _json = new JSONObject();
        _json.put(PROPERTY_ID, JSONRequest.class.cast(request).getId());
    }
    
    public void putProperty(String key, Object value) {
        _json.put(key, value);
    }
    
    /**
     * Mark this response as failed with given status code and exception.
     * 
     * @param status
     * @param ex
     */
    public void fail(int status, Exception ex) {
        JSONObject error = new JSONObject();
        _json.put(PROPERTY_ERROR, error);
        _json.remove(PROPERTY_RESULT);

        
        error.put(ERROR_CODE, status);
        error.put(ERROR_MESSAGE, ex.getMessage());
        error.put(ERROR_DATA, ex.getStackTrace());
    }
    
    public void result(Object result) {
        //JSONObject result = new JSONObject();
        
        // Jackson ObjetMapper?
        
        _json.put(PROPERTY_RESULT, result);
    }
    
    public String toString() {
        return _json.toString();
    }
    
    @Override
    protected void send(SocketChannel channel) throws IOException {
        write(_json.toString());
        super.send(channel);
    }
    
    public static void printOutput(PrintStream out, JSONObject json) {
        if (json.has(JSONRPC.PROPERTY_ERROR)) {
            JSONObject error = json.getJSONObject(JSONRPC.PROPERTY_ERROR);
            
            out.println("Error Status:" + error.get(ERROR_CODE));
            out.println("Error Mesage:" + error.getString(ERROR_MESSAGE));
            JSONArray stack = error.getJSONArray(JSONRPC.ERROR_DATA);
            for (int i = 0; i < stack.length(); i++) {
                out.println("\t" + stack.get(i));
            }
        } else {
            out.println(json.toString());
        }
        
    }
    

}
