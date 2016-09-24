package babble.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * A response at network level is simply an array of bytes.
 * <br>
 * This facility sends the bytes over a network channel 
 * (and buffers them for efficiency). 
 * This facility is not aware of ordering of the bytes or their semantics,
 * a protocol-specific response writes the bytes in order of their meaning
 * as per a particular protocol specification.
 * 
 * <p>
 * This operation buffers the bytes and flushes the internal buffer when 
 * it fills up. The flush may fail if this response is not associated with
 * a writable channel. 
 * 
 * <p>
 * A response is <em>implicitly</em> associated at construction 
 * with the same network channel of the request that originated it. 
 * Hence, by default, the response is written to the same network channel
 * of an incoming request. However, a protocol may require that response is
 * written to a network channel different than that of the request. 
 * In such case, the protocol must {@link #setChannel(SocketChannel) set
 * a channel} before write operations.   
 *  
 * <p>
 * <b>Writing different data types</b>: 
 * <ul>
 * <li><b>Multi-Byte Data</b>:
 * multi-byte data such as an integer or long value can be wriiten. 
 * The facility accounts for network byte order (e.g BIG or LITTLE endian).
 * <li><b>String</b>:
 * Strings are written as array of bytes using the encoding scheme set by
 * the user. The default encoding scheme the default encoding scheme
 * of the Java Virtual Machine. 
 * <li><b>Stream</b>: data form a {@link Response#writeStream(Path) resource 
 * path} or {@link #writeStream(InputStream, boolean) other stream} can be
 * streamed.
 * 
 * </ul>
 * 
 * This facility can also {@link #writeStream(Path) pipe} the data from
 * another resource.  
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public abstract class Response extends NetworkBuffer implements Serializable {
    private final Request _request;
    
    /**
     * Creates an response for a given request.
     * 
     * @param request the request for which this request will be created.
     * Must not be null. 
     * 
     * @throws IllegalArgumentException if request is null
     */
    public Response(Request r) {
        if (r == null) throw new IllegalArgumentException("Can not create "
                + "response for null request");
        _request = r;
    }
    
    public Request getRequest() {
        return _request;
    }
    
    /**
     * Sends the data over a channel. The protocol will set
     * {@link #setChannel(SocketChannel) channel}, invoke one or more
     * write operations and then call this method to complete sending the response
     * over channel.
     * 
     * @param channel a communication channel.
     * @throws IOException
     */
    protected abstract void send(ByteChannel channel) throws IOException;
    
    /**
     * Receives data over a given channel.
     * @param channel
     * @param cb if not null, then invokes the callback function as new 
     * data arrives or exception is raised
     * @throws IOException
     */
    protected abstract void receive(ByteChannel channel, ResponseCallback cb);

}
