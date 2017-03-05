package accounts;

import java.util.HashSet;

import exceptions.IllegalAmountException;
import exceptions.IllegalCloseException;
import exceptions.IllegalTransferException;

/**
 * A simple model of a bank account in an abstract currency.
 * @author Andrei Cojocaru
 */
public class BankAccount {
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
		this.mainHolder = holder;
		this.holders = new HashSet<CustomerAccount>();
		this.holders.add(holder);
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
}
