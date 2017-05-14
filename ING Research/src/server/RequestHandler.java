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
	public void parseJSONRequest (String request) {
		JSONRPC2Request jReq = null;
		
		try {
			jReq = JSONRPC2Request.parse(request);
		} catch (JSONRPC2ParseException e) {
			System.out.println("Discarded unparseable JSON request.");
			return;
		}
		
		String method = jReq.getMethod();
		
		switch(method) {
		case "openAccount":
			openAccount(jReq);
			break;
		case "openAdditionalAccount":
			openAdditionalAccount(jReq);
			break;
		case "closeAccount":
			closeAccount(jReq);
			break;
		case "provideAccess":
			provideAccess(jReq);
			break;
		case "revokeAccess":
			revokeAccess(jReq);
			break;
		case "depositIntoAccount":
			depositIntoAccount(jReq);
			break;
		case "payFromAccount":
			payFromAccount(jReq);
			break;
		case "transferMoney":
			transferMoney(jReq);
			break;
		case "getAuthToken":
			getAuthToken(jReq);
			break;
		case "getBalance":
			getBalance(jReq);
			break;
		case "getTransactionsOverview":
			getTransactionsOverview(jReq);
			break;
		case "getUserAccess":
			getUserAccess(jReq);
			break;
		case "getBankAccountAccess":
			getBankAccountAccess(jReq);
			break;
		default:
			//TODO: Maybe return error to client?
			System.out.println("Discarded invalid JSON-RPC method call.");
			break;
		}
	}

	private void getBankAccountAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void getUserAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void openAccount(JSONRPC2Request jReq) {
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
		sendToClient(response);
	}

	private void openAdditionalAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void closeAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void provideAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void revokeAccess(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void depositIntoAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void payFromAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void transferMoney(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void getAuthToken(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void getBalance(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}

	private void getTransactionsOverview(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
	}
	
	private void sendToClient(JSONRPC2Response jResp) {
		// TODO Implement
	}
}
