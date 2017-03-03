package testing;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import banking.BankAccount;
import banking.CustomerAccount;
import exceptions.IllegalAmountException;
import exceptions.IllegalCloseException;
import exceptions.IllegalTransferException;

public class AccountTest {
	BankAccount bankAccount;
	CustomerAccount customerAccount;
	
	@Before
	public void setUp() throws Exception {
		this.customerAccount = new CustomerAccount("John", "Smith", "1453.25.62", "103 Testings Ave.", "000-TEST", "johntest@testing.test", new Date());
		customerAccount.openBankAccount();
		for (BankAccount account : customerAccount.getBankAccounts()) {
			bankAccount = account;
		}
	}

	@Test
	public void accountCreationTest() {
		assertTrue(customerAccount.getBankAccounts().size() == 1);
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	@Test
	public void accountDepositTest() {
		try {
			bankAccount.deposit(100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		assertTrue(bankAccount.getBalance() == 100);
	}
	
	@Test
	public void illegalClosingExceptionTest() {
		try {
			bankAccount.deposit(100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		try {
			bankAccount.close();
		} catch (IllegalCloseException e) {
			
		}
		
		assertTrue(customerAccount.getBankAccounts().size() == 1);
	}
	
	@Test
	public void accountClosingTest() {
		try {
			bankAccount.close();
		} catch (IllegalCloseException e) {
			e.printStackTrace();
		}
		
		assertTrue(customerAccount.getBankAccounts().size() == 0);
	}
	
	@Test
	public void multipleAccountClosingTest() {
		customerAccount.openBankAccount();
		
		try {
			bankAccount.close();
		} catch (IllegalCloseException e) {
			e.printStackTrace();
		}
		
		assertTrue(customerAccount.getBankAccounts().size() == 1);
		assertFalse(customerAccount.getBankAccounts().contains(bankAccount));
	}

	@Test
	public void illegalDepositTest() {
		try {
			bankAccount.deposit(-100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}

	@Test
	public void transferTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount);
		try {
			bankAccount2.deposit(100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		try {
			bankAccount2.transfer(bankAccount, 100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		} catch (IllegalTransferException e) {
			e.printStackTrace();
		}
		assertTrue(bankAccount.getBalance() == 100);
		assertTrue(bankAccount2.getBalance() == 0);
	}
	
	

	@Test
	public void illegalDebitTest() {
		try {
			bankAccount.debit(-2000);
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	@Test
	public void illegalCreditTest() {
		try {
			bankAccount.credit(-2000);
		} catch (IllegalAmountException e) {
			
		}
		
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	@Test
	public void debitTest() {
		try {
			bankAccount.debit(1000);
		} catch (IllegalAmountException e) {
			
		}

		assertTrue(bankAccount.getBalance() == -1000);
	}
	
	@Test
	public void creditTest() {
		try {
			bankAccount.credit(1000);
		} catch (IllegalAmountException e) {
			
		}

		assertTrue(bankAccount.getBalance() == 1000);
	}
	
	public void illegalTransferAmountTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount);
		try {
			bankAccount2.deposit(100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		try {
			bankAccount2.transfer(bankAccount, -100);
		} catch (IllegalAmountException e) {

		} catch (IllegalTransferException e) {

		}
		
		assertTrue(bankAccount2.getBalance() == 100);
		assertTrue(bankAccount.getBalance() == 0);
	}
	
	public void illegalTransferTest() {
		BankAccount bankAccount2 = new BankAccount(customerAccount);
		try {
			bankAccount2.deposit(100);
		} catch (IllegalAmountException e) {
			e.printStackTrace();
		}
		
		try {
			bankAccount2.transfer(bankAccount, 101);
		} catch (IllegalAmountException e) {

		} catch (IllegalTransferException e) {

		}
		
		assertTrue(bankAccount2.getBalance() == 100);
		assertTrue(bankAccount.getBalance() == 0);
	}
}
