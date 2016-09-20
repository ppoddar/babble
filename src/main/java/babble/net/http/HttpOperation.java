package babble.net.http;


import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import babble.net.Route;


/**
 * An HTTP operation processes a {@link HttpRequest request} to generate 
 * a {@link HttpResponse response}. The request is often processed in
 * a separate thread. 
 * 
 * An operation is specified by a method and an URL pattern.
 * The request to be processed and the execution context are not specified
 * at initialization, but they must be specified before invoking this
 * operation. 
 * 
 * @author pinaki poddar
 *
 */
public abstract class HttpOperation implements Route<HttpRequest,HttpResponse> {
    private final String _method;
    private final String _urlPattern;
    private final Pattern _regex;
    

    public static final List<String> ALLOWED_METHODS = 
            Arrays.asList("GET", "POST","PUT", "DELETE", "HEAD", "OPTION");

    public static final String CRLF = "\r\n";
    public static final String SP = " ";
    
    public static final int STATUS_OK = 200;
    public static final int STATUS_MISDIRECTED = 421;
    public static final int STATUS_BAD_REQUEST = 400;
    
    /**
     * Create a request to handle given given HTTP method at any path.
     * @param method one of HTTP method name e.g. 'GET', 'POST', PUT etc.
     *   */
    protected HttpOperation(String method) {
        this(method, null);
    }
    
    /**
     * Create a request to handle given given HTTP method at given path.
     * 
     * @param method one of HTTP method name e.g. 'GET', 'POST', PUT etc.
     * @param pattern a regular expression pattern. If a request URI
     * {@link HttpRequest#getPath() path} matches the given pattern, then
     * this route will be invoked to handle the request.
     * 
     * A null pattern matches any path.
     * 
     * The pattern follows Java regular expression syntax. For example,
     * regular expression to match any path is <code>.*</code>.
     *   
     */
    protected HttpOperation(String method, String pattern) {
        if (method == null 
        || !ALLOWED_METHODS.contains(method.toUpperCase()))
            throw new IllegalArgumentException("invalid method " + method);
        
        _method = method;
        _urlPattern = pattern;
        _regex = (pattern != null) ? Pattern.compile(pattern) : null;
    }
        
    /**
     * Gets the method to be invoked by this request.
     */
    public final String getMethod() {
        return _method;
    }
    
    /**
     * Gets the regular expression to match request path 
     * @return can be null to indicate that all paths match.
     */
    public final String getMatchingPattern() {
        return _urlPattern;
    }
    
    
    /**
     * Affirms if the given method and path matches the method and pattern
     * of this route.
     */
    @Override
    public final boolean matches(HttpRequest request) {
        if (!getMethod().equalsIgnoreCase(request.getMethod()))
            return false;
        if (_regex == null) {
            return true;
        } else {
            return _regex.matcher(request.getPath()).matches();
        }
    }
    
    
    public String toString() {
        return getMethod() + " " 
            + (getMatchingPattern() == null ? "" : getMatchingPattern());
    }
}
