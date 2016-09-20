package babble.net.exception;

/**
 * Raised when an incoming request can not be routed.
 * 
 *  
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public class RoutingException extends RuntimeException {
    private int _errorcode;
    
    public RoutingException() {
        super();
    }

    public RoutingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoutingException(String message) {
        super(message);
    }

    public RoutingException(Throwable cause) {
        super(cause);
    }
    
    public RoutingException setErrorCode(int code) {
        _errorcode = code;
        return this;
    }
    
    public int getErrorCode() {
        return _errorcode;
    }
    


}
