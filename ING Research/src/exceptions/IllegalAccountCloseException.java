package exceptions;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class IllegalAccountCloseException extends Exception {

	private static final long serialVersionUID = -554961888345835254L;
	
	private String IBAN;
	private double balance;
	
	public IllegalAccountCloseException(String IBAN, double balance) {
		this.IBAN = IBAN;
		this.balance = balance;
	}

	public String toString() {
		return "Can not close account with IBAN " + IBAN + ". Account has a non-zero balance of " + balance + ".";
	}
}
