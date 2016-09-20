package babble.net;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;


/**
 * A request carries input data required execute an operation.
 * Result of the operation is a response. Request and Response are integral
 * part of a communication process.
 * 
 * Request and Response are related. To create a response, a request is required.
 * 
 * Request is not intrinsically network aware or bound to a network channel.
 * However, a request can be communicated over a network channel, 
 * so is the response. 
 * The response of a network-bound request is sent over the same channel.
 * 
 * The response may not be available immediately. The communication process
 * must account for such asynchronous nature. For example, the
 * communication process should not wait indefinitely on a network channel
 * for a response and  thus block the channel for other use.
 * 
 * 
 * @author pinaki poddar
 *
 */
@SuppressWarnings("serial")
public abstract class Request  implements Serializable {
    private SocketChannel    _channel;
    private ResponseCallback _callback;
    
    /**
     * Create a request bound to the given network channel.
     * 
     * @param channel must not be null. May be connected. If not connected,
     * then the target host and port of the request target is undetermined.
     */
    public Request(SocketChannel channel)  {
        _channel = channel;
   }

    public boolean isOneWay() {
        return false;
    }

    /**
     * Gets the channel on which this request had been received.
     * Can be null. 
     * The response ill be sent on the same channel.
     */
    public final SocketChannel getChannel() {
        return _channel;
    }
    
    /**
     * Sets the callback function that would be invoked when the asynchronous
     * response becomes available.
     * 
     * @param cb a callback function
     */
    void setResponseCallback(ResponseCallback cb) {
        _callback = cb;
    }
    
    /**
     * Gets the callback function that would be invoked when the asynchronous
     * response becomes available.
     * 
     * @return  a callback function
     */
    ResponseCallback getResponseCallback() {
        return _callback;
    }
    
    /**
     * Send this request via the given network channel.
     * 
     * @throws IOException if  can not be sent over the channel 
     */
    protected abstract void send(SocketChannel channel) throws IOException;
    
}
