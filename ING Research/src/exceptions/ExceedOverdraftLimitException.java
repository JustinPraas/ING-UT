package exceptions;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class ExceedOverdraftLimitException extends Exception {

	private static final long serialVersionUID = 5436165626961779364L;
	private double overdraftLimit;
	
	public ExceedOverdraftLimitException(double overdraftLimit) {
		this.overdraftLimit = overdraftLimit;
	}

	public String toString() {
		return "Can not complete payment/transfer: exceeding overdraft limit of " + overdraftLimit + ".";
	}
}
