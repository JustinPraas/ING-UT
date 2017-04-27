package userinterface;

import java.util.Scanner;

import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import server.BankingServer;

/**
 * A class that handles local textual display
 * @author Justin Praas
 * @version 2nd of April, 2017
 */
public class TUI {
	
	BankingServer server;

	/**
	 * Binds a <code>Session</code> object to this TUI. Start listening to input.
	 */
	public TUI() {
		server = new BankingServer();
		try {
			listen();
		} catch (IllegalAmountException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalTransferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Occupies the main thread, listens to input and processes the input.
	 * @throws IllegalAmountException thrown when the processing methods perform an illegal operation
	 * @throws IllegalTransferException thrown when the processing methods perform an illegal operation
	 */
	private void listen() throws IllegalAmountException, IllegalTransferException {
		boolean continues = true;
		Scanner inputScanner = new Scanner(System.in);
		String input;
		
		while (continues) {
			printCommands();
			input = inputScanner.nextLine();
			server.processInput(input);
		}	
		
		inputScanner.close();
	}

	/**
	 * Prints the available commands to the output.
	 */
	private void printCommands() {
		switch (server.session.state) {
		case LOGGED_OUT: 
			System.out.println("\nUse one of the following commands:"
					+ "\nCUST_LOGIN <BSN>, "
					+ "\nCREATE_CUSTOMER_ACCOUNT <BSN> <firstname> <surname> <streetaddress> <email> <phonenumber> <birthdate>"
					+ "\nEXIT");
			break;
		case CUST_LOGGED_IN:
			System.out.println("\nUse one of the following commands: "
					+ "\nBANK_LOGIN <number_from_list>"
					+ "\nCREATE_BANK_ACCOUNT"
					+ "\nLIST_BANK_ACCOUNTS"
					+ "\nCUST_LOGOUT"
					+ "\nEXIT");
			break;
		case BANK_LOGGED_IN:
			System.out.println("\nUse one of the following commands: "
					+ "\nTRANSACTIONS"
					+ "\nINFO"
					+ "\nDEPOSIT <amount>"
					+ "\nTRANSFER <destination IBAN> <amount>"
					+ "\nCLOSE"
					+ "\nBANK_LOGOUT"
					+ "\nEXIT");
			break;			
		}
	}
}
