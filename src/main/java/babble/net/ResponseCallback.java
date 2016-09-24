package babble.net;

/**
 * A callback function to receive a {@link Response response}.
 * This function is passed to {@link NioClient#sendRequest(Request, ResponseCallback)
 * make} a network request. The response invokes callback methods of
 * this function.
 * 
 * @author pinaki poddar
 *
 */
public interface ResponseCallback {
    /**
     * Invoked when response is available. For streaming response, this
     * can be invoked multiple times to deliver chunks of responses. The
     * end-of-stream is signaled by the boolean parameter.
     * 
     * @param bytes raw byte data of the response. Never null.
     * @param eos true indicates end of stream
     */
    void onResponse(byte[] bytes, boolean eos);
    
    /**
     * Invoked when an error occurred to process a request. 
     * @param ex an exception raised during request processing.
     */
    void onError(Exception ex);

}
