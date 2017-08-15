package exceptions;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class IllegalAccountCloseException extends Exception {

	private static final long serialVersionUID = -554961888345835254L;
	
	private String IBAN;
	private String message;
	
	public IllegalAccountCloseException(String IBAN, String message) {
		this.IBAN = IBAN;
		this.message = message;
	}

	public String toString() {
		return "Can not close account (" + IBAN + "): " + message;
	}
}
