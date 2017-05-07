package client;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

public class MessageManager {
	public static String AUTHTOKEN;
	public static State state = State.NOT_AUTHENTICATED;
	
	public enum State {
		NOT_AUTHENTICATED,
		AUTHENTICATED;
	}
	
	public static void sendToServer(JSONRPC2Request request) {
		String message = request.toJSONString();
		System.out.println("Sending to server:");
		System.out.println();
		System.out.println(message);
		//TODO: Send HTTP POST message to server
		
		if (request.getMethod().equals("getAuthToken")) {
			//TODO: Dummy authentication -- ditch soon
			AUTHTOKEN = "TOTALLY.AUTHORIZED.YES";
			state = State.AUTHENTICATED;
		}
	}
}
