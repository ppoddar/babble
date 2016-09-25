package babble.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import babble.net.exception.ProtocolException;
import babble.net.http.HttpRequest;
import babble.net.http.HttpResponse;
import babble.net.http.SinglePartHttpResponse;
import babble.service.ServiceMaker;

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
    public void testHttpRequestLineRequiresThreeTokens() throws IOException {
        HttpRequest r = new HttpRequest("GET some-path HTTP/1.1");
        assertEquals("GET",       r.getMethod());
        assertEquals("some-path", r.getPath());
        assertEquals("HTTP/1.1",  r.getVersionString());
    }
    
    @Test
    public void testHttpRequestLineInvalidMethod() throws IOException {
        try {
            HttpRequest r = new HttpRequest("GOT some-path HTTP/1.1");
            fail("Expected protocol exception for invalid method");
        } catch (ProtocolException ex) {
            
        }
    }
    
    @Test
    public void testHttpRequestLineInvalidVersion() throws IOException {
        try {
            HttpRequest r = new HttpRequest("GOT some-path HTTP/1.b");
            fail("Expected protocol exception for invalid version");
        } catch (ProtocolException ex) {
            
        }
    }


    
    @Test
    public void testResponseRequiresChannelToFlush() throws IOException {
        HttpRequest request = new HttpRequest("get", "something");
        HttpResponse response = new SinglePartHttpResponse(request);
        String s = " a string for testing";
        
        assertNull(response.getChannel());
        // response can be written
        response.writeString(s);
        
        try {
            // but can not be flushed
            response.flush();
            Assert.fail("Expected to fail whil eflusing without a channel");
        } catch (IllegalStateException ex) {
            
        }
        
    }
    
    
    @Test
    public void testWrapService() throws Exception {
        new ServiceMaker().wrap(new HashMap<>());
    }
}
