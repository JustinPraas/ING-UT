package server.rest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.SimpleFormatter;

import javax.ws.rs.core.Response;

import accounts.BankAccount;
import accounts.CustomerAccount;
import server.core.InputValidator;

public class RequestValidator {

	public static Response isValidSetOverdraftLimitRequest(HashMap<String, Object> params) {
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("overdraftLimit")) {
			return invalidMethodParametersResponse();
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		try {
			Double.parseDouble((String) params.get("overdraftLimit"));
		} catch (NumberFormatException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (String) params.get("overdraftLimit") + " is not a valid overdraft limit.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}
	
	public static Response isValidGetOverdraftLimitRequest(HashMap<String, Object> params) {
		if (!params.containsKey("authToken") || !params.containsKey("iBAN")) {
			return invalidMethodParametersResponse();
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		return null;
	}

	public static Response isValidUnblockCardRequest(HashMap<String, Object> params) {
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("pinCard")) {
			return invalidMethodParametersResponse();
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		if (!InputValidator.isNumericalOnly((String) params.get("pinCard"))) {
			return invalidPinCardResponse((String) params.get("pinCard"));
		}
		
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}		
		return null;
	}

	public static Response isValidSimulateTimeRequest(Map<String, Object> params) {
		// If the required parameters aren't present, stop and notify the client
		if (!params.containsKey("nrOfDays")) {
			return invalidMethodParametersResponse();
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

	public static Response isValidGetBankAccountAccessRequest(Map<String, Object> params) {
		// If the required parameters aren't present, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			return invalidMethodParametersResponse();
		}		
		
		// If the provided IBAN is not an IBAN, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		// If token is invalid, stop and notify client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		return null;
	}

	public static Response isValidGetUserAccessRequest(Map<String, Object> params) {		
		// If the authToken is invalid, stop and notify the client 
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			return invalidMethodParametersResponse();
		}
		// If no authToken has been sent, stop and notify the client
		if (!params.containsKey("authToken")) {
			return invalidAuthTokenResponse();
		}
		
		return null;
	}

	public static Response isValidOpenAccountRequest(Map<String, Object> params) {
		// If the request is missing any required parameters, stop and notify the client
		if (!params.containsKey("name") || !params.containsKey("surname") || !params.containsKey("initials") 
				|| !params.containsKey("dob") || !params.containsKey("ssn") || !params.containsKey("address") 
				|| !params.containsKey("telephoneNumber") || !params.containsKey("email") || !params.containsKey("username") 
				|| !params.containsKey("password")) {
			return invalidMethodParametersResponse();
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

	public static Response isValidOpenAdditionalAccountRequest(Map<String, Object> params) {
		// If no authToken is provided, stop and notify the client.
		if (!params.containsKey("authToken")) {
			return invalidMethodParametersResponse();
		}
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		return null;
	}

	public static Response isValidCloseAccountRequest(HashMap<String, Object> params) {
		// If not all required parameters are sent, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			return invalidMethodParametersResponse();
		}		
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		// If the provided IBAN is invalid, stop and notify the client.
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		return null;
	}

	public static Response isValidProvideAccessRequest(HashMap<String, Object> params) {
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("username")) {
			return invalidMethodParametersResponse();
		}
		
		// If the token is bogus, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		return null;
	}

	public static Response isValidRevokeAccessRequest(HashMap<String, Object> params) {		
		// If the request is missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			return invalidMethodParametersResponse();
		}
		
		// If the token is bogus, slap the client
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		// If the provided IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		return null;
	}

	public static Response isValidDepositIntoAccountRequest(HashMap<String, Object> params) {
		// If the request is missing required parameters, stop and notify the client 
		if (!params.containsKey("iBAN") || !params.containsKey("pinCard") || !params.containsKey("pinCode")) {
			return invalidMethodParametersResponse();
		}
		
		// If any given parameter values are invalid, stop and notify the client
		try {
			Double.parseDouble((String) params.get("amount"));
		} catch (NumberFormatException e) {
			return invalidAmountResponse((String) params.get("amount"));
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		if (!InputValidator.isValidPIN((String) params.get("pinCode"))) {
			return invalidPinCodeResponse((String) params.get("pinCode"));
		}
		
		if (!InputValidator.isValidCardNumber((String) params.get("pinCard"))) {
			return invalidPinCardResponse((String) params.get("pinCard"));
		}
		
		return null;
	}

	public static Response isValidPayFromAccountRequest(Map<String, Object> params) {
		// If required parameters are missing, stop and notify the client
		if (!params.containsKey("sourceIBAN") || !params.containsKey("targetIBAN") || !params.containsKey("pinCard") 
				|| !params.containsKey("pinCode") || !params.containsKey("amount")) {
			return invalidMethodParametersResponse();
		}
		
		// If any of the parameters have invalid values, stop and notify the client
		try {
			Double.parseDouble((String) params.get("amount"));
		} catch (NumberFormatException e) {
			return invalidAmountResponse((String) params.get("amount"));
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("sourceIBAN"))) {
			return invalidIBANResponse((String) params.get("sourceIBAN"));
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("targetIBAN"))) {
			return invalidIBANResponse((String) params.get("targetIBAN"));
		}
		
		if (!InputValidator.isValidPIN((String) params.get("pinCode"))) {
			return invalidPinCodeResponse((String) params.get("pinCode"));
		}
		
		if (!InputValidator.isValidCardNumber((String) params.get("pinCard"))) {
			return invalidPinCardResponse((String) params.get("pinCard"));
		}
		
		return null;
	}

