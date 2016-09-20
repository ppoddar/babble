package babble.net.json;

import babble.net.ExecutionContext;

public class EchoRoute extends JSONRoute {

    @Override
    public boolean matches(JSONRequest request) {
        return true;
    }

    @Override
    public JSONResponse execute(ExecutionContext ctx, JSONRequest request, JSONResponse response) throws Exception {
        response.putProperty("server", "some value");
        return response;
    }

}
