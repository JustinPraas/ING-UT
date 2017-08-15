package cards;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import database.DataManager;
import exceptions.ClosedAccountTransferException;
import exceptions.CreditCardNotActiveException;
import exceptions.ExceedLimitException;
import exceptions.ExpiredCardException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InvalidPINException;
import exceptions.PinCardBlockedException;
import exceptions.SameAccountTransferException;

@MappedSuperclass
public abstract class Card implements database.DBObject {

	private String PIN;
	private String cardNumber;
	private String bankAccountIBAN;
	private String holderBSN;
	private boolean blocked;
	public static final String CLASSNAME = "cards.Card";
	public static final String PRIMARYKEYNAME = "cardNumber";
	
	public Card() {
		
	}
	
	public Card(String PIN, String cardNumber, String bankAccountIBAN, String holderBSN) {
		this.PIN = PIN;
		this.cardNumber = cardNumber;
		this.bankAccountIBAN = bankAccountIBAN;
		this.holderBSN = holderBSN;		
		this.blocked = false;
	}
	
	/**
	 * Generates a random PIN for the <code>Card</code> object. 
	 * The standardized format (ING): 4 digits.
	 * @return resultPin.toString() The generated PIN for the <code>Card</code>
	 */
	public static String generatePin() {
		StringBuilder resultPIN = new StringBuilder();
		
		//Append 4 random digits in the range of [0, 9] to the resultPIN
		//Of course you could append one random number in the range of [0, 9999], however, this
		//might produce a number below 1000, which would result in a 3 digit number.
		for (int i = 0; i < 4; i++) {
			resultPIN.append((int)(Math.random() * 10));
		}
		
		return resultPIN.toString();
	}
	
	public abstract void pinPayment(double amount, String PIN, String destinationIBAN) throws CreditCardNotActiveException, InvalidPINException, ClosedAccountTransferException, SameAccountTransferException, ExceedLimitException, IllegalAmountException, IllegalTransferException, ExpiredCardException, PinCardBlockedException;

	/**
	 * Checks whether the given PIN matches the PIN that is associated with
	 * the <code>Card</code>.
	 * @param pin The PIN that is entered (for example on an ATM or a PIN machine)
	 * @return true if pin matches the debit card's PIN, otherwise false
	 */
	public boolean isValidPIN(String pin) {
		return PIN.equals(pin);
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
	
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
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
	
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Transient
	public String getClassName() {
		return CLASSNAME;
	}
	
	@Transient
	public String getPrimaryKeyVal() {
		return cardNumber;
	}
	
	public void saveToDB() {
		DataManager.save(this);
	}
	
	public void deleteFromDB() {
		DataManager.removeEntryFromDB(this);
	}
}
