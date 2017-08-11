package server.rest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
			@SuppressWarnings("unused")
			double amount = (double) params.get("overdraftLimit");
		} catch (ClassCastException e) {
			return invalidAmountResponse(params.get("overdraftLimit"));
		}
		
		return null;
	}
	
	public static Response isValidSetTransferLimitRequest(HashMap<String, Object> params) {
		if (!params.containsKey("authToken") || !params.containsKey("iBAN") || !params.containsKey("transferLimit")) {
			return invalidMethodParametersResponse();
		}
		
		if (!InputValidator.isValidIBAN((String) params.get("iBAN"))) {
			return invalidIBANResponse((String) params.get("iBAN"));
		}
		
		if (!ServerHandler.getAccounts().keySet().contains((String) params.get("authToken"))) {
			return invalidAuthTokenResponse();
		}
		
		try {
			Double.parseDouble((String) params.get("transferLimit"));
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
		if (!params.containsKey("nrOfDays") || !params.containsKey("authToken")) {
			return invalidMethodParametersResponse();
		}
		
		// If input is invalid (i.e. not a numerical value)
		if (!InputValidator.isNumericalOnly(Long.toString((long)params.get("nrOfDays")))) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (long) params.get("nrOfDays") + " is not a valid number.");
			return ServerHandler.respondError(err, 500);
		}
		
		// If input is negative...
		if ((long) params.get("nrOfDays") <= 0) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", (long) params.get("nrOfDays") + " is equal to or less than 0.");
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
		
		// If one of the type/guardian parameters is given but not the other, return an error
		if (params.containsKey("type") && !params.containsKey("guardians") || 
				params.containsKey("guardians") && !params.containsKey("type")) {
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
		    String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "Phone number " + params.get("telephoneNumber") + " is invalid.");
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
		
		try {
			@SuppressWarnings("unused")
			double amount = (double) params.get("amount");
		} catch (ClassCastException e) {
			return invalidAmountResponse(params.get("amount"));
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
		
		try {
			@SuppressWarnings("unused")
			double amount = (double) params.get("amount");
		} catch (ClassCastException e) {
			return invalidAmountResponse(params.get("amount"));
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
		
		try {
			@SuppressWarnings("unused")
			boolean newPin = (boolean) params.get("newPin");
		} catch (ClassCastException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "Boolean cannot be parsed from the given paramater for 'newPin'.");
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
		
		try {
			@SuppressWarnings("unused")
			double amount = (double) params.get("amount");
		} catch (ClassCastException e) {
			return invalidAmountResponse(params.get("amount"));
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
		
		try {
			@SuppressWarnings("unused")
			long number = (long) params.get("nrOfTransactions");
		} catch (NumberFormatException e) {
			return invalidMethodParametersResponse();
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
		if (!(params.containsKey("beginDate") || !params.containsKey("endDate")) || !params.containsKey("authToken")) {
			return invalidMethodParametersResponse();
		}
		
		DateFormat fm = new SimpleDateFormat("yyyy-MM-dd");
		Date startDate;
		Date endDate;
		try {
			startDate = fm.parse((String) params.get("beginDate"));
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

	public static Response isValidSetValueRequest(HashMap<String, Object> params) {
		// If we're missing required parameters, stop and notify the client
		if (!(params.containsKey("authToken") || !params.containsKey("key")) || !params.containsKey("value") || !params.containsKey("date")) {
			return invalidMethodParametersResponse();
		}
		
		DateFormat fm = new SimpleDateFormat("yyyy-MM-dd");
		try {
			fm.parse((String) params.get("date"));
		} catch (ParseException e) {
			String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", "The given date is not in the format yyyy-MM-dd");
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

	public static Response invalidMethodParametersResponse() {
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

	private static Response invalidAmountResponse(Object amount) {
		String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", amount.toString() + " is not a valid amount.");
		return ServerHandler.respondError(err, 500);
	}

	private static Response invalidPinCodeResponse(String pinCode) {
		String err = ServerHandler.buildError(418, "One or more parameter has an invalid value. See message.", pinCode + " is not a valid PIN.");
		return ServerHandler.respondError(err, 500);
	}
}
