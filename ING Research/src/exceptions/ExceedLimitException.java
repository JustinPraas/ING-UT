package exceptions;

import accounts.BankAccount;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class ExceedLimitException extends Exception {

	private static final long serialVersionUID = 5436165626961779364L;
	private double amount;
	private BankAccount bankAccount;
	private LimitType limitType;
	
	public enum LimitType {OVERDRAFT_LIMIT, DEBITCARD_LIMIT, TRANSFER_LIMIT};
	
	public ExceedLimitException(double amount, BankAccount bankAccount, LimitType limitType) {
		this.amount = amount;
		this.bankAccount = bankAccount;
		this.limitType = limitType;		
	}

	public String toString() {
		if (limitType == LimitType.OVERDRAFT_LIMIT) {
			double exceedingAmount = -1 * (bankAccount.getOverdraftLimit() - bankAccount.getBalance() - amount);
			return "Can not complete transaction: exceeding the bank account's overdraft limit of " + bankAccount.getOverdraftLimit() + ". Exceeding amount: " + exceedingAmount + ".";
		}
		
		if (limitType == LimitType.TRANSFER_LIMIT) {
			return "Can not complete transaction: exceeding the bank account's transfer limit of " + bankAccount.getTransferLimit() + ".";
		}
		
		if (limitType == LimitType.DEBITCARD_LIMIT) {
			return "Can not complete transaction: exceeding the bank account's daily debit card limit of 250.00.";
		}
		
		return "Unspecified limit type for limit exception.";
	}
}
