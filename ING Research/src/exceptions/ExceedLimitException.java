package exceptions;

import accounts.Account;
import accounts.BankAccount;
import server.rest.BankSystemValue;

/**
 * Thrown when a user wants to pay/transfer money, but the amount exceeds the overdraft limit.
 * @author Justin Praas
 */
public class ExceedLimitException extends Exception {

	private static final long serialVersionUID = 5436165626961779364L;
	private double amount;
	private Account account;
	private LimitType limitType;
	
	public enum LimitType {OVERDRAFT_LIMIT, DEBIT_CARD_LIMIT, CREDIT_CARD_LIMIT, TRANSFER_LIMIT};
	
	public ExceedLimitException(double amount, Account account, LimitType limitType) {
		this.amount = amount;
		this.account = account;
		this.limitType = limitType;		
	}

	public String toString() {
		if (limitType == LimitType.OVERDRAFT_LIMIT) {
			double exceedingAmount = -1 * (((BankAccount) account).getOverdraftLimit() - account.getBalance() - amount);
			return "Can not complete transaction: exceeding the bank account's overdraft limit of " + ((BankAccount) account).getOverdraftLimit() + ". Exceeding amount: " + exceedingAmount + ".";
		}
		
		if (limitType == LimitType.TRANSFER_LIMIT) {
			return "Can not complete transaction: exceeding the bank account's transfer limit of " + ((BankAccount) account).getTransferLimit() + ".";
		}
		
		if (limitType == LimitType.DEBIT_CARD_LIMIT) {
			return "Can not complete transaction: exceeding the bank account's daily debit card limit of " + BankSystemValue.DAILY_WITHDRAW_LIMIT.getAmount();
		}
		
		if (limitType == LimitType.CREDIT_CARD_LIMIT) {
			return "Can not complete transaction: credit card balance can not go into the reds. Nice try.";
		}
		
		return "Unspecified limit type for limit exception.";
	}
}
