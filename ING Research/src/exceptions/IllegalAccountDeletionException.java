package exceptions;

/**
 * Thrown when a customer attempts to delete a BankAccount with a nonzero balance.
 * @author Andrei Cojocaru
 */
public class IllegalAccountDeletionException extends Exception {
	private static final long serialVersionUID = -8340410108674572686L;
	float balance;
	String IBAN;
	
	public IllegalAccountDeletionException(String IBAN, float balance) {
		this.balance = balance;
		this.IBAN = IBAN;
	}
	
	public String toString() {
		return "Attempted to delete account " + IBAN + " which has a balance of " + balance;
	}
}
