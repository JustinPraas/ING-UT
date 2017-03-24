package exceptions;

/**
 * Thrown when a transfer, deposit or other amount-dependent operation is attempted with
 * an amount <= 0 is chosen.
 * @author Andrei Cojocaru
 */
public class IllegalAmountException extends Exception {
	private static final long serialVersionUID = 8327769399825056386L;
	private float amount;
	
	public IllegalAmountException (float amount) {
		this.amount = amount;
	}
	
	public String toString() {
		return "Illegal amount specified: " + amount;
	}
}
