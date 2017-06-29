package testing;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import database.DataManager;
import exceptions.ObjectDoesNotExistException;
import server.rest.InterestHandler;
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
		ServerModel.resetSimulatedDays();
		DataManager.wipeAllData(true);	
		
		// Set up necessary data
		startOfYear.setTimeInMillis(1483225200000L);
		startValue1 = -1000;
		startValue2 = -2000;
		startValue3 = -4000;
		
		bAccount1 = cAccount.openBankAccount();
		bAccount2 = cAccount.openBankAccount();
		bAccount3 = cAccount.openBankAccount();
		
		bAccount1.setBalance((float) startValue1);
		bAccount2.setBalance((float) startValue2);
		bAccount3.setBalance((float) startValue3);
		
		cAccount.saveToDB();
		bAccount1.saveToDB();
		bAccount2.saveToDB();
		bAccount3.saveToDB();	
	}
	
	@Test
	public void calculateShortestSleep() {
		long actualMilliDifference;
		long calculatedMilliSleep;
		
		// Milli seconds of difference between 2017-01-01 00:00 and 2017-01-01 23:46
		actualMilliDifference = 1483310760000L - 1483225200000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);

		// Milli seconds of difference between 2017-01-01 13:46 and 2017-01-01 23:46
		startOfYear.setTimeInMillis(1483274580000L);
		actualMilliDifference = 1483310760000L - 1483274580000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);

		// Milli seconds of difference between 2017-01-01 23:43 and 2017-01-01 23:46
		startOfYear.setTimeInMillis(1483310580000L);
		actualMilliDifference = 1483310760000L - 1483310580000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);
		
		// Milli seconds of difference between 2017-01-01 23:44 and 2017-01-02 23:46
		startOfYear.setTimeInMillis(1483310640000L);
		actualMilliDifference = 1483397160000L - 1483310640000L;
		calculatedMilliSleep = InterestHandler.calculateShortestSleep(startOfYear);
		assertEquals(actualMilliDifference, calculatedMilliSleep);		
	}
	
	@Test
	public void calculateInterestTest() {
		double balance, interest;

		balance = bAccount1.getBalance();
		interest = InterestHandler.calculateInterest(balance, 31);		
		assertTrue(Math.abs(interest) == 0.26);

		balance = bAccount2.getBalance();
		interest = InterestHandler.calculateInterest(balance, 31);
		assertTrue(Math.abs(interest) == 0.51);
		
		balance = bAccount3.getBalance();
		interest = InterestHandler.calculateInterest(balance, 31);
		assertTrue(Math.abs(interest) == 1.03);
	}
	
	@Test 
	public void calculateTimeSimulatedInterestTest() {
		int daysUntilNextYear = Calendar.getInstance().getMaximum(Calendar.DAY_OF_YEAR) - Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		ServerModel.setSimulatedDays(daysUntilNextYear + 365, true);
		System.err.println("Servertime: " + ServerModel.getServerCalendar().getTime().toString());
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
		
	}
	
	@Test
	public void addBalancesTest() {
		
	}
	
	@Test 
	public void isTimeToTransferTest() {
		
	}
	
	@Test
	public void isTimeToAddBalancesTest() {
		
	}
	
	@Test
	public void setTotalInterestMapTest() {
		
	}
	
	@Test
	public void setLowestDailyReachMapTest() {
		
	}

	@Test
	public void setPreviousBalanceStoringDateTest() {
		
	}
	
	@Test 
	public void setPreviousInterestExecutionDateTest() {
		
	}
	
	@Test
	public void resetTest() {
		
	}
}
