package exceptions;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class ExceedLimitException extends Exception {

	private static final long serialVersionUID = 5436165626961779364L;
	private double amount;
	private String IBAN;
	private LimitType limitType;
	
	public enum LimitType {OVERDRAFT_LIMIT, DEBITCARD_LIMIT, WEEKLY_ACCOUNT_LIMIT};
	
	public ExceedLimitException(double amount, String IBAN, LimitType limitType) {
		this.amount = amount;
		this.IBAN = IBAN;
		this.limitType = limitType;		
	}

	public String toString() {
		return "Can not complete transaction: exceeding " + limitType.name() + " with amount: " + amount + ".";
	}
}
