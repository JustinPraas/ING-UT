package server.rest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import client.Client;

/**
 * A model that stores transient (not-persistent) data on the server side. 
 * @author Justin Praas
 */
public class ServerModel {
	
	// Extension 2: 'PIN block' related.	
	// previousPinAttempts[pincard, No. attempts]
	private HashMap<String, Integer> previousPinAttempts = new HashMap<>();
	
	// Extension 4: 'Time simulation' related
	public static final String SIMULATED_DAYS_FILE_PATH = Client.DESKTOP_ING_FOLDER_PATH + "simulatedDays.txt";
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
	


	public static int getSimulatedDays() {
		return simulatedDays;
	}

	public static void setSimulatedDays(int simulatedDays, boolean write) {
		ServerModel.simulatedDays = ServerModel.simulatedDays + simulatedDays;
		
		if (write) {
			File simulatedDaysFile = new File(SIMULATED_DAYS_FILE_PATH);
			System.out.println("Writing simulated days (" + ServerModel.simulatedDays + ") to " + simulatedDaysFile.getAbsolutePath());
			 
			try {
				Writer writer = new BufferedWriter(new FileWriter(SIMULATED_DAYS_FILE_PATH, false));
				writer.write(Integer.toString(ServerModel.simulatedDays));
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}

	public static int getSimulatedDaysFromFile() {
		try {
			return Integer.parseInt(new String(Files.readAllBytes(Paths.get(SIMULATED_DAYS_FILE_PATH))));
		} catch (IOException e) {	
			e.printStackTrace();
			return 0;
		}			
	}
	
	public static void resetSimulatedDays() {
		setSimulatedDays(-1 * simulatedDays, true);
	}

}
