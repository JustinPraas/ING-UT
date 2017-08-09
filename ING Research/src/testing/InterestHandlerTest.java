package testing;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.SavingsAccount;
import database.DataManager;
import exceptions.ObjectDoesNotExistException;
import server.rest.InterestHandler;
import server.rest.ServerDataHandler;
import server.rest.ServerModel;

/**
 * WARNING: Resets all data
 * @author Justin Praas
 *
 */
public class InterestHandlerTest {
	
	// Calendar for 2017-01-01 00:00
	Calendar startOfYear = Calendar.getInstance();
	
	// startValues
	double startValue1, startValue2, startValue3;
	
	CustomerAccount cAccount = new CustomerAccount("John", "Test", "JT", "2020202020", "103 Testings Ave.", "000-TEST", "johntest@testing.test", "TESTDATE", "blah", "blah");
	BankAccount bAccount1, bAccount2, bAccount3;
	
	@Before
	public void setup() {
		
		// Reset all data
		InterestHandler.reset();
		ServerModel.reset();
		DataManager.wipeAllData();	
		
		// Set up necessary data
		startOfYear.setTimeInMillis(1483225320000L);
		startValue1 = -1000;
		startValue2 = -2000;
		startValue3 = -4000;
		
		bAccount1 = cAccount.openBankAccount();
		bAccount1.saveToDB();
		bAccount2 = cAccount.openBankAccount();
		bAccount2.saveToDB();
		bAccount3 = cAccount.openBankAccount();
		bAccount3.saveToDB();
		
		int daysUntilNextYear = Calendar.getInstance().getMaximum(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		ServerModel.setSimulatedDays(daysUntilNextYear, true);
		
		InterestHandler.setPreviousPositiveInterestExecutionDate();
		InterestHandler.setPreviousNegativeInterestExecutionDate();
		InterestHandler.setPreviousPositiveInterestExecutionDate();
	}
	
	@Test
	public void calculateShortestSleep() {
		long actualMilliDifference;
		long calculatedMilliSleep;
		
		// Milli seconds of difference between 2017-01-02 00:02 and 2017-01-01 0:00
		actualMilliDifference = 1483311600000L - 1483225320000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);

		// Milli seconds of difference between 2017-01-01 13:46 and 2017-01-01 23:46
		startOfYear.setTimeInMillis(1483274760000L);
		actualMilliDifference = 1483311600000L - 1483274760000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);

