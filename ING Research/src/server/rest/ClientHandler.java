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
import database.DataManager;
import database.SQLiteDB;
import server.core.InputValidator;
import exceptions.ClosedAccountTransferException;
import exceptions.ExpiredCardException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InvalidPINException;

@Path("/banking")
public class ClientHandler {
	private static HashMap<String, CustomerAccount> accounts = new HashMap<>();
	
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
	
	private static Response respond(String jResp) {
		return Response.status(200).entity(jResp).build();
	}
	
	private static Response respondError(String jResp, int code) {
		return Response.status(500).entity(jResp).build();	
	}
	
	private static Response getDate(JSONRPC2Request jReq) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String systemDate = df.format(date);		
		
		HashMap<String, Object> resp = new HashMap<>();
		resp.put("date", systemDate);
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}
	
	private static Response reset(JSONRPC2Request jReq) {	
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}
	
	private static Response simulateTime(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
	
		// If the required parameters aren't present, stop and notify the client
		if (!params.containsKey("nrOfDays")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String nrOfDays = (String) params.get("nrOfDays");
		
		// If input is invalid (i.e. not a numerical value)
		if (!InputValidator.isNumericalOnly(nrOfDays)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", nrOfDays + " is not a valid number.");
			return respondError(err, 500);
		}
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response getBankAccountAccess(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// If the required parameters aren't present, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// If the provided IBAN is not an IBAN, stop and notify the client
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		// If token is invalid, stop and notify client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		CustomerAccount cAcc = accounts.get(authToken);
		BankAccount bAcc = null;
		
		// If the bank account doesn't exist, stop and notify client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account with IBAN " + IBAN + " not found.");
			return respondError(err, 500);
		}
		
		bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		
		// If the target account is not owned by the authorized user, stop and notify client
		if (!bAcc.getMainHolderBSN().equals(cAcc.getBSN())) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(associations, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response getUserAccess(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		// If no authToken has been sent, stop and notify the client
		if (!params.containsKey("authToken")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String authToken = (String) params.get("authToken");
		
		// If the authToken is invalid, stop and notify the client 
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
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
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(associations, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response openAccount(JSONRPC2Request jReq) {
		CustomerAccount newAcc = new CustomerAccount();
		Map<String, Object> params = jReq.getNamedParams();
		
		// If the request is missing any required parameters, stop and notify the client
		if (!params.containsKey("name") || !params.containsKey("surname") || !params.containsKey("initials") 
				|| !params.containsKey("dob") || !params.containsKey("ssn") || !params.containsKey("address") 
				|| !params.containsKey("telephoneNumber") || !params.containsKey("email") || !params.containsKey("username") 
				|| !params.containsKey("password")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		// Check some of the param values for validity
		if (!InputValidator.isValidEmailAddress((String) params.get("email"))) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", params.get("email") + " is not a valid email address.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidBSN((String) params.get("ssn"))) {
		    String err = buildError(418, "One or more parameter has an invalid value. See message.", params.get("BSN") + " is not a valid BSN.");
		    return respondError(err, 500);
		}
		
		if (!InputValidator.isValidName((String) params.get("username"))) {
		    String err = buildError(418, "One or more parameter has an invalid value. See message.", "Username " + params.get("username") + " contains invalid characters.");
		    return respondError(err, 500);
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
		
		// If no authToken is provided, stop and notify the client.
		if (!params.containsKey("authToken")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String token = (String) params.get("authToken");
		
		// If this is a bogus token, slap the client
		if (!accounts.keySet().contains(token)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
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
		
		// If not all required parameters are sent, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// If this is a bogus token, slap the client
		if (!accounts.keySet().contains(token)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		// If the provided IBAN is invalid, stop and notify the client.
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
			return respondError(err, 500);
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("username")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		String username = (String) params.get("username");
		
		// If the token is bogus, slap the client
		if (!accounts.keySet().contains(token)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN");
			return respondError(err, 500);
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		
		// If the request is missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		boolean usernameSpecified = false;
		String username = null;
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		if (params.containsKey("username")) {
			username = (String) params.get("username");
			usernameSpecified = true;
		}
		
		// If the token is bogus, slap the client
		if (!accounts.keySet().contains(token)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		// If the provided IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
			return respondError(err, 500);
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		
		// If the request is missing required parameters, stop and notify the client 
		if (!params.containsKey("iBAN") || !params.containsKey("pinCard") || !params.containsKey("pinCode")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String IBAN = (String) params.get("iBAN");
		String pinCard = (String) params.get("pinCard");
		String pinCode = (String) params.get("pinCode");
		double amount = 0;
		
		// If any given parameter values are invalid, stop and notify the client
		try {
			amount = Double.parseDouble((String)params.get("amount"));
		} catch (ClassCastException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", params.get("amount") + " is not a valid amount.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN+ " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidPIN(pinCode)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", pinCode + " is not a valid PIN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidCardNumber(pinCard)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", pinCard + " is not a valid PIN.");
			return respondError(err, 500);
		}
		
		// If the card could not be found, notify the client and stop
		if (DataManager.isPrimaryKeyUnique(DebitCard.CLASSNAME, DebitCard.PRIMARYKEYNAME, pinCard)) {
			String err = buildError(500, "Could not find debit card " + pinCard);
			return respondError(err, 500);
		}
		
		DebitCard dc = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, pinCard);		
		
		// If this is the wrong PIN, slap the client
		if (!dc.isValidPIN(pinCode)) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			return respondError(err, 500);
		}
		
		// If the specified account does not exist, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "Could not find bank account with IBAN " + IBAN);
			return respondError(err, 500);
		}
		
		BankAccount bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		
		try {
			bAcc.deposit(amount, pinCard);
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
		
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("sourceIBAN") || !params.containsKey("targetIBAN") || !params.containsKey("pinCard") 
				|| !params.containsKey("pinCode") || !params.containsKey("amount")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String sourceIBAN = (String) params.get("sourceIBAN");
		String targetIBAN = (String) params.get("targetIBAN");
		String pinCard = (String) params.get("pinCard");
		String pinCode = (String) params.get("pinCode");
		double amount = 0;
		
		// If any of the parameters have invalid values, stop and notify the client
		try {
			amount = Double.parseDouble((String) params.get("amount"));
		} catch (ClassCastException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", amount + " is not a valid amount.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN(sourceIBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", sourceIBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN(targetIBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", targetIBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidPIN(pinCode)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", pinCode + " is not a valid PIN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidCardNumber(pinCard)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", pinCard + " is not a valid PIN.");
			return respondError(err, 500);
		}
		
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
		
		DebitCard card = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, "" + pinCard);
		
		// If the payment goes wrong, stop and report the exception
		try {
			card.pinPayment(amount, pinCode, targetIBAN);
		} catch (IllegalAmountException | IllegalTransferException | ExpiredCardException e) {
			String err = buildError(500, "An unexpected error occured, see error details.", e.toString());
			return respondError(err, 500);
		} catch (InvalidPINException e) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			return respondError(err, 500);
		}
		
		HashMap<String, Object> resp = new HashMap<>();
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response invalidateCard(JSONRPC2Request jReq) {
		Map<String, Object> params = jReq.getNamedParams();
		
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || 
				!params.containsKey("pinCard")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		String pinCardNumber = (String) params.get("pinCard");
		String newPinCodeString = (String) params.get("newPin");
		boolean newPinCode = false;
		
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isNumericalOnly(pinCardNumber)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", pinCardNumber + " is not a valid card number");
			return respondError(err, 500);
		}
		
		if (newPinCodeString.equals("yes")) {
			newPinCode = true;
		} else if (!newPinCodeString.equals("yes") && !newPinCodeString.equals("no")) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", "'" + newPinCodeString + "' is not a valid boolean representation.");
			return respondError(err, 500);
		}
		
		// If this is a bogus auth token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		CustomerAccount customerAccount = accounts.get(authToken);
		BankAccount bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		
		// If required parameters are missing from the request, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("sourceIBAN") || !params.containsKey("targetIBAN") 
				|| !params.containsKey("targetName") || !params.containsKey("amount") || !params.containsKey("description")) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String authToken = (String) params.get("authToken");
		String sourceIBAN = (String) params.get("sourceIBAN");
		String targetIBAN = (String) params.get("targetIBAN");
		String targetName = (String) params.get("targetName");
		String description = (String) params.get("description");
		double amount = 0;
		
		// If any parameter has an invalid value, stop and notify the client
		try {
			amount = Double.parseDouble((String) params.get("amount"));
		} catch (ClassCastException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", amount + " is not a valid amount.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN(sourceIBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", sourceIBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN(targetIBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", targetIBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		CustomerAccount cAcc = null;
		BankAccount source = null;
		BankAccount destination = null;
		boolean authorized = false;
		
		// If this is a bogus auth token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		cAcc = accounts.get(authToken);
		
		source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, sourceIBAN);
		
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, targetIBAN);
		
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
		
		ArrayList<Criterion> cr = new ArrayList<>();
		
		if (!(params.containsKey("username") && params.containsKey("password"))) {
			String err = buildError(-32602, "Invalid method parameters.");
			respondError(err, 500);
		}
		
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
		
		// If the request is missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = buildError(-32602, "Invalid method parameters.");
			return respondError(err, 500);
		}
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		CustomerAccount cAcc = null;
		BankAccount source = null;
		boolean authorized = false;
		
		// If this is a bogus token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
			return respondError(err, 500);
		}
		
		cAcc = accounts.get(authToken);
		
		// If the bank account can't be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account " + IBAN + " not found.");
			return respondError(err, 500);
		}
		
		source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		
		// If we're missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN") && params.containsKey("nrOfTransactions"))) {
			String err = buildError(-32602, "Invalid method parameters.");
			respondError(err, 500);
		}
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		long num = 0;
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN(IBAN)) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
			respondError(err, 500);
		}
		
		// If the number of transactions is not an integer, stop and notify the client
		try {
			num = (long) params.get("nrOfTransactions");
		} catch (NumberFormatException e) {
			String err = buildError(418, "One or more parameter has an invalid value. See message.", params.get("nrOfTransactions") + " is not a valid amount.");
			return respondError(err, 500);
		}
			
		CustomerAccount cAcc = null;
		BankAccount source = null;
		boolean authorized = false;
			
		// If this is a bogus token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
				
		// If the bank account could not be found, stop and notify the client
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			String err = buildError(500, "An unexpected error occured, see error details.", "Bank account " + IBAN + " could not be found.");
			respondError(err, 500);
		}
		
		source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.or(Restrictions.eq("sourceIBAN", IBAN), Restrictions.eq("destinationIBAN", IBAN)));
		List<Transaction> transactions = (List<Transaction>) DataManager.getObjectsFromDB(Transaction.CLASSNAME, cr);
		Collections.sort(transactions);
		Collections.reverse(transactions);
		@SuppressWarnings("rawtypes")
		ArrayList<HashMap> transactionMaps = new ArrayList<>();
		long counter = num;
		int i;
		for (i = 0; i < transactions.size(); i++) {
			Transaction t = transactions.get(i);
			HashMap<String, String> tMap = new HashMap<>();
			tMap.put("sourceIBAN", t.getSourceIBAN());
			tMap.put("targetIBAN", t.getDestinationIBAN());
			if (!(t.getTargetName() == null)) {
				tMap.put("targetName", t.getTargetName());
			} else {
				tMap.put("targetName", "N/A");
			}
			tMap.put("date", t.getDateTime());
			tMap.put("amount", Double.toString(t.getAmount()));
			tMap.put("description", t.getDescription());
			transactionMaps.add(tMap);
			counter--;
			if (counter == 0) {
				break;
			}
		}
			
		JSONRPC2Response jResp = new JSONRPC2Response(transactionMaps, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}
}
