package userinterface;

import java.util.HashMap;
import java.util.Map;

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
	Map<Integer, BankAccount> bankAccountMap = new HashMap<>();
	BankAccount bankAccount;
	State state;
	BankingLogger bankingLogger = new BankingLogger();

	public Session() {
		state = State.LOGGED_OUT;
	}

}
