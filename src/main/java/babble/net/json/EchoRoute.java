package babble.net.json;

import org.json.JSONArray;
import org.json.JSONObject;

import babble.net.ExecutionContext;

public class EchoRoute extends JSONRoute {

    @Override
    public boolean matches(JSONRequest request) {
        return true;
    }

    @Override
    public JSONResponse execute(ExecutionContext ctx, JSONRequest request, JSONResponse response) throws Exception {
        JSONObject json = request.getBody();
        JSONArray names = json.names();
        for (int i = 0; i < names.length(); i++) {
            String key = names.get(i).toString();
            response.putProperty(key, json.get(key));
        }
        response.putProperty("server", true);
        return response;
    }
    
    public String toString() {
        return "echo";
    }

}
