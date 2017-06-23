package accounts;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import client.Client;
import database.DataManager;
import exceptions.ExpiredCardException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InvalidPINException;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * A debit card that is associated with a combination of customer account and bank account.
 * @author Justin Praas
 */
@Entity
@Table(name = "debitcards")
public class DebitCard implements database.DBObject {	
	private String PIN;
	private String cardNumber;
	private String expirationDate;
	private String bankAccountIBAN;
	private String holderBSN;
	private boolean blocked;
	public static final String CLASSNAME = "accounts.DebitCard";
	public static final String PRIMARYKEYNAME = "cardNumber";
	
	public DebitCard() {
		
	}

	/**
	 * Create a new <code>DebitCard</code> associated with a BankAccount
	 * and stores its details in the database.
	 * @param bankAccount The <code>bankAccount</code> associated with the new <code>DebitCard</code>
	 */
	public DebitCard(String holderBSN, String bankAccountIBAN) {
		this.bankAccountIBAN = bankAccountIBAN;
		this.holderBSN = holderBSN;
		PIN = generatePin();
		cardNumber = generateCardNumber();
		expirationDate = generateExpirationDate();
		blocked = false;
	}
	
	/**
	 * Create a new <code>DebitCard</code> instance with details loaded
	 * from the database.
	 */
	public DebitCard(String holderBSN, String bankAccountIBAN, String expirationDate, String cardNumber, String PIN) {
		this.holderBSN = holderBSN;
		this.bankAccountIBAN = bankAccountIBAN;
		this.expirationDate = expirationDate;
		this.cardNumber = cardNumber;
		this.PIN = PIN;
		blocked = false;
	}
	
	public DebitCard(String holderBSN, String bankAccountIBAN, String PIN) {
		this.holderBSN = holderBSN;
		this.bankAccountIBAN = bankAccountIBAN;
		this.PIN = PIN;
		cardNumber = generateCardNumber();
		expirationDate = generateExpirationDate();
		blocked = false;
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
			
			//Append 7 random digits
			for (int i = 0; i < 7; i++) {
				resultCardNumber.append((int)(Math.random() * 10));
			}
			
			if (DataManager.isPrimaryKeyUnique(getClassName(), getPrimaryKeyName(), resultCardNumber.toString())) {
				unique = true;
			}
		}
		
		return resultCardNumber.toString();
	}

	/**
	 * Generates the expiration date for the <code>DebitCard</code> object.
	 * The default expiration date (ING - 1): 4 years after card creation
	 * @return
	 */
	public String generateExpirationDate() {		
		//Get a calendar using the default time zone and locale
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());		
		
		//Add the specified amount of time to the given calendar field
		c.add(Calendar.YEAR, 4);
		
		return new Date(c.getTime().getTime()).toString();
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
	@Transient
	public boolean isExpired() {
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date today = new Date(c.getTime().getTime());
		try {
			if (format.parse(expirationDate).before(today)) {
				return true;
			} else {
				return false;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		String format = "%1$-20s %2$s %n";
		result.append(String.format(format, "Main Holder BSN:", holderBSN));
		result.append(String.format(format, "Debit Card number:", cardNumber));
		result.append(String.format(format, "Bank Account IBAN:", bankAccountIBAN));
		result.append(String.format(format, "Expiration date:", expirationDate));
		result.append(String.format(format, "PIN: ", PIN));
		return result.toString();
	}
	
	/**
	 * Simulates a PIN machine transfer attempt.
	 * @param amount The amount to be charged
	 * @param PIN The PIN entered
	 * @param destination The destination IBAN
	 * @throws InvalidPINException 
	 * @throws ExpiredCardException 
	 */
	public void pinPayment(double amount, String PIN, BankAccount destination) throws InvalidPINException, ExpiredCardException {
		BankAccount ownAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bankAccountIBAN);
		if (!isValidPIN(PIN)) {
			throw new InvalidPINException(PIN, getCardNumber());
		} else if (isExpired()) {
			throw new ExpiredCardException(getCardNumber(), getExpirationDate());
		}
		
		try {
			ownAccount.transfer(destination, amount, "Debit card payment.");
		} catch (IllegalAmountException e) {
			System.err.println(e.toString());
			return;
		} catch (IllegalTransferException e) {
			System.err.println(e.toString());
				return;
		}
	}
	
	public void pinPayment(double amount, String PIN, String destinationIBAN) throws IllegalAmountException, IllegalTransferException, InvalidPINException, ExpiredCardException {
		BankAccount ownAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bankAccountIBAN);
		if (!isValidPIN(PIN)) {
			throw new InvalidPINException(PIN, getCardNumber());
		} else if (isExpired()) {
			throw new ExpiredCardException(getCardNumber(), getExpirationDate());
		}
		
		ownAccount.transfer(destinationIBAN, amount, "Debit card payment from " + ownAccount.getIBAN() + " to " + destinationIBAN + ".");
	}
	
	@Column(name = "PIN")
	public String getPIN() {
		return PIN;
	}
	
	@Id
	@Column(name = "card_number")
	public String getCardNumber() {
		return cardNumber;
	}
	
	@Column(name = "expiration_date")
	public String getExpirationDate() {
		return expirationDate;
	}
	
	@Column(name = "customer_BSN")
	public String getHolderBSN() {
		return holderBSN;
	}
	
	@Column(name = "bankaccount_IBAN")
	public String getBankAccountIBAN() {
		return bankAccountIBAN;
	}
	
	@Column(name = "blocked")
	public boolean isBlocked() {
		return blocked;
	}

	public void setCardNumber(String num) {
		cardNumber = num;
	}
	
	public void setHolderBSN(String BSN) {
		holderBSN = BSN;
	}
	
	public void setPIN(String PIN) {
		this.PIN = PIN;
	}
	
	public void setBankAccountIBAN(String IBAN) {
		bankAccountIBAN = IBAN;
	}
	
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}
	
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}
	
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}
	
	@Transient
	public String getPrimaryKeyVal() {
		return cardNumber;
	}
	
	@Transient
	public String getClassName() {
		return CLASSNAME;
	}
	
	public void saveToDB() {
		DataManager.save(this);
	}
	
	public void deleteFromDB() {
		DataManager.removeEntryFromDB(this);
	}
}
