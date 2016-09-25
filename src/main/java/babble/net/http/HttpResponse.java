package babble.net.http;

import static babble.net.http.HttpConstants.CRLF;
import static babble.net.http.HttpConstants.HEADER_CONNECTION;
import static babble.net.http.HttpConstants.HEADER_TRANSFER_ENCODING;
import static babble.net.http.HttpConstants.PROTOCOL_VRESION_STRING;
import static babble.net.http.HttpConstants.SP;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.Response;

/**
 * A response according to HTTP specification.
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public abstract class HttpResponse extends Response {
    int status = HttpConstants.STATUS_OK;
    String message = "OK";
    
    Map<String,HttpHeader> _headers = new HashMap<String, HttpHeader>();

    protected static final Logger _logger = LoggerFactory.getLogger(HttpResponse.class);

    /**
     * Creates a response for given request.
     * 
     * @param request a request. must not be null.
     */
    public HttpResponse(HttpRequest request) {
        super(request);
    }
    
    public HttpRequest getRequest() {
        return (HttpRequest)super.getRequest();
    }

    /**
     * Adds an header, a key-value pair, to the response.
     * 
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        HttpHeader header = new HttpHeader(name, value);
        _headers.put(header.getName(), header);
    }


    /**
     * Sets status of the response with an empty message.
     * @param status status of the response. HTTP specification enumerates
     * various status code.
     */
    public HttpResponse setStatus(int status) {
        return setStatus(status, "");
    }

    /**
     * Sets status of the response with given message.
     * @param status status of the response. HTTP specification enumerates
     * various status code.
     * @param msg a textual explanatory message
     */
    public HttpResponse setStatus(int status, String msg) {
        this.status = status;
        this.message = msg;
        return this;
    }

    
    Object getHeaderValue(String name) {
        HttpHeader header = _headers.get(name);
        return header == null ? null : header.getValue();
    }
    
    
    /**
     * Adds a connection header.
     * If request had a header, its value is copied. Otherwise, connection
     * is 'persistent'
     */
    void addConnectionHeader() {
        HttpHeader requestHeader = getRequest().getHeader(HEADER_CONNECTION);
        if (requestHeader == null) {
            addHeader(HEADER_CONNECTION, "persistent");
        } else {
            addHeader(HEADER_CONNECTION, requestHeader.getValue());
        }
        
        
    }
    
    @Override
    public void send(ByteChannel channel) throws IOException {
        _logger.debug("writing response to channel " + channel);
        setChannel(channel);
        
        addConnectionHeader();
        addHeader(HEADER_TRANSFER_ENCODING, "chunked");
        
        writeString(PROTOCOL_VRESION_STRING, SP, 
                ""+status, SP, message, CRLF);
        
        // write each header separated by CRLF
        for (Map.Entry<String, HttpHeader> e : _headers.entrySet()) {
            writeString(e.getValue().toString());
            writeCRLF();
            _logger.debug("Header:" + e.getValue().toString());
        }
        // separate header and body section by a CRLF
        writeCRLF(); 
        
        writeBody();
        
        flush();
        
        _logger.debug("finished writing response to channel "  + channel);

        if ("close".equals(getHeaderValue(HEADER_CONNECTION))) {
            _logger.debug("closing channel");
            getChannel().close();
        } else {
            _logger.debug("not closing channel");
        }
    }
    
    
    /**
     * Appends the given string to body of this response.
     * If body of this response is already set to a {@link #setBody(Path) 
     * stream}, then textual body may not be appended.
     * @param body a string to be appended to body of this response.
     */
    public abstract void appendBody(String body) throws IOException;
    
    /**
     * Sets the given stream as body of this response.
     * @param path
     */
    public abstract void setBody(Path path) throws IOException;

    protected abstract void writeBody() throws IOException;

}
