package client;

import java.net.Socket;

/**
 * TUI-based client.
 * @author Andrei Cojocaru
 */
public class Client {
	
	@SuppressWarnings("unused")
	private static TUI tui;
	public static Socket s;
	public static final int port = 1337;
	
	public static void main(String[] args) {
		tui = new TUI();
	}
}
