package server;

import database.DataManager;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import userinterface.InputChecker;
import userinterface.Session;
import userinterface.Session.State;
import accounts.CustomerAccount;
import accounts.Transaction;

import java.util.ArrayList;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import accounts.BankAccount;

/**
 * Manages request/response messages.
 * @author Andrei Cojocaru
 */
public class BankingServer {
	public Session session;
	
	public BankingServer() {
		session = new Session();
	}
	
	/**
	 * Redirects the input to the correct handler-method for the given command.
	 * @param input the user input
	 * @throws IllegalAmountException
	 * @throws IllegalTransferException
	 */
	public void processInput(String input) throws IllegalAmountException, IllegalTransferException {
		String[] inputArray = input.split(" ");
		String command = inputArray[0];
		String parameters = "";
		
		// Only look for parameters if they are actually there.
		if (inputArray.length != 1) {
			parameters = input.substring(command.length() + 1, input.length());
		}		
		
		// Exit the application when the command is 'EXIT'.
		if (command.equals("EXIT")) {
			System.err.println("Shutting down.");
			System.exit(0);
		}

		// Act according to the given input. Pass parameters with the handler-methods.
		if (session.state == State.LOGGED_OUT) {
			switch (command) {
			case "CREATE_CUSTOMER_ACCOUNT":
				createCustomerAccount(parameters);
				break;
			case "CUST_LOGIN":
				customerLogin(parameters);
				break;
			default: 
				System.err.println("Invalid command.");
				break;
			}
		} else if (session.state == State.CUST_LOGGED_IN) {
			switch (command) {
			case "BANK_LOGIN":
				bankLogin(parameters);
				break;
			case "CREATE_BANK_ACCOUNT":
				createBankAccount();
				break;
			case "LIST_BANK_ACCOUNTS":
				listBankAccounts();
				break;
			case "CUST_LOGOUT":
				session.logoutCustomer();
				break;
			default: 
				System.err.println("Invalid command");
				break;
			}
		} else if (session.state == State.BANK_LOGGED_IN) {
			switch (command) {
			case "INFO":
				getBankAccountInformation();
				break;
			case "TRANSACTIONS":
				getTransactionHistory();
				break;
			case "DEPOSIT":
				deposit(parameters);
				break;
			case "TRANSFER":
				transfer(parameters);
				break;
			case "CLOSE":
				close();
				break;
			case "BANK_LOGOUT":
				session.logoutBank();
				break;
			default: 
				System.err.println("Invalid command.");
				break;
			}
		}
	}
	
	private void close() {
		String closedIBAN = session.bankAccount.getIBAN();
		
		if (session.bankAccount.getBalance() < 0) {
			System.out.println("Failed to close bank account " + closedIBAN + " due to negative balance of " + session.bankAccount.getBalance());
		}
		
		session.bankAccount.setClosed(true);
		session.bankAccount.saveToDB();
		session.bankAccountList.remove(session.bankAccount);
		session.state = State.CUST_LOGGED_IN;
		System.out.println("Bank account " + closedIBAN + " closed successfully.");
	}

	private void getTransactionHistory() {
		ArrayList<Criterion> criteria = new ArrayList<>();
		Criterion cr = Restrictions.eq("sourceIBAN", session.bankAccount.getIBAN());
		criteria.add(cr);
		ArrayList<Transaction> outgoing = (ArrayList<Transaction>) DataManager.getObjectsFromDB("accounts.Transaction", criteria);
		
		criteria = new ArrayList<>();
		cr = Restrictions.eq("destinationIBAN", session.bankAccount.getIBAN());
		criteria.add(cr);
		ArrayList<Transaction> incoming = (ArrayList<Transaction>) DataManager.getObjectsFromDB("accounts.Transaction", criteria);
		
		System.out.println("Outgoing transactions:");
		for (Transaction key : outgoing) {
			System.out.println(key.toString());
		}
		
		System.out.println("\n\nIncoming transactions:");
		
		for (Transaction key : incoming) {
			System.out.println(key.toString());
		}
	}

	private void getBankAccountInformation() {
		System.out.println(session.bankAccount.toString());
	}
	
	/**
	 * Signs in the <code>CustomerAccount</code> using the given parameter.
	 * @param parameters consists of the customer's BSN
	 */
	private void customerLogin(String parameters) {
		String BSN = parameters;
		
		// Check if the parameter is a valid BSN.
		if (InputChecker.isValidBSN(BSN)) {
			CustomerAccount custAcc = new CustomerAccount();
			// Check if the user exists.
			if (!DataManager.isPrimaryKeyUnique(custAcc.getClassName(), custAcc.getPrimaryKeyName(), BSN)) {
				// Get object by primary key
				custAcc = (CustomerAccount) DataManager.getObjectByPrimaryKey(new CustomerAccount().getClassName(), BSN); 
				session.loginCustomer(custAcc);			
				System.out.println("Logged in to customer (BSN): " + BSN);
			} else {
				System.err.println("Customer with BSN: " + BSN + " does not exist.");
			}		
		}		
	}
	
