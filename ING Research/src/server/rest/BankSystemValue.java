package server.rest;

public enum BankSystemValue {

	CREDIT_CARD_MONTHLY_FEE		(0.0),
	CREDIT_CARD_DEFAULT_CREDIT	(0.0),
	CARD_EXPIRATION_LENGTH		(4.0),
	NEW_CARD_COST				(7.5),
	CARD_USAGE_ATTEMPTS			(3.0),
	MAX_OVERDRAFT_LIMIT			(5000.0),
	INTEREST_RATE_1				(0.15),
	INTEREST_RATE_2				(0.15),
	INTEREST_RATE_3				(0.20),
	OVERDRAFT_INTEREST_RATE		(10.0),
	DAILY_WITHDRAW_LIMIT		(250.0), 
	WEEKLY_TRANSFER_LIMIT		(2500.0);
	
	private final double amount;
	
    BankSystemValue(double amount) {
        this.amount = amount;
    }

	public double getAmount() {
		return amount;
	}
}
