package exceptions;

/**
 * Thrown when a customer attempts to close a BankAccount with a nonzero balance.
 * @author Andrei Cojocaru
 */
public class IllegalCloseException extends Exception {
	float balance;
	String IBAN;
	
	public IllegalCloseException(String IBAN, float balance) {
		this.balance = balance;
		this.IBAN = IBAN;
	}
	
	public String toString() {
		return "Attempted to close account " + IBAN + " which has a balance of " + balance;
	}
}
