package babble.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;

/**
 * A data structure to collect information about a network channel.
 * 
 * @author pinaki poddar
 *
 */
public class ChannelInfo {
    private String _host = "?";
    private int _port    = 0;
    
    /**
     * Gathers remote host port information about the given channel,
     * if it is connected to a remote peer.
     * 
     * @param channel a communication channel
     */
    public ChannelInfo(Channel channel) {
        this(channel, false);
    }
    
    /**
     * Creates network host/port information of a given channel.
     * If given channel is connected then information about the remote
     * location is collected.
     * 
     * @param channel a network channel
     * @param local get local address information
     */
    public ChannelInfo(Channel channel, boolean local) {
        if (channel == null) return;
        if (!SocketChannel.class.isInstance(channel)) return;
        SocketChannel socket = SocketChannel.class.cast(channel);
        if (!local & !socket.isConnected()) return;
        try {
            InetSocketAddress addr = (InetSocketAddress) 
                    (local ? socket.getLocalAddress() 
                           : socket.getRemoteAddress());
            _host = addr.getHostString();
            _port = addr.getPort();
        } catch (IOException ex) {
        }
    }
    
    public String getHost() {
        return _host;
    }
    
    public int getPort() {
        return _port;
    }
    
    public String toString() {
        return _host + ':' + _port;
    }

}
