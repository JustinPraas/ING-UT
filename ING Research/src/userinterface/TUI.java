package userinterface;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import accounts.BankAccount;
import accounts.CustomerAccount;
import database.BankingLogger;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import userinterface.Session.State;

/**
 * A class that processes the input and handles the output of the application
 * @author Justin Praas
 */
public class TUI {
	
	Session session;

	public TUI() {
		session = new Session();
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

	private void listen() throws IllegalAmountException, IllegalTransferException {
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
			System.out.println("\nUse one of the following commands:"
					+ "\nCUST_LOGIN <BSN>, "
					+ "\nCREATE_CUSTOMER_ACCOUNT <BSN> <firstName> <surname> <streetAddress> <email> <phoneNumber> <birthDate>"
					+ "\nEXIT");
			break;
		case CUST_LOGGED_IN:
			System.out.println("\nUse one of the following commands: "
					+ "\nCREATE_BANK_ACCOUNT"
					+ "\nLIST_BANK_ACCOUNTS"
					+ "\nBANK_LOGIN <#>"
					+ "\nCUST_LOGOUT"
					+ "\nEXIT");
			break;
		case BANK_LOGGED_IN:
			System.out.println("\nUse one of the following commands: "
					+ "\nDEBIT <amount> [description]"
					+ "\nCREDIT <amount> [description]"
					+ "\nTRANSFER <toIBAN> <amount>"
					+ "\nBANK_LOGOUT"
					+ "\nEXIT");
		}
	}
	
