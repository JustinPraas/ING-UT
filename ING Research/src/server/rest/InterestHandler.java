package server.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import accounts.BankAccount;
import database.DataManager;
import database.SQLiteDB;
import exceptions.ClosedAccountTransferException;
import exceptions.ExceedOverdraftLimitException;
import exceptions.IllegalAmountException;
import exceptions.InsufficientFundsTransferException;
import exceptions.ObjectDoesNotExistException;
import exceptions.SameAccountTransferException;

public class InterestHandler extends Thread {

	private static Calendar previousInterestExecution;
	private static Calendar previousBalanceStoring;
	
	private static final double MONTHLY_INTEREST_RATE = 0.00797414042;
	
	// Interest map for IBAN -> interest
	private static HashMap<String, Double> lowestDailyReachMap = new HashMap<>();
	private static HashMap<String, Double> totalMonthlyInterestMap = new HashMap<>();
	public int newlySimulatedDays = 0;
	
	public InterestHandler() {		
		intializeData();
		start();		
	}
	
	public static void intializeData() {
		// Set the previousInterestExecution
		long previousInterestExecutionMillis = 0;

		String prevIntrstExecMillisString = ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_INTEREST_LINE);
		if (prevIntrstExecMillisString.equals("") || prevIntrstExecMillisString == null || prevIntrstExecMillisString.equals("0")) {
			setPreviousInterestExecutionDate();
		} else {
			previousInterestExecutionMillis = Long.parseLong(prevIntrstExecMillisString);
			previousInterestExecution = Calendar.getInstance();
			previousInterestExecution.setTimeInMillis(previousInterestExecutionMillis);
		}		

		// Set the previousBalanceExecution
		long previousBalanceStoringMillis = 0;

