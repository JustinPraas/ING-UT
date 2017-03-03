package banking;

import java.util.HashSet;

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
	public void deposit(float amount) {
		balance += amount;
	}
	
	/**
	 * Transfers a specific amount of money from one <code>BankAccount</code> to another.
	 * @param source The <code>BankAccount</code> from which the transfer is made
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from the source to the destination
	 */
	public void transfer(BankAccount source, BankAccount destination, float amount) {
		//TODO: Implement
	}
	
	/**
	 * Close the <code>BankAccount</code>.
	 */
	public void close() {
		//TODO: Implement
	}
}
