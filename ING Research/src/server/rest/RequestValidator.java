package server.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import server.core.InputValidator;

public class RequestValidator {

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidUnblockCardRequest(HashMap<String, Object> params) {
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("pinCard")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}		
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isNumericalOnly((String) params.get("pinCard"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("pinCard") + " is not a valid card number.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidSimulateTimeRequest(Map<String, Object> params) {
		// If the required parameters aren't present, stop and notify the client
		if (!params.containsKey("nrOfDays")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If input is invalid (i.e. not a numerical value)
		if (!InputValidator.isNumericalOnly((String) params.get("nrOfDays"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("nrOfDays") + " is not a valid number.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If input is negative...
		if (Integer.parseInt((String) params.get("nrOfDays")) <= 0) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("nrOfDays") + " is equal to or less than 0.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidGetBankAccountAccessRequest(Map<String, Object> params) {
		// If the required parameters aren't present, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}		
		
		// If the provided IBAN is not an IBAN, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If token is invalid, stop and notify client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidGetUserAccessRequest(Map<String, Object> params) {
		// If no authToken has been sent, stop and notify the client
		if (!params.containsKey("authToken")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}		
		
		// If the authToken is invalid, stop and notify the client 
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidOpenAccountRequest(Map<String, Object> params) {
		// If the request is missing any required parameters, stop and notify the client
		if (!params.containsKey("name") || !params.containsKey("surname") || !params.containsKey("initials") 
				|| !params.containsKey("dob") || !params.containsKey("ssn") || !params.containsKey("address") 
				|| !params.containsKey("telephoneNumber") || !params.containsKey("email") || !params.containsKey("username") 
				|| !params.containsKey("password")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// Check some of the param values for validity
		if (!InputValidator.isValidEmailAddress((String) params.get("email"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", params.get("email") + " is not a valid email address.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidBSN((String) params.get("ssn"))) {
		    String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", params.get("BSN") + " is not a valid BSN.");
		    return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidName((String) params.get("username"))) {
		    String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "Username " + params.get("username") + " contains invalid characters.");
		    return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidPhoneNumber((String) params.get("telephoneNumber"))) {
		    String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "Username " + params.get("username") + " contains invalid characters.");
		    return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidOpenAdditionalAccountRequest(Map<String, Object> params) {
		// If no authToken is provided, stop and notify the client.
		if (!params.containsKey("authToken")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidCloseAccountRequest(HashMap<String, Object> params) {
		// If not all required parameters are sent, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}		
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If the provided IBAN is invalid, stop and notify the client.
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidProvideAccessRequest(HashMap<String, Object> params) {
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("username")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If the token is bogus, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidRevokeAccessRequest(HashMap<String, Object> params) {		
		// If the request is missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If the token is bogus, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If the provided IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidDepositIntoAccountRequest(HashMap<String, Object> params) {
		// If the request is missing required parameters, stop and notify the client 
		if (!params.containsKey("iBAN") || !params.containsKey("pinCard") || !params.containsKey("pinCode")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If any given parameter values are invalid, stop and notify the client
		try {
			Double.parseDouble((String) params.get("amount"));
		} catch (ClassCastException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", params.get("amount") + " is not a valid amount.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidPIN((String) params.get("pinCode"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("pinCode") + " is not a valid PIN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidCardNumber((String) params.get("pinCard"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("pinCard") + " is not a valid PIN.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidPayFromAccountRequest(Map<String, Object> params) {
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("sourceIBAN") || !params.containsKey("targetIBAN") || !params.containsKey("pinCard") 
				|| !params.containsKey("pinCode") || !params.containsKey("amount")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}// If any of the parameters have invalid values, stop and notify the client
		try {
			Double.parseDouble((String) params.get("amount"));
		} catch (ClassCastException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("amount") + " is not a valid amount.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("sourceIBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("sourceIBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("targetIBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("targetIBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidPIN((String) params.get("pinCode"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("pinCode") + " is not a valid PIN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidCardNumber((String) params.get("pinCard"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("pinCard") + " is not a valid PIN.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidInvalidateCardRequest(Map<String, Object> params) {
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || 
				!params.containsKey("pinCard") || !params.containsKey("newPin")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isNumericalOnly((String) params.get("pinCard"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("pinCard") + " is not a valid card number.");
			return ServerHandler.respondError(err, 500);
		}
		
		String newPinCodeString = (String) params.get("newPin");
		if (!newPinCodeString.equals("yes") && !newPinCodeString.equals("no")) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "'" + newPinCodeString + "' is not a valid boolean representation.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If this is a bogus auth token, slap the client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidTransferMoneyRequest(Map<String, Object> params) {
		// If required parameters are missing from the request, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("sourceIBAN") || !params.containsKey("targetIBAN") 
				|| !params.containsKey("targetName") || !params.containsKey("amount") || !params.containsKey("description")) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}

		// If any parameter has an invalid value, stop and notify the client
		try {
			Double.parseDouble((String) params.get("amount"));
		} catch (ClassCastException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("amount") + " is not a valid amount.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("sourceIBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("sourceIBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("targetIBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("targetIBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If this is a bogus auth token, slap the client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	
	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidGetAuthTokenRequest(Map<String, Object> params) {		
		if (!(params.containsKey("username") && params.containsKey("password"))) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidGetBalanceRequest(Map<String, Object> params) {
		// If the request is missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	/**
	 * @param params
	 * @return 
	 */
	public static Response isValidGetTransactionsOverviewRequest(Map<String, Object> params) {
		// If we're missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN") && params.containsKey("nrOfTransactions"))) {
			String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
			ServerHandler.respondError(err, 500);
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("iBAN") + " is not a valid IBAN.");
			ServerHandler.respondError(err, 500);
		}
		
		// If the number of transactions is not an integer, stop and notify the client
		try {
			Integer.parseInt((String) params.get("nrOfTransactions"));
		} catch (NumberFormatException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("nrOfTransactions") + " is not a valid amount.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

}
