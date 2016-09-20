package babble.net.http;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
public class HttpResponse extends Response<HttpRequest> {
    
    private final String HTTP_VERSION = "HTTP/1.1";
    private static final String SP = " ";
    private static final String CRLF = "\r\n";
    int status;
    String message;
    Object _body;
    List<HttpHeader> _headers = new ArrayList<HttpHeader>();

    private static final Logger _logger = LoggerFactory.getLogger(HttpResponse.class);

    /**
     * Creates a response which would be sent on the given channel. 
     * 
     * @param request the request which has originated this response.
     * Often the response is sent via the same channel where request
     * had been received.
     */
    public HttpResponse(HttpRequest request) {
        super(request);
    }

    /**
     * Adds an header, a key-value pair, to the response.
     * 
     * @param name
     * @param value
     */
    public void addHeader(String name, String value) {
        _headers.add(new HttpHeader(name, value));
    }

    public HttpResponse setBody(Path path) throws IOException {
        if (path == null) {
            _body = null;
            return this;
        }
        _body = path;
        return this;
    }
    /**
     * Sets the body of the response.
     * 
     * @param body can be a string or a resource path.
     * 
     * @throws IOException
     */
    public HttpResponse setBody(String body) throws IOException {
        if (body == null) {
            _body = null;
            return this;
        }
        _body = body;
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
     * Sends the response across via communication channel.
     * The response is written in different section according to HTTP
     * specification.
     * 
     */
    @Override
    public void send(SocketChannel channel) throws IOException {
        //SocketChannel channel = getRequest().getChannel();
        _logger.debug("writing response to channel " + channel.getRemoteAddress());
        write(HTTP_VERSION, SP, ""+status, SP, message, CRLF);
        

        long contentLength = getContentLength();
        if (contentLength > 0) {
            addHeader("Content-Length", "" + getContentLength());
        }
        // write each header separated by CRLF
        for (HttpHeader header : _headers) {
            write(header.toString());
            _logger.debug("Header:" + header.toString());
            write(CRLF);
        }
        // separate header and body section by a CRLF
        write(CRLF);
        
        writeBody();
        write(CRLF);
        
        // now send the response over the channel
        super.send(channel);
        
        _logger.debug("finished writing response to channel "  + channel.getRemoteAddress());

        // should or should not the channel be closed?
        // unless the channel is closed, ApacheBench hangs
        //   getChannel().close();
        
    }
    
    void writeBody() throws IOException {
        if (_body == null) {
            return;
        }
        if (String.class.isInstance(_body)) {
            write(String.class.cast(_body));
        } else if (Path.class.isInstance(_body)) {
            stream(Path.class.cast(_body));
        }
    }

    /**
     * Estimates number of bytes in the body section of the response.
     * @return number of bytes in body section, if it can be determined. 
     * -1, otherwise.
     */
    long getContentLength() {
        if (_body == null) {
            return -1;
        }
        if (String.class.isInstance(_body)) {
            return String.class.cast(_body).getBytes().length;
       } else if (Path.class.isInstance(_body)) {
            try {
                return Files.size(Path.class.cast(_body));
            } catch (IOException ex) {
                return -1;
            }
        }
        return -1;
    }
    
    public String toString() {
        return "response-" + getRequest().toString();
    }


}