	private void processInput(String input) throws IllegalAmountException, IllegalTransferException {
		String[] inputArray = input.split(" ");
		String command = inputArray[0];
		String parameters = "";
		
		if (inputArray.length != 1) {
			parameters = input.substring(command.length() + 1, input.length());
		}		
		
		if (command.equals("EXIT")) {
			System.err.println("Shutting down.");
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
			case "LIST_BANK_ACCOUNTS":
				listBankAccounts();
				break;
			case "BANK_LOGIN":
				bankLogin(parameters);
				break;
			case "CUST_LOGOUT":
				session.state = State.LOGGED_OUT;
				session.bankAccount = null;
				session.customerAccount = null;
				session.bankAccountMap = new HashMap<>();
				break;
			default: 
				System.out.println("Invalid command");
				break;
			}
		} else if (session.state == State.BANK_LOGGED_IN) {
			switch (command) {
			case "DEPOSIT":
				debit(parameters);
				break;
			case "CREDIT":
				credit(parameters);
				break;
			case "TRANSFER":
				transfer(parameters);
				break;
			case "BANK_LOGOUT":
				session.state = State.CUST_LOGGED_IN;
			default: 
				System.out.println("Invalid command");
				break;
			}
		}
	}

	private void transfer(String parameters) throws IllegalAmountException, IllegalTransferException {
		String[] parameterArray = parameters.split(":");
		String toIBAN = parameterArray[0];
		
		float amount = 0;		
		try {
			amount = Float.parseFloat(parameters.split(" ")[0]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount.");
			return;
		}
		
		if (InputChecker.isvalidIBAN(toIBAN)) {
			if (BankingLogger.bankAccountExists(toIBAN)) {
				BankAccount toBankAccount = BankingLogger.getBankAccountByIBAN(toIBAN);
				session.bankAccount.transfer(toBankAccount, amount);
				System.out.println("Transfer done: " + amount + ", to: " + toIBAN + ".");
			} else {
				System.err.println("The bankaccount you're trying to transfer money to does not exist.");
			}
		} else {
			System.err.println("Please enter a valid IBAN.");
		}
		
		
	}

	private void credit(String parameters) throws IllegalAmountException {
		String[] parameterArray = parameters.split(":");
		String strAmount = parameters.split(":")[0];
		String description = "No description";
		
		if (parameterArray.length > 1) {
			description = parameters.substring(strAmount.length() + 1);
		}
		
		float amount = 0;
		try {
			amount = Float.parseFloat(strAmount);
			session.bankAccount.credit(amount, description);
			System.out.println("Credit done: " + amount + ", description: " + description + ".");
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount.");
		}
	}

	private void debit(String parameters) throws IllegalAmountException {		
		String[] parameterArray = parameters.split(":");
		String strAmount = parameters.split(":")[0];
		String description = "No description";

		if (parameterArray.length > 1) {
			description = parameters.substring(strAmount.length() + 1);
		}
		
		float amount = 0;
		try {
			amount = Float.parseFloat(parameters.split(":")[0]);
			session.bankAccount.debit(amount, description);
			System.out.println("Debit done: " + amount + ", description: " + description + ".");
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount.");
		}		
	}

	private void listBankAccounts() {
		if (session.bankAccountMap.size() == 0) {
			System.out.println("This customer doesn't own any bankaccount.");
		} else {
			for (Map.Entry<Integer, BankAccount> entry : session.bankAccountMap.entrySet()) {
				System.out.print(entry.getKey() + ": " + entry.getValue().getIBAN());
				// TODO: Better formatting? Show more info per bankaccount?
			}
			System.out.println("");
		}		
	}

	private void bankLogin(String parameters) {
		int numberOfBankAccount = -1;
		
		try {
			numberOfBankAccount = Integer.parseInt(parameters.split(" ")[0]);
		} catch (NumberFormatException e) {
			System.err.println("Choose a number from the bankaccount-list (use: LIST_BANK_ACCOUNTS)");
			return;
		}		
		
		if (session.bankAccountMap.size() == 0) {
			System.out.println("There is no bankaccount to login to.");
		} else {
			if (session.bankAccountMap.containsKey(numberOfBankAccount)) {
				session.bankAccount = session.bankAccountMap.get(numberOfBankAccount);
			} else {
				System.err.println("That number doesn't point to a bankaccount.");
			}
		}		
	}

	private void createBankAccount(String parameters) {
		String[] parameterArray = parameters.split(":"); // TODO doesn't need BSN as parameter
		String BSN = parameterArray[0];
		
		if (parameterArray.length == 1 && InputChecker.isValidBSN(BSN)) {
			if (BankingLogger.getCustomerAccountByBSN(BSN) != null) {
				new BankAccount(BSN);
				System.out.println("Bankaccount created for: " + BSN + "(BSN)");
			}else {
				System.err.println("Customer with BSN: " + BSN + " does not exist.");
			}			
		}		
	}

	private void customerLogin(String parameters) {
		String[] parameterArray = parameters.split(":");
		String BSN = parameterArray[0];
		
		if (parameterArray.length == 1 && InputChecker.isValidBSN(BSN)) {
			if (BankingLogger.customerAccountExists(BSN)) {
				session.customerAccount = BankingLogger.getCustomerAccountByBSN(BSN);
				
				//Put this customer's bank-accounts in the session bankAccounts map
				Iterator<BankAccount> it = session.customerAccount.getBankAccounts().iterator();
				int i = 1;
				while (it.hasNext()) {
					BankAccount bankAccount = it.next();
					session.bankAccountMap.put(i, bankAccount);
					i++;
				}
				
				System.out.println("Logged in to customer (BSN): " + BSN);
				session.state = State.CUST_LOGGED_IN;
			}else {
				System.err.println("Customer with BSN: " + BSN + " does not exist.");
			}			
		}		
	}

	private void createCustomerAccount(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 7) { // TODO: Change the indexes and numbers when postal-code and city fields are added
			// TODO: Throw exception
			System.err.println("Please enter the required parameters");
			return;
		}
		
		String BSN = parameterArray[0];
		String firstName = parameterArray[1];
		String surname = parameterArray[2];
		String streetAddress = parameterArray[3];
		String email = parameterArray[4];
		String phoneNumber = parameterArray[5];
		String birthDate = parameterArray[6];
		
		if (InputChecker.isValidCustomer(BSN, firstName, surname, streetAddress, email, phoneNumber, birthDate)) {
			
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			Date dateOfBirth = Date.valueOf(LocalDate.parse(birthDate, dtf));
			
			new CustomerAccount(firstName, surname, BSN, streetAddress, phoneNumber, email, dateOfBirth, true);
			
			System.out.println("Account created for: " + firstName + " " + surname + "(" + BSN + ")");
		} else {
			
		}
	}
}
