package babble.net.http;


import java.util.regex.Pattern;

import babble.net.Route;


/**
 * An HTTP route processes a {@link HttpRequest request} to generate 
 * a {@link HttpResponse response}. The request is often processed in
 * a separate thread. 
 * 
 * A route is specified by a method and an URL pattern.
 * 
 * @author pinaki poddar
 *
 */
public abstract class HttpRoute implements Route<HttpRequest,HttpResponse> {
    private final String _method;
    private final String _urlPattern;
    private final Pattern _regex;

    /**
     * Create a request to handle given given HTTP method at any path.
     * @param method one of HTTP method name e.g. 'GET', 'POST', PUT etc.
     *   */
    protected HttpRoute(String method) {
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
    protected HttpRoute(String method, String pattern) {
        if (method == null 
        || !HttpConstants.ALLOWED_METHODS.contains(method.toUpperCase()))
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
        return (getMethod().equalsIgnoreCase(request.getMethod()))
            && (_regex == null 
             || _regex.matcher(request.getPath()).matches());
    }
    
    
    public String toString() {
        return getMethod() + " /" 
            + (getMatchingPattern() == null ? "" : getMatchingPattern());
    }
}
