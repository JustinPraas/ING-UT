package exceptions;

/**
 * Thrown when a transfer, deposit or other amount-dependent operation is attempted with
 * an amount <= 0 is chosen.
 * @author Andrei Cojocaru
 */
public class InvalidAmountException extends Exception {
	private float amount;
	
	public InvalidAmountException (float amount) {
		this.amount = amount;
	}
	
	public String toString() {
		return "Invalid amount specified: " + amount;
	}
}
