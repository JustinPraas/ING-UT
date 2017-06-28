package testing;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import database.DataManager;
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
		interest = InterestHandler.calculateInterest(balance);
		assertTrue(Math.abs(interest) - Math.abs(0.273972602) < 0.0001);

		balance = bAccount2.getBalance();
		interest = InterestHandler.calculateInterest(balance);
		assertTrue(Math.abs(interest) - Math.abs(0.54794521) < 0.0001);
		
		balance = bAccount2.getBalance();
		interest = InterestHandler.calculateInterest(balance);
		assertTrue(Math.abs(interest) - Math.abs(1.09589041) < 0.0001);
	}
	
	@Test 
	public void calculateTimeSimulatedInterest() {

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
