package server;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;

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
		default:
			//TODO: Maybe return error to client?
			System.out.println("Discarded invalid JSON-RPC method call.");
			break;
		}
	}

	private void openAccount(JSONRPC2Request jReq) {
		// TODO Auto-generated method stub
		
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
}
