package server;

import java.util.HashMap;
import java.util.Map;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;

public class RequestHandler {
	
	public static String parseJSONRequest (String request) {
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
			//TODO: Maybe return error to client?
			System.out.println("Discarded invalid JSON-RPC method call.");
			return "";
		}
	}

	private static String getBankAccountAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String getUserAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String openAccount(JSONRPC2Request jReq) {
		// TODO Error handling
		CustomerAccount newAcc = new CustomerAccount();
		Map<String, Object> params = jReq.getNamedParams();
		
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
		
		BankAccount bankAcc = newAcc.openBankAccount();
		DebitCard card = new DebitCard(newAcc.getBSN(), bankAcc.getIBAN());
		newAcc.saveToDB();
		card.saveToDB();
		
		String IBAN = bankAcc.getIBAN();
		String pinCard = card.getCardNumber();
		String pinCode = card.getPIN();
		
		
		// TODO Verify that the below code works
		HashMap<String, String> resp = new HashMap<>();
		
		resp.put("iBAN", IBAN);
		resp.put("pinCard", pinCard);
		resp.put("pinCode", pinCode);
		
		JSONRPC2Response response = new JSONRPC2Response(resp, "response-" + java.lang.System.currentTimeMillis());
		return response.toJSONString();
	}

	private static String openAdditionalAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String closeAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String provideAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String revokeAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String depositIntoAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String payFromAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String transferMoney(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String getAuthToken(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String getBalance(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}

	private static String getTransactionsOverview(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		return "";
	}
}
