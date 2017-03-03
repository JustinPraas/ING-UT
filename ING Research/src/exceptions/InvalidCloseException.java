package exceptions;

/**
 * Thrown when a customer attempts to close a BankAccount with a negative balance.
 * Outstanding debts must be paid before an account can be closed. 
 * @author Andrei Cojocaru
 */
public class InvalidCloseException extends Exception {
	float balance;
	String IBAN;
	
	public InvalidCloseException(String IBAN, float balance) {
		this.balance = balance;
		this.IBAN = IBAN;
	}
	
	public String toString() {
		return "Attempted to close account " + IBAN + " which has a negative balance of " + balance;
	}
}
