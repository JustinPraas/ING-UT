package exceptions;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class IllegalAccountCloseException extends Exception {

	private static final long serialVersionUID = -554961888345835254L;
	
	private String IBAN;
	private double balance;
	private boolean isSavingsAccount;
	
	public IllegalAccountCloseException(String IBAN, double balance, boolean isSavingsAccount) {
		this.IBAN = IBAN;
		this.balance = balance;
		this.isSavingsAccount = isSavingsAccount;
	}

	public String toString() {
		if (!isSavingsAccount) {
			return "Can not close account with IBAN " + IBAN + ". Account has a non-zero balance of " + balance + ".";
		} else {
			return "Can not close account with IBAN " + IBAN + ". Savings account contains money.";
		}
		
	}
}
