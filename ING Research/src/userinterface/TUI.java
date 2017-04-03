package userinterface;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import accounts.BankAccount;
import accounts.CustomerAccount;
import userinterface.Session.State;

/**
 * A class that processes the input and handles the output of the application
 * @author Justin Praas
 */
public class TUI {
	
	Session session;

	public TUI() {
		session = new Session();
		listen();
	}

	private void listen() {
		boolean continues = true;
		Scanner inputScanner = new Scanner(System.in);
		String input;
		
		while (continues) {
			printCommands();
			input = inputScanner.nextLine();
			processInput(input);
		}	
		
		inputScanner.close();
	}

	private void printCommands() {
		switch (session.state) {
		case LOGGED_OUT: 
			System.out.println("Use one of the following commands:"
					+ "\nCUST_LOGIN <BSN>, "
					+ "\nCREATE_CUSTOMER_ACCOUNT <BSN> <firstName> <surname> <streetAddress> <email> <phoneNumber> <birthDate>"
					+ "\nEXIT");
			break;
		case CUST_LOGGED_IN:
			System.out.println("Use one of the following commands: "
					+ "\nCREATE_BANK_ACCOUNT"
					+ "\nBANK_LOGIN <#>"
					+ "\nCUST_LOGOUT"
					+ "\nEXIT");
			break;
		case BANK_LOGGED_IN:
			System.out.println("Use one of the following commands: "
					+ "\nDEPOSIT <amount>"
					+ "\nWITHDRAW <amount>"
					+ "\nTRANSFER <toIBAN> <amount>"
					+ "\nBANK_LOGOUT"
					+ "\nEXIT");
		}
	}
	
	private void processInput(String input) {
		
		String command = input.split(" ")[0];
		String parameters = input.substring(command.length() + 1, input.length());
		
		if (command.equals("EXIT")) {
			System.exit(0);
		}

		if (session.state == State.LOGGED_OUT) {
			switch (command) {
			case "CREATE_CUSTOMER_ACCOUNT":
				createCustomerAccount(parameters);
				break;
			case "CUST_LOGIN":
				customerLogin(parameters);
				break;
			default: 
				System.out.println("Invalid command");
				break;
			}
		} else if (session.state == State.CUST_LOGGED_IN) {
			switch (command) {
			case "CREATE_BANK_ACCOUNT":
				createBankAccount(parameters);
				break;
			case "BANK_LOGIN":
				bankLogin(parameters);
				break;
			case "CUST_LOGOUT":
				session.state = State.LOGGED_OUT;
				break;
			default: 
				System.out.println("Invalid command");
				break;
			}
		} else if (session.state == State.BANK_LOGGED_IN) {
			switch (command) {
			case "DEPOSIT":
				deposit(parameters);
				break;
			case "WITHDRAW":
				bankLogin(parameters);
				break;
			case "TRANSFER":
				transfer();
				break;
			case "BANK_LOGOUT":
				session.state = State.CUST_LOGGED_IN;
			default: 
				System.out.println("Invalid command");
				break;
			}
		}
	}

	private void createCustomerAccount(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		String BSN = parameterArray[0];
		String firstName = parameterArray[1];
		String surname = parameterArray[2];
		String streetAddress = parameterArray[3];
		String email = parameterArray[4];
		String phoneNumber = parameterArray[5];
		String birthDate = parameterArray[6];
		
		if (InputChecker.isValidCustomer(BSN, firstName, surname, streetAddress, email, phoneNumber, birthDate)) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-mm-yyyy");
			Date dateOfBirth = Date.valueOf(LocalDate.parse(birthDate, dtf));
			new CustomerAccount(firstName, surname, BSN, streetAddress, phoneNumber, email, dateOfBirth, true);
			System.out.println("Account created for: " + firstName + " " + surname + "(" + BSN + ")");
		}		
	}
}
