package babble.net;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A buffer for data to be written on a  network  channel.
 * An user can write byte, byte array,  multi-byte data (e.g.  
 * integer or long) and character-oriented data such as string.
 * This facility can also stream data (in chunks) when number of bytes is not
 * <em>a priori</em> determinable.
 * <br>
 * The byte-oriented data is read and written through <code>read*()</code> and
 * <code>write*</code> methods. 
 * <br>
 * A buffer must be attached to a communication channel before data can be sent to a 
 * remote process. The communication channel is set on this buffer either 
 * at construction or {@link #setChannel(ByteChannel) explicitly} later. 
 * <br>
 * Because this facility use internal buffers to lower network i/o, it is
 * important to {@link #flush() flush} to end communication, otherwise
 * data that have been written may remain in buffer and not sent to remote
 * process. 
 * 
 * @author pinaki poddar
 *
 */
public class NetworkBuffer {
    private ByteChannel _channel;
    
    private final ByteBuffer _writeBuffer;

    public static final int MAX_BUFFER_SIZE   = 8*1024;
    public static final int CHUNK_BUFFER_SIZE = 1024;
    public static final byte[] CRLF_BYTES = {(byte)'\r', (byte)'\n'};
    
    
    private static Logger _logger = LoggerFactory.getLogger("channel");
    
    /**
     * Create a buffer without a network channel.
     * 
     */
    public NetworkBuffer() {
        this(null);
    }
    
    /**
     * Create a buffer with given network channel.
     */
    public NetworkBuffer(SocketChannel channel) {
        _channel = channel;
        _writeBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    }


    
    /**
     * Sets a network channel where response data will be written.
     * A channel must be set either at construction or by this method  
     * before write*() operation.
     * The write*() operations do not immediately write data to a
     * channel, but they might {@link #flush() flush} when intermediate 
     * buffer fills up and would require a channel.
     * 
     * @param channel a network channel
     */
    protected void setChannel(ByteChannel channel) {
        _channel = channel;
    }
    
    /**
     * Gets the channel on which this request had been received.
     * Can be null. 
     */
    protected final ByteChannel getChannel() {
        return _channel;
    }
    
    /**
     * Writes the given array of bytes.
     * @param bytes array of bytes. If null, no action taken.
     * @return the same buffer 
     * @throws IOException if given value can not be written
     */
    protected NetworkBuffer writeBytes(byte[] bytes) throws IOException {
        if (bytes == null ||  bytes.length == 0) return this;
        
        int n = Math.min(_writeBuffer.remaining(), bytes.length);
        _writeBuffer.put(bytes, 0, n);
        
        if (_writeBuffer.remaining() == 0) {
            flush(_writeBuffer, _channel);
        }

        if (bytes.length > n) {
            byte[] remaining = new byte[bytes.length-n];
            System.arraycopy(bytes, n, remaining, 0, remaining.length);
            writeBytes(remaining);
        };
        return this;
    }
    
    /**
     * Writes the given long value
     * @param value long number
     * @return the same buffer
     * @throws IOException if given value can not be written
     */
    protected NetworkBuffer writeLong(long value) throws IOException {
        if (_writeBuffer.remaining() < Long.BYTES) {
            flush(_writeBuffer, _channel);
        }
        _writeBuffer.putLong(value);
        return this;
    }

    /**
     * Writes the given integer value
     * @param value integer number
     * @return the same buffer
     * @throws IOException if given value can not be written
     */
    protected NetworkBuffer writeInt(int value) throws IOException {
        if (_writeBuffer.remaining() < Integer.BYTES) {
            flush(_writeBuffer, _channel);
        }
        _writeBuffer.putInt(value);
        return this;
    }
    
    /**
     * Writes the given strings. The non-null strings are written as 
     * array of bytes.  Encoding is applied.
     * 
     * @param values an array of strings. If null, no action is taken.
     * If an element is null, it is skipped. 
     * @return the same buffer
     * @throws IOException if given values can not be written
     */
    protected NetworkBuffer writeString(Charset encoding, String... values) throws IOException {
        if (values == null) return this;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                writeBytes(values[i].getBytes(encoding));
            }
        }
        return this;
    }
    
    protected NetworkBuffer writeCRLF() throws IOException {
        return writeBytes(CRLF_BYTES);
    }
    
    protected NetworkBuffer writeString(String... values) throws IOException {
        return writeString(Charset.defaultCharset(), values);
    }
    
    protected NetworkBuffer writeStringInChunks(String s) throws IOException {
        byte[] bytes = s.getBytes();
        byte[] chunk = new byte[CHUNK_BUFFER_SIZE];
        int N = bytes.length/CHUNK_BUFFER_SIZE;
        for (int i = 0, i0 = 0; i < N; i++, i0 += CHUNK_BUFFER_SIZE) {
            System.arraycopy(bytes, i0, chunk, 0, CHUNK_BUFFER_SIZE);
            writeChunk(chunk);
        }
        byte[] reminder = new byte[bytes.length%CHUNK_BUFFER_SIZE];
        if (reminder.length > 0) {
            System.arraycopy(bytes, N*CHUNK_BUFFER_SIZE, 
                    reminder, 0, reminder.length);
            writeChunk(reminder);
            
        }
        
        return this;
    }

    
    /**
     * Writes the content of given path in chunk encoding.
     * @param path to an input stream. If null, no action is taken.
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    protected NetworkBuffer writeStream(Path path) throws IOException {
        if (path == null) return this;
        return writeStream(Files.newInputStream(path));
    }

    
    /**
     * Writes the content of given input stream in chunks.
     * @param in an input stream. 
     * @return the same response for chaining
     * @throws IOException if given value can not be written
     */
    protected NetworkBuffer writeStream(InputStream in) throws IOException {
         flush(_writeBuffer, _channel);
         
         byte[] buf = new byte[CHUNK_BUFFER_SIZE];
         byte[] chunk = null;
         int L = 0;
         while ((L = in.read(buf, 0, buf.length)) > 0) {
            if (L != CHUNK_BUFFER_SIZE) {
                chunk = new byte[L];
                System.arraycopy(buf, 0, chunk, 0, L);
            } else {
                chunk = buf;
            }
            writeChunk(chunk);
        }
         in.close();
        
        return this;
        
    }
     
     /**
      * writes given array as  a chunk.
      * no of bytes CRLF data CRLF
      * @param bytes
      */
     protected void writeChunk(byte[] chunk) throws IOException {
         String length = Long.toHexString(chunk.length);
         byte[] lengthAscii = length.getBytes(StandardCharsets.US_ASCII);
         
         _logger.debug("writing chunk of 0x" + length + " bytes");
         
         writeBytes(lengthAscii)
         .writeBytes(CRLF_BYTES)
         .writeBytes(chunk)
         .writeBytes(CRLF_BYTES);
     }
     
     /**
      * Flush the content on to network channel
      * 
      * @throws IOException
      */
     protected void flush() throws IOException {
         flush(_writeBuffer, _channel);
     }
     /**
      * Writes the buffered bytes to network channel.
      * The buffer is cleared after write.
      * 
      * @param buffer the bye buffer to be written
      * @param channel a writable network channel
      * @throws IOException
      */
     private void flush(ByteBuffer buffer, ByteChannel channel) throws IOException {
         assertWritable();
         _logger.debug("flushing " + buffer.position() + " bytes");
         buffer.flip();
         channel.write(buffer);
         buffer.clear();
     }
     
     
     /**
      * Read chunk from given network channel.
      * A chunk follows a format:
      * <pre>
      * no. of bytes in chunk (as ASCII text for Hexadecimal) CRLF
      * actual bytes
      * CRLF
      * <pre>
      * @param channel readable channel of  byte data 
      * @return
      */
     protected byte[] readChunk(ByteChannel channel) throws IOException {
         int READ_BUFFER_SIZE = 32;
         ByteBuffer firstBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
         byte[] firstLine = readLine(channel, firstBuffer);
         
         String s = new String(firstLine, StandardCharsets.US_ASCII);
         long L = Long.parseLong(s, 16);
         _logger.debug("chunk length=" + L);
         
         ByteBuffer excess = firstBuffer.slice();
         _logger.debug("excess from first line=" + excess.limit() + " bytes");

         ByteBuffer content = ByteBuffer.allocate((int)L + 2);
         content.put(excess);   
         int c = channel.read(content);
         _logger.debug("read " + c + " bytes of chunk content");
         
         if (content.remaining() != 0) {
             throw new RuntimeException("did not read chunk content fully remaining " + 
                     content.remaining() + " bytes");
         }
         
         byte[] contentWithCRLF = content.array();
         byte[] contentWithoutCRLF = new byte[contentWithCRLF.length-2];
         System.arraycopy(contentWithCRLF, 0, contentWithoutCRLF, 0, 
                 contentWithoutCRLF.length);
         return contentWithoutCRLF; // chunk content
     }
     
     /**
      * Reads from given socket channel on to the given buffer.
      * The data is 
      * @param channel
      * @param buf a buffer where bytes will be read.
      * After the buffer is positioned at the beginning of excess data 
      * and limit is end of all data read.
      * byte[] bytes = new byte[buf.limit() - buf.position()];
      * byte[] excessBytes = buf.get(bytes); 
      * @return the bytes till the first occurrence of CRLF byte sequence
      * (byte 10 followed by byte 13).
      * 
      */
     protected byte[] readLine(ByteChannel channel, ByteBuffer buf) throws IOException {
         _logger.debug("reading a line from channel onto " + buf);
         int p0 = buf.position();
         int n = channel.read(buf);
         buf.limit(buf.position());
         if (n < 0) throw new IOException("channel has been closed by remote");
         if (n == 0) {
             return new byte[0];
         }
         _logger.debug("read " + n + " bytes from channel " + channel);
         buf.position(p0); // start reading from initial  buffer position
         byte ch1 = buf.get();
         byte ch2 = '\0';
         while ((ch2 = buf.get()) != '\n' && ch1 != '\r') {
             ch1 = ch2;
         }
         _logger.debug("found CRLF at buffer position " + buf.position());
         
         int mark = buf.position();
         
         byte[] line = new byte[buf.position()-p0-2];

         _logger.debug("line " + line.length + " bytes at buffer position [" + p0 + "," + buf.position() + ']');
         buf.position(p0);
         buf.get(line);
         
         buf.position(mark);
         _logger.debug("slice buffer from position " + mark);
         buf = buf.slice();
         buf.limit(n-(line.length+2));
         _logger.debug("extra data read=" + buf.limit() + " bytes");
         
         return line;
     }
     
     
     
     /**
     * Asserts that a network channel is available and is writable.
     * @throws IOException
     */
   void assertWritable() throws IOException {
       if (_channel == null) 
           throw new IOException("can not be writen " 
               + "as it is not bound to any i/o channel");
       if (!_channel.isOpen()) 
           throw new IOException("can not be written " 
                   + " as i/o channel " + _channel + " is not open");
       
   }

}
