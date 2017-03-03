package banking;

import java.util.HashSet;

import exceptions.IllegalAmountException;
import exceptions.IllegalCloseException;

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
	 * Transfers a specific amount of money from one <code>BankAccount</code> to another.
	 * @param source The <code>BankAccount</code> from which the transfer is made
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from the source to the destination
	 */
	public void transfer(BankAccount source, BankAccount destination, float amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		//TODO: Implement
	}
	
	/**
	 * Close the <code>BankAccount</code>.
	 */
	public void close() throws IllegalCloseException {
		if (balance < 0) {
			throw new IllegalCloseException(IBAN, balance);
		}
		//TODO: Implement
	}
	
	/**
	 * Displays the <code>BankAccount</code>'s balance to the TUI.
	 */
	public void viewBalance() {
		System.out.println("Current balance for account " + IBAN + ": " + balance);
	}
}
