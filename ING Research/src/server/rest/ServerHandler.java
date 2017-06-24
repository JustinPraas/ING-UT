package server.rest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import client.Client;
import database.DataManager;
import database.SQLiteDB;
import exceptions.ClosedAccountTransferException;
import exceptions.ExpiredCardException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InvalidPINException;
import exceptions.ObjectDoesNotExistException;
import exceptions.PinCardBlockedException;

@Path("/banking")
public class ServerHandler {
	
	private static HashMap<String, CustomerAccount> accounts = new HashMap<>();
	
	//Data-container for this server's session
	private static ServerModel serverModel = new ServerModel();
	
	@POST
	@Path("/postRequest")
	@Consumes(MediaType.APPLICATION_JSON)
	public static Response parseJSONRequest (String request) {
		JSONRPC2Request jReq = null;
		String method;
		
		// Update simulated time for further use
		Client.setSimulatedDays(Client.getSimulatedDaysFromFile(), false);
		
		try {
			jReq = JSONRPC2Request.parse(request);
		} catch (JSONRPC2ParseException e) {
			String err = buildError(-32700, "An error occurred while parsing the JSON input.");
			return respondError(err, 500);
		}
		
		method = jReq.getMethod();
		
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
		case "setOverdraftLimit":
			return setOverdraftLimit(jReq);
		case "getOverdraftLimit":
			return getOverdraftLimit(jReq);
		default:
			String err = buildError(-32601, "The requested remote-procedure does not exist.");
			return respondError(err, 500);
		}
	}

	public static String buildError(int code, String message) {
		JSONRPC2Error jErr = new JSONRPC2Error(code, message);
		JSONRPC2Response jResp = new JSONRPC2Response(jErr, "response-" + java.lang.System.currentTimeMillis());
		return jResp.toJSONString();
	}
	
	public static String buildError(int code, String message, String data) {
		JSONRPC2Error jErr = new JSONRPC2Error(code, message, data);
		JSONRPC2Response jResp = new JSONRPC2Response(jErr, "response-" + java.lang.System.currentTimeMillis());
		return jResp.toJSONString();
	}
	
	public  static Response respond(String jResp) {
		return Response.status(200).entity(jResp).build();
	}
	
	public static Response respondError(String jResp, int code) {
		return Response.status(500).entity(jResp).build();	
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
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not peek at the overdraft limit of another person's bank account.");
			return respondError(err, 500);
		}
		
		HashMap<String, Object> resp = new HashMap<>();	
		resp.put("overdraftLimit", Double.toString(bankAccount.getOverdraftLimit()));
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
		Double overdraftLimit = Double.parseDouble((String) params.get("overdraftLimit"));
		
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		// Check if the user actually owns the bank account
		if (!RequestValidator.userOwnsBankAccount(customerAccount, bankAccount)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not change the overdraft limit for someone else's bank account.");
			return respondError(err, 500);
		}
		
		if (overdraftLimit > 0f) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", "The overdraft limit can't be greater than 0");
			return respondError(err, 500);
		} else if (overdraftLimit == bankAccount.getOverdraftLimit()) {
			String err = buildError(420, "The action has no effect. See message.", "The bank account already has this overdraft limit.");
			return respondError(err, 500);
		} else if (overdraftLimit < -5000f) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", "The overdraft limit go below -5000.00.");
			return respondError(err, 500);
		}
		
		bankAccount.setOverdraftLimit(overdraftLimit);
		bankAccount.saveToDB();
		
		HashMap<String, Object> resp = new HashMap<>();		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
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
			return respondError(err, 500);
		}
		
		if (debitCard.isBlocked()) {
			debitCard.setBlocked(false);
			debitCard.saveToDB();
			serverModel.getPreviousPinAttempts().remove(pinCard);
		} else {
			String err = buildError(420, "The action has no effect. See message.", "Pincard with number " + pinCard + " is not blocked.");
			return respondError(err, 500);
		}		
	
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	/**
	 * Extension 4: 'Time simulation' related.
	 * Returns the real server time. (TODO to be changed)
	 */
	private static Response getDate(JSONRPC2Request jReq) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String systemDate = df.format(date);		
		
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("date", systemDate);
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	/**
	 * Extension 4: 'Time simulation' related.
	 * Resets the database, i.e. wipes all data.
	 */
	private static Response reset(JSONRPC2Request jReq) {	
		HashMap<String, Object> resp = new HashMap<>();
		
		// Wipe all data from database
		DataManager.wipeAllData(true);
		Client.resetSimulatedDays();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
			return respondError(err, 500);
		}
		
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		// If the target account is not owned by the authorized user, stop and notify client
		if (!bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. User does not own the given account.");
			return respondError(err, 500);
		}
		
		@SuppressWarnings("rawtypes")
		ArrayList<HashMap> associations = new ArrayList<>();
		ResultSet rs = null;
		
		try {
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
			return respondError(err, 500);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(associations, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
			return respondError(err, 500);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(associations, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response openAccount(JSONRPC2Request jReq) {
		CustomerAccount newAcc = new CustomerAccount();
		Map<String, Object> params = jReq.getNamedParams();
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidOpenAccountRequest(params);
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
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
		
		// If this is a duplicate account, respond with an appropriate error
		if (DataManager.objectExists(newAcc)) {
			String err = buildError(500, "User attempted to create duplicate customer account with SSN " + newAcc.getBSN());
			return respondError(err, 500);
		}
		
		// If this is not a duplicate account, open a bank account for it and save it to DB
		BankAccount bankAcc = newAcc.openBankAccount();
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
		
		JSONRPC2Response response = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(response.toJSONString());
	}

	private static Response openAdditionalAccount(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();		
		String token = (String) params.get("authToken");
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidOpenAdditionalAccountRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		// If the user IS authorized, open a new account under his user account.
		CustomerAccount acc = accounts.get(token);
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
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		
		return respond(jResp.toJSONString());
	}

	private static Response closeAccount(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidCloseAccountRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		CustomerAccount acc = accounts.get(token);
		boolean found = false;
		BankAccount target = null;
		
		
		// Look for the bank account
		for (BankAccount b : acc.getBankAccounts()) {
			if (b.getIBAN().equals(IBAN)) {
				if (b.getClosed()) {
					String err = buildError(420, "The action has no effect. See message.", "Account " + IBAN + " is already closed");
					return respondError(err, 500);
				}
				b.setClosed(true);
				b.saveToDB();
				acc.saveToDB();
				found = true;
				target = b;
				break;
			}
		}
		
		// If the bank account doesn't exist under the authenticated user account, send an error
		if (!found) {
			String err = buildError(500, "No account found with the specified IBAN under user account " + acc.getUsername() + ".");
			return respondError(err, 500);
		}
		
		// If the target account is not owned by the user, stop and notify the client
		if (!acc.getBSN().equals(target.getMainHolderBSN()) ) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. User does not own this account.");
			return respondError(err, 500);
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
				return respondError(err, 500);
			}
		}
		
		// If all is well, respond with true.
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response provideAccess(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		String username = (String) params.get("username");

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidProvideAccessRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		CustomerAccount cAcc = accounts.get(token);
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
			String err = buildError(500, "Could not find the specified bank account with IBAN " + IBAN + " under user account " + cAcc.getUsername() + ".");
			return respondError(err, 500);
		}
		
		// If the sender is not the owner of the account, stop and notify the client
		if (!bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You are not the owner of this account.");
			return respondError(err, 500);
		}
		
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.eq("username", username));
		@SuppressWarnings("unchecked")
		ArrayList<CustomerAccount> target = (ArrayList<CustomerAccount>) DataManager.getObjectsFromDB(CustomerAccount.CLASSNAME, cr);
		
		// If we couldn't find the target user, tell the client
		if (target.size() == 0) {
			String err = buildError(500, "Could not find user " + username + ".");
			return respondError(err, 500);
		}
		
		// If the target user already has access, stop and notify the client
		for (CustomerAccount c : bAcc.getOwners()) {
			if (c.getUsername().equals(username)) {
				String err = buildError(420, "The action has no effect. See message.", "User " + username + " already has access to account " + bAcc.getIBAN());
				return respondError(err, 500);
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
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response revokeAccess(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();

		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidRevokeAccessRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		boolean usernameSpecified = false;
		String username = null;
		if (params.containsKey("username")) {
			username = (String) params.get("username");
			usernameSpecified = true;
		}
		
		CustomerAccount cAcc = accounts.get(token);
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
			return respondError(err, 500);
		}
		
		// If the user doesn't own the account he wants to revoke someone's access from, stop and notify the client
		// Alternatively, if the user is trying to revoke his own privileges from an account he owns, stop and notify.
		if (usernameSpecified && !bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You don't have the right to revoke "
					+ "someone's access from this account");
			return respondError(err, 500);
		} else if (!usernameSpecified && bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(500, "An unexpected error occured, see error details.", "You are the owner of account " + IBAN 
					+ ", so you cannot revoke your own privileges.");
			return respondError(err, 500);
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
				return respondError(err, 500);
			}
			
			for (CustomerAccount acc : target) {
				targetAcc = acc;
				break;
			}
		} else {
			targetAcc = accounts.get(token);
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
			return respondError(err, 500);
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
		return respond(jResp.toJSONString());
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
		double amount = Double.parseDouble((String) params.get("amount"));		
		
		// If the card could not be found, notify the client and stop
		if (DataManager.isPrimaryKeyUnique(DebitCard.CLASSNAME, DebitCard.PRIMARYKEYNAME, pinCard)) {
			String err = buildError(500, "Could not find debit card " + pinCard);
			return respondError(err, 500);
		}
		
		DebitCard dc;
		try {
			dc = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, pinCard);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		// If the card is not yet blocked and the wrong PIN is given, slap the client
		if (!dc.isBlocked() && !dc.isValidPIN(pinCode)) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			serverModel.increaseInvalidPinAttempt(pinCard);
			
			if (!dc.isBlocked() && serverModel.getPreviousPinAttempts().get(pinCard) >= 3) {
				dc.setBlocked(true);
				dc.saveToDB();
			}
			
			return respondError(err, 500);
		}
		
		// If the specified account does not exist, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "Could not find bank account with IBAN " + IBAN);
			return respondError(err, 500);
		}
		
		BankAccount bAcc;
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		try {
			bAcc.deposit(amount, pinCard);
		} catch (PinCardBlockedException e) {
			String err = buildError(419, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		} catch (IllegalAmountException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		} catch (ClosedAccountTransferException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		}
		
		bAcc.saveToDB();
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
		double amount = Double.parseDouble((String) params.get("amount"));
		
		
		// If the source bank account could not be found, stop and notify the client.
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, sourceIBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Account " + sourceIBAN + " could not be found.");
			return respondError(err, 500);
		}
		
		// If the debit card could not be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(DebitCard.CLASSNAME, DebitCard.PRIMARYKEYNAME, "" + pinCard)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Card " + pinCard + " could not be found.");
			return respondError(err, 500);
		}
		
		DebitCard card;
		try {
			card = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, "" + pinCard);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		// If the payment goes wrong, stop and report the exception
		try {
			card.pinPayment(amount, pinCode, targetIBAN);
		} catch (PinCardBlockedException e) {
			String err = buildError(419, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		} catch (IllegalAmountException | IllegalTransferException | ExpiredCardException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		} catch (InvalidPINException e) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			serverModel.increaseInvalidPinAttempt(pinCard);
			
			if (serverModel.getPreviousPinAttempts().get(pinCard) >= 3) {
				card.setBlocked(true);
				card.saveToDB();
			}
			
			return respondError(err, 500);
		}
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
		boolean newPinCode = false;	
		if (params.get("newPin").equals("yes")) {
			newPinCode = true;
		}			
		
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount;
		try {
			bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
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
			return respondError(err, 500);
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
			return respondError(err, 500);
		}
		
		DebitCard newDebitCard = null;
		if (newPinCode) {
			newDebitCard = new DebitCard(customerAccount.getBSN(), bankAccount.getIBAN());
		} else {
			newDebitCard = new DebitCard(customerAccount.getBSN(), bankAccount.getIBAN(), currentDebitCard.getPIN());
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
		return respond(jResp.toJSONString());
		
	}

	private static Response transferMoney(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();

		// Check Request validity, return an error Response if the Request is invalid
		Response invalidRequest = RequestValidator.isValidTransferMoneyRequest(params);		
		if (invalidRequest != null) {
			return invalidRequest;
		}		
		
		String authToken = (String) params.get("authToken");
		String sourceIBAN = (String) params.get("sourceIBAN");
		String targetIBAN = (String) params.get("targetIBAN");
		String targetName = (String) params.get("targetName");
		String description = (String) params.get("description");
		double amount = Double.parseDouble((String) params.get("amount"));
		
		
		CustomerAccount cAcc = null;
		BankAccount source = null;
		BankAccount destination = null;
		boolean authorized = false;
		
		
		cAcc = accounts.get(authToken);
		
		try {
			source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, sourceIBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		if (cAcc.getBSN().equals(source.getMainHolderBSN())) {
			authorized = true;
		} else {
			for (CustomerAccount c : source.getOwners()) {
				if (c.getBSN().equals(cAcc.getBSN())) {
					authorized = true;
				}
			}
		}
		
		// If the client is trying to transfer money from someone else's account, send an error
		if (!authorized) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. You can not transfer money from someone else's account.");
			return respondError(err, 500);
		}
		
		try {
			destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, targetIBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		// If something goes wrong with the transfer, stop and report it
		try {
			source.transfer(destination, amount, description, targetName);
		} catch (IllegalAmountException | IllegalTransferException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		}

		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
			return respondError(err, 500);
		}
		
		// If the account is already logged in, return error
		if (accounts.containsValue(account)) {
			String err = buildError(500, "The user is already logged in on this account.");
			return respondError(err, 500);
		}
		
		// Generate the authentication token
		String token = UUID.randomUUID().toString().toUpperCase() + "/" + params.get("username") + "/" + java.lang.System.currentTimeMillis();
		
		// Associate the account with the authentication token
		accounts.put(token, account);
		
		// Send the generated token to the client
		HashMap<String, String> resp = new HashMap<>();
		resp.put("authToken", token);
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
			return respondError(err, 500);
		}
		
		try {
			source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		if (cAcc.getBSN().equals(source.getMainHolderBSN())) {
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
			return respondError(err, 500);
		}
		
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("result", new Double(source.getBalance()));
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
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
		int num = Integer.parseInt((String) params.get("nrOfTransactions"));
		
			
		CustomerAccount cAcc = null;
		BankAccount source = null;
		boolean authorized = false;
			
		// If this is a bogus token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return respondError(err, 500);
		}
				
		// If the bank account could not be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account " + IBAN + " could not be found.");
			respondError(err, 500);
		}
		
		try {
			source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message. \n" + e.toString());
			return respondError(err, 500);
		}
		
		cAcc = accounts.get(authToken);
				
		if (cAcc.getBSN().equals(source.getMainHolderBSN())) {
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
			return respondError(err, 500);
		}
		
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.or(Restrictions.eq("sourceIBAN", IBAN), Restrictions.eq("destinationIBAN", IBAN)));
		List<Transaction> transactions = (List<Transaction>) DataManager.getObjectsFromDB(Transaction.CLASSNAME, cr);
		Collections.sort(transactions);
		Collections.reverse(transactions);
		@SuppressWarnings("rawtypes")
		
		HashMap[] transactionMapsArray;
		
		if (num >= transactions.size()) {
			transactionMapsArray = new HashMap[transactions.size()];
		} else {
			transactionMapsArray = new HashMap[num];
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
		return respond(jResp.toJSONString());
	}

	public static HashMap<String, CustomerAccount> getAccounts() {
		return accounts;
	}
	
	
}