		String prevBalanceStoringMillisString = ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_BALANCE_STORE_LINE);
		if (prevBalanceStoringMillisString.equals("") || prevBalanceStoringMillisString == null || prevBalanceStoringMillisString.equals("0")) {
			setPreviousBalanceStoringDate();
		} else {
			previousBalanceStoringMillis = Long.parseLong(prevBalanceStoringMillisString);
			previousBalanceStoring = Calendar.getInstance();
			previousBalanceStoring.setTimeInMillis(previousBalanceStoringMillis);
		}	
		
		// Set the lowestDailyReachMap
		lowestDailyReachMap = ServerDataHandler.getLowestDailyReachMap();
		if (lowestDailyReachMap.size() == 0) {
			initializeLowestDailyReachMap();
		}
		
		// Set the totalMonthlyInterestMap 
		totalMonthlyInterestMap = ServerDataHandler.getTotalInterestMap();
		
	}

	public static void setTotalInterestMap(HashMap<String, Double> totalInterestMap) {
		totalMonthlyInterestMap = totalInterestMap;
		ServerDataHandler.setTotalInterestMap(totalInterestMap);
	}
	
	public static void setLowestDailyReachMap(HashMap<String, Double> lowestDailyMap) {
		lowestDailyReachMap = lowestDailyMap;
		ServerDataHandler.setLowestDailyReachMap(lowestDailyMap);
	}
	
	public static void setLowestDailyReachMapEntry(String IBAN, double balance) {
		// FETCH: map
		HashMap<String, Double> currentLowestDailyReachMap = ServerDataHandler.getLowestDailyReachMap();
		Double currentLowestBalance = currentLowestDailyReachMap.get(IBAN);
		
		if (balance < 0) {
			if (currentLowestBalance == null || currentLowestBalance > balance) {
				HashMap<String, Double> map = ServerDataHandler.getLowestDailyReachMap();
				map.put(IBAN, balance);
				
				// SET: map
				setLowestDailyReachMap(currentLowestDailyReachMap);
			}
		}		
	}
	
	public static void initializeLowestDailyReachMap() {
		HashMap<String, Double> newLowestDailyReachMap = new HashMap<>();
		try {
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM bankaccounts WHERE balance < 0;");
			while (rs.next()) {
				newLowestDailyReachMap.put(rs.getString("IBAN"), rs.getDouble("balance"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// SET: map
		setLowestDailyReachMap(newLowestDailyReachMap);
	}
	
	public static void setPreviousBalanceStoringDate(Calendar c) {
		previousBalanceStoring = c;
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_BALANCE_STORE_LINE, 
				Long.toString(previousBalanceStoring.getTimeInMillis()));
	}

	public static void setPreviousInterestExecutionDate(Calendar c) {
		previousInterestExecution = c;
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_INTEREST_LINE, 
				Long.toString(previousInterestExecution.getTimeInMillis()));
	}
	
	public static void setPreviousBalanceStoringDate() {
		previousBalanceStoring = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_BALANCE_STORE_LINE, 
				Long.toString(previousBalanceStoring.getTimeInMillis()));
	}
	
	public static void setPreviousInterestExecutionDate() {
		previousInterestExecution = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_INTEREST_LINE, 
				Long.toString(previousInterestExecution.getTimeInMillis()));
	}
	
	@Override
	public void run() {		
		boolean continues = true;
		while (continues) {
			System.out.println("INTEREST: Re-entering loop");
			// The server's current time and date
			Calendar c = ServerModel.getServerCalendar();
			
			// If it's the time to add the daily lowest reaches to the total interest AND transfer
			// the interest to the ING account:
			if (isTimeToTransfer(c)) {
				System.out.println("INTEREST: Time to transfer interest");
				addBalancesToTotalInterest(c);
				transferInterest();
			} else if (isTimeToAddBalances(c)) {
				System.out.println("INTEREST: Time to add balances");
				addBalancesToTotalInterest(c);				
			}
			
			try {
				Thread.sleep(calculateShortestSleep(c));
			} catch (InterruptedException e) {
				System.out.println("INTEREST: interrupted");
				calculateTimeSimulatedInterest(newlySimulatedDays);
				newlySimulatedDays = 0;
			}					
		}		
	}
	
	public static long calculateShortestSleep(Calendar c) {
		Calendar now = c;
		long nowMillis = now.getTimeInMillis();
		
		// Time until next midnight
		Calendar nextMidnight = c;
		
		if (now.get(Calendar.HOUR_OF_DAY) >= 23 && now.get(Calendar.MINUTE) >= 44) {
			nextMidnight.add(Calendar.DATE, 1);
		}
		
		nextMidnight.set(Calendar.HOUR_OF_DAY, 23);
		nextMidnight.set(Calendar.MINUTE, 46);
		Date date = nextMidnight.getTime();
		long dateMillis = date.getTime();
		long millisUntilNextMidNight = dateMillis - nowMillis;
		
		//System.out.println("INTEREST: Going to sleep for " + millisUntilNextMidNight + "milliseconds (" + date.toString() + ")");

		return millisUntilNextMidNight;
	}

	public static double calculateInterest(double balance, int maxDateOfMonth) {
		double interest = balance * MONTHLY_INTEREST_RATE / maxDateOfMonth; 
		System.out.println("Interest: " + interest);
		return interest;
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static void calculateTimeSimulatedInterest(int days) {
		Calendar c = ServerModel.getServerCalendar();
		
		for (int i = 1; i <= days; i++) {
			System.out.println("========= " + c.getTime().toString() + " ==================================");
			
			// Add balances
			addBalancesToTotalInterest(c);
			
			// Transfer the money
			if (c.get(Calendar.DATE) == c.getActualMaximum(Calendar.DATE)) {
				transferInterest();
			}
			
			// Add a day to the calendar
			c.add(Calendar.DATE, 1);
		}		
	}

	public static void transferInterest() {
		// FETCH: map
		HashMap<String, Double> currentTotalMonthlyInterestMap = ServerDataHandler.getTotalInterestMap();
		
		//System.out.println("INTEREST: transfering interest from " + currentTotalMonthlyInterestMap.size() + " customers");
		for (Entry<String, Double> entry : currentTotalMonthlyInterestMap.entrySet()) {
			BankAccount bankAccount;
			try {
				// Transfer the money to the ING account
				double rounded = round(entry.getValue(), 2);
				System.out.println(entry.getKey() + ": transfering " + rounded);
				bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, entry.getKey());
				double currentBalance = bankAccount.getBalance();
				bankAccount.transfer(BankAccount.ING_BANK_ACCOUNT_IBAN, -1 * entry.getValue(), "Negative interest credit");
				double newBalance = bankAccount.getBalance();
				
				System.out.println(bankAccount.getIBAN() + ": old balance is " + currentBalance + ", new balance is " + newBalance);
				
			} catch (ObjectDoesNotExistException | InsufficientFundsTransferException | ClosedAccountTransferException | SameAccountTransferException 
					| IllegalAmountException | ExceedOverdraftLimitException e) {
				e.printStackTrace();
			}
		}
		
		currentTotalMonthlyInterestMap = new HashMap<>();
		
		// SET: map
		setTotalInterestMap(currentTotalMonthlyInterestMap);
		initializeLowestDailyReachMap();
	}

	public static void addBalancesToTotalInterest(Calendar c) {
		// FETCH: maps
		HashMap<String, Double> currentLowestDailyReachMap = ServerDataHandler.getLowestDailyReachMap();
		HashMap<String, Double> currentTotalInterestMap = ServerDataHandler.getTotalInterestMap();
		
		//System.out.println("INTEREST: adding balances for " + currentLowestDailyReachMap.size() + " customers");
		// For all IBAN entries, add the interest to the total interest map 
		for (Entry<String, Double> entry : currentLowestDailyReachMap.entrySet()) {
			String IBAN = entry.getKey();
			double currentInterest;
			double totalInterest; 
			if (!currentTotalInterestMap.containsKey(IBAN)) {
				currentInterest = 0;
				totalInterest = calculateInterest(entry.getValue(), c.getActualMaximum(Calendar.DATE));
			} else {
				currentInterest = currentTotalInterestMap.get(IBAN);
				totalInterest = currentInterest + calculateInterest(entry.getValue(), c.getActualMaximum(Calendar.DATE));
			}
			
			System.out.println(IBAN + ": daily low: " + entry.getValue());
			System.out.println(IBAN + ": current total interest " + currentInterest + ", new total interest" + totalInterest);
			
			currentTotalInterestMap.put(IBAN, totalInterest);
		}
		
		// SET: maps
		setTotalInterestMap(currentTotalInterestMap);
		
		// Update the daily lowest reach map to the values that the customers 
		// currently have on the account (monthly reset)
		initializeLowestDailyReachMap();		
	}

	public static boolean isTimeToAddBalances(Calendar c) {
		// Check if it's between 11:45 PM and 11:59 AM (23:45-23:59)
		int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		boolean isCorrectTime = hourOfDay > 11 && minute >= 45 && minute <= 59;
		
		// Check if we haven't added the balances today already
		Calendar lastBalanceStore = previousBalanceStoring;
		boolean didBalanceStoreToday = c.get(Calendar.DATE) == lastBalanceStore.get(Calendar.DATE) &&
				c.get(Calendar.MONTH) == lastBalanceStore.get(Calendar.MONTH) &&
				c.get(Calendar.YEAR) == lastBalanceStore.get(Calendar.YEAR);
						
		return isCorrectTime && !didBalanceStoreToday;
	}

	public static boolean isTimeToTransfer(Calendar c) {
		// Check if it is the last of the month
		boolean lastOfMonth = c.get(Calendar.DATE) == c.getMaximum(Calendar.DATE);
		
		// Check if we haven't transfered this month 
		Calendar lastTransfer = previousInterestExecution;
		boolean didTransferThisMonth = c.get(Calendar.MONTH) == lastTransfer.get(Calendar.MONTH) &&
				c.get(Calendar.YEAR) == lastTransfer.get(Calendar.YEAR);
		
		// Check if we've already added the daily lowest balances to the lowestDailyReach map
		boolean storedBalances = previousBalanceStoring.get(Calendar.DATE) == c.getMaximum(Calendar.DATE);
		
		return lastOfMonth && !didTransferThisMonth && storedBalances;
	}
	
	public static void reset() {
		setTotalInterestMap(new HashMap<String, Double>());
		setLowestDailyReachMap(new HashMap<String, Double>());
		setPreviousBalanceStoringDate();
		setPreviousInterestExecutionDate();
	}

}
