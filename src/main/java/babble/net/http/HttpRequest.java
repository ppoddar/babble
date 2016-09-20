package babble.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.Request;
import babble.net.Route;
import babble.net.exception.ProtocolException;
import babble.util.ChannelInfo;


/**
 * A HTTP request.
 * A request carries requisite input to an {@link Route operation}.
 * This facility {@link #parse(SocketChannel, byte[]) parses} an array of 
 * raw bytes (possibly read from a channel at the network layer).  
 * The parsing process enforces HTTP request grammar.
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public class HttpRequest extends Request {
    String _method;
    String _path;
    String _version = "";
    URI _uri;
    Map<String,String> _params = new HashMap<String,String>();
    List<HttpHeader> _headers = new ArrayList<HttpHeader>();
    
    
    private static final Logger _logger = LoggerFactory.getLogger(HttpRequest.class);
    
    /**
     * Creates a request that is not bound to any network channel.
     * 
     * @param data raw byte for request parameters
     * 
     * @throws ProtocolException
     */
    public HttpRequest(byte[] data) throws ProtocolException {
        this(null, data);
    }
    
    public HttpRequest(String method, String path) throws ProtocolException {
        this(null, (method + " " + path).getBytes());
    }


    
    /**
     * Create a request given the incoming network channel and raw
     * byte data (possibly read from the same channel).
     * A request is bound to this channel and the response from this
     * request will be sent via the same channel.
     * 
     * @param channel must not be null.
     * @param data raw byte data. Must follow HTTP request syntax.
     */
    public HttpRequest(SocketChannel channel, byte[] data) throws ProtocolException {
        super(channel);
        parse(channel, data);
        
    }
    
    public void addParameter(String key, String value) {
        _params.put(key, value);
    }

    /**
     * Parses parameters of this request from given data as per HTTP
     * request syntax.
     * 
     * HTTP Request syntax
     * <pre>
     * </pre>
     * 
     */
    
    private static final String WHITESPACE = "\\s+";
    private static final String HTTP_VERSION_PATTERN = "HTTP/(\\d)\\.(\\d)";
    public /* for testing */
    void parse(SocketChannel channel, byte[] data) throws ProtocolException {
        String content = new String(data);
        _logger.debug("parsing request data " + content);
        
       
       BufferedReader reader = new BufferedReader(new StringReader(content));
       String requestLine = null;
       try {
           requestLine = reader.readLine();
           if (requestLine == null) return;
       } catch (IOException ex) {
           throw new ProtocolException(ex);
       }
       String[] tokens = requestLine.split(WHITESPACE);
       if (tokens.length > 1) {
           try {
               setMethod(tokens[0].trim());
           } catch (IllegalArgumentException ex) {
               throw new ProtocolException(ex.getMessage(), ex);
           }
           setPath(tokens[1]);
           
           if (tokens.length > 2) {
               Pattern versionPattern = Pattern.compile(HTTP_VERSION_PATTERN);
               Matcher matcher = versionPattern.matcher(tokens[2].trim());
               if (matcher.matches()) {
                   setVersion(matcher.group(1) + "." + matcher.group(2));
               } else {
                   throw new ProtocolException("Invalid version " + tokens[2]);
               }
           }
       } else {
             throw new ProtocolException("Invalid request line " + requestLine);
       }
       
       String line = "";
       try {
           while ((line = reader.readLine()) != null) {
               if (line.equals(HttpOperation.CRLF)) break;
               if (line.trim().length() == 0) break;
               int idx = line.indexOf(":");
               addHeader(line.substring(0, idx).toLowerCase(), 
                       line.substring(idx+1).trim());
           }
       } catch (IOException ex) {
           throw new ProtocolException(ex);
       }
       
       // read body
       
       setURI(channel);
    }
    
    
    /**
     * Adds a header.
     * 
     * @param name name of the header field e.g. Content-Length
     * @param value value of the header e.g. 2034 as a string
     */
    public void addHeader(String name, String value) {
        _headers.add(new HttpHeader(name, value));
    }
    
    /**
     * Sets HTTP method.
     * 
     * @param method must be a HTTP method name e.g. GET, PUT etc.
     * This argument can be in any case, but {@link HttpRequest#getMethod()
     * getMethod()} will return the name in upper case.
     * @return the same request
     */
    public HttpRequest setMethod(String method) {
        if (method == null 
        || !HttpOperation.ALLOWED_METHODS.contains(method.toUpperCase()))
            throw new IllegalArgumentException("invalid method " + method);
        _method = method.toUpperCase();
        return this;
    }
    
    
    /**
     * Sets HTTP version of this string
     * @param version HTTP version specifier e.g. HTTP/1.1
     * @return the same request
     */
    public HttpRequest setVersion(String version) {
        _version = version;
        return this;
    }
    
    /**
     * Gets HTTP method name.
     * 
     * @return HTTP method name e.g. GET PUT etc. Never null.
     */
    public String getMethod() {
        return _method;
    }
    
    /**
     * Gets path of this request without any leading forward slash '/'
     * @return path of this request. Can be empty or null.
     * null implies that path has not been defined.
     */
    public String getPath() {
        return _path.startsWith("/") ? _path.substring(1) : _path;
    }
    
    /**
     * Gets HTTP version.
     * 
     * @return HTTP version string e.g. HTTP/1.1
     */
    public String getVersion() {
        return _version;
    }
    

    /**
     * Writes this request to i/o channel.
     * HTTP request follows a particular format:
     * method SP path SP version CRLF
     * 
     */
    @Override
    public void send(SocketChannel channel) throws IOException {
        String s = getMethod() + " " + getPath() + " " + getVersion();
        channel.write(ByteBuffer.wrap(s.getBytes()));
    }
    
    
    /**
     * Sets the path. The path is saved as it is received. a path string
     * can begin with a forward slash. However, 
     * {@link #getPath() getPath()} will return a path without a forward slash.
     *  
     * @param path
     */
    public void setPath(String path) {
        if (path == null) throw new IllegalArgumentException("Path must not be null");
        
    }
    public String toString() {
        return _uri == null ? "null" : _uri.toString();
    }
    
    void setURI(SocketChannel channel) {
        ChannelInfo addr = new ChannelInfo(channel);
        try {
            _uri = new URI(HttpConstants.PROTOCOL_NAME, null,
                    addr.getHost(), addr.getPort(), "/" + getPath(), 
                    null, null);
        } catch (URISyntaxException ex) {
             
        }
    }
    
    public URI getURI() {
        return _uri;
    }
}
