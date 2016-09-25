package babble.net.http;


import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import babble.net.ResponseCallback;

@SuppressWarnings("serial")
public class MultipartHttpResponse extends HttpResponse {
    LinkedList<Object> _parts;

    public MultipartHttpResponse(HttpRequest request) {
        super(request);
    }
    
    
    /**
     * Adds content to a part of a multi-part response.
     * @param index 0-based index of the part where content is to be added.
     * A part will be created if it does not exist
     * @param content the content to be set or appended to the part
     */
    public void addPart(int index, Object content) throws IOException {
        Object part = _parts.get(index);
        if (part == null) {
            assertContentAllowedInParts(content);
            _parts.add(index, convertToAllowedContentType(content));
        } else {
            appendContentToPart(content, part);
        }
    }
    
    void assertContentAllowedInParts(Object content) {
        if (InputStream.class.isInstance(content)
         || Path.class.isInstance(content)       
         || StringBuilder.class.isInstance(content)
         || StringBuffer.class.isInstance(content)
         || String.class.isInstance(content)) {
             return;
         }
         throw new IllegalArgumentException("Content of type " 
                 + content.getClass().getName() + " is not allowed "
                 + " in multi-part HTTP response. Allowed content are "
                 + " String, StringBuilder, StringBuffer, InputStream and Path");
    }
    
    Object convertToAllowedContentType(Object content) throws IOException {
        if (InputStream.class.isInstance(content)) return content;
        if (Path.class.isInstance(content)) {
            return Files.newInputStream((Path)content);
        }
        if (StringBuilder.class.isInstance(content)) {
            return content;
        }
        if (StringBuffer.class.isInstance(content)) {
            return new StringBuilder(content.toString());
        }
        if (String.class.isInstance(content)) {
            return new StringBuilder(content.toString());
        }
        throw new IllegalArgumentException("Content of type " 
                + content.getClass().getName() + " is not allowed "
                + " in multi-part HTTP response. Allowed content are "
                + " String, StringBuilder, StringBuffer, InputStream and Path");
    }
    
    void appendContentToPart(Object content, Object part) throws IOException {
        if (InputStream.class.isInstance(part)) {
            throw new IllegalArgumentException("Can not add content to stream part");
        } else {
            StringBuilder existing = (StringBuilder)part;
            if (StringBuilder.class.isInstance(content)) {
                existing.append(content);
            } else if (StringBuffer.class.isInstance(content)) {
                existing.append(content);
            } else if (String.class.isInstance(content)) {
                existing.append(content);
            } else {
                throw new IllegalArgumentException("Content of type " 
                + content.getClass().getName() + " can not be appended "
                + " in exsiting part. Allowed content to append are "
                + " String, StringBuilder, StringBuffer");
            }
        }
    }

    

    
    protected void writeBody() throws IOException {
        if (_parts == null || _parts.isEmpty()) return;
        _logger.debug("writing body "+ _parts.size() + " parts");
        for (int i = 0; i < _parts.size(); i++) {
            Object part = _parts.get(i);
            if (part instanceof StringBuilder) {
                writeStringInChunks(part.toString());
            } else if (part instanceof InputStream) {
                writeStream((InputStream)part);
            } else {
                throw new RuntimeException("unrecognized type of part " + part);
            }
        }
        _logger.debug("ending with last chunk");
        writeChunk(new byte[0]);
    }

    @Override
    protected void receive(ByteChannel channel, ResponseCallback cb) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void appendBody(String body) throws IOException {
        addPart(_parts.size()-1, body);
    }


    @Override
    public void setBody(Path path) throws IOException {
        addPart(_parts.size(), path);
    }
    
    
    public void addFailedPart(int index, Exception ex) {
        _parts.get(index);
    }

}
