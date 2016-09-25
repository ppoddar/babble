package babble.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
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
    private int _versionMajor, _versionMinor;
    URI _uri;
    
    private static String REQUESTLINE_REGEX = 
     "(?<method>\\S+)\\ (?<path>\\S+)\\ HTTP/(?<major>\\d+)\\.(?<minor>\\d+)";
    private Pattern _requestLinePattern = Pattern.compile(REQUESTLINE_REGEX);
    
    Map<String,String> _params = new HashMap<String,String>();
    Map<String, HttpHeader> _headers = new HashMap<String, HttpHeader>();
    
    
    private static final Logger _logger = LoggerFactory.getLogger(HttpRequest.class);

    HttpRequest() {
        super();
    }
    
    /**
     * Supply a HTTP method and a path.
     */
    public HttpRequest(String method, String path) throws ProtocolException {
        this(method + HttpConstants.SP + path + HttpConstants.SP 
                + HttpConstants.PROTOCOL_VRESION_STRING);
    }
    
    public HttpRequest(String requestLine) throws ProtocolException {
        super();
        parse(requestLine.getBytes());
    }

    
    /**
     * Create a request given the a network channel and raw
     * byte data (possibly read from the same channel).
     * A request is bound to this channel and the response from this
     * request will be sent via the same channel unless specified 
     * another channel is explicitly set.
     * 
     * @param channel must not be null.
     * @param data raw byte data. Must follow HTTP request syntax.
     */
    public HttpRequest(ByteChannel channel, byte[] data) throws ProtocolException {
        super(channel);
        parse(data);
        setURI(channel, true);
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
    
    
    /**
     * Parses given array of bytes as per HTTP request format.
     * @param data
     * @throws ProtocolException
     */
    protected void parse(byte[] data ) throws ProtocolException {
        parse(new String(data));
    }
    
    /**
     * Parses given string as per HTTP Request format. 
     * @param content a string as HTTP request
     * @throws ProtocolException if parsing fails
     */
    protected void parse(String content) throws ProtocolException {
        _logger.debug("parsing request data \r\n" + content);
        
       BufferedReader reader = new BufferedReader(new StringReader(content));
       String requestLine = null;
       try {
           requestLine = reader.readLine();
           _logger.debug("request line [" + requestLine + ']');
       } catch (IOException ex) {
           _logger.warn("i/o error reading request :" + ex);
           throw new ProtocolException("error reading request line", ex);
       }
       
       parseRequestLine(requestLine);
           
       // read headers. Ignore empty line. Stop at CRLF
       String line = null;
       try {
           while ((line = reader.readLine()) != null) {
               if (line.equals(HttpConstants.CRLF)) break;
               if (line.trim().length() == 0) break;
               HttpHeader header = new HttpHeader(line);
               _headers.put(header.getName(), header);
           }
       } catch (IOException ex) {
           throw new ProtocolException(ex);
       }
       
       // read body
       
    }
    
    void parseRequestLine(String line) throws ProtocolException {
        if (line == null) 
            throw new ProtocolException("null request line [" + line + "]");
        Matcher matcher = _requestLinePattern.matcher(line);
        if (matcher.matches()) {
            setMethod(matcher.group("method"));
            setPath(matcher.group("path"));
            setVersionMajor(Integer.parseInt(matcher.group("major")));
            setVersionMinor(Integer.parseInt(matcher.group("minor")));
        } else {
            throw new ProtocolException(line + " does not match request "
                    + "pattern " + REQUESTLINE_REGEX);
        }
    }
    
    
    void setVersionMajor(int v) {
        _versionMajor = v;
    }
    
    void setVersionMinor(int v) {
        _versionMinor = v;
    }
    
    /**
     * Adds a header.
     * 
     * @param name name of the header field e.g. Content-Length. Must not be
     * null.
     * @param value value of the header e.g. 2034 as a string. Can be null.
     */
    public void addHeader(String name, String value) {
        HttpHeader header = new HttpHeader(name, value);
        _headers.put(header.getName(), header);
    }
    
    public HttpHeader getHeader(String name) {
        return _headers.get(name);
    }
    
    /**
     * Sets HTTP method.
     * 
     * @param method must be a HTTP method name e.g. GET, PUT etc.
     * This argument can be in any case, but {@link HttpRequest#getMethod()
     * getMethod()} will return the name in upper case.
     * @return the same request
     */
    protected HttpRequest setMethod(String method) throws ProtocolException {
        if (method == null 
        || !HttpConstants.ALLOWED_METHODS.contains(method.toUpperCase()))
            throw new ProtocolException("invalid method " + method);
        _method = method.toUpperCase();
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
    public String getVersionString() {
        return HttpConstants.PROTOCOL_NAME.toUpperCase() + '/'
                + _versionMajor + '.' + _versionMinor;
    }
    

    /**
     * Writes this request to i/o channel.
     * HTTP request follows a particular format:
     * method SP path SP version CRLF
     * 
     */
    @Override
    public void send(ByteChannel channel) throws IOException {
        setChannel(channel);
        writeString(getMethod(), " " , getPath(), " ", getVersionString());
        flush();
    }
    
    
    /**
     * Sets the path. The path is saved as it is received. a path string
     * can begin with a forward slash. However, 
     * {@link #getPath() getPath()} will return a path without a forward slash.
     *  
     * @param path
     */
    protected void setPath(String path) throws ProtocolException {
        if (path == null) throw new ProtocolException("Path must not be null");
        _path = path;
        
    }
    public String toString() {
        return _uri == null ? "null" : _uri.toString();
    }
    
    protected void setURI(Channel channel, boolean local) {
        ChannelInfo addr = new ChannelInfo(channel, local);
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

    @Override
    protected void receive(ByteChannel channel) throws IOException {
        setChannel(channel);

        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        ByteBuffer buf = ByteBuffer.allocate(1024);
        boolean end = false;
        _logger.debug("receiving request from channel " + channel + " ...");
        while (!end) {
            buf.clear();
            int n = channel.read(buf);
            if (n < 0) throw new IOException("channel has been closed by remote");
            if (n == 0) continue;
            if (n >= 4) {
                byte[] endMarker = new byte[4];
                buf.position(buf.position()-4);
                buf.get(endMarker);
                _logger.debug("last 4-byte " + Arrays.toString(endMarker));
                end = endMarker[0] == (byte)'\r'
                        && endMarker[1] == (byte)'\n'
                        && endMarker[2] == (byte)'\r'
                        && endMarker[3] == (byte)'\n';
            }
            buf.flip();
            readBuffer.put(buf);
        }
        _logger.debug("received " + readBuffer.position() + " bytes request");
        parse(readBuffer.array());
        setURI(channel, true);
    }
    
}
