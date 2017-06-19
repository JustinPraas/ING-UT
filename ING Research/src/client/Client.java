package client;

import javax.swing.filechooser.FileSystemView;

/**
 * TUI-based client.
 * @author Andrei Cojocaru
 */
public class Client {
	
	public static final String DESKTOP_ING_FOLDER_PATH = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + "\\ING-UT Justin Praas\\";
	private static int simulatedDays = 0;
	
	@SuppressWarnings("unused")
	private static TUI tui;
	
	public static void main(String[] args) {
		tui = new TUI();
	}

	public static int getSimulatedDays() {
		return simulatedDays;
	}

	public static void setSimulatedDays(int simulatedDays) {
		Client.simulatedDays = simulatedDays;
		System.out.println("Simulating " + Client.simulatedDays + " days.");
	}
	
	
}
