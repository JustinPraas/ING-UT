package client;

import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import com.thetransactioncompany.jsonrpc2.*;

import java.util.*;

/**
 * Manages TUI input, sends HTTP POST request to server after input validation.
 * @author Andrei Cojocaru
 */
public class InputProcessor {
	
	public InputProcessor() {
		
	}
	
	/**
	 * Redirects the input to the correct handler-method for the given command.
	 * @param input the user input
	 * @throws IllegalAmountException
	 * @throws IllegalTransferException
	 */
	public void processInput(String input) {
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
		if (MessageManager.state == MessageManager.State.NOT_AUTHENTICATED) {
			switch (command) {
			case "OPEN_BANK_ACCOUNT":
				openAccount(parameters);
				break;
			case "LOGIN":
				getAuthToken(parameters);
				break;
			case "PAY_BY_CARD":
				payFromAccount(parameters);
				break;
			case "DEPOSIT":
				depositIntoAccount(parameters);
				break;
			default: 
				System.err.println("Invalid command.");
				break;
			}
		} else if (MessageManager.state == MessageManager.State.AUTHENTICATED) {
			switch (command) {
			case "OPEN_ADDITIONAL_ACCOUNT":
				openAdditionalAccount();
				break;
			case "PAY_BY_CARD":
				payFromAccount(parameters);
				break;
			case "DEPOSIT":
				depositIntoAccount(parameters);
				break;
			case "CLOSE":
				close(parameters);
				break;
			case "GET_BALANCE":
				getBalance(parameters);
				break;
			case "TRANSACTION_OVERVIEW":
				getTransactionsOverview(parameters);
				break;
			case "ADD_OWNER":
				provideAccess(parameters);
				break;
			case "REMOVE_OWNER":
				revokeAccess(parameters);
				break;
			case "TRANSFER":
				transfer(parameters);
				break;
			default: 
				System.err.println("Invalid command.");
				break;
			}
		}
	}
	
	private void revokeAccess(String parameters) {
		String[] parameterArray = parameters.split(":");
		String method = "revokeAccess";
		HashMap<String, Object> params = new HashMap<>();
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		params.put("authToken", MessageManager.AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		params.put("username", parameterArray[1]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	private void provideAccess(String parameters) {
		String[] parameterArray = parameters.split(":");
		String method = "provideAccess";
		HashMap<String, Object> params = new HashMap<>();
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		params.put("authToken", MessageManager.AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		params.put("username", parameterArray[1]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	/**
	 * Splits parameter String from TUI into the necessary variables
	 * and attempts a PIN machine payment with them if they are valid.
	 * @param parameters The provided parameter String
	 */
	private void payFromAccount(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 5) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String sourceIBAN = parameterArray[0];
		String targetIBAN = parameterArray[1];
		String cardNumber = parameterArray[2];
		String PIN = parameterArray[3];
		String amount = parameterArray[4];
		
		String method = "payFromAccount";
		HashMap<String, Object> params = new HashMap<>();
		params.put("sourceIBAN", sourceIBAN);
		params.put("targetIBAN", targetIBAN);
		params.put("pinCard", cardNumber);
		params.put("pinCode", PIN);
		params.put("amount", amount);
		
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	/**
	 * Closes the specified BankAccount.
	 */
	private void close(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 1) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String method = "closeAccount";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", MessageManager.AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	/**
	 * Fetches the transaction history for the currently active BankAccount and
	 * outputs it to console.
	 */
	private void getTransactionsOverview(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String IBAN = parameterArray[0];
		String numTransactions = parameterArray[1];
		
		String method = "getTransactionsOverview";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", MessageManager.AUTHTOKEN);
		params.put("iBAN", IBAN);
		params.put("nrOfTransactions", numTransactions);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	private void getBalance(String parameters) {
		String method = "getBalance";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", MessageManager.AUTHTOKEN);
		params.put("iBAN", parameters);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}
	
	/**
	 * Signs in the <code>CustomerAccount</code> using the given parameter.
	 * @param parameters consists of the customer's BSN
	 */
	private void getAuthToken(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String username = parameterArray[0];
		String password = parameterArray[1];
		
		String method = "getAuthToken";
		HashMap<String, Object> params = new HashMap<>();
		params.put("username", username);
		params.put("password", password);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	/**
	 * Handler-method for the 'TRANSFER' command. Takes the IBAN and the
	 * amount from the parameters, checks if they are valid and makes the 
	 * transfer if they are valid.
	 * @param parameters consists of the destination IBAN and the amount to be transfered
	 * @throws IllegalAmountException thrown when an invalid amount is entered
	 * @throws IllegalTransferException thrown when the transfer is invalid
	 */
	private void transfer(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 5) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String sourceIBAN = parameterArray[0];
		String targetIBAN = parameterArray[1];
		String targetName = parameterArray[2];
		String amount = parameterArray[3];
		String description = parameterArray[4];
		
		String method = "transferMoney";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", MessageManager.AUTHTOKEN);
		params.put("sourceIBAN", sourceIBAN);
		params.put("targetIBAN", targetIBAN);
		params.put("targetName", targetName);
		params.put("amount", amount);
		params.put("description", description);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}
	
	/**
	 * Deposits a given amount of money into the currently active account.
	 * @param parameters Parameters given with the DEPOSIT command
	 * @throws IllegalAmountException Thrown if the given amount is not a number, or negative
	 */
	private void depositIntoAccount(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 4) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String method = "depositIntoAccount";
		String IBAN = parameterArray[0];
		String cardNumber = parameterArray[1];
		String PIN = parameterArray[2];
		String amount = parameterArray[3];
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("iBAN", IBAN);
		params.put("pinCard", cardNumber);
		params.put("pinCode", PIN);
		params.put("amount", amount);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	/**
	 * Creates a <code>BankAccount</code> for the session's <code>CustomerAccount</code>.
	 */
	private void openAdditionalAccount() {
		String method = "openAdditionalAccount";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", MessageManager.AUTHTOKEN);
		JSONRPC2Request request = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(request);
	}

	/**
	 * Creates a <code>CustomerAccount</code> using the given parameters.
	 * @param parameters consists of the BSN, first name, surname, street address,
	 * email address, phone number and the birth date
	 */
	private void openAccount(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		// Check if the required parameters are given.
		if (parameterArray.length != 10) { 
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		// Assign the parameter values to the right fields.
		String firstName = parameterArray[0];
		String lastName = parameterArray[1];
		String initials = parameterArray[2];
		String birthDate = parameterArray[3];
		String ssn = parameterArray[4];
		String address = parameterArray[5];
		String phoneNumber = parameterArray[6];
		String email = parameterArray[7];
		String username = parameterArray[8];
		String password = parameterArray[9];
		
		String method = "openAccount";
		HashMap<String, Object> params = new HashMap<>();
		params.put("name", firstName);
		params.put("surname", lastName);
		params.put("initials", initials);
		params.put("dob", birthDate);
		params.put("ssn", ssn);
		params.put("address", address);
		params.put("telephoneNumber", phoneNumber);
		params.put("email", email);
		params.put("username", username);
		params.put("password", password);
		
		JSONRPC2Request openAccountRequest = new JSONRPC2Request(method, params, "request-" + java.lang.System.currentTimeMillis());
		MessageManager.sendToServer(openAccountRequest);
	}
}
