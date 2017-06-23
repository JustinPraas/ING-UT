package server.rest;

import java.util.HashMap;

public class ServerModel {
	
	// Extension 2	
	// previousPinAttempts[pincard, No. attempts]
	private HashMap<String, Integer> previousPinAttempts = new HashMap<>();

	public void increaseInvalidPinAttempt(String cardNumber) {
		System.out.println(previousPinAttempts);
		if (!previousPinAttempts.containsKey(cardNumber)) {
			previousPinAttempts.put(cardNumber, 1);
		} else {
			previousPinAttempts.put(cardNumber, previousPinAttempts.get(cardNumber) + 1);
		}		
	}
	
	public HashMap<String, Integer> getPreviousPinAttempts() {
		return previousPinAttempts;
	}

}
