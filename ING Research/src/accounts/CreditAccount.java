package accounts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import cards.CreditCard;
import database.DataManager;
import exceptions.ClosedAccountTransferException;
import exceptions.ExceedLimitException;
import exceptions.ExceedLimitException.LimitType;
import server.rest.ServerModel;
import exceptions.IllegalAccountCloseException;
import exceptions.IllegalAmountException;
import exceptions.ObjectDoesNotExistException;
import exceptions.SameAccountTransferException;

@Entity
@Table(name = "creditaccounts")
public class CreditAccount extends Account {

	private String customerBSN;
	private BankAccount bankAccount;
	
	public static final String CLASSNAME = "accounts.CreditAccount";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	public CreditAccount() {
		
	}
	
	public CreditAccount(BankAccount bankAccount) {
		super(bankAccount.getIBAN(), 1000, false);
		this.bankAccount = bankAccount;
		this.customerBSN = bankAccount.getMainHolderBSN();
	}
	
	public CreditAccount(String IBAN) throws ObjectDoesNotExistException {
		super(IBAN, 1000, false);
		this.bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
	}
	
	public void transfer(String destinationIBAN, double amount, String description) throws ClosedAccountTransferException, ExceedLimitException, IllegalAmountException, SameAccountTransferException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (super.getBalance() - amount < 0) {
			throw new ExceedLimitException(amount, this, LimitType.CREDIT_CARD_LIMIT);
		} else if (super.isClosed()) {
			throw new ClosedAccountTransferException();
		}
		
		boolean knownAccount = false;
		BankAccount destination = null;
		
		if (!DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, destinationIBAN)) {
			knownAccount = true;
			try {
				destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, destinationIBAN);
			} catch (ObjectDoesNotExistException e) {
				System.err.println(e.toString());
				return;
			}
		}

		if (knownAccount) {
			if (destination.isClosed()) {
				throw new ClosedAccountTransferException();
			} else if (destination.getIBAN().equals(this.getIBAN())) {
				throw new SameAccountTransferException();
			}
			destination.debit(amount);
			destination.saveToDB();
		}
		
		this.credit(amount);
		this.saveToDB();
		
		Calendar c = ServerModel.getServerCalendar();
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(this.getIBAN() + "C");
		t.setDestinationIBAN(destinationIBAN);
		t.setPinTransaction(true);
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
	}

	public void credit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		super.setBalance(super.getBalance() - (float) amount);
	}
	
	/**
	 * Debits a <code>CreditAccount</code> with a specific amount of money
	 * @param amount The amount of money to debit the <code>SavingsAccount</code> with
	 * @throws Thrown when the specified amount is 0 or negative
	 */
	public void debit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		super.setBalance(super.getBalance() + (float) amount);
	}
	
	public void close() throws IllegalAccountCloseException {
		if (super.getBalance() < 1000) {
			throw new IllegalAccountCloseException(super.getIBAN(), "Credit account contains a balance less than 1000 euros: " + super.getBalance() + ".");
		}
		
		this.setClosed(true);
		
	}

	@SuppressWarnings("unchecked")
	@Transient
	public List<CreditCard> getCreditCard() {
		List<CreditCard> result = new ArrayList<>();
		ArrayList<Criterion> criteria = new ArrayList<>();
		criteria.add(Restrictions.eq("bankAccountIBAN", getPrimaryKeyVal()));
		result = (List<CreditCard>) DataManager.getObjectsFromDB(CreditCard.CLASSNAME, criteria);
		return result;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public BankAccount getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
	}

	@Column(name = "customer_BSN")
	public String getOwnerBSN() {
		return customerBSN;
	}

	public void setOwnerBSN(String ownerBSN) {
		this.customerBSN = ownerBSN;
	}
}
