package client;

import accounts.Account;
import accounts.BankAccount;

public class Test {
	public static void main(String[] args) {
		Account account = new BankAccount();
		System.out.println(account.getClassName());
	}
}
