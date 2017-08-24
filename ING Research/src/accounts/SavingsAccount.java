package accounts;

import java.util.Calendar;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DataManager;
import exceptions.ClosedAccountTransferException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.ObjectDoesNotExistException;
import interesthandler.InterestHandler2;
import server.rest.ServerModel;

@Entity
@Table(name = "savingsaccounts")
public class SavingsAccount extends Account {
	
	private BankAccount bankAccount;	
	
	public static final String CLASSNAME = "accounts.SavingsAccount";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	public SavingsAccount() {
		
	}
	
	public SavingsAccount(BankAccount bankAccount) {
		super(bankAccount.getIBAN(), 0, true);
		this.bankAccount = bankAccount;
	}
	
	public SavingsAccount(String IBAN) throws ObjectDoesNotExistException {
		super(IBAN, 0, true);
		this.bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
	}
	
	public void transfer(double amount) throws IllegalAmountException, IllegalTransferException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (super.getBalance() - amount < 0) {
			throw new IllegalTransferException();
		} else if (super.isClosed()) {
			throw new ClosedAccountTransferException();
		}
		
		credit(amount);
		bankAccount.debit(amount);
		
		Calendar c = ServerModel.getServerCalendar();
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(super.getBalance() + "S");
		t.setDestinationIBAN(bankAccount.getIBAN());
		t.setAmount(amount);
		t.setDescription("Transfer from savings account to main account.");
		t.saveToDB();
		this.saveToDB();
		bankAccount.saveToDB();
	}
	
	public void credit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		super.setBalance(super.getBalance() - (float) amount);
		InterestHandler2.updateBalanceReachEntry(this);
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
		super.setBalance(super.getBalance() + (float) amount);
	}

	@OneToOne(fetch = FetchType.LAZY)
	@PrimaryKeyJoinColumn
	public BankAccount getBankAccount() {
		return bankAccount;
	}

	public void setBankAccount(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
	}

	@Transient
	public String getClassName() {
		return CLASSNAME;
	}
}