	public static Response isValidInvalidateCardRequest(Map<String, Object> params) {
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || 
				!params.containsKey("pinCard") || !params.containsKey("newPin")) {
			return invalidMethodParametersResponse();
		}
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		if (!InputValidator.isNumericalOnly((String) params.get("pinCard"))) {
			return invalidPinCardResponse((String) params.get("pinCard"));
		}
		
		String newPinCodeString = (String) params.get("newPin");
		if (!newPinCodeString.equals("yes") && !newPinCodeString.equals("no")) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "'" + newPinCodeString + "' is not a valid boolean representation.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If this is a bogus auth token, slap the client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		return null;
	}

	public static Response isValidTransferMoneyRequest(Map<String, Object> params) {
		// If required parameters are missing from the request, stop and notify the client
		if (!params.containsKey("authToken") || !params.containsKey("sourceIBAN") || !params.containsKey("targetIBAN") 
				|| !params.containsKey("targetName") || !params.containsKey("amount") || !params.containsKey("description")) {
			return invalidMethodParametersResponse();
		}

		// If any parameter has an invalid value, stop and notify the client
		try {
			Double.parseDouble((String) params.get("amount"));
		} catch (NumberFormatException e) {
			return invalidAmountResponse((String) params.get("amount"));
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("sourceIBAN"))) {
			return invalidIBANResponse((String) params.get("sourceIBAN"));
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("targetIBAN"))) {
			return invalidIBANResponse((String) params.get("targetIBAN"));
		}
		
		// If this is a bogus auth token, slap the client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		return null;
	}

	public static Response isValidGetAuthTokenRequest(Map<String, Object> params) {		
		if (!(params.containsKey("username") && params.containsKey("password"))) {
			return invalidMethodParametersResponse();
		}
		
		return null;
	}

	public static Response isValidGetBalanceRequest(Map<String, Object> params) {
		// If the request is missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			return invalidMethodParametersResponse();
		}
		
		// If this is a bogus token, slap the client
		if (!ServerHandler.getAccounts().containsKey((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		return null;
	}

	public static Response isValidGetTransactionsOverviewRequest(Map<String, Object> params) {
		// If we're missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN") && params.containsKey("nrOfTransactions"))) {
			return invalidMethodParametersResponse();
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
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
	
	public static Response isValidSavingsAccountRequest(HashMap<String, Object> params) {
		// If we're missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") && params.containsKey("iBAN"))) {
			return invalidMethodParametersResponse();
		}
		
		// If the IBAN is invalid, stop and notify the client
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		return null;
	}

	public static Response isValidGetEventLogRequest(HashMap<String, Object> params) {
		// If we're missing required parameters, stop and notify the client
		if (!(params.containsKey("startDate") && params.containsKey("endDate"))) {
			return invalidMethodParametersResponse();
		}
		
		DateFormat fm = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate;
		Date endDate;
		try {
			startDate = fm.parse((String) params.get("startDate"));
			endDate = fm.parse((String) params.get("endDate"));
		} catch (ParseException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "One of the given dates is not in the format yyyy-MM-dd");
			return ServerHandler.respondError(err, 500);
		}
		
		if (endDate.before(startDate)) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "The given end date come before the start date.");
			return ServerHandler.respondError(err, 500);
		}
		
		return null;
	}

	public static boolean userOwnsBankAccount(CustomerAccount customerAccount, BankAccount bankAccount) {
		for (BankAccount b : customerAccount.getBankAccounts()) {
			if (b.getIBAN().equals(bankAccount.getIBAN())) {
				return true;
			}
		}
		return false;
	}

	private static Response invalidMethodParametersResponse() {
		String err = ServerHandler.buildError(-32602, "Invalid method parameters.");
		return ServerHandler.respondError(err, 500);
	}

	private static Response invalidIBANResponse(String IBAN) {
		String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", IBAN + " is not a valid IBAN.");
		return ServerHandler.respondError(err, 500);
	}

	private static Response invalidPinCardResponse(String pinCard) {
		String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", pinCard+ " is not a valid card number.");
		return ServerHandler.respondError(err, 500);
	}

	private static Response invalidAuthTokenResponse() {
		String err = ServerHandler.buildError(419, "The authenticated user is not authorized to perform this action. Invalid authentication token.");
		return ServerHandler.respondError(err, 500);
	}

	private static Response invalidAmountResponse(String amount) {
		String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", amount + " is not a valid amount.");
		return ServerHandler.respondError(err, 500);
	}

	private static Response invalidPinCodeResponse(String pinCode) {
		String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", pinCode + " is not a valid PIN.");
		return ServerHandler.respondError(err, 500);
	}
}
