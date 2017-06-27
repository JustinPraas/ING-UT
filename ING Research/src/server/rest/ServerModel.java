package server.rest;

import java.util.Calendar;
import java.util.HashMap;

/**
 * A model that stores transient (not-persistent) data on the server side. 
 * @author Justin Praas
 */
public class ServerModel {
	
	// Extension 2: 'PIN block' related.	
	// previousPinAttempts[pincard, No. attempts]
	private HashMap<String, Integer> previousPinAttempts = new HashMap<>();
	
	// Extension 4: 'Time simulation' related
	private static int simulatedDays = 0;
	
	public ServerModel() {
		simulatedDays = getSimulatedDaysFromFile();
	}

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
	
	public static Calendar getServerCalendar() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, simulatedDays);
		return c;
	}

	public static int getSimulatedDays() {
		return simulatedDays;
	}

	public static void setSimulatedDays(int simulatedDays, boolean write) {
		ServerModel.simulatedDays = ServerModel.simulatedDays + simulatedDays;
		
		if (write) {
			ServerDataHandler.setServerPropertyValue(ServerDataHandler.SIMULATED_DAYS_LINE, 
					Integer.toString(ServerModel.simulatedDays));
			System.out.println("Writing simulated days: " + ServerModel.simulatedDays + ".");
			
		}		
	}

	public static int getSimulatedDaysFromFile() {
		return Integer.parseInt(ServerDataHandler.getServerPropertyValue(ServerDataHandler.SIMULATED_DAYS_LINE));		
	}
	
	public static void resetSimulatedDays() {
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.SIMULATED_DAYS_LINE, "0");
		simulatedDays = 0;
	}

}
