package client;

import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import com.thetransactioncompany.jsonrpc2.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.hc.client5.http.impl.sync.CloseableHttpClient;
import org.apache.hc.client5.http.impl.sync.HttpClients;
import org.apache.hc.client5.http.methods.HttpPost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.entity.ContentType;
import org.apache.hc.core5.http.entity.StringEntity;

/**
 * Manages TUI input, sends HTTP POST request to server after input validation.
 * 
 * @author Andrei Cojocaru
 */
public class MessageHandler {

	private static String AUTHTOKEN;
	public static UserState userState = UserState.NO_USER;
	public static final String HTTPPOST = "POST / HTTP/1.1\nHost: 127.0.0.1";
	CloseableHttpClient httpclient = HttpClients.createDefault();

	public enum UserState {
		NO_USER, CONSUMER, ADMINISTRATOR;
	}

	/**
	 * Redirects the input to the correct handler-method for the given command.
	 * 
	 * @param input
	 *            the user input
	 * @throws IllegalAmountException
	 * @throws IllegalTransferException
	 */
	public void processInput(String input) {
		String[] inputArray = input.split(" ");
		String command = inputArray[0];
		command = command.toUpperCase();
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

		// Act according to the given input. Pass parameters with the
		// handler-methods.
		if (userState == UserState.NO_USER) {
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
			case "GET_DATE":
				getDate();
				break;
			default:
				System.err.println("Invalid command.");
				break;
			}
		} else if (userState == UserState.CONSUMER) {
			switch (command) {
			case "OPEN_ADDITIONAL_ACCOUNT":
				openAdditionalAccount();
				break;
			case "PAY_BY_CARD":
				payFromAccount(parameters);
				break;
			case "INVALIDATE_PIN_CARD":
				invalidatePinCard(parameters);
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
			case "GET_USER_ACCESS":
				getUserAccess(parameters);
				break;
			case "GET_BANK_ACCOUNT_ACCESS":
				getBankAccountAccess(parameters);
				break;
			case "UNBLOCK_PINCARD":
				unblockCard(parameters);
				break;
			case "OPEN_SAVINGS_ACCOUNT":
				openSavingsAccount(parameters);
				break;
			case "CLOSE_SAVINGS_ACCOUNT":
				closeSavingsAccount(parameters);
				break;
			case "SET_OVERDRAFT_LIMIT":
				setOverdraftLimit(parameters);
				break;
			case "GET_OVERDRAFT_LIMIT":
				getOverdraftLimit(parameters);
				break;
			case "SET_TRANSFER_LIMIT":
				setTransferLimit(parameters);
				break;
			case "GET_DATE":
				getDate();
				break;
			case "LOGOUT":
				logout(parameters);
				break;
			default:
				System.err.println("Invalid command.");
				break;
			}
		} else if (userState == UserState.ADMINISTRATOR) {
			switch (command) {
			case "SIMULATE_TIME":
				simulateTime(parameters);
				break;
			case "RESET":
				reset();
				break;
			case "GET_DATE":
				getDate();
				break;
			case "GET_EVENT_LOGS":
				getEventLogs(parameters);
				break;
			case "GET_OVERDRAFT_LIMIT":
				getOverdraftLimit(parameters);
				break;
			case "GET_BANK_ACCOUNT_ACCESS":
				getBankAccountAccess(parameters);
				break;
			case "GET_BALANCE":
				getBalance(parameters);
				break;
			case "TRANSACTION_OVERVIEW":
				getTransactionsOverview(parameters);
				break;
			case "LOGOUT":
				logout(parameters);
				break;
			default:
				System.err.println("Invalid command.");
				break;
			}
		}
		System.out.println("\n");
	}
	
	private void logout(String parameters) {
		AUTHTOKEN = "";
		userState = UserState.NO_USER;
		System.out.println("Logout successful.");
	}

	/**
	 * Extension 5: 'Overdraft' related.
	 * Sets the overdraft limit of the given bank account
	 */
	private void setTransferLimit(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "setTransferLimit";
		HashMap<String, Object> params = new HashMap<>();
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}

		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		params.put("transferLimit", parameterArray[1]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Transfer limit succesfully set to " + Double.parseDouble(parameterArray[1]) + ".");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}		
	}
	
	private void getEventLogs(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "getEventLogs";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);

		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		params.put("beginDate", parameterArray[0]);
		params.put("endDate", parameterArray[1]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> results = (ArrayList<HashMap<String, Object>>) jResp.getResult();
			
			System.out.println("Error logs: " + results.size());
			for (HashMap<String, Object> log : results) {
				System.out.println(log.get("timeStamp") + ": " + log.get("eventLog"));
			}
			
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}
	
	/**
	 * Extension 6: 'Savings account' related.
	 * Opens a savings account for the given bank account.
	 */
	private void openSavingsAccount(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "openSavingsAccount";
		HashMap<String, Object> params = new HashMap<>();

		if (parameterArray.length != 1) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Succesfully opened savings account for " + parameterArray[0]);
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}		
	}
	
	/**
	 * Extension 6: 'Savings account' related.
	 * Closes a savings account for the given bank account.
	 */
	private void closeSavingsAccount(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "closeSavingsAccount";
		HashMap<String, Object> params = new HashMap<>();

		if (parameterArray.length != 1) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Succesfully closed the savings account for " + parameterArray[0]);
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}		
	}
	
	/**
	 * Extension 5: 'Overdraft' related.
	 * Sets the overdraft limit of the given bank account
	 */
	private void setOverdraftLimit(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "setOverdraftLimit";
		HashMap<String, Object> params = new HashMap<>();
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		double amount = 0;
		try {
			amount = Double.parseDouble(parameterArray[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount representation.");
			return;
		}

		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		params.put("overdraftLimit", amount);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Overdraft limit succesfully set to " + Double.parseDouble(parameterArray[1]) + ".");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}		
	}

	/**
	 * Extension 5: 'Overdraft' related.
	 * Gets the overdraft limit of the given bank account
	 */
	private void getOverdraftLimit(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "getOverdraftLimit";
		HashMap<String, Object> params = new HashMap<>();
		
		if (parameterArray.length != 1) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			
			@SuppressWarnings("unchecked")
			HashMap<String, Object> result = (HashMap<String, Object>) jResp.getResult();
			String overdraftLimit = (String) result.get("overdraftLimit");
			System.out.println("Overdraft limit for " + parameterArray[0] + " is " + overdraftLimit + ".");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}		
	}

	/**
	 * Extension 2: 'PIN block' related.
	 * Unblocks a given pin card, given the right parameters and given it is actually blocked.
	 */
	private void unblockCard(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "unblockCard";
		HashMap<String, Object> params = new HashMap<>();
		
		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}

		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		params.put("pinCard", parameterArray[1]);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Pin card succesfully unblocked.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}
	
	private void getDate() {
		String method = "getDate";
		HashMap<String, Object> params = new HashMap<>();
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			
			@SuppressWarnings("unchecked")
			HashMap<String, Object> result = (HashMap<String, Object>) jResp.getResult();
			System.out.println("The server's date is: " + (String) result.get("date"));
			
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}
	
	private void reset() {
		String method = "reset";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Succesfully reset database and simulated time.");			
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	private void simulateTime(String parameters) {
		String[] parameterArray = parameters.split(":");
		String method = "simulateTime";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);

		if (parameterArray.length != 1) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		if (Integer.parseInt(parameterArray[0]) <= 0) {
			System.err.println("Please enter a number greater than 0.");
			return;
		}
		
		params.put("nrOfDays", Integer.parseInt(parameterArray[0]));
		
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Succesfully simulated " + parameterArray[0] + " days ahead.");		
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
		
	}
	
	
	@SuppressWarnings("rawtypes")
	private void getBankAccountAccess(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "getBankAccountAccess";
		HashMap<String, Object> params = new HashMap<>();

		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			System.out.println("The following users have access to the account:");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap> users = (ArrayList<HashMap>) jResp.getResult();

			for (HashMap hm : users) {
				System.out.print(hm.get("username") + ", ");
			}
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	@SuppressWarnings("rawtypes")
	private void getUserAccess(String parameters) {
		String parameterArray[] = parameters.split(":");
		String method = "getUserAccess";
		HashMap<String, Object> params = new HashMap<>();

		params.put("authToken", AUTHTOKEN);
		params.put("username", parameterArray[0]);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			System.out.println("You have access to the following accounts:");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap> transactions = (ArrayList<HashMap>) jResp.getResult();

			for (HashMap hm : transactions) {
				System.out.printf("%-30s %s %n", "Owner: " + (String)hm.get("owner"), "IBAN: " + (String)hm.get("iBAN"));
//				System.out.println("==============================");
//				System.out.println("Owner: " + hm.get("owner"));
//				System.out.println("IBAN: " + hm.get("iBAN"));
//				System.out.println("==============================");
//				System.out.println();
			}
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	private void revokeAccess(String parameters) {
		String[] parameterArray = parameters.split(":");
		String method = "revokeAccess";
		HashMap<String, Object> params = new HashMap<>();

		if (parameterArray.length != 2 && parameterArray.length != 1) {
			System.err.println("Please enter the requested parameters.");
			return;
		}

		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);

		if (parameterArray.length == 2) {
			params.put("username", parameterArray[1]);
		}

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			System.out.println("Successfully revoked privileges.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	private void provideAccess(String parameters) {
		String[] parameterArray = parameters.split(":");
		String method = "provideAccess";
		HashMap<String, Object> params = new HashMap<>();

		if (parameterArray.length != 2) {
			System.err.println("Please enter the requested parameters.");
			return;
		}

		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);
		params.put("username", parameterArray[1]);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			@SuppressWarnings("unchecked")
			HashMap<String, Object> results = (HashMap<String, Object>) jResp.getResult();
			
			String pinCard = (String) results.get("pinCard");
			String pinCode = (String) results.get("pinCode");
			System.out.println("Successfully linked accounts.");
			System.out.println("Card number: " + pinCard);
			System.out.println("PIN: " + pinCode);

			System.out.println();
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	/**
	 * Splits parameter String from TUI into the necessary variables and
	 * attempts a PIN machine payment with them if they are valid.
	 * 
	 * @param parameters
	 *            The provided parameter String
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
		double amount = 0;
		try {
			amount = Double.parseDouble(parameterArray[4]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount representation.");
			return;
		}

		String method = "payFromAccount";
		HashMap<String, Object> params = new HashMap<>();
		params.put("sourceIBAN", sourceIBAN);
		params.put("targetIBAN", targetIBAN);
		params.put("pinCard", cardNumber);
		params.put("pinCode", PIN);
		params.put("amount", amount);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Payment successful.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	private void invalidatePinCard(String parameters) {
		String[] parameterArray = parameters.split(":");
		
		if (parameterArray.length != 3) {
			System.err.println("Please enter the requested parameters.");
			return;
		}
		
		String IBAN = parameterArray[0];
		String pinCard = parameterArray[1];
		String newPin = parameterArray[2];
		
		String method = "invalidateCard";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", IBAN);
		params.put("pinCard", pinCard);
		
		if (newPin.equals("yes")) {
			params.put("newPin", true);
		} else if (newPin.equals("no")) {
			params.put("newPin", false);
		} else {
			params.put("newPin", "NA");
		}		
		
		JSONRPC2Request request = new JSONRPC2Request(method, params, 
				"request-" + java.lang.System.currentTimeMillis());
		
		String resp = sendToServer(request);
		
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			
			@SuppressWarnings("unchecked")
			HashMap<String, Object> results = (HashMap<String, Object>) jResp.getResult();
			String newPinCardNumber = (String)results.get("pinCard");
			System.out.println("Pincard succesfully invalidated \nNew Pincard card obtained with number: " + newPinCardNumber);
			
			if (results.containsKey("pinCode")) {
				String newPinCode = (String)results.get("pinCode");
				System.out.println("New Pincode obtained: " + newPinCode);
			}
			
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
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
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameterArray[0]);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			System.out.println("Account closed successfully.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
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
		
		String method = "getTransactionsOverview";
		String IBAN = parameterArray[0];
		int number = 0;
		try {
			number = Integer.parseInt(parameterArray[1]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount representation.");
			return;
		}

		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", IBAN);		
		params.put("nrOfTransactions", number);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			System.out.println("Transaction history:");
			@SuppressWarnings("unchecked")
			ArrayList<HashMap<String, Object>> transactions = (ArrayList<HashMap<String, Object>>) jResp.getResult();
			
			System.out.printf("%-19s %-19s %-15s %-30s %-7s %-100s %n", "Source IBAN:", "Target IBAN:", "Target name:", "Date: ", "Amount:", "Description:");
			for (HashMap<String, Object> hm : transactions) {
				System.out.printf("%-19s %-19s %-15s %-30s %-7s %-100s %n", hm.get("sourceIBAN"), hm.get("targetIBAN"), 
						(String) hm.get("targetName"), hm.get("date"), hm.get("amount"), hm.get("description"));
			}
		} catch (JSONRPC2ParseException e) {
			e.printStackTrace();
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	private void getBalance(String parameters) {
		String method = "getBalance";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);
		params.put("iBAN", parameters);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			@SuppressWarnings("unchecked")
			HashMap<String, Object> results = (HashMap<String, Object>) jResp.getResult();
			System.out.println("Balance for account " + params.get("iBAN") + " is " + results.get("result"));
			
			if (results.containsKey("savingAccountBalance")) {
				System.out.println("Balance for savings account is " + results.get("savingAccountBalance"));				
			}

		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	/**
	 * Signs in the <code>CustomerAccount</code> using the given parameter.
	 * 
	 * @param parameters
	 *            consists of the customer's BSN
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

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);

			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}
			@SuppressWarnings("unchecked")
			HashMap<String, Object> results = (HashMap<String, Object>) jResp.getResult();
			AUTHTOKEN = (String) results.get("authToken");
			userState = UserState.CONSUMER;
			
			// if user is an admin, set state to admin
			if (username.equals("admin")) {
				userState = UserState.ADMINISTRATOR;
			}
			
			System.out.println("Authentication successful.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	/**
	 * Handler-method for the 'TRANSFER' command. Takes the IBAN and the amount
	 * from the parameters, checks if they are valid and makes the transfer if
	 * they are valid.
	 * 
	 * @param parameters
	 *            consists of the destination IBAN and the amount to be
	 *            transfered
	 * @throws IllegalAmountException
	 *             thrown when an invalid amount is entered
	 * @throws IllegalTransferException
	 *             thrown when the transfer is invalid
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
		double amount = 0;
		try {
			amount = Double.parseDouble(parameterArray[3]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount representation.");
			return;
		}
		String description = parameterArray[4];

		String method = "transferMoney";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);
		params.put("sourceIBAN", sourceIBAN);
		params.put("targetIBAN", targetIBAN);
		params.put("targetName", targetName);
		params.put("amount", amount);
		params.put("description", description);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);
		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			System.out.println("Transfer successful.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	/**
	 * Deposits a given amount of money into the currently active account.
	 * 
	 * @param parameters
	 *            Parameters given with the DEPOSIT command
	 * @throws IllegalAmountException
	 *             Thrown if the given amount is not a number, or negative
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
		double amount = 0;
		try {
			amount = Double.parseDouble(parameterArray[3]);
		} catch (NumberFormatException e) {
			System.err.println("Please enter a valid amount representation.");
			return;
		}

		HashMap<String, Object> params = new HashMap<>();
		params.put("iBAN", IBAN);
		params.put("pinCard", cardNumber);
		params.put("pinCode", PIN);
		params.put("amount", amount);

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			System.out.println("Deposit successful.");
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	/**
	 * Creates a <code>BankAccount</code> for the session's
	 * <code>CustomerAccount</code>.
	 */
	private void openAdditionalAccount() {
		String method = "openAdditionalAccount";
		HashMap<String, Object> params = new HashMap<>();
		params.put("authToken", AUTHTOKEN);
		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			@SuppressWarnings("unchecked")
			HashMap<String, Object> results = (HashMap<String, Object>) jResp.getResult();

			String iBAN = (String) results.get("iBAN");
			String pinCard = (String) results.get("pinCard");
			String pinCode = (String) results.get("pinCode");
			System.out.println("Your new IBAN is: " + iBAN);
			System.out.println("Your new card number is: " + pinCard);
			System.out.println("Your new PIN is: " + pinCode);
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	/**
	 * Creates a <code>CustomerAccount</code> using the given parameters.
	 * 
	 * @param parameters
	 *            consists of the BSN, first name, surname, street address,
	 *            email address, phone number and the birth date
	 */
	@SuppressWarnings("unchecked")
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

		JSONRPC2Request request = new JSONRPC2Request(method, params,
				"request-" + java.lang.System.currentTimeMillis());
		String resp = sendToServer(request);

		try {
			JSONRPC2Response jResp = JSONRPC2Response.parse(resp);
			if (!jResp.indicatesSuccess()) {
				System.out.printf("Error " + jResp.getError().getCode() + ": " + jResp.getError().getMessage());
				if (jResp.getError().getData() != null) {
					System.out.println((String) jResp.getError().getData());
				}
				return;
			}

			HashMap<String, Object> results = (HashMap<String, Object>) jResp.getResult();

			String iBAN = (String) results.get("iBAN");
			String pinCard = (String) results.get("pinCard");
			String pinCode = (String) results.get("pinCode");
			System.out.println("Your new IBAN is: " + iBAN);
			System.out.println("Your new card number is: " + pinCard);
			System.out.println("Your new PIN is: " + pinCode);
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded invalid JSON-RPC response from server.");
		}
	}

	public String sendToServer(JSONRPC2Request request) {
		String message = request.toJSONString();

		HttpPost httpPost = new HttpPost("http://localhost:8080/ING-UT/rest/banking/postRequest");
		StringEntity msg = new StringEntity(message, ContentType.create("application/json", "UTF-8"));
		httpPost.setEntity(msg);

		try {
			HttpResponse x = httpclient.execute(httpPost);
			BufferedReader reader = new BufferedReader(new InputStreamReader((x.getEntity().getContent())));
			String out, output = "";
			while ((out = reader.readLine()) != null) {
				output += out;
			}
			return output;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