		// Milli seconds of difference between 2017-01-01 23:43 and 2017-01-01 23:46
		startOfYear.setTimeInMillis(1483310580000L);
		actualMilliDifference = 1483311600000L - 1483310580000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);
		
		// Milli seconds of difference between 2017-01-01 23:44 and 2017-01-02 23:46
		startOfYear.setTimeInMillis(1483311540000L);
		actualMilliDifference = 1483311600000L - 1483311540000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);		
	}
	
	@Test
	public void calculateInterestTest() {
		bAccount1.setBalance((float) startValue1);
		bAccount2.setBalance((float) startValue2);
		bAccount3.setBalance((float) startValue3);
		
		cAccount.saveToDB();
		bAccount1.saveToDB();
		bAccount2.saveToDB();
		bAccount3.saveToDB();
		
		double balance, interest;

		balance = bAccount1.getBalance();
		interest = InterestHandler.calculateNegativeInterest(balance, ServerModel.getServerCalendar().getMaximum(Calendar.DATE));		
		assertEquals(0.26, Math.abs(interest), 0.01);

		balance = bAccount2.getBalance();
		interest = InterestHandler.calculateNegativeInterest(balance, ServerModel.getServerCalendar().getMaximum(Calendar.DATE));
		assertEquals(0.51, Math.abs(interest), 0.01);
		
		balance = bAccount3.getBalance();
		interest = InterestHandler.calculateNegativeInterest(balance, ServerModel.getServerCalendar().getMaximum(Calendar.DATE));
		assertEquals(1.03, Math.abs(interest), 0.01);
	}
	
	@Test
	public void calculateTimeSimulatedPositiveInterestTest() {
		
		// Wait for the interest handler to finish...
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		SavingsAccount savings1 = bAccount1.getSavingsAccount();
		savings1.setBalance(1000);
		savings1.saveToDB();
		
		SavingsAccount savings2 = bAccount2.getSavingsAccount();
		savings2.setBalance(30000);
		savings2.saveToDB();
		
		SavingsAccount savings3 = bAccount3.getSavingsAccount();
		savings3.setBalance(100000);
		savings3.saveToDB();
		
		InterestHandler interestHandler = new InterestHandler();
		interestHandler.newlySimulatedDays = 365;
		interestHandler.interrupt();
		
		// Wait for the interest handler to finish...
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		double balance1 = 0;
		double balance2 = 0;
		double balance3 = 0;
		try {
			balance1 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount1.getIBAN())).getSavingsAccount().getBalance();
			balance2 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount2.getIBAN())).getSavingsAccount().getBalance();
			balance3 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount3.getIBAN())).getSavingsAccount().getBalance();		
		} catch (ObjectDoesNotExistException e) {
			e.printStackTrace();
		}

		assertEquals(1001.50, balance1, 0.1);
		assertEquals(30045.00, balance2, 0.1);
		assertEquals(100200.00, balance3, 0.1);
	}
	
	@Test 
	public void calculateTimeSimulatedNegativeInterestTest() {
		bAccount1.setBalance((float) startValue1);
		bAccount2.setBalance((float) startValue2);
		bAccount3.setBalance((float) startValue3);
		
		cAccount.saveToDB();
		bAccount1.saveToDB();
		bAccount2.saveToDB();
		bAccount3.saveToDB();
		
		InterestHandler interestHandler = new InterestHandler();
		interestHandler.newlySimulatedDays = 365;
		interestHandler.interrupt();
		
		// Wait for the interest handler to finish...
		try {
			Thread.sleep(12000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		double balance1 = 0;
		double balance2 = 0;
		double balance3 = 0;
		try {
			balance1 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount1.getIBAN())).getBalance();
			balance2 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount2.getIBAN())).getBalance();
			balance3 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount3.getIBAN())).getBalance();		
		} catch (ObjectDoesNotExistException e) {
			e.printStackTrace();
		}

		assertEquals(-1100.000, balance1, 0.001);
		assertEquals(-2200.000, balance2, 0.001);
		assertEquals(-4400.000, balance3, 0.001);
	}
	
	@Test
	public void transferTest() {
		bAccount1.setBalance((float) startValue1);
		bAccount2.setBalance((float) startValue2);
		bAccount3.setBalance((float) startValue3);
		
		cAccount.saveToDB();
		bAccount1.saveToDB();
		bAccount2.saveToDB();
		bAccount3.saveToDB();
		
		InterestHandler interestHandler = new InterestHandler();
		
		// Sleep to give interestHandler some time to set up
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		interestHandler.newlySimulatedDays = 32;
		interestHandler.interrupt();
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		double balance1 = 0;
		double balance2 = 0;
		double balance3 = 0;
		try {
			balance1 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount1.getIBAN())).getBalance();
			balance2 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount2.getIBAN())).getBalance();
			balance3 = ((BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAccount3.getIBAN())).getBalance();		
		} catch (ObjectDoesNotExistException e) {
			e.printStackTrace();
		}

		assertEquals(-1007.97412109375, balance1, 0.00001);
		assertEquals(-2015.9482421875, balance2, 0.00001);
		assertEquals(-4031.896484375, balance3, 0.00001);
	}
	
	@Test
	public void addBalancesTest() {		
		int daysUntilNextYear = Calendar.getInstance().getMaximum(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		ServerModel.setSimulatedDays(daysUntilNextYear, true);
		

		bAccount1.setBalance((float) startValue1);
		bAccount2.setBalance((float) startValue2);
		bAccount3.setBalance((float) startValue3);
		
		cAccount.saveToDB();
		bAccount1.saveToDB();
		bAccount2.saveToDB();
		bAccount3.saveToDB();
		
		InterestHandler interestHandler = new InterestHandler();
		interestHandler.newlySimulatedDays = 1;
		interestHandler.interrupt();
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		HashMap<String, Double> totalInterestMap = ServerDataHandler.getTotalNegativeInterestMap();
		double amount1 = totalInterestMap.get(bAccount1.getIBAN());
		double amount2 = totalInterestMap.get(bAccount2.getIBAN());
		double amount3 = totalInterestMap.get(bAccount3.getIBAN());
		
		assertEquals(-0.25723033612903223, amount1, 0.00001);
		assertEquals(-0.5144606722580645, amount2, 0.00001);
		assertEquals(-1.028921344516129, amount3, 0.00001);
	}

	
	@Test 
	public void isTimeToTransferTest() {
		Calendar c = Calendar.getInstance();

		// 23:50, 01/01/2018, didn't store balances balances
		c.setTimeInMillis(1514810700000L);	
		assertFalse(InterestHandler.isTimeToTransferNegativeInterest(c));

		// 15:30, 01/02/2018
		c.setTimeInMillis(1514897100000L);
		InterestHandler.setPreviousBalanceStoringDate(c);
		assertFalse(InterestHandler.isTimeToTransferNegativeInterest(c));
		
		// 23:50, 12/31/2017
		c.setTimeInMillis(1514761140000L);
		InterestHandler.setPreviousBalanceStoringDate(c);
		assertFalse(InterestHandler.isTimeToTransferNegativeInterest(c));
		
		// 23:50, 01/01/2018, stored balances
		c.setTimeInMillis(1514810700000L);
		InterestHandler.setPreviousBalanceStoringDate(c);		
		assertFalse(InterestHandler.isTimeToTransferNegativeInterest(c));		
		
	}
	
	@Test
	public void isTimeToAddBalancesTest() {
		Calendar c = Calendar.getInstance();

		// 15:30, 01/01/2018
		c.setTimeInMillis(1514817180000L);
		assertFalse(InterestHandler.isTimeToAddBalances(c));

		// 23:44, 01/01/2018
		c.setTimeInMillis(1514846640000L);
		assertFalse(InterestHandler.isTimeToAddBalances(c));

		// 23:46, 01/01/2018
		c.setTimeInMillis(1514846760000L);
		assertFalse(InterestHandler.isTimeToAddBalances(c));

		// 0:00, 01/02/2018
		c.setTimeInMillis(1514847600000L);
		assertTrue(InterestHandler.isTimeToAddBalances(c));
	}
	
	@Test
	public void setTotalInterestMapTest() {
		HashMap<String, Double> map = new HashMap<>();
		map.put("test", 0.50);
		InterestHandler.setTotalNegativeInterestMap(map);
		
		HashMap<String, Double> map2 = ServerDataHandler.getTotalNegativeInterestMap();
		assertEquals(0.50, map2.get("test"), 0);
	}
	
	@Test
	public void setLowestDailyReachMapTest() {
		HashMap<String, Double> map = new HashMap<>();
		map.put("test", 0.50);
		InterestHandler.setNegativeLowestDailyReachMap(map);
		
		HashMap<String, Double> map2 = ServerDataHandler.getNegativeLowestDailyReachMap();
		assertEquals(0.50, map2.get("test"), 0);
	}

	@Test
	public void setPreviousBalanceStoringDateTest() {
		Calendar c = Calendar.getInstance();
		InterestHandler.setPreviousBalanceStoringDate(c);
		assertEquals(Long.toString(c.getTimeInMillis()), ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_BALANCE_STORE_LINE));
		
	}
	
	@Test 
	public void setPreviousInterestExecutionDateTest() {
		Calendar c = Calendar.getInstance();
		InterestHandler.setPreviousInterestExecutionDate(c);
		assertEquals(Long.toString(c.getTimeInMillis()), ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_INTEREST_LINE));
		
	}
	
	@After
	public void reset() {	
		// Reset all data
		InterestHandler.reset();
		ServerModel.reset();
		DataManager.wipeAllData();	
	}
}
