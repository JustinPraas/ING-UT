package userinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	List<BankAccount> bankAccountMap = new ArrayList<>();
	BankAccount bankAccount;
	State state;
	BankingLogger bankingLogger = new BankingLogger();

	public Session() {
		state = State.LOGGED_OUT;
	}

}
