package cards;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import accounts.CreditAccount;
import database.DataManager;
import exceptions.InvalidPINException;
import exceptions.ObjectDoesNotExistException;
import exceptions.PinCardBlockedException;
import exceptions.SameAccountTransferException;
import exceptions.ClosedAccountTransferException;
import exceptions.CreditCardNotActiveException;
import exceptions.ExceedLimitException;
import exceptions.IllegalAmountException;

@Entity
@Table(name = "creditcards")
public class CreditCard extends Card implements database.DBObject {
	
	private boolean active;
	
	public static final String CLASSNAME = "cards.CreditCard";
	public static final String PRIMARYKEYNAME = "cardNumber";

	public CreditCard() {
		
	}
	
	public CreditCard(String holderBSN, String bankAccountIBAN, String cardNumber) {
		super(generatePin(), cardNumber, bankAccountIBAN, holderBSN);
		this.active = false;		
	}

	public CreditCard(String holderBSN, String bankAccountIBAN, String pin, String cardNumber) {
		super(pin, cardNumber, bankAccountIBAN, holderBSN);
		this.active = false;	
	}

	public void pinPayment(double amount, String PIN, String destinationIBAN) throws CreditCardNotActiveException, InvalidPINException,
	ClosedAccountTransferException, SameAccountTransferException, ExceedLimitException, IllegalAmountException, PinCardBlockedException {
		CreditAccount ownAccount;
		try {
			ownAccount = (CreditAccount) DataManager.getObjectByPrimaryKey(CreditAccount.CLASSNAME, super.getBankAccountIBAN());
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
			return;
		}
		
		if (isBlocked()) {
			throw new PinCardBlockedException(super.getCardNumber());
		} else if (!isActive()) {
			throw new CreditCardNotActiveException(super.getCardNumber());
		} else if (!isValidPIN(PIN)) {
			throw new InvalidPINException(PIN, getCardNumber());
		}
		
		ownAccount.transfer(destinationIBAN, amount, "Credit card payment from " + ownAccount.getIBAN() + " to " + destinationIBAN + ".");
	}
	
	public static String generateCardNumber() {
		boolean unique = false;
		StringBuilder resultCardNumber = null;
		
		while (!unique) {
			resultCardNumber = new StringBuilder();
			resultCardNumber.append("524886");
			
			//Append 7 random digits
			for (int i = 0; i < 10; i++) {
				resultCardNumber.append((int)(Math.random() * 10));
			}
			
			if (DataManager.isPrimaryKeyUnique(CLASSNAME, PRIMARYKEYNAME, resultCardNumber.toString())) {
				unique = true;
			}
		}
		
		return resultCardNumber.toString();
	}

	@Column(name = "active")
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
