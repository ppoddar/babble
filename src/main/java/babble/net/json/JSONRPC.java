package babble.net.json;

public final class JSONRPC {
    public static final String PROTOCOL_NAME = "json-rpc";
    public static final String PROTOCOL_VERSION = "2.0";
    
    
    public static final String PROPERTY_JSONRPC = "jsonrpc";
    public static final String PROPERTY_METHOD  = "method";
    public static final String PROPERTY_PARAMS  = "params";
    public static final String PROPERTY_ID      = "id";
    public static final String PROPERTY_RESULT  = "result";
    public static final String PROPERTY_ERROR   = "error";
    
    public static final String ERROR_CODE    = "code";
    public static final String ERROR_MESSAGE = "message";
    public static final String ERROR_DATA    = "data";
    
    
    public static final int ERROR_CODE_PARSE_ERROR      = -32700;
    public static final int ERROR_CODE_INVALID_REQUEST  = -32600;
    public static final int ERROR_CODE_METHOD_NOT_FOUND = -32601;
    public static final int ERROR_CODE_INVALID_PARAMS   = -32602;
    public static final int ERROR_CODE_INTERNAL_ERROR   = -32603;
    public static final int ERROR_CODE_SERVER_ERROR     = -32604;

}
