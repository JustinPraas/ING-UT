package client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.filechooser.FileSystemView;

/**
 * TUI-based client.
 * @author Andrei Cojocaru
 */
public class Client {
	
	public static final String DESKTOP_ING_FOLDER_PATH = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + "\\ING-UT Justin Praas\\";
	public static final String SIMULATED_DAYS_FILE_PATH = DESKTOP_ING_FOLDER_PATH + "simulatedDays.txt";
	private static int simulatedDays = 0;
	
	@SuppressWarnings("unused")
	private static TUI tui;
	
	public static void main(String[] args) {
		simulatedDays = getSimulatedDaysFromFile();
		tui = new TUI();
	}

	public static int getSimulatedDays() {
		return simulatedDays;
	}

	public static void setSimulatedDays(int simulatedDays) {
		Client.simulatedDays = simulatedDays;
		System.out.println("Simulating " + Client.simulatedDays + " days.");
		
		File simulatedDaysFile = new File(SIMULATED_DAYS_FILE_PATH);
		System.out.println("Writing simulated days to " + simulatedDaysFile.getAbsolutePath());
		
		try {
			Writer writer = new BufferedWriter(new FileWriter(SIMULATED_DAYS_FILE_PATH, false));
			writer.write(Integer.toString(simulatedDays));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int getSimulatedDaysFromFile() {
		try {
			return Integer.parseInt(new String(Files.readAllBytes(Paths.get(SIMULATED_DAYS_FILE_PATH))));
		} catch (IOException e) {	
			e.printStackTrace();
			return 0;
		}			
	}
	
	private static void resetSimulatedDays() {
		setSimulatedDays(0);
		System.out.println("Simulating " + Client.simulatedDays + " days.");
	}
}
