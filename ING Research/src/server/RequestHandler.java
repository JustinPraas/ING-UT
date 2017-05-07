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
			//TODO: Do
			break;
		case "openAdditionalAccount":
			//TODO: Do
			break;
		case "closeAccount":
			//TODO: Do
			break;
		case "provideAccess":
			//TODO: Do
			break;
		case "revokeAccess":
			//TODO: Do
			break;
		case "depositIntoAccount":
			//TODO: Do
			break;
		case "payFromAccount":
			//TODO: Do
			break;
		case "transferMoney":
			//TODO: Do
			break;
		case "getAuthToken":
			//TODO: Do
			break;
		case "getBalance":
			//TODO: Do
			break;
		case "getTransactionsOverview":
			//TODO: Do
			break;
		default:
			System.out.println("Discarded invalid JSON-RPC method call.");
			break;
		}
	}
}