	/**
	 * Signs in to the <code>BankAccount</code> using the given parameter.
	 * @param parameters consists of the number linked to the <code>BankAccount</code>
	 */
	private void bankLogin(String parameters) {
		int numberOfBankAccount = -1;
		
		// Parse the number from the parameters.
		try {
			numberOfBankAccount = Integer.parseInt(parameters.split(" ")[0]);
		} catch (NumberFormatException e) {
			System.err.println("Choose a number from the bank-account-list. Use: LIST_BANK_ACCOUNTS");
			return;
		}		
		
		// You can't login to a bank-account if there's none.
		if (session.bankAccountList.size() == 0) {
			System.out.println("There is no bank-account to login to.");
		} else {
			if (numberOfBankAccount >= 0 && numberOfBankAccount < session.bankAccountList.size()) {
				session.loginBank(session.bankAccountList.get(numberOfBankAccount));
				System.out.println("Logged in: " + session.bankAccountList.get(numberOfBankAccount).getIBAN());
			} else {
				System.err.println("That number is not linked to a bank-account.");
			}
		}		
	}

	/**
	 * Handler-method for the 'TRANSFER' command. Takes the IBAN and the
	 * amount from the parameters, checks if they are valid and makes the 
	 * transfer if they are valid.
	 * @param parameters consists of the destination IBAN and the amount to be transfered
	 * @throws IllegalAmountException thrown when an invalid amount is entered
	 * @throws IllegalTransferException thrown when the transfer is invalid
	 */
	private void transfer(String parameters) throws IllegalAmountException, IllegalTransferException {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 2) {
			System.err.println("Invalid transfer input. Example: NL10INGB0002365302:80.50");
			return;
		}
		String toIBAN = parameterArray[0];
		
		// Try to parse the given amount. Throws an exception if it's an invalid amount.
		float amount = 0;		
		try {
			amount = Float.parseFloat(parameterArray[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount. Example: 36.10");
			return;
		}
		
		// Check if the IBAN is valid. If the IBAN is valid then proceed to make the actual transfer.
		if (InputChecker.isValidIBAN(toIBAN)) {
			BankAccount toBankAccount = new BankAccount();
			if (!DataManager.isPrimaryKeyUnique(toBankAccount.getClassName(), toBankAccount.getPrimaryKeyName(), toIBAN)) {
				toBankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(toBankAccount.getClassName(), toIBAN);
				session.bankAccount.transfer(toBankAccount, amount);
				System.out.println("Transfer done: " + amount + ", to: " + toIBAN + ".");
			} else {
				System.err.println("The bankaccount you're trying to transfer money to does not exist.");
			}
		} else {
			System.err.println("Please enter a valid IBAN. Example: NL10INGB0002352362");
		}		
	}
	
	private void deposit(String parameters) throws IllegalAmountException {
		String[] parameterArray = parameters.split(":");
		String strAmount = parameters.split(":")[0];
		
		if (parameterArray.length != 1) {
			System.err.println("Invalid credit input. Exapmle: 30.50:Cash for groceries");
			return;
		}
		
		// Parse the amount. Perform the credit if it's a valid and legal amount.
		float amount = 0;
		try {
			amount = Float.parseFloat(strAmount);
			session.bankAccount.deposit(amount);
			System.out.println("Deposited: " + amount + ".");
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount. Example: 30.10");
			return;
		}
	}

	/**
	 * Prints the list of <code>BankAccount</code> objects currently owned 
	 * by the session's <code>CustomerAccount</code>.
	 */
	private void listBankAccounts() {
		if (session.bankAccountList.size() == 0) {
			System.out.println("This customer doesn't own any bankaccount.");
		} else {
			for (int i = 0; i < session.bankAccountList.size(); i++) {
				if(!session.bankAccountList.get(i).getClosed()) {
					System.out.println(i + ": " + session.bankAccountList.get(i).getIBAN());
				}
				// TODO: Better text formatting? Show more info per bankaccount?
			}
		}		
	}

	/**
	 * Creates a <code>BankAccount</code> for the session's <code>CustomerAccount</code>.
	 */
	private void createBankAccount() {		
		CustomerAccount customer = session.customerAccount;
		if (customer != null) {
			BankAccount newBankAccount = new BankAccount(customer.getBSN());
			session.customerAccount.addBankAccount(newBankAccount);
			session.bankAccountList.add(newBankAccount);
			session.customerAccount.saveToDB();
			System.out.println("Bankaccount created for: " + customer.getName() + " (BSN: "+ customer.getBSN() + ")");
		} else {
			System.err.println("Session does not have a customer assigned.");
		}
	}

	/**
	 * Creates a <code>CustomerAccount</code> using the given parameters.
	 * @param parameters consists of the BSN, first name, surname, street address,
	 * email address, phone number and the birth date
	 */
	private void createCustomerAccount(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		// Check if the required parameters are given.
		if (parameterArray.length != 7) { 
			// TODO: Change the indexes and numbers when postal-code and city fields are added.
			// TODO: Throw exception
			System.err.println("Please enter the required parameters");
			return;
		}
		
		// Assign the parameter values to the right fields.
		String BSN = parameterArray[0];
		String firstName = parameterArray[1];
		String surname = parameterArray[2];
		String streetAddress = parameterArray[3];
		String email = parameterArray[4];
		String phoneNumber = parameterArray[5];
		String birthDate = parameterArray[6];
		CustomerAccount custAcc = new CustomerAccount(firstName, surname, BSN, streetAddress, phoneNumber, email, birthDate);
		// Create the account if all is fine.
		if (InputChecker.isValidCustomer(BSN, firstName, surname, streetAddress, email, phoneNumber, birthDate) && 
				!DataManager.objectExists(custAcc)) {
			custAcc.saveToDB();
			System.out.println("Account created for: " + firstName + " " + surname + "(" + BSN + ")");
		} else {
			System.err.println("Something went wrong.");
		}
	}
}
