package testing;

import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.HashSet;

import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import database.BankingLogger;

public class BankingLoggerTest {
	public static String testBSN = "TEST";
	@Test 
	public void testBankAccountAdditionRemovalLocation() {
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
		BankingLogger.removeBankAccount(bankAccount.getIBAN());
		assertTrue(BankingLogger.getBankAccountByIBAN(bankAccount.getIBAN()) == null);
		assertTrue(BankingLogger.getBankAccountsByBSN(testBSN).size() == 0);
		BankingLogger.removeCustomerAccount(testBSN);
	}
}
