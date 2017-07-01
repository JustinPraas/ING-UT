package accounts;

import java.beans.Transient;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Id;

import database.DataManager;
import exceptions.ClosedAccountTransferException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.ObjectDoesNotExistException;
import server.rest.ServerModel;

public class SavingsAccount implements database.DBObject {
	
	private float balance;
	private String IBAN;
	private BankAccount bankAccount;
	private boolean closed;
	
	public static final String CLASSNAME = "accounts.SavingsAccount";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	public SavingsAccount(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
		this.IBAN = bankAccount.getIBAN();
		this.balance = 0;
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
		t.setSourceIBAN(IBAN);
		t.setDestinationIBAN(bankAccount.getIBAN());
		t.setAmount(amount);
		t.setDescription("Transfer to " + bankAccount.getIBAN() + ".");
		t.saveToDB();
		saveToDB();
		bankAccount.saveToDB();
	}
	
	public void credit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance -= amount;
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

	@Transient
	public BankAccount getBankAccount() throws ObjectDoesNotExistException {
		return (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
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

	@Override
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Override
	public Object getPrimaryKeyVal() {
		return IBAN;
	}

	@Override
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
