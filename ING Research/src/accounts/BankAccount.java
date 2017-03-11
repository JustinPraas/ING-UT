package accounts;

import java.util.HashSet;
import java.math.BigInteger;

import exceptions.IllegalAmountException;
import exceptions.IllegalCloseException;
import exceptions.IllegalTransferException;

/**
 * A simple model of a bank account in an abstract currency.
 * @author Andrei Cojocaru
 */
public class BankAccount {
	private final static String COUNTRY_CODE = "NL";
	private final static String BANK_CODE = "INGB";
	
	private float balance;
	private String IBAN;
	private CustomerAccount mainHolder;
	private HashSet<CustomerAccount> holders;
	
	/**
	 * Create a new <code>BankAccount</code> with a specific account holder and an initial balance of 0.
	 * @param holder The <code>CustomerAccount</code> considered to be the main holder of this <code>BankAccount</code>
	 */
	public BankAccount(CustomerAccount holder) {
		this.balance = 0;
		this.IBAN = generateIBAN(COUNTRY_CODE, BANK_CODE);
		this.mainHolder = holder;
		this.holders = new HashSet<CustomerAccount>();
		this.holders.add(holder);
	}
	
	/**
	 * Generates an IBAN for this BankAccount
	 * @param countryCode The code that represents a country (e.g. NL, BE, DE, etc.)
	 * @param bankCode The code that represents the Bank (e.g. INGB, ABNA, etc.)
	 * @return resultIBAN The IBAN 
	 */
	public static String generateIBAN(String countryCode, String bankCode) {
		//The personalAccountNumber (last 10 digits) should be hierarchically distributed.
		//However, we do not keep track of used numbers yet, so for now assign 10 random digits
		StringBuilder personalAccountNumber = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			personalAccountNumber.append((int) (Math.random() * 10));
		}		
		
		//Compute the controlNumber for this IBAN
		int controlNumber = generateControlNumber(countryCode, bankCode, personalAccountNumber.toString());
		
		//If the control number consists of 1 digit, prepend a 0
		String controlNumberString = controlNumber < 10 ? "0" + controlNumber : "" + controlNumber;	
		
		//Concatenate all parts of the IBAN to a complete IBAN	
		String resultIBAN = countryCode + controlNumberString + bankCode + personalAccountNumber;
		
		return resultIBAN;
	}
	
	/**
	 * Generates the controlNumber for the IBAN (3rd and 4th digit).
	 * @param countryCode The code that represents a country (e.g. NL, BE, DE, etc.)
	 * @param bankCode The code that represents the Bank (e.g. INGB, ABNA, etc.)
	 * @param pan The personal account number (the last 10 digits)
	 * @return controlNumber The controlNumber that belongs to the combination of 
	 * countryCode, BankCode and pan
	 * @see <a href="http://www.ibannl.org/uitleg-over-iban/">Formula for the control number (Dutch)</a>
	 */
	public static int generateControlNumber(String countryCode, String bankCode, String pan) {
		/* 
		 * Formula in short:
		 * String a = bank code converted to digits (e.g. 'A' = 10, 'B' = 11, 'Z' = 35, etc.)
		 * String b = personal account number (10 digits, prepend with 0's if needed)
		 * String c = country code converted to digits (see String a)
		 * BigInteger value = parse the following to a BigInteger: (a + b + c)
		 * int controlNumber = 98 - (value % 97)
		 */
		
		//Convert the bankCode to digits
		StringBuilder bankCodeInDigits = new StringBuilder();
		for (Character c : bankCode.toCharArray()) {
			bankCodeInDigits.append(Character.getNumericValue(c));
		} 
		
		//Convert the countryCode to digits
		StringBuilder countryCodeInDigits = new StringBuilder();
		for (Character c : countryCode.toCharArray()) {
			countryCodeInDigits.append(Character.getNumericValue(c));
		}
		
		//A BigInteger is necessary for a value of that does not fit a long
		BigInteger computationValue = new BigInteger(bankCodeInDigits + pan + countryCodeInDigits + "00");
		
		//Get the remainder of computationValue % 97
		computationValue = computationValue.mod(new BigInteger("97"));
		
		//Subtract the computationValue from 98, this results in the controlNumber of the debitcard
		int controlNumber = 98 - computationValue.intValue();
		
		return controlNumber;
	}
	
	/**
	 * Deposit a specific sum of money into the <code>BankAccount</code>.
	 * @param amount The amount of money to be deposited
	 */
	public void deposit(float amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance += amount;
	}
	
	/**
	 * Transfers a specific amount of money from this <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 */
	public void transfer(BankAccount destination, float amount) throws IllegalAmountException, IllegalTransferException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance < amount) {
			throw new IllegalTransferException(balance, IBAN, amount);
		}
		
		this.credit(amount);
		destination.debit(amount);
	}
	
	/**
	 * Close the <code>BankAccount</code>.
	 */
	public void close() throws IllegalCloseException {
		if (balance != 0) {
			throw new IllegalCloseException(IBAN, balance);
		}
		
		for (CustomerAccount account : holders) {
			account.removeBankAccount(this);
		}
		
		mainHolder = null;
		holders = null;
	}
	
	public float getBalance() {
		return balance;
	}
	
	/**
	 * Displays the <code>BankAccount</code>'s balance to the TUI.
	 */
	public void viewBalance() {
		System.out.println("Current balance for account " + IBAN + ": " + balance);
	}
	
	/**
	 * Credits a <code>BankAccount</code> with a specific amount of money
	 * @param amount The amount of money to credit the <code>BankAccount</code> with
	 * @throws IllegalAmountException Thrown when the specified amount is 0 or negative
	 */
	public void credit (float amount) throws IllegalAmountException {
		//TODO: Log
		if (amount <= 0) {
			// You cannot credit an account with a negative amount of money
			throw new IllegalAmountException(amount);
		}
		balance -= amount;
	}
	
	/**
	 * Debits a <code>BankAccount</code> with a specific amount of money
	 * @param amount The amount of money to debit the <code>BankAccount</code> with
	 * @throws Thrown when the specified amount is 0 or negative
	 */
	public void debit (float amount) throws IllegalAmountException {
		//TODO: Log
		if (amount <= 0) {
			// You cannot debit an account by a positive amount of money
			throw new IllegalAmountException(amount);
		}
		balance += amount;
	}
	
	public String getIBAN() {
		return IBAN;
	}
}
