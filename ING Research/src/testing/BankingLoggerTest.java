package testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.HashSet;

import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;
import database.BankingLogger;

public class BankingLoggerTest {
	public static String testBSN = "TEST";
	
	@Test 
	public void testBankingLoggerGeneral() {
		CustomerAccount custAccount = new CustomerAccount("John", "Smith", testBSN, "103 Testings Ave.", "000-TEST", "johntest@testing.test", new Date(0));
		BankAccount bankAccount = null;
		custAccount.openBankAccount();
		HashSet<BankAccount> bankAccounts = BankingLogger.getBankAccountsByBSN(testBSN);
		for (BankAccount key : bankAccounts) {
			bankAccount = key;
			break;
		}
		assertTrue(bankAccount.getIBAN() == BankingLogger.getBankAccountByIBAN(bankAccount.getIBAN()).getIBAN());
		assertTrue(BankingLogger.getBankAccountsByBSN(testBSN).size() == 1);
		BankingLogger.removeBankAccount(bankAccount.getIBAN(), true);
		assertTrue(BankingLogger.getBankAccountByIBAN(bankAccount.getIBAN()) == null);
		assertTrue(BankingLogger.getBankAccountsByBSN(testBSN).size() == 0);
		BankingLogger.removeCustomerAccount(testBSN, true);
	}
	
	@Test
	public void testBankAccountExists() {
		BankAccount bankAccount = new BankAccount(testBSN);
		assertTrue(BankingLogger.bankAccountExists(bankAccount.getIBAN()));
		BankingLogger.removeBankAccount(bankAccount.getIBAN(), true);
		assertFalse(BankingLogger.bankAccountExists(bankAccount.getIBAN()));
	}
	
	@Test
	public void testCustomerAccountExists() {
		@SuppressWarnings("unused")
		CustomerAccount customerAccount = new CustomerAccount("John", "Smith", testBSN, "103 Testings Ave.", "000-TEST", "johntest@testing.test", new Date(0));
		assertTrue(BankingLogger.customerAccountExists(testBSN));
		BankingLogger.removeCustomerAccount(testBSN, true);
		assertFalse(BankingLogger.customerAccountExists(testBSN));
	}
	
	@Test
	public void testPairing() {
		BankAccount bankAccount = new BankAccount("NOTJOHN");
		BankAccount bankAccount2 = new BankAccount("DAVE");
		BankAccount bankAccount3 = new BankAccount("SALLY");
		CustomerAccount customerAccount = new CustomerAccount("John", "Smith", testBSN, "103 Testings Ave.", "000-TEST", "johntest@testing.test", new Date(0));
		customerAccount.addBankAccount(bankAccount);
		customerAccount.addBankAccount(bankAccount2);
		customerAccount.addBankAccount(bankAccount3);
		
		assertTrue(BankingLogger.customerBankAccountPairingExists(testBSN, bankAccount.getIBAN()));
		assertTrue(BankingLogger.customerBankAccountPairingExists(testBSN, bankAccount2.getIBAN()));
		assertTrue(BankingLogger.customerBankAccountPairingExists(testBSN, bankAccount3.getIBAN()));
		
		BankingLogger.removeCustomerAccount(testBSN, true);
		
		assertFalse(BankingLogger.customerBankAccountPairingExists(testBSN, bankAccount.getIBAN()));
		assertFalse(BankingLogger.customerBankAccountPairingExists(testBSN, bankAccount2.getIBAN()));
		assertFalse(BankingLogger.customerBankAccountPairingExists(testBSN, bankAccount3.getIBAN()));
		
		assertTrue(BankingLogger.bankAccountExists(bankAccount.getIBAN()));
		assertTrue(BankingLogger.bankAccountExists(bankAccount2.getIBAN()));
		assertTrue(BankingLogger.bankAccountExists(bankAccount3.getIBAN()));
		
		BankingLogger.removeBankAccount(bankAccount.getIBAN(), true);
		BankingLogger.removeBankAccount(bankAccount2.getIBAN(), true);
		BankingLogger.removeBankAccount(bankAccount3.getIBAN(), true);
	}
	
	@Test
	public void testDebitCardManagement() {
		assertFalse(BankingLogger.debitCardExists("TESTNUM"));
		assertTrue(BankingLogger.getDebitCardsByBSN(testBSN).size() == 0);
		DebitCard testCard = new DebitCard(testBSN, "TESTIBAN", new Date(0), "TESTNUM" ,"7357");
		BankingLogger.addDebitCardEntry(testCard, true);
		assertTrue(BankingLogger.debitCardExists("TESTNUM"));
		assertTrue(BankingLogger.getDebitCardsByBSN(testBSN).size() == 1);
		DebitCard testCard2 = new DebitCard(testBSN, "TESTIBAN", new Date(0), "TESTNUM2" ,"7357");
		BankingLogger.addDebitCardEntry(testCard2, true);
		assertTrue(BankingLogger.getDebitCardsByBSN(testBSN).size() == 2);
		BankingLogger.removeDebitCard(testCard2.getCardNum(), true);
		assertFalse(BankingLogger.debitCardExists(testCard2.getCardNum()));
		assertTrue(BankingLogger.getDebitCardsByBSN(testBSN).size() == 1);
		BankingLogger.removeDebitCard(testCard.getCardNum(), true);
		assertFalse(BankingLogger.debitCardExists(testCard.getCardNum()));
		assertTrue(BankingLogger.getDebitCardsByBSN(testBSN).size() == 0);
	}
}
