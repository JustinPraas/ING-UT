package server.rest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;
import accounts.Transaction;
import database.DataManager;
import database.SQLiteDB;
import exceptions.ClosedAccountTransferException;
import exceptions.ExceedLimitException;
import exceptions.ExpiredCardException;
import exceptions.IllegalAccountCloseException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InvalidPINException;
import exceptions.ObjectDoesNotExistException;
import exceptions.PinCardBlockedException;
import logging.Logger;
import logging.Log.Type;
import net.minidev.json.JSONArray;
import server.core.InputValidator;

@Path("/banking")
public class ServerHandler {
	
	private static HashMap<String, CustomerAccount> accounts = new HashMap<>();
	
	// Data-container for this server's session
	private static ServerModel serverModel = new ServerModel();
	
	// Handles the monthly interest
	@SuppressWarnings("unused")
	private static InterestHandler interestHandler = new InterestHandler();
	@SuppressWarnings("unused")
	private static TimeOperator timeOperator = new TimeOperator();
	
	@POST
	@Path("/postRequest")
	@Consumes(MediaType.APPLICATION_JSON)
	public static Response parseJSONRequest (String request) {
		JSONRPC2Request jReq = null;
		String method;
		
		try {
			jReq = JSONRPC2Request.parse(request);
		} catch (JSONRPC2ParseException e) {
			String err = buildError(-32700, "An error occurred while parsing the JSON input.");
			return respondError(err);
		}
		
		method = jReq.getMethod();
		Logger.addMethodRequestLog(method, jReq.getNamedParams());
		
		switch(method) {
		case "openAccount":
			return openAccount(jReq);
		case "openAdditionalAccount":
			return openAdditionalAccount(jReq);
		case "closeAccount":
			return closeAccount(jReq);
		case "provideAccess":
			return provideAccess(jReq);
		case "revokeAccess":
			return revokeAccess(jReq);
		case "depositIntoAccount":
			return depositIntoAccount(jReq);
		case "payFromAccount":
			return payFromAccount(jReq);
		case "invalidateCard":
			return invalidateCard(jReq);
		case "transferMoney":
			return transferMoney(jReq);
		case "getAuthToken":
			return getAuthToken(jReq);
		case "getBalance":
			return getBalance(jReq);
		case "getTransactionsOverview":
			return getTransactionsOverview(jReq);
		case "getUserAccess":
			return getUserAccess(jReq);
		case "getBankAccountAccess":
			return getBankAccountAccess(jReq);
		case "simulateTime":
			return simulateTime(jReq);
		case "reset":
			return reset(jReq);
		case "getDate":
			return getDate(jReq);
		case "unblockCard":
			return unblockCard(jReq);
		case "openSavingsAccount":
			return openSavingsAccount(jReq);
		case "closeSavingsAccount":
			return closeSavingsAccount(jReq);
		case "setOverdraftLimit":
			return setOverdraftLimit(jReq);
		case "getOverdraftLimit":
			return getOverdraftLimit(jReq);
		case "getEventLogs":
			return getEventLog(jReq);
		case "setTransferLimit":
			return setTransferLimit(jReq);
		case "setValue":
			return setValue(jReq);
		default:
			String err = buildError(-32601, "The requested remote-procedure does not exist.");
			Logger.addMethodErrorLog(jReq.getMethod(), err, -32601);
			return respondError(err);
		}
	}

	public static String buildError(int code, String message) {
		Logger.addMethodErrorLog(message, code);
		JSONRPC2Error jErr = new JSONRPC2Error(code, message);
		JSONRPC2Response jResp = new JSONRPC2Response(jErr, "response-" + java.lang.System.currentTimeMillis());
		return jResp.toJSONString();
	}
	
	public static String buildError(int code, String message, String data) {
		Logger.addMethodErrorLog(message + " " + data, code);
		JSONRPC2Error jErr = new JSONRPC2Error(code, message, "\n" + data);
		JSONRPC2Response jResp = new JSONRPC2Response(jErr, "response-" + java.lang.System.currentTimeMillis());
		return jResp.toJSONString();
	}
	
