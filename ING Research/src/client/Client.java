package client;

import javax.swing.filechooser.FileSystemView;

/**
 * TUI-based client.
 * @author Andrei Cojocaru
 */
public class Client {
	
	public static final String DESKTOP_ING_FOLDER_PATH = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + "\\ING-UT Justin Praas\\";
	
	@SuppressWarnings("unused")
	private static TUI tui;
	
	public static void main(String[] args) {
		tui = new TUI();
	}
}
