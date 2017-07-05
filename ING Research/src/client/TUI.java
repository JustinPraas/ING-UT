package client;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;

/**
 * A class that handles local textual display
 * @author Justin Praas
 * @version 2nd of April, 2017
 */
public class TUI {
	
	MessageHandler inputProcessor;

	/**
	 * Binds a <code>Session</code> object to this TUI. Start listening to input.
	 */
	public TUI() {
		
		if (Client.getSimulatedDays() > 0) { 
			System.out.println("Simulating " + Client.getSimulatedDays() + " days ahead");	
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, Client.getSimulatedDays());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			String simulatedDate = format.format(c.getTime());
			System.out.println("Simulated date: " + simulatedDate);
		} else {
			System.out.println("System date: " + Calendar.getInstance().getTime().toString());
		}
		
		inputProcessor = new MessageHandler();
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
		switch (MessageHandler.state) {
		case NOT_AUTHENTICATED: 
			System.out.println("=====================================================================================================");
			System.out.printf("%25s %1s %s %n", "Command:", "", "Parameters:");
			System.out.printf("%25s %1s %s %n", "LOGIN", "", "<username>:<password>");
			System.out.printf("%25s %1s %s %n", "OPEN_BANK_ACCOUNT", "", "<firstname>:<lastname>:<initials>:<dateofbirth>:<SSN>:<address>:<phonenumber>:<email>:<username>:<password>");
			System.out.printf("%25s %1s %s %n", "PAY_BY_CARD", "", "<sourceIBAN>:<targetIBAN>:<cardnumber>:<PIN>:<amount>");
			System.out.printf("%25s %1s %s %n", "DEPOSIT", "", "<IBAN>:<cardnumber>:<PIN>:<amount>");
			System.out.printf("%25s %1s %s %n", "SIMULATE_TIME", "", "<nrOfDays>");
			System.out.printf("%25s %1s %s %n", "RESET", "", "");
			System.out.printf("%25s %1s %s %n", "GET_DATE", "", "");
			System.out.printf("%25s %1s %s %n", "GET_EVENT_LOGS", "", "<startDate>:<endDate> (format = yyyy-MM-dd)");
			System.out.printf("%25s %1s %s %n", "EXIT", "", "");

			break;
		case AUTHENTICATED:			
			System.out.println("=====================================================================================================");
			System.out.printf("%25s %1s %s %n", "Command:", "", "Parameters:");
			System.out.printf("%25s %1s %s %n", "OPEN_ADDITIONAL_ACCOUNT", "", "");
			System.out.printf("%25s %1s %s %n", "PAY_BY_CARD", "", "<sourceIBAN>:<targetIBAN>:<cardnumber>:<PIN>:<amount>");
			System.out.printf("%25s %1s %s %n", "INVALIDATE_PIN_CARD", "", "<IBAN>:<nrOfPinCard>:<New Pin Code: yes | no>");
			System.out.printf("%25s %1s %s %n", "DEPOSIT", "", "<IBAN>:<cardnumber>:<PIN>:<amount>");
			System.out.printf("%25s %1s %s %n", "TRANSACTION_OVERVIEW", "", "<IBAN>:<nrOfTransactions>");
			System.out.printf("%25s %1s %s %n", "GET_BALANCE", "", "<IBAN>");
			System.out.printf("%25s %1s %s %n", "TRANSFER", "", "<sourceIBAN>:<destinationIBAN>:<targetName>:<amount>:<description>");
			System.out.printf("%25s %1s %s %n", "ADD_OWNER", "", "<IBAN>:<username>");
			System.out.printf("%25s %1s %s %n", "REMOVE_OWNER", "", "<IBAN>[:username]");
			System.out.printf("%25s %1s %s %n", "GET_USER_ACCESS", "", "");
			System.out.printf("%25s %1s %s %n", "GET_BANK_ACCOUNT_ACCESS", "", "<IBAN>");
			System.out.printf("%25s %1s %s %n", "CLOSE", "", "<IBAN>");
			System.out.printf("%25s %1s %s %n", "SIMULATE_TIME", "", "<nrOfDays>");
			System.out.printf("%25s %1s %s %n", "GET_DATE", "", "");
			System.out.printf("%25s %1s %s %n", "UNBLOCK_PINCARD", "", "<IBAN>:<cardNumber>");
			System.out.printf("%25s %1s %s %n", "GET_OVERDRAFT_LIMIT", "", "<IBAN>");
			System.out.printf("%25s %1s %s %n", "SET_OVERDRAFT_LIMIT", "", "<IBAN>:<overdraftLimit>");
			System.out.printf("%25s %1s %s %n", "OPEN_SAVINGS_ACCOUNT", "", "<IBAN>");
			System.out.printf("%25s %1s %s %n", "CLOSE_SAVINGS_ACCOUNT", "", "<IBAN>");
			System.out.printf("%25s %1s %s %n", "GET_EVENT_LOGS", "", "<startDate>:<endDate> (format = yyyy-MM-dd)");
			System.out.printf("%25s %1s %s %n", "EXIT", "", "");
			break;			
		}
	}
}
