package babble.net.http;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import babble.net.ExecutionContext;

/**
 * Streams a resource of a given HTTP GET request path as response.
 * Matches all requests.
 * 
 * @author pinaki poddar
 *
 */
public class DefaultGet extends HttpRoute {

    public DefaultGet() {
        super("GET", ".*");
    }

    @Override
    public HttpResponse execute(ExecutionContext ctx, HttpRequest request) throws Exception {
        
        HttpResponse response = new SinglePartHttpResponse(request);
        
        Path path = Paths.get(request.getPath());
        response.addHeader(HttpConstants.HEADER_CONTENT_TYPE, 
                guessMimeType(path));

        if (path.toFile().exists() && !path.toFile().isDirectory()) {
            response.setBody(path);
        } else if (path.toFile().isDirectory()) {
            response.setStatus(403, request.getPath() + " is a directory");
            throw new IllegalArgumentException(request.getPath() + " is a directory");
        } else {
            response.setStatus(404, request.getPath() + " not found");
            throw new FileNotFoundException(request.getPath() + " not found");
        }
        return response;
    }
    
    
    static String guessMimeType(Path path) {
        String mimeType = null;//Files.probeContentType(path);
        if (mimeType == null) {
            String fileName = path.toFile().getName();
            int idx = fileName.lastIndexOf('.');
            if (idx == -1) return "plain/text";
            String ext = fileName.substring(idx+1);
            if ("xml".equalsIgnoreCase(ext)) return "application/xml";
        }
        return "plain/text";
    }
}
