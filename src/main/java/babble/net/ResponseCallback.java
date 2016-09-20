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
     * Invoked when normal response is available.
     * @param bytes raw byte data of the response. Never null.
     */
    void onResponse(byte[] bytes);
    
    /**
     * Invoked when an error occurred to process a request. 
     * @param ex an exception raised during request processing.
     */
    void onError(Exception ex);

}
