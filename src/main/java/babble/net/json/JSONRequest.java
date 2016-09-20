package babble.net.json;

import static babble.net.json.JSONRPC.PROPERTY_ID;
import static babble.net.json.JSONRPC.PROPERTY_JSONRPC;
import static babble.net.json.JSONRPC.PROPERTY_METHOD;
import static babble.net.json.JSONRPC.PROPERTY_PARAMS;
import static babble.net.json.JSONRPC.PROTOCOL_VERSION;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.Request;
import babble.net.exception.ProtocolException; 

/**
 * A JSON-RPC request.
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public class JSONRequest extends Request {
    private JSONObject _json;    
    
    private static Logger _logger = LoggerFactory.getLogger("JSONRPC-Request");
    
    
    public JSONRequest(String s) throws ProtocolException {
        super(null);
        parse(s);
    }
    
    public void putProperty(String key, Object value) {
        _json.put(key, value);
    }
    
    /**
     * Create a JSON-RPC request from supplied array of raw byte data.
     * 
     * @param channel a network channel where 
     * @param data
     */
    public JSONRequest(SocketChannel channel, byte[] data) throws ProtocolException {
        super(channel);
        parse(new String(data));
        _logger.debug("constructing request from " + new String(data));
  }
    
    
    void parse(String s) throws ProtocolException {
        try {
            _json = new JSONObject(s);
        } catch (JSONException ex) {
            throw new ProtocolException(ex)
                .setErrorCode(JSONRPC.ERROR_CODE_INVALID_REQUEST);
        }
        
        if (!_json.has(PROPERTY_METHOD)) {
            throw new ProtocolException("Missing property " + 
                     PROPERTY_METHOD + " in incoming JSON request")
                    .setErrorCode(JSONRPC.ERROR_CODE_INVALID_REQUEST);
        }
        
        if (_json.has(PROPERTY_JSONRPC)) {
            String version = _json.getString(PROPERTY_JSONRPC);
            if ("2.0".equals(version)) {
                _logger.warn("Received request on JSON-RPC version " + version
                        + " expecting protcol version " + PROTOCOL_VERSION);
            } else {
                _logger.warn("Received request does not have JSON-RPC version"
                        + " expecting protcol version " + PROTOCOL_VERSION);
            }
        }
        
        if (isParameterByPosition()) {
            throw new ProtocolException("Parameters are supplied "
                    + " by-position i.e. the Request JSON Object contains "
                    + "parameter values in a JSON Array object. "
                    + " The supported protocol requires that parametrs "
                    + " to be suppled by-name i.e. in a JSO Object "
                    + " whose properties are same as the name of the"
                    + " declared arguments in the method signature")
            .setErrorCode(JSONRPC.ERROR_CODE_INVALID_REQUEST);
        }
      }
    
    public String getId() {
        Object id = _json.has(PROPERTY_ID) ? _json.get(PROPERTY_ID) : null;
        return id == null ? null : id.toString();
    }
    
    @Override
    public boolean isOneWay() {
        return !_json.has(PROPERTY_ID);
        
    }

    @Override
    protected void send(SocketChannel channel) throws IOException {
        channel.write(ByteBuffer.wrap(_json.toString().getBytes()));
    }

    public String getMethod() {
        return _json.getString(PROPERTY_METHOD);
    }

    /**
     * Gets the value of the parameter of given name.
     * In JSON-RPC request message, parameter values can be specified
     * either
     * According to JSON-RPC specification
     * <pre>
     * by-position: params MUST be an Array, containing the values in  the
     * Server expected order.
     * by-name: params MUST be an Object, with member names 
     * that match the Server expected parameter names. The absence of 
     * expected names MAY result in an error being generated. The names 
     * MUST match exactly, including case, to the method's expected 
     * parameters.
     * </pre>
     * This implementation only supports message parameters <code>by-name</code>
     * 
     * @param paramName name of a parameter, case-sensitive
     * 
     * @return value of the parameter as appeared in the request message
     */
    public Object getParameterValue(String paramName) {
        JSONObject paramObject = getParameters();
        if (paramObject.has(paramName)) {
            return paramObject.get(paramName);
        } else {
            throw new RuntimeException("Parameter [" + paramName + "]"
                    + " does not appear in suplied parameters. "
                    + "Parameetr names are " 
                    + getPropertyNames(paramObject));
        }
    }
    
    JSONObject getParameters() {
        return _json.getJSONObject(PROPERTY_PARAMS);
        
    }
    
    List<String> getPropertyNames(JSONObject json) {
        JSONArray names = json.names();
        if (names == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < names.length(); i++) {
            result.add(names.getString(i));
        }
        return result;
    }
    
    
    /**
     * Affirms if request parameters are supplied in an array. When arguments
     * are supplied in an array, then array position is same as the position 
     * of the argument in method declaration.
     * 
     * The parameter values can also be supplied as a nested JSONObecject
     * with each argument appearing as a property of the nested JSONObject.
     * 
     * @return true if parameters are supplied in an array property
     */
    public boolean isParameterByPosition() {
        return _json.has(PROPERTY_PARAMS) 
            && JSONArray.class.isInstance(_json.get(PROPERTY_PARAMS));
    }
    
    public String toString() {
        return _json.toString();
    }


}