package exceptions;

/**
 * Thrown when a transfer is attempted with a source <code>BankAccount</code> that does not have 
 * a sufficient balance to cover the chosen transfer amount, or a transfer from a bank account to itself/a closed bank account.
 * @author Andrei Cojocaru
 */
public class InsufficientFundsTransferException extends IllegalTransferException {
	private static final long serialVersionUID = -8284837193637155915L;
	double balance;
	String IBAN;
	double amount;
	
	public InsufficientFundsTransferException(double balance, String IBAN, double amount) {
		this.balance = balance;
		this.IBAN = IBAN;
		this.amount = amount;
	}
	
	public String toString() {
		return "Attempted to transfer sum of " + amount + " from account " + IBAN + " which has a balance of " + balance;
	}
}
