package server.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;
import database.DataManager;

@Path("/banking")
public class RequestHandler {
	
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
			HashMap<String, String> resp = new HashMap<>();
			resp.put("code", "500");
			resp.put("message", "Error: User attempted to create duplicate customer account with SSN " + newAcc.getBSN());
			
			JSONRPC2Response jResp = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
			return respondError(jResp.toJSONString(), 500);
		}
		
		// If this is not a duplicate account, open a bank account for it and save it to DB
		BankAccount bankAcc = newAcc.openBankAccount();
		DebitCard card = new DebitCard(newAcc.getBSN(), bankAcc.getIBAN());
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
		// TODO Auto-generated method stub
		return null;
	}

	private static Response closeAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response provideAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response revokeAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response depositIntoAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response payFromAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response transferMoney(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response getAuthToken(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response getBalance(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Response getTransactionsOverview(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return null;
	}
}
