package cards;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import accounts.BankAccount;
import client.Client;
import database.DataManager;
import exceptions.ExceedLimitException;
import exceptions.ExpiredCardException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InvalidPINException;
import exceptions.ObjectDoesNotExistException;
import exceptions.PinCardBlockedException;
import server.rest.BankSystemValue;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * A debit card that is associated with a combination of customer account and bank account.
 * @author Justin Praas
 */
@Entity
@Table(name = "debitcards")
public class DebitCard extends Card {
	
	private String expirationDate;
	public static final String CLASSNAME = "cards.DebitCard";
	
	public DebitCard() {
		
	}

	/**
	 * Create a new <code>DebitCard</code> associated with a BankAccount
	 * and stores its details in the database.
	 * @param bankAccount The <code>bankAccount</code> associated with the new <code>DebitCard</code>
	 */
	public DebitCard(String holderBSN, String bankAccountIBAN, String cardNumber) {
		super(generatePin(), cardNumber, bankAccountIBAN, holderBSN);
		expirationDate = generateExpirationDate();
	}
	
	/**
	 * Create a new <code>DebitCard</code> instance with details loaded
	 * from the database.
	 */
	public DebitCard(String holderBSN, String bankAccountIBAN, String expirationDate, String cardNumber, String PIN) {
		super(PIN, cardNumber, bankAccountIBAN, holderBSN);
		this.expirationDate = expirationDate;
	}
	
	public DebitCard(String holderBSN, String bankAccountIBAN, String PIN, String cardNumber, boolean testPurpose) {
		super(PIN, cardNumber, bankAccountIBAN, holderBSN);
		expirationDate = generateExpirationDate();
	}
	
	/**
	 * Generates a random card number for the <code>DebitCard</code> object.
	 * The standardized format (ING): 3 digits + 1 alphabetical character + 3 digits.
	 * @return resultCardNumber.toString() The generated card number for the <code>DebitCard</code>
	 */
	public static String generateCardNumber() {
		boolean unique = false;
		StringBuilder resultCardNumber = null;
		while (!unique) {
			resultCardNumber = new StringBuilder();
			
			//Append 7 random digits
			for (int i = 0; i < 7; i++) {
				resultCardNumber.append((int)(Math.random() * 10));
			}
			
			if (DataManager.isPrimaryKeyUnique(CLASSNAME, PRIMARYKEYNAME, resultCardNumber.toString())) {
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
		c.add(Calendar.YEAR, (int) BankSystemValue.CARD_EXPIRATION_LENGTH.getAmount());
		
		return new Date(c.getTime().getTime()).toString();
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
		result.append(String.format(format, "Main Holder BSN:", super.getHolderBSN()));
		result.append(String.format(format, "Debit Card number:", super.getCardNumber()));
		result.append(String.format(format, "Bank Account IBAN:", super.getBankAccountIBAN()));
		result.append(String.format(format, "Expiration date:", expirationDate));
		result.append(String.format(format, "PIN: ", super.getPIN()));
		return result.toString();
	}
	
	/**
	 * Simulates a PIN machine transfer attempt.
	 * @param amount The amount to be charged
	 * @param PIN The PIN entered
	 * @param destination The destination IBAN
	 * @throws InvalidPINException 
	 * @throws ExpiredCardException 
	 * @throws ExceedLimitException 
	 * @throws PinCardBlockedException 
	 */
	public void pinPayment(double amount, String PIN, BankAccount destination) throws InvalidPINException, ExpiredCardException, ExceedLimitException, PinCardBlockedException {
		BankAccount ownAccount;
		try {
			ownAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, super.getBankAccountIBAN());
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
			return;
		}
		
		// Check if a valid PIN is given
		if (isBlocked()) {
			throw new PinCardBlockedException(super.getCardNumber());
		} else if (!isValidPIN(PIN)) {
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
	
	public void pinPayment(double amount, String PIN, String destinationIBAN) throws IllegalAmountException, IllegalTransferException, InvalidPINException, ExpiredCardException, PinCardBlockedException, ExceedLimitException {
		BankAccount ownAccount;
		try {
			ownAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, super.getBankAccountIBAN());
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
			return;
		}
		
		if (isBlocked()) {
			throw new PinCardBlockedException(super.getCardNumber());
		} else if (!isValidPIN(PIN)) {
			throw new InvalidPINException(PIN, getCardNumber());
		} else if (isExpired()) {
			throw new ExpiredCardException(getCardNumber(), getExpirationDate());
		}
		
		ownAccount.transfer(destinationIBAN, amount, "Debit card payment from " + ownAccount.getIBAN() + " to " + destinationIBAN + ".");
	}
	
	@Column(name = "expiration_date")
	public String getExpirationDate() {
		return expirationDate;
	}
	
	@Transient
	public String getClassName() {
		return CLASSNAME;
	}
	
	
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}
}
