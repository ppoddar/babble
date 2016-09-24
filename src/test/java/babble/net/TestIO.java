package babble.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.BeforeClass;
import org.junit.Test;

import babble.net.http.HttpRequest;
import babble.net.http.HttpResponse;

public class TestIO {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }
    
    @Test
    public void testReadWriteNetworkBufferOnTestChannel() throws Exception {
        String s = "some text";
        int capacity = s.getBytes().length;
        TestChannel channel = new TestChannel(s);
        
        ByteBuffer src = ByteBuffer.wrap(s.getBytes());
        
        // transfer string content to channel
        channel.write(src);
        
        assertEquals(capacity, channel.position());
        
        ByteBuffer dst = ByteBuffer.allocate(capacity); 
        channel.position(0);
        
        // read string from channel to dst buffer 
        channel.read(dst);
        
        String s2 = new String(dst.array());
        
        assertEquals(s, s2);
        
        channel.close();
    }

    @Test
    public void testCharacterEncoding() {
        String s = "some ascill text";
        char[] chars = s.toCharArray();
        CharBuffer cBuf = CharBuffer.wrap(s);
        ByteBuffer buf = StandardCharsets.US_ASCII.encode(cBuf);
        
        assertEquals("",  chars.length, cBuf.limit());
        assertEquals("",  buf.limit(), cBuf.limit());
    }
    
    @Test
    public void testChunk() throws IOException {
        String chunk = "this is a chunk";
        int L = chunk.getBytes().length;
        NetworkBuffer buf = new NetworkBuffer();
        TestChannel channel = new TestChannel(24);
        buf.setChannel(channel);
        buf.writeChunk(chunk.getBytes());
        buf.flush();
        
        //System.err.println("Channel after write " + channel);
        assertTrue("channel position " + channel.position() 
            + " is not greater than chunk size " + L,
                channel.position() > L);
        
        TestChannel reversed = channel.reverse();
        
        //System.err.println("Channel after reverse " + reversed);
        
        byte[] s = buf.readChunk(reversed);
        String chunk2 = new String(s);
        
       
        assertEquals(chunk, chunk2);
    }
    
    @Test
    public void testResponse() throws IOException {
        HttpRequest request = new HttpRequest("get something");
        HttpResponse response = new HttpResponse(request);
        String s = " a string for testing";
        response.writeString(s);
        response.flush();
        
    }
}
