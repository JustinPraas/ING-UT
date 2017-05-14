package client;

import java.util.Scanner;

import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;

/**
 * A class that handles local textual display
 * @author Justin Praas
 * @version 2nd of April, 2017
 */
public class TUI {
	
	InputProcessor inputProcessor;

	/**
	 * Binds a <code>Session</code> object to this TUI. Start listening to input.
	 */
	public TUI() {
		inputProcessor = new InputProcessor();
		try {
			listen();
		} catch (IllegalAmountException e) {
			System.err.println(e.toString());
		} catch (IllegalTransferException e) {
			System.err.println(e.toString());
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
			inputProcessor.processInput(input);
		}	
		
		inputScanner.close();
	}

	/**
	 * Prints the available commands to the output.
	 */
	private void printCommands() {
		switch (MessageManager.state) {
		case NOT_AUTHENTICATED: 
			System.out.println("\nUse one of the following commands:"
					+ "\nLOGIN <username>:<password>"
					+ "\nOPEN_BANK_ACCOUNT <firstname>:<lastname>:<initials>:<dateofbirth>:<SSN>:<address>:<phonenumber>:<email>:<username>:<password>"
					+ "\nPAY_BY_CARD <sourceIBAN>:<targetIBAN>:<cardnumber>:<PIN>:<amount>"
					+ "\nDEPOSIT <IBAN>:<cardnumber>:<PIN>:<amount>"
					+ "\nEXIT");
			break;
		case AUTHENTICATED:
			System.out.println("\nUse one of the following commands:"
					+ "\nOPEN_ADDITIONAL_ACCOUNT"
					+ "\nPAY_BY_CARD <sourceIBAN>:<targetIBAN>:<cardnumber>:<PIN>:<amount>"
					+ "\nDEPOSIT <IBAN>:<cardnumber>:<PIN>:<amount>"
					+ "\nTRANSACTION_OVERVIEW <IBAN>:<nrOfTransactions>"
					+ "\nGET_BALANCE <IBAN>"
					+ "\nTRANSFER <sourceIBAN>:<destinationIBAN>:<targetName>:<amount>:<description>"
					+ "\nADD_OWNER <IBAN>:<username>"
					+ "\nREMOVE_OWNER <IBAN>:<username>"
					+ "\nGET_USER_ACCESS <username>"
					+ "\nGET_BANK_ACCOUNT_ACCESS <IBAN>"
					+ "\nCLOSE <IBAN>"
					+ "\nEXIT");
			break;			
		}
	}
}