	private static Response sendEmptyResult(String methodName) {
		HashMap<String, Object> resp = new HashMap<>();			
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), methodName);
	}

	public static Response respond(String jResp, String methodName) {
		if (!methodName.equals("reset")) {
			Logger.addMethodSuccessLog(methodName);
		}		
		return Response.status(200).entity(jResp).build();
	}
	
	public static Response respondError(String error) {
		return Response.status(500).entity(error).build();	
	}
	
	public static Response respondError(String error, int code) {
		return Response.status(code).entity(error).build();
	}
	
	/**
	 * Extension 12: 'Administrative User (3)' related.
	 * Sets the value for the banking system.
	 */
	private static Response setValue(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();	
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidSetValueRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		String authToken = (String) params.get("authToken");
		String key = (String) params.get("key");
		String value = (String) params.get("value");
		String date = (String) params.get("date");		
		
		// An non-administrator can not set the bank system values
		if (!isAdministrativeUser(authToken)) {
			String err = buildError(419, "Only an administrator can not request the event logs.");
			return respondError(err);
		}
		
		String err = buildError(418, "One or more parameter has an invalid value. See message.", "The value " + value + " is not a valid value.");
		TimeEvent t = new TimeEvent();
		t.setName("BANK_SYSTEM_VALUE_UPDATE");
		t.setExecuted(false);
		try {
			t.setTimestamp(Logger.parseDateToMillis(date, "yyyy-MM-dd"));
			Calendar c = Calendar.getInstance();
			Calendar serverC = ServerModel.getServerCalendar();
			c.setTimeInMillis(t.getTimestamp());
			
			// if the given date is less or equal to the current date, return an error.
			if (c.get(Calendar.YEAR) == serverC.get(Calendar.YEAR) && 
					c.get(Calendar.MONTH) == serverC.get(Calendar.MONTH) && 
					c.get(Calendar.DATE) <= serverC.get(Calendar.DATE)) {
				String error = buildError(500, "The given date is previous or equal to the current Server date.");
				return respondError(error);
			}
		} catch (ParseException e) {
			e.printStackTrace();
			String error = buildError(418, "One or more parameter has an invalid value. See message.", "Invalid date format.");
			return respondError(error);
		}
		switch(key) {
		/*case "CREDIT_CARD_MONTHLY_FEE":
			// TODO: necessary extension not implemented yet
			break;
		case "CREDIT_CARD_DEFAULT_CREDIT":
			//TODO: necessary extension not implemented yet
			break;*/
		case "CARD_EXPIRATION_LENGTH":
		case "CARD_USAGE_ATTEMPTS":
			if (InputValidator.isPositiveInteger(value)) {
				t.setDescription(key + ":" + value);
			} else {
				return respondError(err);
			}
			break;
		case "NEW_CARD_COST":
		case "MAX_OVERDRAFT_LIMIT":
		case "INTEREST_RATE_1":
		case "INTEREST_RATE_2":
		case "INTEREST_RATE_3":
		case "OVERDRAFT_INTEREST_RATE":
		case "DAILY_WITHDRAW_LIMIT":
		case "WEEKLY_TRANSFER_LIMIT":
			if (InputValidator.isPositiveDouble(value)) {
				t.setDescription(key + ":" + value);
			} else {
				return respondError(err);
			}
			break;
		default:
			String error = buildError(500, "Unknown banking value key: " + key);
			return respondError(error);
		}
		t.saveToDB();
	
		return sendEmptyResult(jReq.getMethod());
	}

	/**
	 * Extension 9: 'Spending limits' related.
	 * Sets the transferLimit of the bank account with the given IBAN.
	 */
	private static Response setTransferLimit(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();	
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidSetTransferLimitRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");		
		Double weeklyLimit = Double.parseDouble((String) params.get("transferLimit"));		

		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// Check if the user actually owns the bank account
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not change the transfer limit for someone else's bank account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		if (weeklyLimit < 0f) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", "The transfer limit cannot be less than 0");
			return respondError(err);
		} else if (weeklyLimit == bankAccount.getTransferLimit()) {
			String err = buildError(420, "The action has no effect. See message.", "The bank account already has this transfer limit.");
			return respondError(err);
		}
		
		HashMap<String, Double> updatedTransferLimitMap = ServerDataHandler.getUpdatedTransferLimitMap();
		updatedTransferLimitMap.put(bankAccount.getIBAN(), weeklyLimit);
		ServerDataHandler.setUpdatedTransferLimitMap(updatedTransferLimitMap);
	
		return sendEmptyResult(jReq.getMethod());
	}
	
	private static Response getEventLog(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();	
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetEventLogRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}
	
		String startString = (String) params.get("beginDate");	
		String endString = (String) params.get("endDate");
		String authToken = (String) params.get("authToken");

		// An non-administrator can not get the event logs
		if (!isAdministrativeUser(authToken)) {
			String err = buildError(500, "Only an administrator can not request the event logs.");
			return respondError(err);
		}
		
		ArrayList<HashMap<String, Object>> resp;
		
		try {
			resp = Logger.getEventLogs(startString, endString);
		} catch (ParseException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "One of the given dates is not in the format yyyy-MM-dd");
			return ServerHandler.respondError(err);
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response openSavingsAccount(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();	
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidSavingsAccountRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}

		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// Check if the customer has a bank account with this IBAN
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(500, "An unexpected error occured, see error details", "A bank account with the given IBAN does not exist.");
			return respondError(err);
		}
		
		// An administrator can not set up a savings account
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not open a savings account.");
			return respondError(err);
		}
		
		// A child can not set up a savings account
		if (bankAccount.getAccountType().equals("child")) {
			String err = buildError(500, "A child can not open a savings account.");
			return respondError(err);
		}
		
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not peek at the overdraft limit of another person's bank account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		// Check if the bank account is closed
		if (bankAccount.getClosed()) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. The main bank account is closed.");
			return respondError(err);	
		} else {
			// Check if savings account is closed
			if (bankAccount.getSavingsAccount().isClosed()) {
				bankAccount.getSavingsAccount().setClosed(false);
				bankAccount.getSavingsAccount().saveToDB();
				return sendEmptyResult(jReq.getMethod());
			} else {
				String err = buildError(420, "The action has no effect. See message.", "The savings account is already open.");
				return respondError(err);
			}			
		}
	}

	private static Response closeSavingsAccount(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();	
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidSavingsAccountRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// Check if the customer has a bank account with this IBAN
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(500, "An unexpected error occured, see error details", "A bank account with the given IBAN does not exist.");
			return respondError(err);
		}
		
		// An administrator can not close a savings account
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not close a savings account.");
			return respondError(err);
		}
		
		// An administrator can not close a savings account
		if (bankAccount.getAccountType().equals("child")) {
			String err = buildError(500, "A child can not close a savings account.");
			return respondError(err);
		}
		
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not peek at the overdraft limit of another person's bank account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		// Check if the savings account is already closed
		if (bankAccount.getSavingsAccount().isClosed()) {
			String err = buildError(420, "The action has no effect. See message.", "The savings account is already closed.");
			return respondError(err);
		} else {
			// Transfer the balance of the savings account to the real account
			double savingsBalance = bankAccount.getSavingsAccount().getBalance();
			if (savingsBalance != 0) {
				try {
					bankAccount.getSavingsAccount().transfer(savingsBalance);
				} catch (IllegalAmountException | IllegalTransferException e) {
					e.printStackTrace();
				}
			}
			
			// Close account 
			bankAccount.getSavingsAccount().setClosed(true);
			bankAccount.getSavingsAccount().saveToDB();
			return sendEmptyResult(jReq.getMethod());
		}
	}

	private static Response getOverdraftLimit(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetOverdraftLimitRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}
	
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");		
	
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount) && !isAdministrativeUser(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not peek at the overdraft limit of another person's bank account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		HashMap<String, Object> resp = new HashMap<>();	
		resp.put("overdraftLimit", Double.toString(bankAccount.getOverdraftLimit()));
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	/**
	 * Extension 5: 'Overdraft' related.
	 * Sets the overdraft limit of the given IBAN
	 */
	private static Response setOverdraftLimit(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();	
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidSetOverdraftLimitRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}

		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		double overdraftLimit = (double) params.get("overdraftLimit");
		
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// Check if the user actually owns the bank account
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not change the overdraft limit for someone else's bank account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		// A child can not set their overdraft limit
		if (bankAccount.getAccountType().equals("child")) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. Overdraft limit cannot be changed for a child bank account.");
			return respondError(err);
		}
		
		if (overdraftLimit < 0f) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", "The overdraft limit cannot be less than 0");
			return respondError(err);
		} else if (overdraftLimit == bankAccount.getOverdraftLimit()) {
			String err = buildError(420, "The action has no effect. See message.", "The bank account already has this overdraft limit.");
			return respondError(err);
		} else if (overdraftLimit > BankSystemValue.MAX_OVERDRAFT_LIMIT.getAmount()) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", "The overdraft limit cannot be greater than 5000.00.");
			return respondError(err);
		}
		
		bankAccount.setOverdraftLimit(overdraftLimit);
		bankAccount.saveToDB();
		
		return sendEmptyResult(jReq.getMethod());
	}
	
	/**
	 * Extension 2: 'PIN block' related.
	 * Unblocks a given pin card, given the right parameters and given it is actually blocked.
	 */
	private static Response unblockCard(JSONRPC2Request jReq) {	
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidUnblockCardRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}

		String IBAN = (String) params.get("iBAN");
		String pinCard = (String) params.get("pinCard");	
		
		DebitCard debitCard;
		BankAccount bankAccount;
		try {
			debitCard = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, pinCard);
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// Check if pin card is linked with IBAN
		boolean isLinked = false;
		for (DebitCard dc : bankAccount.getDebitCards()) {
			if (dc.getCardNumber().equals(pinCard)) {
				isLinked = true;
				break;
			}
		}
		
		if (!isLinked) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. Pin card is not linked with this IBAN.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		if (debitCard.isBlocked()) {
			debitCard.setBlocked(false);
			debitCard.saveToDB();
			serverModel.getPreviousPinAttempts().remove(pinCard);
		} else {
			String err = buildError(420, "The action has no effect. See message.", "Pincard with number " + pinCard + " is not blocked.");
			return respondError(err);
		}		
	
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	/**
	 * Extension 4: 'Time simulation' related.
	 * Returns the real server time.
	 */
	private static Response getDate(JSONRPC2Request jReq) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, ServerModel.getSimulatedDays());
		Date date = c.getTime();		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		String simulatedDateString = df.format(date);
		
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("date", simulatedDateString);
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	/**
	 * Extension 4: 'Time simulation' related.
	 * Resets the database, i.e. wipes all data.
	 */
	private static Response reset(JSONRPC2Request jReq) {	
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		if (!params.containsKey("authToken")) {
			return RequestValidator.invalidMethodParametersResponse();
		}
		
		String authToken = (String) params.get("authToken");
		
		// An non-administrator can not reset the system
		if (!isAdministrativeUser(authToken)) {
			String err = buildError(500, "Only an administrator can reset the system.");
			return respondError(err);
		}
		
		// Wipe all data from database
		DataManager.wipeAllData();
		ServerModel.reset();
		InterestHandler.reset();
		BankSystemValue.reset();
		
		return sendEmptyResult(jReq.getMethod());
	}
	
	/**
	 * Extension 4: 'Time simulation' related.
	 * Simulates the time for a given number of days.
	 */
	private static Response simulateTime(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidSimulateTimeRequest(params);	
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		String authToken = (String) params.get("authToken");
		
		// An non-administrator can not reset the system
		if (!isAdministrativeUser(authToken)) {
			String err = buildError(500, "Only an administrator can simulate time.");
			return respondError(err);
		}
		
		int newlySimulatedDays = Integer.parseInt(Long.toString((long) params.get("nrOfDays")));
		
		InterestHandler.calculateTimeSimulatedInterest(newlySimulatedDays);
		TimeOperator.simulateDays(newlySimulatedDays);		
		ServerModel.setSimulatedDays(newlySimulatedDays, true);
		
		return sendEmptyResult(jReq.getMethod());
	}

	private static Response getBankAccountAccess(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();

		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetBankAccountAccessRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		CustomerAccount cAcc = accounts.get(authToken);
		BankAccount bAcc = null;
		
		// If the bank account doesn't exist, stop and notify client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account with IBAN " + IBAN + " not found.");
			return respondError(err);
		}
		
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// If the target account is not owned by the authorized user or is not an administrative account, 
		// stop and notify client
		if (!bAcc.getMainHolderBSN().equals(cAcc.getBSN()) && !isAdministrativeUser(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. User does not own the given account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		@SuppressWarnings("rawtypes")
		ArrayList<HashMap> associations = new ArrayList<>();
		ResultSet rs = null;
		
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			rs = s.executeQuery("SELECT * FROM customerbankaccounts WHERE IBAN='" + IBAN + "';");
			while (rs.next()) {
				String BSN = rs.getString("customer_BSN");
				HashMap<String, String> association = new HashMap<>();
				CustomerAccount holder = (CustomerAccount) DataManager.getObjectByPrimaryKey(CustomerAccount.CLASSNAME, BSN);
				association.put("username", holder.getUsername());
				associations.add(association);
			}
		} catch (SQLException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", "SQL error occurred on server.");
			return respondError(err);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(associations, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response getUserAccess(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetUserAccessRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}	
		
		String authToken = (String) params.get("authToken");		
		CustomerAccount cAcc = accounts.get(authToken);
		
		@SuppressWarnings("rawtypes")
		ArrayList<HashMap> associations = new ArrayList<>();
		ResultSet rs = null;		

		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			rs = s.executeQuery("SELECT * FROM customerbankaccounts WHERE customer_BSN='" + cAcc.getBSN() + "';");
			while (rs.next()) {
				String BSN = rs.getString("customer_BSN");
				if (BSN.equals(cAcc.getBSN())) {
					HashMap<String, String> association = new HashMap<>();
					String IBAN = rs.getString("IBAN");
					BankAccount bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
					CustomerAccount owner = (CustomerAccount) DataManager.getObjectByPrimaryKey(CustomerAccount.CLASSNAME, bAcc.getMainHolderBSN());
					association.put("iBAN", IBAN);
					association.put("owner", owner.getUsername());
					associations.add(association);
				}
			}
		} catch (SQLException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", "SQLException occurred on server.");
			return respondError(err);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(associations, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response openAccount(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();	

		CustomerAccount newAcc = new CustomerAccount();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidOpenAccountRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		boolean isChild = params.containsKey("type") && params.get("type").equals("child");
		
		// Create a CustomerAccount instance with the given details
		newAcc.setName((String)params.get("name"));
		newAcc.setSurname((String)params.get("surname"));
		newAcc.setInitials((String)params.get("initials"));
		newAcc.setBirthdate((String)params.get("dob"));
		newAcc.setBSN((String)params.get("ssn"));
		newAcc.setStreetAddress((String)params.get("address"));
		newAcc.setPhoneNumber((String)params.get("telephoneNumber"));
		newAcc.setEmail((String)params.get("email"));
		newAcc.setUsername((String)params.get("username"));
		newAcc.setPassword((String)params.get("password"));
		
		// Check if the username is 'admin'
		if (newAcc.getUsername().equals("admin")) {
			String err = buildError(500, "User attempted to create an account with the username 'admin'");
			return respondError(err);
		}
		
		// If this is a duplicate account, respond with an appropriate error
		if (DataManager.objectExists(newAcc)) {
			String err = buildError(500, "User attempted to create duplicate customer account with SSN " + newAcc.getBSN());
			return respondError(err);
		}
		
		// If this is not a duplicate account, open a bank account for it and save it to DB
		BankAccount bankAcc = newAcc.openBankAccount();
		
		// If it is a child account, add the guardians with access to this account
		if (isChild) {
			// Check of the age of the child is below 18
			if (!CustomerAccount.isYoungerThan(18, newAcc)) {
				String err = buildError(500, "The child account should be owned by a child younger than 18 years old.");
				return respondError(err);
			}
			bankAcc.setAccountType("child");
			int nrOfGuardians = ((JSONArray) params.get("guardians")).size();
			String[] guardians = new String[nrOfGuardians];
			((JSONArray) params.get("guardians")).toArray(guardians);
			System.out.println(Arrays.toString(guardians));
			if (guardians.length < 1) {
				String err = buildError(500, "No guardians are specified; at least one guardian should be specified");
				return respondError(err);
			}
			for (int i = 0; i < guardians.length; i++) {
				CustomerAccount guardian = CustomerAccount.getAccountByName(guardians[i]);
				if (CustomerAccount.isValidGuardian(guardian)) {
					guardian.addBankAccount(bankAcc);
					guardian.saveToDB();
				} else {
					String err = buildError(500, "Person " + guardians[i] + " is not a valid guardian.");
					return respondError(err);
				}
			}
			
			TimeEvent t = new TimeEvent();
			t.setName("ACCOUNT_TYPE_CHANGE");
			t.setDescription("FOR:" + bankAcc.getIBAN());
			Calendar c = Calendar.getInstance();
			long millis = 0;
			try {
				millis = Logger.parseDateToMillis((String) params.get("dob"), "dd-MM-yyyy");
				c.setTimeInMillis(millis);
				c.add(Calendar.YEAR, 18);
				t.setTimestamp(c.getTimeInMillis());
				t.setExecuted(false);
				t.saveToDB();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		DebitCard card = new DebitCard(newAcc.getBSN(), bankAcc.getIBAN());
		newAcc.addBankAccount(bankAcc);
		newAcc.saveToDB();
		card.saveToDB();
		
		String IBAN = bankAcc.getIBAN();
		String pinCard = card.getCardNumber();
		String pinCode = card.getPIN();
		
		
		// Create the JSON response and send it to the client
		HashMap<String, Object> resp = new HashMap<>();
		
		resp.put("iBAN", IBAN);
		resp.put("pinCard", pinCard);
		resp.put("pinCode", pinCode);
		resp.put("expirationDate", card.getExpirationDate());
		
		JSONRPC2Response response = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(response.toJSONString(), jReq.getMethod());
	}

	private static Response openAdditionalAccount(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();		
		
		String authToken = (String) params.get("authToken");
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidOpenAdditionalAccountRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		// An admin can not open an additional account
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not open an additional account.");
			return respondError(err);
		}
		
		// If the user IS authorized, open a new account under his user account.
		CustomerAccount acc = accounts.get(authToken);
		
		// A child can not open multiple accounts
		if (CustomerAccount.isYoungerThan(18, acc)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. A child can not open multiple bank accounts.");
			return respondError(err);
		}
		
		BankAccount bAcc = acc.openBankAccount();
		DebitCard card = new DebitCard(acc.getBSN(), bAcc.getIBAN());
		String IBAN = bAcc.getIBAN();
		String pinCard = card.getCardNumber();
		String pinCode = card.getPIN();
		acc.addBankAccount(bAcc);
		acc.saveToDB();
		card.saveToDB();
		
		// Send the user the details of his new account and card
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("iBAN", IBAN);
		resp.put("pinCard", pinCard);
		resp.put("pinCode", pinCode);
		resp.put("expirationDate", card.getExpirationDate());
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response closeAccount(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidCloseAccountRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		CustomerAccount acc = accounts.get(authToken);
		boolean found = false;
		BankAccount target = null;
		
		// An admin can not open an additional account
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not close an account.");
			return respondError(err);
		}
		
		// Look for the bank account
		for (BankAccount b : acc.getBankAccounts()) {
			if (b.getIBAN().equals(IBAN)) {
				if (b.getClosed()) {
					String err = buildError(420, "The action has no effect. See message.", "Account " + IBAN + " is already closed");
					return respondError(err);
				}
				found = true;
				try {
					target = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
				} catch (ObjectDoesNotExistException e) {
					e.printStackTrace();
				}
				break;
			}
		}
		
		// If the bank account doesn't exist under the authenticated user account, send an error
		if (!found) {
			String err = buildError(500, "No account found with the specified IBAN under user account " + acc.getUsername() + ".");
			return respondError(err);
		}
		
		// If the target account is not owned by the user, stop and notify the client
		if (!acc.getBSN().equals(target.getMainHolderBSN()) ) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. User does not own this account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}		

		try {
			target.close();
			target.saveToDB();
		} catch (IllegalAccountCloseException e1) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. " + e1.toString());
			return respondError(err);
		}
		
		// Check if this was the customer's last bank account
		boolean lastAcc = true;
		for (BankAccount b : acc.getBankAccounts()) {
			if (!b.getClosed()) {
				lastAcc = false;
			}
		}
		
		// If this was the customer's last bank account, vaporize the customer
		if (lastAcc) {
			try {
				acc.SQLdeleteFromDB();
			} catch (SQLException e) {
				String err = buildError(500, "One or more parameter has an invalid value. See message.", "An SQL error occurred on the server.");
				return respondError(err);
			}
		}
		
		// If all is well, respond with true.
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response provideAccess(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		String username = (String) params.get("username");

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidProvideAccessRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		CustomerAccount cAcc = accounts.get(authToken);
		BankAccount bAcc = null;
		boolean found = false;
		
		// An admin can not provide access to an account
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not provide access to an account.");
			return respondError(err);
		}
		
		for (BankAccount b : cAcc.getBankAccounts()) {
			if (b.getIBAN().equals(IBAN)) {
				found = true;
				bAcc = b;
			}
		}
		
		// If we couldn't find the bank account, tell the client
		if (!found) {
			String err = buildError(500, "Could not find the specified bank account with IBAN " + IBAN + " under user account " + cAcc.getUsername() + ".");
			return respondError(err);
		}
		
		// A child can not provide access
		if (bAcc.getAccountType().equals("child")) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. A child can not provide access to the bank account.");
			return respondError(err);
		}
		
		// If the sender is not the owner of the account, stop and notify the client
		if (!bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You are not the owner of this account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.eq("username", username));
		@SuppressWarnings("unchecked")
		ArrayList<CustomerAccount> target = (ArrayList<CustomerAccount>) DataManager.getObjectsFromDB(CustomerAccount.CLASSNAME, cr);
		
		// If we couldn't find the target user, tell the client
		if (target.size() == 0) {
			String err = buildError(500, "Could not find user " + username + ".");
			return respondError(err);
		}
		
		// If the target user already has access, stop and notify the client
		for (CustomerAccount c : bAcc.getOwners()) {
			if (c.getUsername().equals(username)) {
				String err = buildError(420, "The action has no effect. See message.", "User " + username + " already has access to account " + bAcc.getIBAN());
				return respondError(err);
			}
		}
		
		CustomerAccount targetAcc = null;;
		
		for (CustomerAccount acc : target) {
			targetAcc = acc;
			break;
		}
		
		// If everything is fine, create the new card for the target user, tell the client the details
		targetAcc.addBankAccount(bAcc);
		DebitCard card = new DebitCard(targetAcc.getBSN(), bAcc.getIBAN());
		card.saveToDB();
		targetAcc.saveToDB();
		String pinCard = card.getCardNumber();
		String pinCode = card.getPIN();
		
		HashMap<String, Object> resp = new HashMap<>();
		
		resp.put("pinCard", pinCard);
		resp.put("pinCode", pinCode);
		resp.put("expirationDate", card.getExpirationDate());
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response revokeAccess(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
	
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidRevokeAccessRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		// An admin can not open revoke access from an account
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not revoke access from an account.");
			return respondError(err);
		}
		
		boolean usernameSpecified = false;
		String username = null;
		if (params.containsKey("username")) {
			username = (String) params.get("username");
			usernameSpecified = true;
		}
		
		CustomerAccount cAcc = accounts.get(authToken);
		BankAccount bAcc = null;
		boolean found = false;
		
		for (BankAccount b : cAcc.getBankAccounts()) {
			if (b.getIBAN().equals(IBAN)) {
				found = true;
				bAcc = b;
			}
		}
		
		// If we couldn't find the bank account, tell the client
		if (!found) {
			String err = buildError(500, "Could not find the specified bank account with IBAN " + IBAN + ".");
			return respondError(err);
		}
		
		// A child can not provide access
		if (bAcc.getAccountType().equals("child")) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. A child can not provide access to the bank account.");
			return respondError(err);
		}
		
		// If the user doesn't own the account he wants to revoke someone's access from, stop and notify the client
		// Alternatively, if the user is trying to revoke his own privileges from an account he owns, stop and notify.
		if (usernameSpecified && !bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You don't have the right to revoke "
					+ "someone's access from this account");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		} else if (!usernameSpecified && bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(500, "An unexpected error occured, see error details.", "You are the owner of account " + IBAN 
					+ ", so you cannot revoke your own privileges.");
			return respondError(err);
		}
		
		CustomerAccount targetAcc = null;
		
		if (usernameSpecified) {
			ArrayList<Criterion> cr = new ArrayList<>();
			cr.add(Restrictions.eq("username", username));
			@SuppressWarnings("unchecked")
			ArrayList<CustomerAccount> target = (ArrayList<CustomerAccount>) DataManager.getObjectsFromDB(CustomerAccount.CLASSNAME, cr);
			
			// If we couldn't find the target user, tell the client
			if (target.size() == 0) {
				String err = buildError(500, "Could not find user " + username + ".");
				return respondError(err);
			}
			
			for (CustomerAccount acc : target) {
				targetAcc = acc;
				break;
			}
		} else {
			targetAcc = accounts.get(authToken);
		}
		
		boolean hasAccess = false;
		for (CustomerAccount c : bAcc.getOwners()) {
			if (c.getUsername().equals(targetAcc.getUsername())) {
				hasAccess = true;
				break;
			}
		}
		
		// If the target user does not have access, the method has no effect
		if (!hasAccess) {
			String err = buildError(420, "The action has no effect. See message.", "User " + targetAcc.getUsername() + " has no access to account " + IBAN + ".");
			return respondError(err);
		}
		
		// If everything is fine, delete all cards creating an association between the target user and bank account, notify the client
		for (DebitCard dc : bAcc.getDebitCards()) {
			if (dc.getHolderBSN().equals(targetAcc.getBSN())) {
				dc.deleteFromDB();
				bAcc.getDebitCards().remove(dc);
			}
		}
		
		SQLiteDB.executeStatement("DELETE FROM customerbankaccounts WHERE customer_BSN='" + targetAcc.getBSN() + "' AND IBAN='" + bAcc.getIBAN() + "'");
		bAcc.removeOwner(targetAcc.getBSN());
		
		bAcc.saveToDB();
		targetAcc.saveToDB();
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response depositIntoAccount(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidDepositIntoAccountRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		String IBAN = (String) params.get("iBAN");
		String pinCard = (String) params.get("pinCard");
		String pinCode = (String) params.get("pinCode");
		double amount = (double) params.get("amount");		
		
		// If the card could not be found, notify the client and stop
		if (DataManager.isPrimaryKeyUnique(DebitCard.CLASSNAME, DebitCard.PRIMARYKEYNAME, pinCard)) {
			String err = buildError(500, "Could not find debit card " + pinCard);
			return respondError(err);
		}
		
		DebitCard dc;
		try {
			dc = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, pinCard);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// If the card is not yet blocked and the wrong PIN is given, slap the client
		if (!dc.isBlocked() && !dc.isValidPIN(pinCode)) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: PINcard, -code or -combination was incorrect.");
			serverModel.increaseInvalidPinAttempt(pinCard);
			
			if (!dc.isBlocked() && serverModel.getPreviousPinAttempts().get(pinCard) >= BankSystemValue.CARD_USAGE_ATTEMPTS.getAmount()) {
				dc.setBlocked(true);
				dc.saveToDB();
			}
			
			return respondError(err);
		}
		
		// If the specified account does not exist, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "Could not find bank account with IBAN " + IBAN);
			return respondError(err);
		}
		
		BankAccount bAcc;
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		try {
			bAcc.deposit(amount, pinCard);
		} catch (PinCardBlockedException e) {
			String err = buildError(419, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		} catch (IllegalAmountException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		} catch (ClosedAccountTransferException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		}
		
		bAcc.saveToDB();
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response payFromAccount(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();		
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidPayFromAccountRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		String sourceIBAN = (String) params.get("sourceIBAN");
		String targetIBAN = (String) params.get("targetIBAN");
		String pinCard = (String) params.get("pinCard");
		String pinCode = (String) params.get("pinCode");
		double amount = (double) params.get("amount");
		
		
		// If the source bank account could not be found, stop and notify the client.
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, sourceIBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Account " + sourceIBAN + " could not be found.");
			return respondError(err);
		}
		
		// If the debit card could not be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(DebitCard.CLASSNAME, DebitCard.PRIMARYKEYNAME, "" + pinCard)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Card " + pinCard + " could not be found.");
			return respondError(err);
		}
		
		DebitCard card;
		try {
			card = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, "" + pinCard);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// If the payment goes wrong, stop and report the exception
		try {
			card.pinPayment(amount, pinCode, targetIBAN);
		} catch (PinCardBlockedException e) {
			String err = buildError(419, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		} catch (InvalidPINException e) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			serverModel.increaseInvalidPinAttempt(pinCard);
			
			if (serverModel.getPreviousPinAttempts().get(pinCard) >= BankSystemValue.CARD_USAGE_ATTEMPTS.getAmount()) {
				card.setBlocked(true);
				card.saveToDB();
			}
			
			return respondError(err);
		} catch (IllegalAmountException | IllegalTransferException | ExpiredCardException | ExceedLimitException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		}
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response invalidateCard(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidInvalidateCardRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}			
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		String pinCardNumber = (String) params.get("pinCard");
		boolean newPinCode = (boolean) params.get("newPin");		
		
		// An admin can not invalidate a card
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not invalidate a card.");
			return respondError(err);
		}
				
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		boolean authorized = false;
		
		if (customerAccount.getBSN().equals(bankAccount.getMainHolderBSN())) {
			authorized = true;
		} else {
			for (CustomerAccount c : bankAccount.getOwners()) {
				if (c.getBSN().equals(customerAccount.getBSN())) {
					authorized = true;
				}
			}
		}
		
		// If the user is trying to invalidate someone else's pincard
		if (!authorized) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not invalidate someone else's pin card.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		// If there is no such pincard for this bankAccount
		List<DebitCard> debitCards = bankAccount.getDebitCards();
		DebitCard currentDebitCard = null;
		for (DebitCard db : debitCards) {
			if (db.getCardNumber().equals(pinCardNumber)) {
				currentDebitCard = db;
				break;
			}
		}
		
		if (currentDebitCard == null) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. The user has no access to such a pin card with number " + pinCardNumber);
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		DebitCard newDebitCard = null;
		if (newPinCode) {
			newDebitCard = new DebitCard(customerAccount.getBSN(), bankAccount.getIBAN());
		} else {
			newDebitCard = new DebitCard(customerAccount.getBSN(), bankAccount.getIBAN(), currentDebitCard.getPIN());
		}
		
		BankAccount feeDestinationBankAccount;
		try {
			feeDestinationBankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, "NL36INGB8278309172");
			bankAccount.transfer(feeDestinationBankAccount, BankSystemValue.NEW_CARD_COST.getAmount(), "Fee for new pincard", "ING");
		} catch (ObjectDoesNotExistException | IllegalAmountException | IllegalTransferException | ExceedLimitException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		}
		
		
		bankAccount.saveToDB();
		newDebitCard.saveToDB();
		currentDebitCard.deleteFromDB();
		
		// Send response
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("pinCard", newDebitCard.getCardNumber());
		
		if (newPinCode) {
			resp.put("pinCode", newDebitCard.getPIN());
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
		
	}

	private static Response transferMoney(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidTransferMoneyRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		String authToken = (String) params.get("authToken");		
		
		// An admin can not transfer money
		if (isAdministrativeUser(authToken)) {
			String err = buildError(500, "An administrator can not transfer money.");
			return respondError(err);
		}
		
		String sourceIBAN = (String) params.get("sourceIBAN");
		boolean isSourceSavingsAccount = sourceIBAN.charAt(sourceIBAN.length() - 1) == 'S' ? true : false;
		if (isSourceSavingsAccount) {
			sourceIBAN = sourceIBAN.substring(0, sourceIBAN.length() - 1);
		}
		
		String targetIBAN = (String) params.get("targetIBAN");
		boolean isTargetSavingsAccount = targetIBAN.charAt(targetIBAN.length() - 1) == 'S' ? true : false;
		if (isTargetSavingsAccount) {
			targetIBAN = targetIBAN.substring(0, targetIBAN.length() - 1);
		}		
		
		String targetName = (String) params.get("targetName");
		String description = (String) params.get("description");
		double amount = (double) params.get("amount");		
		
		CustomerAccount customerAccount = null;
		BankAccount source = null;
		BankAccount destination = null;
		boolean authorized = false;		
		
		customerAccount = accounts.get(authToken);		
		try {
			source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, sourceIBAN);
			destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, targetIBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		// Check if the transferer owns the source bank account
		if (customerAccount.getBSN().equals(source.getMainHolderBSN())) {
			authorized = true;
		} else {
			for (CustomerAccount c : source.getOwners()) {
				if (c.getBSN().equals(customerAccount.getBSN())) {
					authorized = true;
				}
			}
		}
		
		// Check if savings account A -> main account B
		if (isSourceSavingsAccount && !sourceIBAN.equals(targetIBAN)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not transfer money from your savings account to some other main bank account/savings account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		// Check if main account A -> savings account B
		if (isTargetSavingsAccount && !targetIBAN.equals(sourceIBAN)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not transfer money from your main bank account to some other bank account/savings account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		// Check if savings account A -> savings account A/B
		if (isTargetSavingsAccount && isSourceSavingsAccount) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not transfer money between savings accounts.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		// If the client is trying to transfer money from someone else's account, send an error
		if (!authorized) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not transfer money from someone else's account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to manipulate information from another account.");
			return respondError(err);
		}
		
		// If something goes wrong with the transfer, stop and report it
		try {
			if (!isSourceSavingsAccount && !isTargetSavingsAccount) {
				source.transfer(destination, amount, description, targetName);
			} else if (isSourceSavingsAccount) {
				source.getSavingsAccount().transfer(amount);
			} else if (isTargetSavingsAccount) {
				destination.transfer(amount);
			}
			
		} catch (IllegalAmountException | IllegalTransferException | ExceedLimitException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err);
		}

		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response getAuthToken(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetAuthTokenRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}
		
		ArrayList<Criterion> cr = new ArrayList<>();
		
		String username = (String) params.get("username");
		String password = (String) params.get("password");
		
		// Get the object with the given username/password combination (always unique)
		cr.add(Restrictions.eq("username", username));
		cr.add(Restrictions.eq("password", password));
		@SuppressWarnings("unchecked")
		ArrayList<CustomerAccount> list = (ArrayList<CustomerAccount>) DataManager.getObjectsFromDB(CustomerAccount.CLASSNAME, cr);
		
		// Fetch the appropriate CustomerAccount
		CustomerAccount account = null;
		for (CustomerAccount cAcc : list) {
			account = cAcc;
			break;
		}
		
		// If the account is not found, return the appropriate error
		if (list.size() == 0) {
			String err = buildError(422, "The user could not be authenticated: Invalid username, password or combination.");			
			return respondError(err);
		}
		
		// If the account is already logged in, return error
		if (accounts.containsValue(account)) {
			String err = buildError(500, "The user is already logged in on this account.");
			return respondError(err);
		}
		
		// Generate the authentication token
		String token = UUID.randomUUID().toString().toUpperCase() + "/" + params.get("username") + "/" + java.lang.System.currentTimeMillis();
		
		// Associate the account with the authentication token
		accounts.put(token, account);
		
		// Send the generated token to the client
		HashMap<String, String> resp = new HashMap<>();
		resp.put("authToken", token);
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	private static Response getBalance(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetBalanceRequest(params);	
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		CustomerAccount cAcc = null;
		BankAccount source = null;
		boolean authorized = false;
		
		
		cAcc = accounts.get(authToken);
		
		// If the bank account can't be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account " + IBAN + " not found.");
			return respondError(err);
		}
		
		try {
			source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		if (cAcc.getBSN().equals(source.getMainHolderBSN()) || isAdministrativeUser(authToken)) {
			authorized = true;
		} else {
			for (CustomerAccount c : source.getOwners()) {
				if (c.getBSN().equals(cAcc.getBSN())) {
					authorized = true;
				}
			}
		}
		
		// If the client is trying to snoop on someone else's account, send an error
		if (!authorized) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can't view the balance of this account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("result", new Double(source.getBalance()));
		
		// If there's a savings account open, send the balance of it
		if (source.getSavingsAccount() != null && !source.getSavingsAccount().isClosed()) {
			resp.put("savingAccountBalance", source.getSavingsAccount().getBalance());
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	@SuppressWarnings("unchecked")
	private static Response getTransactionsOverview(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidGetTransactionsOverviewRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}			
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		long num = (long) params.get("nrOfTransactions");
		
			
		CustomerAccount cAcc = null;
		BankAccount source = null;
		boolean authorized = false;
			
		// If this is a bogus token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return respondError(err);
		}
				
		// If the bank account could not be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account " + IBAN + " could not be found.");
			respondError(err);
		}
		
		try {
			source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", e.toString());
			return respondError(err);
		}
		
		cAcc = accounts.get(authToken);
				
		if (cAcc.getBSN().equals(source.getMainHolderBSN()) || isAdministrativeUser(authToken)) {
			authorized = true;
		} else {
			for (CustomerAccount c : source.getOwners()) {
				if (c.getBSN().equals(cAcc.getBSN())) {
					authorized = true;
				}
			}
		}
			
		// If the client is trying to snoop on someone else's account, send an error
		if (!authorized) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not view the transactions of this account.");
			Logger.addLogToDB(ServerModel.getServerCalendar().getTimeInMillis(), Type.WARNING, "Possible harmful activity: trying to access information from another account.");
			return respondError(err);
		}
		
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.or(Restrictions.eq("sourceIBAN", IBAN + "S"), Restrictions.eq("destinationIBAN", IBAN + "S"), Restrictions.eq("sourceIBAN", IBAN), Restrictions.eq("destinationIBAN", IBAN)));
		List<Transaction> transactions = (List<Transaction>) DataManager.getObjectsFromDB(Transaction.CLASSNAME, cr);
		Collections.sort(transactions);
		Collections.reverse(transactions);
		@SuppressWarnings("rawtypes")
		
		HashMap[] transactionMapsArray;
		
		if (num >= transactions.size()) {
			transactionMapsArray = new HashMap[transactions.size()];
		} else {
			transactionMapsArray = new HashMap[(int) num];
		}
		
		long counter = num;
		for (int i = 0; i < transactionMapsArray.length & counter > 0; i++) {
			HashMap<String, String> tMap = new HashMap<>();
			Transaction t = transactions.get(i);			
			
			if (t.getSourceIBAN() != null) {
				tMap.put("sourceIBAN", t.getSourceIBAN());				
			} else {
				tMap.put("sourceIBAN", "N/A");
			}
			
			if (t.getDestinationIBAN() != null) {
				tMap.put("targetIBAN", t.getDestinationIBAN());
			} else {
				tMap.put("targetIBAN", "N/A");
			}
			
			if (t.getTargetName() != null) {
				tMap.put("targetName", t.getTargetName());
			} else {
				tMap.put("targetName", "N/A");
			}
			
			tMap.put("date", t.getDateTime());
			tMap.put("amount", Double.toString(t.getAmount()));
			tMap.put("description", t.getDescription());
			transactionMapsArray[i] = tMap;
			
			counter--;
		}
			
		JSONRPC2Response jResp = new JSONRPC2Response(transactionMapsArray, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString(), jReq.getMethod());
	}

	public static HashMap<String, CustomerAccount> getAccounts() {
		return accounts;
	}
	
	public static boolean isAdministrativeUser(String authToken) {
		if (accounts.containsKey(authToken)) {
			return accounts.get(authToken).getUsername().equals("admin");
		} else return false;
	}
	
	
}
