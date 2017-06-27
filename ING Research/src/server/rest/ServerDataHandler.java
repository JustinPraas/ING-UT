package server.rest;

import java.io.EOFException;
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

public class ServerDataHandler {

	private static final String CONFIG_PATH = Client.DESKTOP_ING_FOLDER_PATH + "config.txt";
	public static final int SIMULATED_DAYS_LINE = 0;
	public static final int PREVIOUS_INTEREST_LINE = 1;
	public static final int PREVIOUS_BALANCE_STORE_LINE = 2;
	
	private static final String LOWEST_DAILY_REACH_MAP_PATH = Client.DESKTOP_ING_FOLDER_PATH + "daily_lowest.ser";
	private static final String TOTAL_INTEREST_MAP_PATH = Client.DESKTOP_ING_FOLDER_PATH + "total_interest.ser";

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

	public static void setLowestDailyReachMap(HashMap<String, Double> lowestReachMap) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOWEST_DAILY_REACH_MAP_PATH, false));
			oos.writeObject(lowestReachMap);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> getLowestDailyReachMap() {
		initIfRequired();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(LOWEST_DAILY_REACH_MAP_PATH));
			HashMap<String, Double> lowestDailyReachMap = (HashMap<String, Double>) ois.readObject();
			ois.close();
			return lowestDailyReachMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void setTotalInterestMap(HashMap<String, Double> totalInterest) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TOTAL_INTEREST_MAP_PATH, false));
			oos.writeObject(totalInterest);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, Double> getTotalInterestMap() {
		initIfRequired();
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TOTAL_INTEREST_MAP_PATH));
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

	private static void initIfRequired() {
		File lowestReachMapFile = new File(LOWEST_DAILY_REACH_MAP_PATH);
		if (!lowestReachMapFile.exists()) {
			try {
				lowestReachMapFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			InterestHandler.setLowestDailyReachMap(new HashMap<String, Double>());
		}
		
		File totalInterestMap = new File(TOTAL_INTEREST_MAP_PATH);
		if (!totalInterestMap.exists()) {
			try {
				totalInterestMap.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			InterestHandler.setTotalInterestMap(new HashMap<String, Double>());
		}
		
		File configFile = new File(CONFIG_PATH);
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			setServerPropertyValue(SIMULATED_DAYS_LINE, Integer.toString(ServerModel.getSimulatedDays()));
			setServerPropertyValue(PREVIOUS_INTEREST_LINE, "0");
			setServerPropertyValue(PREVIOUS_BALANCE_STORE_LINE, "0");
		}
		
	}
}
