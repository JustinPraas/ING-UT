package exceptions;

/**
 * Thrown when a transfer is attempted with a source <code>BankAccount</code> that does not have 
 * a sufficient balance to cover the chosen transfer amount.
 * @author Andrei Cojocaru
 */
public class IllegalTransferException extends Exception {
	private static final long serialVersionUID = -8284837193637155915L;
	float balance;
	String IBAN;
	float amount;
	
	public IllegalTransferException(float balance, String IBAN, float amount) {
		this.balance = balance;
		this.IBAN = IBAN;
		this.amount = amount;
	}
	
	public String toString() {
		return "Attempted to transfer sum of " + amount + " from account " + IBAN + " which has a balance of " + balance;
	}
}
