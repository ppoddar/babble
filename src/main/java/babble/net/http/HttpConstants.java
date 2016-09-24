package babble.net.http;

import java.util.Arrays;
import java.util.List;

/**
 * Set of constants declarations related to HTTP protocol.
 * 
 * @author pinaki poddar
 *
 */
public class HttpConstants {

    static final String PROTOCOL_NAME    = "http";
    static final String PROTOCOL_VRESION = "HTTP/1.1";

    public static final String CRLF = "\r\n";
    public static final String SP = " ";
    
    public static final String HEADER_HOST              = "Host";
    public static final String HEADER_CONNECTION        = "Connection";
    public static final String HEADER_CONTENT_TYPE      = "Content-Type";
    public static final String HEADER_CONTENT_LENGTH    = "Content-Length";
    public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
    
    public static final int STATUS_OK          = 200;
    public static final int STATUS_MISDIRECTED = 421;
    public static final int STATUS_BAD_REQUEST = 400;
    
    public static final List<String> ALLOWED_METHODS = 
            Arrays.asList("GET", "POST","PUT", "DELETE", "HEAD", "OPTION");

}
