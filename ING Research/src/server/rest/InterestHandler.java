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

/**
 * A class that handles the interest calculation for real time banking AND
 * time simulated banking. 
 * Since it supports real time, it needs to keep act upon time, hence the Thread extension.
 * @author Justin Praas
 */
public class InterestHandler extends Thread {

	/**
	 * Indicates the time when we last deducted all customer's interest from their accounts.
	 */
	private static Calendar previousInterestExecution;
	
	/**
	 * Indicates the time when we last stored the (lowest) balance of all customers into the
	 * <code>totalMonthlyInterestMap</code>.
	 */
	private static Calendar previousBalanceStoring;
	
	/**
	 * The monthly interest rate of this bank.
	 */
	private static final double MONTHLY_INTEREST_RATE = 0.00797414042;
	
	/**
	 * A map that keeps track of the lowest daily balances of accounts.
	 */
	private static HashMap<String, Double> lowestDailyReachMap = new HashMap<>();
	
	/**
	 * A map that keeps track of the total interest of accounts.
	 */
	@SuppressWarnings("unused")
	private static HashMap<String, Double> totalMonthlyInterestMap = new HashMap<>();
	
	/**
	 * A variable that is used for when we simulate time.
	 */
	public int newlySimulatedDays = 0;
	
	public InterestHandler() {		
		intializeData();
		start();		
	}
	
	/**
	 * Initialized the data needed for the interest handler.
	 * If no data can be found in the files (See ServerDataHandler), then all data
	 * is initialized with 'hard coded' values.
	 */
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

	/**
	 * Sets the map of total interest for the interest handler and stores it a file.
	 * @param totalInterestMap the new map of total interest
	 */
	public static void setTotalInterestMap(HashMap<String, Double> totalInterestMap) {
		totalMonthlyInterestMap = totalInterestMap;
		ServerDataHandler.setTotalInterestMap(totalInterestMap);
	}
	
	/**
	 * Sets the map of lowest daily balances for the interest handler and stores it in a file.
	 * @param lowestDailyMap the new map of lowest daily balances
	 */
	public static void setLowestDailyReachMap(HashMap<String, Double> lowestDailyMap) {
		lowestDailyReachMap = lowestDailyMap;
		ServerDataHandler.setLowestDailyReachMap(lowestDailyMap);
	}
	
	/**
	 * Sets the lowest daily balance of a specific bank account and then writes the map
	 * like <code>setLowestDailyReachMap()</code> does.
	 * @param IBAN the IBAN that has a new lower balance
	 * @param balance the bank account's balance
	 */
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
	
	/**
	 * Initialized the map of lowest daily balances (happens at the start of every month).
	 * Simply checks which account has a negative balance and writes it to the file.
	 * Uses <code>setLowestDailyReachMap</code> to write the map to the file.
	 */
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
	
	/**
	 * Sets the previous balance storing date.
	 * @param c the calendar that represents the date
	 */
	public static void setPreviousBalanceStoringDate(Calendar c) {
		previousBalanceStoring = c;
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_BALANCE_STORE_LINE, 
				Long.toString(previousBalanceStoring.getTimeInMillis()));
	}

	/**
	 * Sets the previous interest execution date.
	 * @param c the calendar that represents the date
	 */
	public static void setPreviousInterestExecutionDate(Calendar c) {
		previousInterestExecution = c;
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_INTEREST_LINE, 
				Long.toString(previousInterestExecution.getTimeInMillis()));
	}
	
	/**
	 * Sets the previous balance storing date to the (perhaps simulated) server-time.
	 */
	public static void setPreviousBalanceStoringDate() {
		previousBalanceStoring = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_BALANCE_STORE_LINE, 
				Long.toString(previousBalanceStoring.getTimeInMillis()));
	}
	
	/**
	 * Sets the previous interest execution date to the (perhaps simulated) server-time.
	 */
	public static void setPreviousInterestExecutionDate() {
		previousInterestExecution = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_INTEREST_LINE, 
				Long.toString(previousInterestExecution.getTimeInMillis()));
	}
	
	/**
	 * The Thread that runs as if it constantly watches the time (unless it doesn't need to do
	 * anything). There are two situations:
	 * Situation 1: The server is running real time (or real time ON simulated time)...
	 * 		- What this means is that, when we wait for the clock to strike nearly midnight,
	 * 		  it will calculate the interest or even transfer money if the Server's date is the last
	 * 		  day of the month.
	 * 		  The thread is put to sleep as long as it doesn't need to do anything, see
	 * 		  <code>calculateShortestSleep()</code>.
	 * Situation 2: Time is simulated and this Thread is interrupted.
	 * 		- The <code>newlySimulatedDays</code> is set by the 'interrupter' and the method 
	 * 		  to calculate interest over simulated time is executed. Afterwards continues with the 
	 * 		  real time checking (situation 1).
	 */
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
	
	/**
	 * Calculates how long the InterestHandler thread needs to sleep until it either needs to
	 * transfer and/or add balances to the total interest map.
	 * @param c the calendar representation of the server's time
	 * @return the milliseconds of time that the InterestHandler needs to sleep.
	 */
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

	/**
	 * Calculates the interest for a balance for one day in a specific month.
	 * @param balance the balance on which interest needs to be calculated
	 * @param maxDateOfMonth the date of the last day of the month (e.g. 30, 28, 31, 29)
	 * @return the interest on the given balance
	 */
	public static double calculateInterest(double balance, int maxDateOfMonth) {
		return balance * MONTHLY_INTEREST_RATE / maxDateOfMonth; 
	}
	
	/**
	 * Accurately rounds a double to the given number of decimals. Used for transferring money from
	 * the bank accounts.
	 * SOURCE: https://stackoverflow.com/a/2808648/7133329
	 * @param value the value that needs to be rounded to the given number of decimals
	 * @param places the number of decimals 
	 * @return the same value representation with the precision of the given number of decimals
	 */
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	/**
	 * Calculates the interest on all bank accounts for the given number of days.
	 * @param days the number of days on which all bank accounts need to be simulated
	 */
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

	/**
	 * Transfers the interest values of all bank accounts that are in the map of total interest 
	 * to the ING bank account.
	 */
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

	/**
	 * Adds the lowest daily balances of all bank accounts that have a negative balance to the
	 * total interest map.
	 * @param c the calendar that is used to set the previousBalanceStoring variable.
	 */
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

	/**
	 * Checks if it is time to add balances to the total interest map.
	 * (Check skipped if we simulate time)
	 * @param c the Calendar that is used to check if it is the time to add balances
	 * @return true if it is the time, false otherwise
	 */
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

	/**
	 * Checks if it is time to deduct the interest from the total interest map to the ING account.
	 * (Check skipped if we simulate time)
	 * @param c the Calendar that is used to check if it is the time to transfer the money
	 * @return true if it is the time, false otherwise
	 */
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
	
	/**
	 * Resets the system to the initial state.
	 */
	public static void reset() {
		setTotalInterestMap(new HashMap<String, Double>());
		setLowestDailyReachMap(new HashMap<String, Double>());
		setPreviousBalanceStoringDate();
		setPreviousInterestExecutionDate();
	}

}
