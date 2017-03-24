package accounts;
import java.util.Calendar;
import java.sql.Date;

import database.BankingLogger;

/**
 * A debit card that is associated with a combination of customer account and bank account.
 * @author Justin Praas
 */
public class DebitCard {	
	private final String PIN;
	private String cardNumber;
	private Date expirationDate;
	private String bankAccountIBAN;
	private String holderBSN;
	
	/*TODO implement 'follownumbers'? (follownumbers are basically the total number of passes 
	that have been associated with a BankAccount during the BankAccount's lifespan) */

	/**
	 * Create a new <code>DebitCard</code> associated with a BankAccount.
	 * @param bankAccount The <code>bankAccount</code> associated with the new <code>DebitCard</code>
	 */
	public DebitCard(String mainHolderBSN, String bankAccountIBAN) {
		this.bankAccountIBAN = bankAccountIBAN;
		this.holderBSN = mainHolderBSN;
		PIN = generatePin();
		cardNumber = generateCardNumber();
		expirationDate = generateExpirationDate();
	}

	/**
	 * Generates a random PIN for the <code>DebitCard</code> object. 
	 * The standardized format (ING): 4 digits.
	 * @return resultPin.toString() The generated PIN for the <code>DebitCard</code>
	 */
	private String generatePin() {
		StringBuilder resultPIN = new StringBuilder();
		
		//Append 4 random digits in the range of [0, 9] to the resultPIN
		//Of course you could append one random number in the range of [0, 9999], however, this
		//might produce a number below 1000, which would result in a 3 digit number.
		for (int i = 0; i < 4; i++) {
			resultPIN.append((int)(Math.random() * 10));
		}
		
		return resultPIN.toString();
	}
	
	/**
	 * Generates a random card number for the <code>DebitCard</code> object.
	 * The standardized format (ING): 3 digits + 1 alphabetical character + 3 digits.
	 * @return resultCardNumber.toString() The generated card number for the <code>DebitCard</code>
	 */
	private String generateCardNumber() {
		boolean unique = false;
		StringBuilder resultCardNumber = null;
		while (!unique) {
			resultCardNumber = new StringBuilder();
			
			//Append the first 3 digits
			for (int i = 0; i < 3; i++) {
				resultCardNumber.append((int)(Math.random() * 10));
			}
			
			//Append a random alphabetical character in the range of [A-Z]
			char c = (char) ('A' + (int) (Math.random() * 26));
			resultCardNumber.append(c);
			
			//Append the last 3 digits
			for (int i = 0; i < 3; i++) {
				resultCardNumber.append((int)(Math.random() * 10));
			}
			
			if (!BankingLogger.debitCardExists(resultCardNumber.toString())) {
				unique = true;
			}
		}
		
		return resultCardNumber.toString();
	}

	/**
	 * Generates the expiration date for the <code>DebitCard</code> object.
	 * The default expiration date (ING): 5 years after card creation
	 * @return
	 */
	public Date generateExpirationDate() {		
		//Get a calendar using the default time zone and locale
		Calendar c = Calendar.getInstance();
		
		//Add the specified amount of time to the given calendar field
		c.add(Calendar.YEAR, 5);
		
		return new Date(c.getTime().getTime());
	}
	
	/**
	 * Checks whether the given PIN matches the PIN that is associated with
	 * the <code>DebitCard</code>.
	 * @param pin The PIN that is entered (for example on an ATM or a PIN machine)
	 * @return true if pin matches the debit card's PIN, otherwise false
	 */
	public boolean isValidPIN(String pin) {
		return PIN.equals(pin);
	}
	
	/**
	 * Checks whether the <code>DebitCard</code> is expired.
	 * @return true if the <code>DebitCard</code> is expired, otherwise false
	 */
	public boolean isExpired() {
		Calendar c = Calendar.getInstance();
		Date today = new Date(c.getTime().getTime());
		if (expirationDate.before(today)) {
			return false;
		} else {
			return true;
		}
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		String format = "%1$-20s %2$s %n";
		result.append(String.format(format, "Main Holder BSN:", holderBSN));
		result.append(String.format(format, "Debit Card number:", cardNumber));
		result.append(String.format(format, "Bank Account IBAN:", bankAccountIBAN));
		result.append(String.format(format, "Expiration date:", expirationDate));
		return result.toString();
	}
	
	public String getPIN() {
		return PIN;
	}
	
	public String getCardNum() {
		return cardNumber;
	}
	
	public Date getExpirationDate() {
		return expirationDate;
	}
	
	public String getHolderBSN() {
		return holderBSN;
	}
	
	public String getBankAccountIBAN() {
		return bankAccountIBAN;
	}
}
