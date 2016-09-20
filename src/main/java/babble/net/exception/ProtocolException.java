package babble.net.exception;

import java.io.IOException;

/**
 * Raised when an incoming request is not confirming to a protocol.
 * 
 *  
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public class ProtocolException extends IOException {
    private int _errorcode;
    
    public ProtocolException() {
        super();
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(Throwable cause) {
        super(cause);
    }
    
    public ProtocolException setErrorCode(int code) {
        _errorcode = code;
        return this;
    }
    
    public int getErrorCode() {
        return _errorcode;
    }
    


}
