package testing;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;
import accounts.Transaction;
import database.DataManager;
import exceptions.ClosedAccountTransferException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;

public class AccountTest {
	public static String customerBSN = "1453.25.62";
	public static BankAccount bankAccount;
	public static CustomerAccount customerAccount;
	public static String CARDNUM = "TESTCARDNUM";
	
	@Before
	public void init() throws Exception {
		customerAccount = new CustomerAccount("John", "Test", "JT", customerBSN, "103 Testings Ave.", "000-TEST", "johntest@testing.test", "TESTDATE", "blah", "blah");
		customerAccount.openBankAccount();
		Set<BankAccount> bankAccounts = customerAccount.getBankAccounts();
		for (BankAccount key : bankAccounts) {
			bankAccount = key;
			break;
		}
	}
	
	@After
	public void end() throws Exception {
		customerAccount.deleteFromDB();
		customerAccount = null;
		bankAccount = null;
	}

	//Test whether new BankAccounts are correctly initialized and linked to the originating CustomerAccount
	@Test
	public void accountCreationTest() {
		assertTrue(customerAccount.getBankAccounts().size() == 1);
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	//Test whether deposits are correctly handled
	@Test
	public void accountDepositTest() {
		try {
			bankAccount.deposit(100, CARDNUM);
		} catch (IllegalAmountException | ClosedAccountTransferException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 100);
	}

	//Test whether invalid amounts in deposits are handled correctly
	@Test
	public void illegalDepositTest() {
		try {
			bankAccount.deposit(-100, CARDNUM);
		} catch (IllegalAmountException | ClosedAccountTransferException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}

	//Test whether transfers are executed correctly
	@Test
	public void transferTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount.getBSN());
		try {
			bankAccount2.deposit(100, CARDNUM);
		} catch (IllegalAmountException | ClosedAccountTransferException e) {
			
		}
		
		try {
			bankAccount2.transfer(bankAccount, 100);
		} catch (IllegalAmountException e) {
			
		} catch (IllegalTransferException e) {
			
		}
		assertTrue(bankAccount.getBalance() == 100);
		assertTrue(bankAccount2.getBalance() == 0);
		bankAccount2.deleteFromDB();
	}
	
	

	//Test whether illegal (negative) debit amounts are recognized and handled correctly
	@Test
	public void illegalDebitTest() {
		try {
			bankAccount.debit(-2000);
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	//Test whether illegal (negative) credit amounts are recognized and handled correctly
	@Test
	public void illegalCreditTest() {
		try {
			bankAccount.credit(-2000);
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	//Test whether debiting a BankAccount correctly decreases its balance
	@Test
	public void creditTest() {
		try {
			bankAccount.credit(1000);
		} catch (IllegalAmountException e) {
			
		}

		assertTrue(bankAccount.getBalance() == -1000);
	}
	
	//Test whether crediting a BankAccount correctly increases its balance
	@Test
	public void debitTest() {
		try {
			bankAccount.debit(1000);
		} catch (IllegalAmountException e) {
			
		}

		assertTrue(bankAccount.getBalance() == 1000);
	}
	
	//Test whether transfers with illegal (negative) amounts are recognized and handled correctly
	@Test
	public void illegalTransferAmountTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount.getBSN());
		try {
			bankAccount2.deposit(100, CARDNUM);
		} catch (IllegalAmountException e) {
			
		} catch (ClosedAccountTransferException e) {
			
		}
		
		try {
			bankAccount2.transfer(bankAccount, -100);
		} catch (IllegalAmountException e) {

		} catch (IllegalTransferException e) {

		}
		
		assertTrue(bankAccount2.getBalance() == 100);
		assertTrue(bankAccount.getBalance() == 0);
		bankAccount2.deleteFromDB();
	}
	
	//Test whether attempting to transfer without having enough money is handled correctly
	@Test
	public void illegalTransferTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount.getBSN());
		try {
			bankAccount2.deposit(100, CARDNUM);
		} catch (IllegalAmountException | ClosedAccountTransferException e) {
			
		}
		
		try {
			bankAccount2.transfer(bankAccount, 101);
		} catch (IllegalAmountException e) {

		} catch (IllegalTransferException e) {

		}
		
		assertTrue(bankAccount2.getBalance() == 100);
		assertTrue(bankAccount.getBalance() == 0);
		bankAccount2.deleteFromDB();
	}
	
	@AfterClass
	public static void cleanup() {
		@SuppressWarnings("unchecked")
		ArrayList<Transaction> ts = (ArrayList<Transaction>) DataManager.getObjectsFromDB(Transaction.CLASSNAME);
		
		for (Transaction t : ts) {
			t.deleteFromDB();
		}
	}
}
