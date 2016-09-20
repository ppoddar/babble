package babble.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A response at network level is simply an array of bytes.
 * Given a writable channel, the bytes can be written to the channel.
 * <br>
 * The user of this facility can write multi-byte data of higher forms 
 * such as an integer or long value. The facility will account for 
 * network byte order (e.g BIG or LITTLE endian).
 * <br> 
 * This facility can also {@link #stream(Path) pipe} the data from
 * another resource.  
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public abstract class Response<R extends Request>  implements Serializable {
    final R _request;
    final ByteBuffer _buffer;
    private boolean _error;
    
    public static final int MAX_BUFFER_SIZE = 8*1024;
    
    /**
     * Creates an response for a given request.
     * The request must be bound to a network channel. The response will 
     * be sent via the same channel.
     * 
     * @param request the request for which this request will be created 
     * 
     * @throws IllegalArgumentException if channel is null
     */
    public Response(R request) {
        if (request == null) 
            throw new IllegalArgumentException("Can not create response for null request");
        _request = request;
        _buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    }
    
    public R getRequest() {
        return _request;
    }
    

    /**
     * Writes this response on to the given network channel.
     * 
     * @throws IllegalStateException if channel is closed or disconnected
     * 
     */
    protected void send(SocketChannel channel) throws IOException {
        //SocketChannel channel = _request.getChannel();
        if (!channel.isOpen())
            throw new IllegalStateException("Can not write to closed channel");
        if (!channel.isConnected())
            throw new IllegalStateException("Can not write to disconnected channel");
        
        _buffer.flip();
        channel.write(_buffer);
    }

    /**
     * Writes the given strings. The non-null strings are written as 
     * array of bytes. No encoding is applied.
     * 
     * @param values an array of strings. If null, no action is taken.
     * If an element is null, it is skipped. 
     * @return the same response for chaining
     * @throws IOException if given values can not be written
     */
    public Response<R> write(String... values) throws IOException {
        if (values == null) return this;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                _buffer.put(values[i].getBytes());
            }
        }
        return this;
    }

    /**
     * Writes the given long value
     * @param value long number
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    public Response<R> write(long value) throws IOException {
        _buffer.putLong(value);
        return this;
    }
    
    /**
     * Writes the given integer value
     * @param value integer number
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    public Response<R> write(int value) throws IOException {
        _buffer.putInt(value);
        return this;
    }
    
    /**
     * Writes the given array of bytes.
     * @param bytes array of bytes. If null, no action taken.
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    public Response<R> writeBytes(byte[] bytes) throws IOException {
        if (bytes != null) {
            _buffer.put(bytes);
        }
        return this;
    }
    
    /**
     * Writes the content of given path.
     * @param path to an input stream. If null, no action is taken.
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    public Response<R> stream(Path path) throws IOException {
        if (path == null) return this;
        return stream(Files.newInputStream(path), true);
    }

    
    /**
     * Writes the content of given input stream.
     * @param in an input stream. 
     * @param close if true closes the input stream
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    Response<R> stream(InputStream in, boolean close) throws IOException {
        if (in == null) return this;
        int b = -1;
        while ((b = in.read()) != -1) {
            _buffer.put((byte)b);
        }
        if (close) {
            in.close();
        }
        return this;
    }
    
    /**
     * Affirms if this response represent an error condition.
     */
    public boolean isError() {
        return _error;
    }
    
    /**
     * Mark this response as an error response
     */
    public Response<R> markError() {
         _error = true;
         return this;
    }

}
