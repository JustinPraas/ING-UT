package server.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import client.Client;

/**
 * A class that handles the writing and reading of config files or object files.
 * @author Justin Praas
 */
public class ServerDataHandler {

	private static final String CONFIG_PATH = Client.DESKTOP_ING_FOLDER_PATH + "config.txt";
	public static final int SIMULATED_DAYS_LINE = 0;
	public static final int PREVIOUS_NEGATIVE_INTEREST_LINE = 1;
	public static final int PREVIOUS_NEGATIVE_BALANCE_STORE_LINE = 2;
	public static final int PREVIOUS_POSITIVE_INTEREST_LINE = 3;
	public static final int PREVIOUS_POSITIVE_BALANCE_STORE_LINE= 4;

	private static final String NEGATIVE_LOWEST_DAILY_REACH_MAP_PATH = Client.DESKTOP_ING_FOLDER_PATH + "negative_daily_lowest.ser";
	private static final String POSITIVE_LOWEST_DAILY_REACH_MAP_PATH = Client.DESKTOP_ING_FOLDER_PATH + "positive_daily_lowest.ser";
	private static final String TOTAL_NEGATIVE_INTEREST_MAP_PATH = Client.DESKTOP_ING_FOLDER_PATH + "negative_interest.ser";
	private static final String TOTAL_POSITIVE_INTEREST_MAP_PATH = Client.DESKTOP_ING_FOLDER_PATH + "positive_interest.ser";
	

	/**
	 * Gets the property by reading the given line (position) in the config file.
	 * @param position the position of the line that needs to be read.
	 * @return a String representation of the property
	 */
	public static String getServerPropertyValue(int position) {
		initIfRequired();
		try {
			List<String> lines = Files.readAllLines(Paths.get(CONFIG_PATH));
			try {
				return lines.get(position);
			} catch (IndexOutOfBoundsException e) {
				return "";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";			
	}
	
	/**
	 * Sets the property of the given line (position) in the config file.
	 * @param position the position of the line that needs to be written to
	 * @param value the value that needs to be written
	 */
	public static void setServerPropertyValue(int position, String value) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(CONFIG_PATH));
			if (lines.size() < position + 1) {
				for (int i = 0; i < position + 1; i++) {
					lines.add("");
				}
			}
			lines.set(position, value);
			Files.write(Paths.get(CONFIG_PATH), lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the map of lowest daily balance reaches to the <code>daily_lowest.ser</code> file.
	 * @param negativeLowestReachMap the map to be written
	 */
	public static void setNegativeLowestDailyReachMap(HashMap<String, Double> negativeLowestReachMap) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(NEGATIVE_LOWEST_DAILY_REACH_MAP_PATH, false));
			oos.writeObject(negativeLowestReachMap);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Reads the map of lowest daily balance reaches from the <code>daily_lowest.ser</code> file.
	 * @return a map of lowest daily balance reaches
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> getNegativeLowestDailyReachMap() {
		initIfRequired();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(NEGATIVE_LOWEST_DAILY_REACH_MAP_PATH));
			HashMap<String, Double> negativeLowestDailyReachMap = (HashMap<String, Double>) ois.readObject();
			ois.close();
			return negativeLowestDailyReachMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Writes the map of lowest daily balance reaches to the <code>daily_lowest.ser</code> file.
	 * @param positiveLowestReachMap the map to be written
	 */
	public static void setPositiveLowestDailyReachMap(HashMap<String, Double> positiveLowestReachMap) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(POSITIVE_LOWEST_DAILY_REACH_MAP_PATH, false));
			oos.writeObject(positiveLowestReachMap);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Reads the map of lowest daily balance reaches from the <code>daily_lowest.ser</code> file.
	 * @return a map of lowest daily balance reaches
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> getPositiveLowestDailyReachMap() {
		initIfRequired();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(POSITIVE_LOWEST_DAILY_REACH_MAP_PATH));
			HashMap<String, Double> positiveLowestDailyReachMap = (HashMap<String, Double>) ois.readObject();
			ois.close();
			return positiveLowestDailyReachMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * Writes the map of total interests to the <code>negative_interest.ser</code> file.
	 * @param lowestReachMap the map to be written
	 */
	public static void setTotalNegativeInterestMap(HashMap<String, Double> totalInterest) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TOTAL_NEGATIVE_INTEREST_MAP_PATH, false));
			oos.writeObject(totalInterest);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Reads the map of total interest from the <code>negative_interest.ser</code> file.
	 * @return a map of total interests
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> getTotalNegativeInterestMap() {
		initIfRequired();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TOTAL_NEGATIVE_INTEREST_MAP_PATH));
			HashMap<String, Double> totalInterestMap = (HashMap<String, Double>) ois.readObject();
			ois.close();
			return totalInterestMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Writes the map of total interests to the <code>positive_interest.ser</code> file.
	 * @param lowestReachMap the map to be written
	 */
	public static void setTotalPositiveInterestMap(HashMap<String, Double> totalInterest) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TOTAL_POSITIVE_INTEREST_MAP_PATH, false));
			oos.writeObject(totalInterest);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Reads the map of total interest from the <code>positive_interest.ser</code> file.
	 * @return a map of total interests
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> getTotalPositiveInterestMap() {
		initIfRequired();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TOTAL_POSITIVE_INTEREST_MAP_PATH));
			HashMap<String, Double> totalInterestMap = (HashMap<String, Double>) ois.readObject();
			ois.close();
			return totalInterestMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Checks if the necessary files exists and if not, create them.
	 */
	private static void initIfRequired() {
		File negativeLowestReachMapFile = new File(NEGATIVE_LOWEST_DAILY_REACH_MAP_PATH);
		if (!negativeLowestReachMapFile.exists()) {
			try {
				negativeLowestReachMapFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			InterestHandler.setNegativeLowestDailyReachMap(new HashMap<String, Double>());
		}
		
		File totalNegativeInterestMap = new File(TOTAL_NEGATIVE_INTEREST_MAP_PATH);
		if (!totalNegativeInterestMap.exists()) {
			try {
				totalNegativeInterestMap.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			InterestHandler.setTotalNegativeInterestMap(new HashMap<String, Double>());
		}
		
		File positiveLowestReachMapFile = new File(POSITIVE_LOWEST_DAILY_REACH_MAP_PATH);
		if (!positiveLowestReachMapFile.exists()) {
			try {
				positiveLowestReachMapFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//InterestHandler.setPositiveLowestDailyReachMap(new HashMap<String, Double>());
		}
		
		File totalPositiveInterestMap = new File(TOTAL_POSITIVE_INTEREST_MAP_PATH);
		if (!totalPositiveInterestMap.exists()) {
			try {
				totalPositiveInterestMap.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//InterestHandler.setTotalPositiveInterestMap(new HashMap<String, Double>());
		}		
		
		File configFile = new File(CONFIG_PATH);
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			setServerPropertyValue(SIMULATED_DAYS_LINE, Integer.toString(ServerModel.getSimulatedDays()));
			setServerPropertyValue(PREVIOUS_NEGATIVE_INTEREST_LINE, "0");
			setServerPropertyValue(PREVIOUS_NEGATIVE_BALANCE_STORE_LINE, "0");
		}
		
	}
}
