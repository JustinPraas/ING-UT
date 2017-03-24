package testing;

import static org.junit.Assert.*;

import java.sql.Date;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import database.BankingLogger;
import exceptions.IllegalAmountException;
import exceptions.IllegalAccountDeletionException;
import exceptions.IllegalTransferException;

public class AccountTest {
	public static String customerBSN = "1453.25.62";
	public static BankAccount bankAccount;
	public static CustomerAccount customerAccount;
	
	@Before
	public void init() throws Exception {
		customerAccount = new CustomerAccount("John", "Test", customerBSN, "103 Testings Ave.", "000-TEST", "johntest@testing.test", new Date(0));
		customerAccount.openBankAccount();
		HashSet<BankAccount> bankAccounts = BankingLogger.getBankAccountsByBSN(customerBSN);
		for (BankAccount key : bankAccounts) {
			bankAccount = key;
			break;
		}
	}
	
	@After
	public void end() throws Exception {
		BankingLogger.removeCustomerAccount(customerBSN, true);
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
			bankAccount.deposit(100);
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 100);
	}
	
	//Test whether illegal attempts to close a BankAccount are correctly handled
	@Test
	public void illegalClosingExceptionTest() {
		try {
			bankAccount.deposit(100);
		} catch (IllegalAmountException e) {
			
		}
		
		try {
			bankAccount.deleteAccount();
		} catch (IllegalAccountDeletionException e) {
			
		}
		
		assertTrue(customerAccount.getBankAccounts().size() == 1);
	}
	
	//Test whether BankAccounts are correctly removed from the originating CustomerAccount on closing them 
	@Test
	public void accountClosingTest() {
		try {
			bankAccount.deleteAccount();
		} catch (IllegalAccountDeletionException e) {
			
		}
		
		assertTrue(customerAccount.getBankAccounts().size() == 0);
	}
	
	//Test whether the correct BankAccount is removed from the originating CustomerAccount on closing
	@Test
	public void multipleAccountClosingTest() {
		customerAccount.openBankAccount();
		
		try {
			bankAccount.deleteAccount();
		} catch (IllegalAccountDeletionException e) {
			
		}
		
		assertTrue(customerAccount.getBankAccounts().size() == 1);
		assertFalse(customerAccount.getBankAccounts().contains(bankAccount));
	}

	//Test whether invalid amounts in deposits are handled correctly
	@Test
	public void illegalDepositTest() {
		try {
			bankAccount.deposit(-100);
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}

	//Test whether transfers are executed correctly
	@Test
	public void transferTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount.getBSN());
		try {
			bankAccount2.deposit(100);
		} catch (IllegalAmountException e) {
			
		}
		
		try {
			bankAccount2.transfer(bankAccount, 100);
		} catch (IllegalAmountException e) {
			
		} catch (IllegalTransferException e) {
			
		}
		assertTrue(bankAccount.getBalance() == 100);
		assertTrue(bankAccount2.getBalance() == 0);
		BankingLogger.removeBankAccount(bankAccount2.getIBAN(), true);
	}
	
	

	//Test whether illegal (negative) debit amounts are recognized and handled correctly
	@Test
	public void illegalDebitTest() {
		try {
			bankAccount.debit(-2000, "Test");
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	//Test whether illegal (negative) credit amounts are recognized and handled correctly
	@Test
	public void illegalCreditTest() {
		try {
			bankAccount.credit(-2000, "Test");
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	//Test whether debiting a BankAccount correctly decreases its balance
	@Test
	public void creditTest() {
		try {
			bankAccount.credit(1000, "Test");
		} catch (IllegalAmountException e) {
			
		}

		assertTrue(bankAccount.getBalance() == -1000);
	}
	
	//Test whether crediting a BankAccount correctly increases its balance
	@Test
	public void debitTest() {
		try {
			bankAccount.debit(1000, "Test");
		} catch (IllegalAmountException e) {
			
		}

		assertTrue(bankAccount.getBalance() == 1000);
	}
	
	//Test whether transfers with illegal (negative) amounts are recognized and handled correctly
	@Test
	public void illegalTransferAmountTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount.getBSN());
		try {
			bankAccount2.deposit(100);
		} catch (IllegalAmountException e) {
			
		}
		
		try {
			bankAccount2.transfer(bankAccount, -100);
		} catch (IllegalAmountException e) {

		} catch (IllegalTransferException e) {

		}
		
		assertTrue(bankAccount2.getBalance() == 100);
		assertTrue(bankAccount.getBalance() == 0);
		BankingLogger.removeBankAccount(bankAccount2.getIBAN(), true);
	}
	
	//Test whether attempting to transfer without having enough money is handled correctly
	@Test
	public void illegalTransferTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount.getBSN());
		try {
			bankAccount2.deposit(100);
		} catch (IllegalAmountException e) {
			
		}
		
		try {
			bankAccount2.transfer(bankAccount, 101);
		} catch (IllegalAmountException e) {

		} catch (IllegalTransferException e) {

		}
		
		assertTrue(bankAccount2.getBalance() == 100);
		assertTrue(bankAccount.getBalance() == 0);
		BankingLogger.removeBankAccount(bankAccount2.getIBAN(), true);
	}
}
