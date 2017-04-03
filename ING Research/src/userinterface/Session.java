package userinterface;

import accounts.BankAccount;
import accounts.CustomerAccount;
import database.BankingLogger;

public class Session {
	
	public enum State {
		LOGGED_OUT,
		CUST_LOGGED_IN,
		BANK_LOGGED_IN;
	}
	
	CustomerAccount customerAccount;
	BankAccount bankAccount;
	State state;
	BankingLogger bankingLogger = new BankingLogger();

	public Session() {
		state = State.LOGGED_OUT;
	}

}
