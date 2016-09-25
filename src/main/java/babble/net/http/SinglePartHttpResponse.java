package babble.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import babble.net.ResponseCallback;

@SuppressWarnings("serial")
public class SinglePartHttpResponse extends HttpResponse {
    private Object _body;
    
    public SinglePartHttpResponse(HttpRequest request) {
        super(request);
    }

    

    @Override
    protected void receive(ByteChannel channel, ResponseCallback cb) {
        // TODO Auto-generated method stub
        
    }



    @Override
    public void appendBody(String body) throws IOException {
        if (_body == null) {
            _body = new StringBuilder(body);
        } else if (_body instanceof StringBuilder){
            ((StringBuilder)_body).append(body);
        } else {
            throw new IllegalArgumentException("Can not append body");
        }
        
    }



    @Override
    public void setBody(Path path) throws IOException {
        _body = Files.newInputStream(path);
    }



    @Override
    protected void writeBody() throws IOException {
        if (_body instanceof StringBuilder) {
            writeStringInChunks(_body.toString());
        } else if (_body instanceof InputStream) {
            writeStream((InputStream)_body);
        }
    }

}
