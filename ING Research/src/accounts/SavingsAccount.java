package accounts;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DataManager;
import exceptions.ClosedAccountTransferException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.ObjectDoesNotExistException;
import server.rest.ServerModel;

@Entity
@Table(name = "savingsaccounts")
public class SavingsAccount implements database.DBObject {
	
	private float balance;
	private String IBAN;
	private BankAccount bankAccount;	
	private boolean closed;
	
	public static final String CLASSNAME = "accounts.SavingsAccount";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	public SavingsAccount() {
		
	}
	
	public SavingsAccount(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
		this.IBAN = bankAccount.getIBAN();
		this.balance = 0;
		this.closed = true;
	}
	
	public SavingsAccount(String IBAN) throws ObjectDoesNotExistException {
		this.bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		this.IBAN = IBAN;
		this.balance = 0;
		this.closed = true;
	}
	
	public void transfer(double amount) throws IllegalAmountException, IllegalTransferException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance - amount < 0) {
			throw new IllegalTransferException();
		} else if (closed) {
			throw new ClosedAccountTransferException();
		}
		
		credit(amount);
		bankAccount.debit(amount);
		
		Calendar c = ServerModel.getServerCalendar();
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(IBAN + "S");
		t.setDestinationIBAN(bankAccount.getIBAN());
		t.setAmount(amount);
		t.setDescription("Transfer to " + bankAccount.getIBAN() + ".");
		t.saveToDB();
		this.saveToDB();
		bankAccount.saveToDB();
	}
	
	public void credit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance -= amount;
	}
	
	/**
	 * Debits a <code>SavingsAccount</code> with a specific amount of money
	 * @param amount The amount of money to debit the <code>SavingsAccount</code> with
	 * @throws Thrown when the specified amount is 0 or negative
	 */
	public void debit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance += amount;
	}

	@Column(name = "balance")
	public float getBalance() {
		return balance;
	}

	public void setBalance(float balance) {
		this.balance = balance;
	}

	@Id
	@Column(name = "IBAN")
	public String getIBAN() {
		return IBAN;
	}

	public void setIBAN(String iBAN) {
		IBAN = iBAN;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public BankAccount getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
	}

	@Column(name = "closed")
	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Transient
	public Object getPrimaryKeyVal() {
		return IBAN;
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
