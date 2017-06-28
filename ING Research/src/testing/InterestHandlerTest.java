package testing;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;

import database.DataManager;
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
	
	@Before
	public void setup() {
		startOfYear.setTimeInMillis(1483225200000L);
		startValue1 = -1000;
		startValue2 = -2000;
		startValue3 = -4000;
		
		// Reset all data
		InterestHandler.reset();
		ServerModel.resetSimulatedDays();
		DataManager.wipeAllData(true);		
	}
	
	@Test
	public void calculateShortestSleep() {
		
	}
	
	@Test
	public void calculateInterestTest() {

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
