package babble.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Arrays;

/**
 * A simple readable-writable byte channel, used for testing.
 * 
 * @author pinaki poddar
 *
 */
public class TestChannel implements ByteChannel {
    private int _position;
    private byte[] _data;
    
    /**
     * create an empty channel of given capacity.
     * @param capacity
     */
    public TestChannel(int capacity) {
        _position = 0;
        _data = new byte[capacity];
    }
    
    /**
     * create a channel filled with bytes of given string.
     * @param capacity
     */
    public TestChannel(String s) {
        _data = s.getBytes();
        _position = 0;
    }
    
    /**
     * create a new channel with its bytes in reverse order of this channel 
     * @return
     */
    TestChannel reverse() {
        TestChannel reverse = new TestChannel(this._position);
        reverse._data = new byte[this._position];
        System.arraycopy(_data, 0, reverse._data, 0, reverse._data.length);
        swapArray(reverse._data);
        reverse._position = 0;
        return reverse;
    }
    
    void swapArray(byte[] a) {
        for (int i = 0,  k = a.length-1; i < a.length; i++, k--) {
            byte b = a[i];
            a[i] = a[k];
            a[k] = b;
        }
    }

    /**
     * the bytes of the given buffer are put to this channel
     */
    @Override
    public int write(ByteBuffer dst) throws IOException {
        int n = 0;
        while (dst.remaining() > 0 && _position < _data.length) {
            n++;
            _data[_position++] = dst.get();
        }
        return n;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

    /**
     * the bytes of this channel are put to given byte buffer
     */
    @Override
    public int read(ByteBuffer src) throws IOException {
        int n = 0;
    
        while (src.remaining() > 0 && _position < _data.length) {
            n++;
            src.put(_data[_position++]);
        }
        return n;
    }
    
    /**
     * 
     * @return
     */
    public int position() {
        return _position;
    }
    public void position(int n) {
        _position = n;
    }
    
    public String toString() {
        return "pos=" + _position + " cap=" + _data.length + " " + 
                    Arrays.toString(_data);
    }

}
