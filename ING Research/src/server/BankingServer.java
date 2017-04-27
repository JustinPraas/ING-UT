package server;

/**
 * Receives RPC requests, processes them and sends RPC responses.
 * @author Andrei Cojocaru
 */
public class BankingServer {
	
	public static void processRequest(String message) {
		if (message.matches(BankMessages.CUSTLOGIN)) {
			customerLogin(message);
		} else if (message.matches(BankMessages.BANKLOGIN)) {
			
		}
	}

	private static void bankLogin(String message) {
		// TODO Auto-generated method stub
	}

	private static void customerLogin(String message) {
		// TODO Auto-generated method stub
		
	}
}
