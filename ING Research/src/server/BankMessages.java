package server;

public class BankMessages {
	//Format: custlogin BSN
	public static final String CUSTLOGIN = "^custlogin \\d{9}$";
	
	// Format: login IBAN
	public static final String BANKLOGIN = "^login [[:alnum:]]{15,34}$";
	public static final String LOGINRESPONSE = "^loginresponse (0|1)";
	public static final String LISTBANKACCOUNTS = "^listbankaccounts$";
	public static final String BANKACCOUNTS = "^bankaccounts .+$";
	
	// Format: transactionrequest sourceIBAN destinationIBAN amount
	public static final String TRANSACTIONREQUEST = "^transferrequest [[:alnum:]]{15,34} [[:alnum:]]{15,34} \\d+(.+|$)";
	
	// Format: transactionrespone 0/1 (success/failure) destinationIBAN 
	public static final String TRANSACTIONRESPONSE = "^transferresponse (0|1)$";
	
	// Format: pinmachinerequest amount cardnumber destinationIBAN
	public static final String PINMACHINEREQUEST = "^pinmachinerequest \\d+ \\d{7} [[:alnum:]]{15,34}$";
	public static final String PINMACHINERESPONSE = "^pinmachineresponse (0|1)$";
}
