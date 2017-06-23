package server.rest;

import java.util.HashMap;

/**
 * A model that stores transient (not-persistent) data on the server side. 
 * @author Justin Praas
 */
public class ServerModel {
	
	// Extension 2: 'PIN block' related.	
	// previousPinAttempts[pincard, No. attempts]
	private HashMap<String, Integer> previousPinAttempts = new HashMap<>();

	/**
	 * Extension 2: 'PIN block' related.
	 * Increases the number of failed attempts linked to pin card.
	 * @param cardNumber the card number that was erroneously used
	 */
	public void increaseInvalidPinAttempt(String cardNumber) {
		if (!previousPinAttempts.containsKey(cardNumber)) {
			previousPinAttempts.put(cardNumber, 1);
		} else {
			previousPinAttempts.put(cardNumber, previousPinAttempts.get(cardNumber) + 1);
		}		
	}
	
	/**
	 * Extension 2: 'PIN block' related.
	 * Gets the HashMap containing the previous pin attempts of pin cards.
	 * @return previousPinAttempts
	 */
	public HashMap<String, Integer> getPreviousPinAttempts() {
		return previousPinAttempts;
	}

}
