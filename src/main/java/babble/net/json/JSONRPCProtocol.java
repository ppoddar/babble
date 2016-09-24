package babble.net.json;

import babble.net.NioServer;
import babble.net.Protocol;
import babble.net.Router;

class JSONRPCProtocol implements Protocol<JSONRequest, JSONResponse>{

    @Override
    public String getName() {
        return "json-rpc";
    }

    @Override
    public JSONRequest newRequest() {
        return new JSONRequest();
    }

    @Override
    public JSONResponse newResponse(JSONRequest request) {
        return new JSONResponse(request);
    }
    
    @Override
    public JSONResponse newErrorResponse(JSONRequest request, final Exception ex) {
        JSONResponse response = new JSONResponse(request);
        int status = 0;
        response.fail(status, ex);
        return response;
     }

    @Override
    public Router<JSONRequest, JSONResponse> newRouter(NioServer<JSONRequest, JSONResponse> server) {
        return new JSONRPCRouter(server);
    }


}
