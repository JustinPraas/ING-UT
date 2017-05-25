package server.rest;

import java.util.ArrayList;
import java.util.HashMap;
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
import database.DataManager;
import database.SQLiteDB;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;

@Path("/banking")
public class ClientHandler {
	//TODO Make sure only the right users can use account features. Previously, users could transfer from others' accounts. This is not good.
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
			System.out.println("Discarded unparseable JSON request.");
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
			System.out.println("Discarded invalid JSON-RPC method call.");
			return null;
			//TODO: Respond with error.
		}
	}

	public static String buildError(int code, String message) {
		JSONRPC2Error jErr = new JSONRPC2Error(code, message);
		JSONRPC2Response jResp = new JSONRPC2Response(jErr, "response-" + java.lang.System.currentTimeMillis());
		return jResp.toJSONString();
	}
	
	private static Response respond(String jResp) {
		return Response.status(200).entity(jResp).build();
	}
	
	private static Response respondError(String jResp, int code) {
		return Response.status(500).entity(jResp).build();	
	}

	private static Response getBankAccountAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response getUserAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response openAccount(JSONRPC2Request jReq) {
		// TODO Error handling
		CustomerAccount newAcc = new CustomerAccount();
		Map<String, Object> params = jReq.getNamedParams();
		
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
		HashMap<String, String> resp = new HashMap<>();
		
		resp.put("iBAN", IBAN);
		resp.put("pinCard", pinCard);
		resp.put("pinCode", pinCode);
		
		JSONRPC2Response response = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(response.toJSONString());
	}

	private static Response openAdditionalAccount(JSONRPC2Request jReq) {
		// TODO Error-proof
		
		Map<String, Object> params = jReq.getNamedParams();
		
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
		HashMap<String, String> resp = new HashMap<>();
		resp.put("iBAN", IBAN);
		resp.put("pinCard", pinCard);
		resp.put("pinCode", pinCode);
		
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		
		return respond(jResp.toJSONString());
	}

	private static Response closeAccount(JSONRPC2Request jReq) {
		// TODO Error-proof
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		// If this is a bogus token, slap the client
		if (!accounts.keySet().contains(token)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		CustomerAccount acc = accounts.get(token);
		boolean found = false;
		
		// Look for the bank account
		for (BankAccount b : acc.getBankAccounts()) {
			if (b.getIBAN().equals(IBAN)) {
				b.setClosed(true);
				b.saveToDB();
				acc.saveToDB();
				found = true;
				break;
			}
		}
		
		// If the bank account doesn't exist under the authenticated user account, send an error
		if (!found) {
			String err = buildError(500, "No account found with the specified IBAN under user account " + acc.getUsername() + ".");
			return respondError(err, 500);
		}
		
		// If all is well, respond with true.
		JSONRPC2Response jResp = new JSONRPC2Response(true, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response provideAccess(JSONRPC2Request jReq) {
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String token = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		String username = (String) params.get("username");
		
		// If the token is bogus, slap the client
		if (!accounts.keySet().contains(token)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
		
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.eq("username", username));
		@SuppressWarnings("unchecked")
		ArrayList<CustomerAccount> target = (ArrayList<CustomerAccount>) DataManager.getObjectsFromDB(CustomerAccount.CLASSNAME, cr);
		
		// If we couldn't find the target user, tell the client
		if (target.size() == 0) {
			String err = buildError(500, "Could not find user " + username + ".");
			return respondError(err, 500);
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
		// TODO Add missing error cases
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
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
		
		// If everything is fine, delete all cards creating an association between the target user and bank account, notify the client
		for (DebitCard dc : bAcc.getDebitCards()) {
			if (dc.getHolderBSN().equals(targetAcc.getBSN())) {
				dc.deleteFromDB();
				bAcc.getDebitCards().remove(dc);
			}
		}
		
		bAcc.saveToDB();
		targetAcc.saveToDB();
		
		SQLiteDB.executeStatement("DELETE FROM customerbankaccounts WHERE customer_BSN='" + targetAcc.getBSN() + "' AND IBAN='" + bAcc.getIBAN() + "'");
		
		JSONRPC2Response jResp = new JSONRPC2Response(true, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response depositIntoAccount(JSONRPC2Request jReq) {
		// TODO Handle all error cases
		HashMap<String, Object> params = (HashMap<String, Object>) jReq.getNamedParams();
		
		String IBAN = (String) params.get("iBAN");
		String pinCard = (String) params.get("pinCard");
		String pinCode = (String) params.get("pinCode");
		float amount = Float.parseFloat((String) params.get("amount"));
		
		DebitCard dc = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, pinCard);
		
		if (dc == null) {
			String err = buildError(500, "Could not find debit card " + pinCard);
			return respondError(err, 500);
		}		
		
		// If this is the wrong PIN, slap the client
		if (!dc.isValidPIN(pinCode)) {
			String err = buildError(421, "An invalid PINcard, -code or -combination was used.");
			return respondError(err, 500);
		}
		
		BankAccount bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		
		if (bAcc == null) {
			String err = buildError(500, "Could not find bank account with IBAN " + IBAN);
			return respondError(err, 500);
		}
		
		try {
			bAcc.deposit(amount, pinCard);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		bAcc.saveToDB();
		
		JSONRPC2Response jResp = new JSONRPC2Response(true, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response payFromAccount(JSONRPC2Request jReq) {
		//TODO Error-proofing
		//TODO check for invalid parameters, bank accounts, card or PIN
		Map<String, Object> params = jReq.getNamedParams();
		
		String sourceIBAN = (String) params.get("sourceIBAN");
		String targetIBAN = (String) params.get("targetIBAN");
		String pinCard = (String) params.get("pinCard");
		String pinCode = (String) params.get("pinCode");
		String strAmount = (String) params.get("amount");
		float amount = Float.parseFloat(strAmount);
		
		//if (!params.keySet().contains("sourceIBAN"))
		
		if (DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, sourceIBAN)) {
			//TODO Error stuff
		}
		
		DebitCard card = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, pinCard);
		
		card.pinPayment(amount, pinCode, targetIBAN);
		
		JSONRPC2Response jResp = new JSONRPC2Response(true, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response transferMoney(JSONRPC2Request jReq) {
		// TODO Error-proofing
		// TODO Do something with targetName?
		Map<String, Object> params = jReq.getNamedParams();
		
		String authToken = (String) params.get("authToken");
		String sourceIBAN = (String) params.get("sourceIBAN");
		String targetIBAN = (String) params.get("targetIBAN");
		String targetName = (String) params.get("targetName");
		String strAmount = (String) params.get("amount");
		String description = (String) params.get("description");
		float amount = Float.parseFloat(strAmount);
		
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
		destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, targetIBAN);
		
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
		
		try {
			source.transfer(destination, amount, description);
		} catch (IllegalAmountException e) {
			// TODO Return error message to client
			e.printStackTrace();
		} catch (IllegalTransferException e) {
			// TODO Return error message to client
			e.printStackTrace();
		}
		
		JSONRPC2Response jResp = new JSONRPC2Response(true, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response getAuthToken(JSONRPC2Request jReq) {
		// TODO Error-proofing
		Map<String, Object> params = jReq.getNamedParams();
		
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
		
		// Generate the authentication token
		String token = UUID.randomUUID().toString().toUpperCase() + "/" + params.get("username") + "/" + java.lang.System.currentTimeMillis();
		
		// Associate the account with the authentication token
		accounts.put(token, account);
		
		// Send the generated token to the client
		HashMap<String, String> resp = new HashMap<>();
		resp.put("result", token);
		JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	
	private static Response getBalance(JSONRPC2Request jReq) {
		// TODO Error-proof
		Map<String, Object> params = jReq.getNamedParams();
		
		String authToken = (String) params.get("authToken");
		String IBAN = (String) params.get("iBAN");
		
		CustomerAccount cAcc = null;
		BankAccount source = null;
		boolean authorized = false;
		
		source = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		
		// If this is a bogus token, slap the client
		if (!accounts.containsKey(authToken)) {
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
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
			String err = buildError(419, "The authenticated user is not authorized to perform this action.");
			return respondError(err, 500);
		}
		
		
		JSONRPC2Response jResp = new JSONRPC2Response(new Float(source.getBalance()), "response-" + java.lang.System.currentTimeMillis());
		return respond(jResp.toJSONString());
	}

	private static Response getTransactionsOverview(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}
}
