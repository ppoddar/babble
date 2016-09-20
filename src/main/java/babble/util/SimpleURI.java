package babble.util;

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A URI that can parse from a string with a protocol of arbitrary name.
 * 
 * @author pinaki poddar
 *
 */
public class SimpleURI {
    String _protocol;
    String _host;
    int _port;
    
    private String _urlRegex = "(?<protocol>\\S+?://)?"
                             + "(?<host>\\S+?:)?"
                             + "(?<port>\\d+)";
    
    
    public SimpleURI(String url) throws URISyntaxException {
        Matcher matcher = Pattern.compile(_urlRegex).matcher(url);
        if (matcher.matches()) {
            _protocol = trimEnd(matcher.group("protocol"), "://".length());
            _host = trimEnd(matcher.group("host"), ":".length());
            _port = Integer.parseInt(matcher.group("port"));
        } else {
            throw new URISyntaxException(url, url 
                    + " does not match protocol://host:port pattern");
        }
    }
    
    String trimEnd(String s, int n) {
        if (s == null) return s;
        int M = s.length();
        return s.substring(0, Math.min(0,M-n));
    }
    
    public String getProtocol() {
        return _protocol;
    }
    
    public String getHost() {
        return _host;
    }
    
    public int getPort() {
        return _port;
    }

}
