package babble.net;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.ByteChannel;

import babble.net.exception.ProtocolException;


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
public abstract class Request extends NetworkBuffer implements Serializable {
    
    public Request()  {
   }

    /**
     * Create a request bound to the given network channel.
     * 
     * @param channel must not be null. May be connected. If not connected,
     * then the target host and port of the request target is undetermined.
     */
    public Request(ByteChannel channel)  {
        super();
        setChannel(channel);
   }

    public boolean isOneWay() {
        return false;
    }

     
    
    /**
     * Send this request via the given network channel.
     * 
     * @throws IOException if  can not be sent over the channel 
     */
    protected abstract void send(ByteChannel channel) throws IOException;
    
    /**
     * Receive this request from a given channel.
     * 
     * @param channel
     * @return true if content is useful (not 0-by data, for example)
     * @throws ProtocolException if content is ill-formed
     * @throws IOException if content can not be read from channel
     * 
     */
    protected abstract void receive(ByteChannel channel) 
            throws ProtocolException, IOException;
}
