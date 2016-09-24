package babble.net.http;

import static babble.net.http.HttpConstants.*;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import babble.net.Response;
import babble.net.ResponseCallback;

/**
 * A response according to HTTP specification.
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public class HttpResponse extends Response {
    int status = HttpConstants.STATUS_OK;
    String message = "OK";
    LinkedList<Object> _sections;
    
    Map<String,HttpHeader> _headers = new HashMap<String, HttpHeader>();

    private static final Logger _logger = LoggerFactory.getLogger(HttpResponse.class);

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

    public HttpResponse addBodySection(Path path) throws IOException {
        if (path == null) {
            _logger.warn("null path can not be to body");
            return this;
        }
        if (_sections == null) _sections = new LinkedList<Object>();
        _sections.add(path);
        _logger.debug("added path body section " + _sections.size() + " path=" + path);
        return this;
    }
    /**
     * Sets the body of the response.
     * 
     * @param body can be a string or a resource path.
     * 
     * @throws IOException
     */
    public HttpResponse addBodySection(String s) throws IOException {
        if (s == null) return this;
        if (_sections == null) {
            _sections = new LinkedList<Object>();
        } 
        
        StringBuilder builder = new StringBuilder();
        _sections.add(builder);
        builder.append(s);
        return this;
    }
    
    public HttpResponse appendBody(String s) throws IOException {
        if (s == null) return this;
        if (_sections == null && !_sections.isEmpty()
         && StringBuilder.class.isInstance(_sections.getLast())) {
            StringBuilder.class.cast(_sections.getLast()).append(s);
        } else {
            addBodySection(s);
        }
        return this;
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

    /**
     * Sends the response across via network channel.
     * The response is written with Transfer-encoding according to HTTP
     * specification.
     * 
     */
    @Override
    public void send(ByteChannel channel) throws IOException {
        if (_sections == null) return;
        _logger.debug("writing response to channel " + channel);
        setChannel(channel);
        
        addConnectionHeader();
        addHeader(HEADER_TRANSFER_ENCODING, "chunked");
        
        writeString(PROTOCOL_VRESION, SP, ""+status, SP, message, CRLF);
        
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
    
    void writeBody() throws IOException {
        _logger.debug("writing body " + (_sections == null ? "no"
                :""+_sections.size()) + " sections");
        for (int i = 0; _sections != null && i < _sections.size(); i++) {
            Object section = _sections.get(i);
            if (section instanceof StringBuilder) {
                writeStringInChunks(section.toString());
            } else if (section instanceof Path) {
                writeStream((Path)section);
            } else {
                throw new RuntimeException("unrecognized section " + section);
            }
        }
        _logger.debug("ending with last chunk");
        writeChunk(new byte[0]);
    }
    
    

    

    @Override
    public void receive(ByteChannel channel, ResponseCallback cb) {
        // read channel data
        // call callabck
    }


}
