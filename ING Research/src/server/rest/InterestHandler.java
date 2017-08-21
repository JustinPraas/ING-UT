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
import exceptions.ExceedLimitException;
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
	private static Calendar previousNegativeInterestExecution;
	
	private static Calendar previousPositiveInterestExecution;
	
	/**
	 * Indicates the time when we last stored the (lowest) balance of all customers into the
	 * <code>totalMonthlyInterestMap</code>.
	 */
	private static Calendar previousNegativeBalanceStoring;
	
	private static Calendar previousPositiveBalanceStoring;
	
	/**
	 * The monthly interest rate of this bank.
	 */
	private static final double MONTHLY_INTEREST_RATE = 
			Math.pow((1 + BankSystemValue.OVERDRAFT_INTEREST_RATE.getAmount()), (1 / 12)) - 1;

	private static final double DAILY_INTEREST_RATE_RANGE_1 =
			BankSystemValue.INTEREST_RATE_1.getAmount() / 365;
	
	private static final double DAILY_INTEREST_RATE_RANGE_2 = 
			BankSystemValue.INTEREST_RATE_2.getAmount() / 365;
	
	private static final double DAILY_INTEREST_RATE_RANGE_3 = 
			BankSystemValue.INTEREST_RATE_3.getAmount() / 365;
	
	private static final double CHILD_INTEREST_RATE = 
			BankSystemValue.CHILD_INTEREST_RATE.getAmount() / 365;
	
	/**
	 * A map that keeps track of the lowest daily balances of accounts.
	 */
	private static HashMap<String, Double> lowestNegativeDailyReachMap = new HashMap<>();
	
	private static HashMap<String, Double> lowestPositiveDailyReachMap = new HashMap<>();

	/**
	 * A map that keeps track of the total interest of accounts.
	 */
	@SuppressWarnings("unused")
	private static HashMap<String, Double> totalMonthlyInterestMap = new HashMap<>();
	
	@SuppressWarnings("unused")
	private static HashMap<String, Double> totalYearlyPositiveInterestMap = new HashMap<>();

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
		// Set the previousNegativeInterestExecution
		long previousNegativeInterestExecutionMillis = 0;

		String prevNegativeIntrstExecMillisString = ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_INTEREST_LINE);
		if (prevNegativeIntrstExecMillisString.equals("") || prevNegativeIntrstExecMillisString == null || prevNegativeIntrstExecMillisString.equals("0")) {
			setPreviousNegativeInterestExecutionDate();
		} else {
			previousNegativeInterestExecutionMillis = Long.parseLong(prevNegativeIntrstExecMillisString);
			previousNegativeInterestExecution = Calendar.getInstance();
			previousNegativeInterestExecution.setTimeInMillis(previousNegativeInterestExecutionMillis);
		}		

		// Set the previousNegativeBalanceExecution
		long previousNegativeBalanceStoringMillis = 0;

		String prevNegativeBalanceStoringMillisString = ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_BALANCE_STORE_LINE);
		if (prevNegativeBalanceStoringMillisString.equals("") || prevNegativeBalanceStoringMillisString == null || prevNegativeBalanceStoringMillisString.equals("0")) {
			setPreviousNegativeBalanceStoringDate();
		} else {
			previousNegativeBalanceStoringMillis = Long.parseLong(prevNegativeBalanceStoringMillisString);
			previousNegativeBalanceStoring = Calendar.getInstance();
			previousNegativeBalanceStoring.setTimeInMillis(previousNegativeBalanceStoringMillis);
		}
		
		// Set the previousPositiveInterestExecution
		long previousPositiveInterestExecutionMillis = 0;

		String prevPositiveIntrstExecMillisString = ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_INTEREST_LINE);
		if (prevPositiveIntrstExecMillisString.equals("") || prevPositiveIntrstExecMillisString == null || prevPositiveIntrstExecMillisString.equals("0")) {
			setPreviousPositiveInterestExecutionDate();
		} else {
			previousPositiveInterestExecutionMillis = Long.parseLong(prevPositiveIntrstExecMillisString);
			previousPositiveInterestExecution = Calendar.getInstance();
			previousPositiveInterestExecution.setTimeInMillis(previousPositiveInterestExecutionMillis);
		}		

		// Set the previousPositiveBalanceExecution
		long previousPositiveBalanceStoringMillis = 0;

		String prevPositiveBalanceStoringMillisString = ServerDataHandler.getServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_BALANCE_STORE_LINE);
		if (prevPositiveBalanceStoringMillisString.equals("") || prevPositiveBalanceStoringMillisString == null || prevPositiveBalanceStoringMillisString.equals("0")) {
			setPreviousPositiveBalanceStoringDate();
		} else {
			previousPositiveBalanceStoringMillis = Long.parseLong(prevPositiveBalanceStoringMillisString);
			previousPositiveBalanceStoring = Calendar.getInstance();
			previousPositiveBalanceStoring.setTimeInMillis(previousPositiveBalanceStoringMillis);
		}
		
		// Set the lowestNegativeDailyReachMap
		lowestNegativeDailyReachMap = ServerDataHandler.getNegativeLowestDailyReachMap();
		if (lowestNegativeDailyReachMap.size() == 0) {
			initializeLowestNegativeDailyReachMap();
		}
		
		// Set the totalMonthlyInterestMap 
		totalMonthlyInterestMap = ServerDataHandler.getTotalNegativeInterestMap();	

		// Set the lowestPositiveDailyReachMap
		lowestPositiveDailyReachMap = ServerDataHandler.getPositiveLowestDailyReachMap();
		if (lowestPositiveDailyReachMap.size() == 0) {
			initializeLowestPositiveDailyReachMap();
		}
		
		// Set the totalYearlyInterestMap 
		totalYearlyPositiveInterestMap = ServerDataHandler.getTotalPositiveInterestMap();
		
	}

	/**
	 * Sets the map of total interest for the interest handler and stores it a file.
	 * @param totalInterestMap the new map of total interest
	 */
	public static void setTotalNegativeInterestMap(HashMap<String, Double> totalInterestMap) {
		totalMonthlyInterestMap = totalInterestMap;
		ServerDataHandler.setTotalNegativeInterestMap(totalInterestMap);
	}
	
	public static void setTotalPositiveInterestMap(HashMap<String, Double> totalInterstMap) {
		totalYearlyPositiveInterestMap = totalInterstMap;
		ServerDataHandler.setTotalPositiveInterestMap(totalInterstMap);
	}

	/**
	 * Sets the map of lowest daily balances for the interest handler and stores it in a file.
	 * @param lowestNegativeDailyMap the new map of lowest daily balances
	 */
	public static void setNegativeLowestDailyReachMap(HashMap<String, Double> lowestNegativeDailyMap) {
		lowestNegativeDailyReachMap = lowestNegativeDailyMap;
		ServerDataHandler.setNegativeLowestDailyReachMap(lowestNegativeDailyMap);
	}
	
	public static void setPositiveLowestDailyReachMap(HashMap<String, Double> lowestPositiveDailyMap) {
		lowestPositiveDailyReachMap = lowestPositiveDailyMap;
		ServerDataHandler.setPositiveLowestDailyReachMap(lowestPositiveDailyReachMap);
	}

	/**
	 * Sets the lowest daily balance of a specific bank account and then writes the map
	 * like <code>setLowestDailyReachMap()</code> does.
	 * @param IBAN the IBAN that has a new lower balance
	 * @param balance the bank account's balance
	 */
	public static void setLowestNegativeDailyReachMapEntry(String IBAN, double balance) {
		// FETCH: map
		HashMap<String, Double> currentLowestDailyReachMap = ServerDataHandler.getNegativeLowestDailyReachMap();
		Double currentLowestBalance = currentLowestDailyReachMap.get(IBAN);
		
		if (balance < 0) {
			if (currentLowestBalance == null || currentLowestBalance > balance) {
				HashMap<String, Double> map = ServerDataHandler.getNegativeLowestDailyReachMap();
				map.put(IBAN, balance);
				
				// SET: map
				setNegativeLowestDailyReachMap(currentLowestDailyReachMap);
			}
		}		
	}
	
	public static void setLowestPositiveDailyReachMapEntry(String IBAN, double balance) {
		// FETCH: map
		HashMap<String, Double> currentLowestPositiveDailyReachMap = ServerDataHandler.getPositiveLowestDailyReachMap();
		Double currentLowestBalance = currentLowestPositiveDailyReachMap.get(IBAN);
		
		if (balance < 0) {
			if (currentLowestBalance == null || currentLowestBalance > balance) {
				HashMap<String, Double> map = ServerDataHandler.getPositiveLowestDailyReachMap();
				map.put(IBAN, balance);
				
				// SET: map
				setPositiveLowestDailyReachMap(currentLowestPositiveDailyReachMap);
			}
		}	
	}

	/**
	 * Initialized the map of lowest daily balances (happens at the start of every month).
	 * Simply checks which account has a negative balance and writes it to the file.
	 * Uses <code>setLowestDailyReachMap</code> to write the map to the file.
	 */
	public static void initializeLowestNegativeDailyReachMap() {
		HashMap<String, Double> newLowestDailyReachMap = new HashMap<>();
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM bankaccounts WHERE balance < 0;");
			while (rs.next()) {
				newLowestDailyReachMap.put(rs.getString("IBAN"), rs.getDouble("balance"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
		
		// SET: map
		setNegativeLowestDailyReachMap(newLowestDailyReachMap);
	}
	
	/**
	 * Initialized the map of lowest daily balances (happens at the start of every month).
	 * Simply checks which account has a negative balance and writes it to the file.
	 * Uses <code>setLowestDailyReachMap</code> to write the map to the file.
	 */
	public static void initializeLowestPositiveDailyReachMap() {
		HashMap<String, Double> newLowestPositiveDailyReachMap = new HashMap<>();
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs1 = s.executeQuery("SELECT IBAN, balance FROM savingsaccounts WHERE balance > 0;");
			while (rs1.next()) {
				newLowestPositiveDailyReachMap.put(rs1.getString("IBAN"), rs1.getDouble("balance"));
			}
			
			// Child bank accounts should have their interest calculated over their normal bank accounts
			ResultSet rs2 = s.executeQuery("SELECT IBAN, balance FROM bankaccounts WHERE accounttype = 'child' AND balance > 0;");
			while (rs2.next()) {
				newLowestPositiveDailyReachMap.put(rs2.getString("IBAN"), rs2.getDouble("balance"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
		
		// SET: map
		setPositiveLowestDailyReachMap(newLowestPositiveDailyReachMap);
	}
	
	/**
	 * Sets the previous balance storing date.
	 * @param c the calendar that represents the date
	 */
	public static void setPreviousBalanceStoringDate(Calendar c) {
		previousNegativeBalanceStoring = c;
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_BALANCE_STORE_LINE, 
				Long.toString(previousNegativeBalanceStoring.getTimeInMillis()));
	}

	/**
	 * Sets the previous interest execution date.
	 * @param c the calendar that represents the date
	 */
	public static void setPreviousInterestExecutionDate(Calendar c) {
		previousNegativeInterestExecution = c;
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_INTEREST_LINE, 
				Long.toString(previousNegativeInterestExecution.getTimeInMillis()));
	}
	
	/**
	 * Sets the previous balance storing date to the (perhaps simulated) server-time.
	 */
	public static void setPreviousNegativeBalanceStoringDate() {
		previousNegativeBalanceStoring = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_BALANCE_STORE_LINE, 
				Long.toString(previousNegativeBalanceStoring.getTimeInMillis()));
	}
	
	/**
	 * Sets the previous interest execution date to the (perhaps simulated) server-time.
	 */
	public static void setPreviousNegativeInterestExecutionDate() {
		previousNegativeInterestExecution = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_NEGATIVE_INTEREST_LINE, 
				Long.toString(previousNegativeInterestExecution.getTimeInMillis()));
	}
	
	public static void setPreviousPositiveBalanceStoringDate() {
		previousPositiveBalanceStoring = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_POSITIVE_BALANCE_STORE_LINE, 
				Long.toString(previousPositiveBalanceStoring.getTimeInMillis()));
		
	}

	public static void setPreviousPositiveInterestExecutionDate() {
		previousPositiveInterestExecution = ServerModel.getServerCalendar();
		ServerDataHandler.setServerPropertyValue(ServerDataHandler.PREVIOUS_POSITIVE_INTEREST_LINE, 
				Long.toString(previousPositiveInterestExecution.getTimeInMillis()));
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
		while (true) {			
			SQLiteDB.connectionLock.lock();	
			
			// The server's current time and date
			Calendar c = ServerModel.getServerCalendar();
			
			// If it's the time to add the daily lowest reaches to the total interest AND transfer
			// the interest to the ING account:
			if (isTimeToTransferPositiveInterest(c)) {
				System.out.println("is time to transfer positive interest");
				// Do everything
				addNegativeBalancesToTotalNegativeInterest(c);
				addPositiveBalancesToTotalPositiveInterest(c);
				transferNegativeInterest();
				transferPositiveInterest();
			} else if (isTimeToTransferNegativeInterest(c)) {
				System.out.println("INTEREST: Time to transfer interest");
				addNegativeBalancesToTotalNegativeInterest(c);
				transferNegativeInterest();
			} else if (isTimeToAddBalances(c)) {
				System.out.println("INTEREST: Time to add balances");
				addNegativeBalancesToTotalNegativeInterest(c);		
				addPositiveBalancesToTotalPositiveInterest(c);				
			}
			
			SQLiteDB.connectionLock.unlock();	
			try {
				// Sleep for an hour
				Thread.sleep(1000 * 3600);
			} catch (InterruptedException e) {
				
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
		
		if (now.get(Calendar.HOUR_OF_DAY) >= 0 && now.get(Calendar.MINUTE) >= 1) {
			nextMidnight.add(Calendar.DATE, 1);
		}
		
		nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
		nextMidnight.set(Calendar.MINUTE, 0);
		Date date = nextMidnight.getTime();
		long dateMillis = date.getTime();
		long millisUntilNextMidNight = dateMillis - nowMillis;
		
		return millisUntilNextMidNight;
	}

	/**
	 * Calculates the interest for a balance for one day in a specific month.
	 * @param balance the balance on which interest needs to be calculated
	 * @param maxDateOfMonth the date of the last day of the month (e.g. 30, 28, 31, 29)
	 * @return the interest on the given balance
	 */
	public static double calculateNegativeInterest(double balance, int maxDateOfMonth) {
		return balance * MONTHLY_INTEREST_RATE / maxDateOfMonth; 
	}
	
	public static double calculatePositiveInterest(double balance, boolean child) {
		if (child) {
			if (balance < 2500) {
				return balance * CHILD_INTEREST_RATE;
			} else {
				return 2500 * CHILD_INTEREST_RATE;
			}			
		} else {
			if (balance < 25000) {
				return balance * DAILY_INTEREST_RATE_RANGE_1;
			} else if (balance >= 2500 && balance < 75000) {
				return balance * DAILY_INTEREST_RATE_RANGE_2;
			} else if (balance >= 75000 && balance < 1000000) {
				return balance * DAILY_INTEREST_RATE_RANGE_3;
			} else {
				return 0;
			}
		}		
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
	
	public static void handleInterest(Calendar serverCalendar) {
		initializeLowestNegativeDailyReachMap();
		initializeLowestPositiveDailyReachMap();		
		
		// Add balances
		addNegativeBalancesToTotalNegativeInterest(serverCalendar);
		addPositiveBalancesToTotalPositiveInterest(serverCalendar);			
		
		// Transfer the money
		if (serverCalendar.get(Calendar.DATE) == 1) {
			transferNegativeInterest();
		}
		
		if (serverCalendar.get(Calendar.DAY_OF_YEAR) == 1) {
			transferPositiveInterest();
		}
		
	}
	
	/**
	 * Calculates the interest on all bank accounts for the given number of days.
	 * @param days the number of days on which all bank accounts need to be simulated
	 */
	public static void calculateTimeSimulatedInterest(int days) {
		Calendar c = ServerModel.getServerCalendar();
		
		SQLiteDB.connectionLock.lock();	
		
		initializeLowestNegativeDailyReachMap();
		initializeLowestPositiveDailyReachMap();
		
		for (int i = 1; i <= days; i++) {				
			
			// Add balances
			addNegativeBalancesToTotalNegativeInterest(c);
			addPositiveBalancesToTotalPositiveInterest(c);	
			
			// Add a day to the calendar
			c.add(Calendar.DATE, 1);
			
			
			// Transfer the money
			if (c.get(Calendar.DATE) == 1) {
				transferNegativeInterest();
			}
			
			if (c.get(Calendar.DAY_OF_YEAR) == 1) {
				transferPositiveInterest();
			}
		}
		
		SQLiteDB.connectionLock.unlock();			
	}

	/**
	 * Transfers the interest values of all bank accounts that are in the map of total interest 
	 * to the ING bank account.
	 */
	public static void transferNegativeInterest() {
		// FETCH: map
		HashMap<String, Double> currentTotalMonthlyInterestMap = ServerDataHandler.getTotalNegativeInterestMap();
		
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
					| IllegalAmountException | ExceedLimitException e) {
				e.printStackTrace();
			}
		}
		
		currentTotalMonthlyInterestMap = new HashMap<>();
		
		// SET: map
		setTotalNegativeInterestMap(currentTotalMonthlyInterestMap);
		initializeLowestNegativeDailyReachMap();
	}

	private static void transferPositiveInterest() {
		// FETCH: map
		HashMap<String, Double> currentTotalYearlyInterestMap = ServerDataHandler.getTotalPositiveInterestMap();
		
		//System.out.println("INTEREST: transfering interest from " + currentTotalMonthlyInterestMap.size() + " customers");
		for (Entry<String, Double> entry : currentTotalYearlyInterestMap.entrySet()) {
			BankAccount bankAccount;
			BankAccount ingBankAccount;
			try {
				// Transfer the money to the ING account
				double rounded = round(entry.getValue(), 2);
				bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, entry.getKey());
				ingBankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, BankAccount.ING_BANK_ACCOUNT_IBAN);
				System.out.println("Transfering " + rounded + " to " + bankAccount.getIBAN());
				ingBankAccount.transfer(bankAccount.getIBAN(), entry.getValue());
			} catch (ObjectDoesNotExistException e) {
				e.printStackTrace();
			}
		}
		
		currentTotalYearlyInterestMap = new HashMap<>();
		
		// SET: map
		setTotalPositiveInterestMap(currentTotalYearlyInterestMap);
		initializeLowestPositiveDailyReachMap();
	}

	/**
	 * Adds the lowest daily balances of all bank accounts that have a negative balance to the
	 * total interest map.
	 * @param c the calendar that is used to set the previousBalanceStoring variable.
	 */
	public static void addNegativeBalancesToTotalNegativeInterest(Calendar c) {
		// FETCH: maps
		HashMap<String, Double> currentLowestDailyReachMap = ServerDataHandler.getNegativeLowestDailyReachMap();
		HashMap<String, Double> currentTotalInterestMap = ServerDataHandler.getTotalNegativeInterestMap();
		
		//System.out.println("INTEREST: adding balances for " + currentLowestDailyReachMap.size() + " customers");
		// For all IBAN entries, add the interest to the total interest map 
		for (Entry<String, Double> entry : currentLowestDailyReachMap.entrySet()) {
			String IBAN = entry.getKey();
			double currentInterest;
			double totalInterest; 
			if (!currentTotalInterestMap.containsKey(IBAN)) {
				currentInterest = 0;
				totalInterest = calculateNegativeInterest(entry.getValue(), c.getActualMaximum(Calendar.DATE));
			} else {
				currentInterest = currentTotalInterestMap.get(IBAN);
				totalInterest = currentInterest + calculateNegativeInterest(entry.getValue(), c.getActualMaximum(Calendar.DATE));
			}
			
			System.out.println(IBAN + ": daily low: " + entry.getValue());
			System.out.println(IBAN + ": current total interest " + currentInterest + ", new total interest" + totalInterest);
			
			currentTotalInterestMap.put(IBAN, totalInterest);
		}
		
		// SET: maps
		setTotalNegativeInterestMap(currentTotalInterestMap);
		
		// Update the daily lowest reach map to the values that the customers 
		// currently have on the account (monthly reset)
		initializeLowestNegativeDailyReachMap();		
	}

	private static void addPositiveBalancesToTotalPositiveInterest(Calendar c) {
		// FETCH: maps
		HashMap<String, Double> currentLowestPositiveDailyReachMap = ServerDataHandler.getPositiveLowestDailyReachMap();
		HashMap<String, Double> currentTotalPositiveInterestMap = ServerDataHandler.getTotalPositiveInterestMap();
		
		//System.out.println("INTEREST: adding balances for " + currentLowestDailyReachMap.size() + " customers");
		// For all IBAN entries, add the interest to the total interest map 
		for (Entry<String, Double> entry : currentLowestPositiveDailyReachMap.entrySet()) {
			String IBAN = entry.getKey();
			BankAccount b = null;
			try {
				b = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
			} catch (ObjectDoesNotExistException e) {
				e.printStackTrace();
			}
			boolean isChild = b.getAccountType().equals("child");
			double currentInterest;
			double totalInterest; 
			if (!currentTotalPositiveInterestMap.containsKey(IBAN)) {
				currentInterest = 0;
				totalInterest = calculatePositiveInterest(entry.getValue(), isChild);
			} else {
				currentInterest = currentTotalPositiveInterestMap.get(IBAN);
				totalInterest = currentInterest + calculatePositiveInterest(entry.getValue(), isChild);
			}
			System.out.println("[INFO] Current total positive interest for " + IBAN + " is " + totalInterest);
			currentTotalPositiveInterestMap.put(IBAN, totalInterest);
		}
		
		// SET: maps
		setTotalPositiveInterestMap(currentTotalPositiveInterestMap);
		
		// Update the daily lowest reach map to the values that the customers 
		// currently have on the account (monthly reset)
		initializeLowestNegativeDailyReachMap();
	}

	/**
	 * Checks if it is time to add balances to the total interest map.
	 * (Check skipped if we simulate time)
	 * @param c the Calendar that is used to check if it is the time to add balances
	 * @return true if it is the time, false otherwise
	 */
	public static boolean isTimeToAddBalances(Calendar c) {
		// Check if it's between 12:00 AM and 12:15 AM (midnight)
		int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		boolean isCorrectTime = hourOfDay == 0 && minute >= 0 && minute < 1;
		
		// Check if we haven't added the balances today already
		Calendar lastBalanceStore = previousNegativeBalanceStoring;
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
	public static boolean isTimeToTransferNegativeInterest(Calendar c) {
		// Check if it is the last of the month
		boolean firstOfMonth = c.get(Calendar.DATE) == 1;
		
		// Check if we haven't transfered this month 
		Calendar lastTransfer = previousNegativeInterestExecution;
		boolean didTransferThisMonth = c.get(Calendar.MONTH) == lastTransfer.get(Calendar.MONTH) &&
				c.get(Calendar.YEAR) == lastTransfer.get(Calendar.YEAR);
		
		// Check if we've already added the daily lowest balances to the lowestDailyReach map
		boolean storedBalances = previousNegativeBalanceStoring.get(Calendar.DATE) == 1;
		
		return firstOfMonth && !didTransferThisMonth && storedBalances;
	}
	
	public static boolean isTimeToTransferPositiveInterest(Calendar c) {
		// Check if it is the first of the year
		boolean isFirstOfYear = c.get(Calendar.DAY_OF_YEAR) == 1;
		
		// Check if we haven't transfered this year
		Calendar lastTransfer = previousPositiveInterestExecution;
		boolean didTransferThisYear = c.get(Calendar.YEAR) == lastTransfer.get(Calendar.YEAR);
		
		// Check if we've already added the daily lowest positive balances to the map
		boolean storedBalances = previousPositiveBalanceStoring.get(Calendar.DATE) == 1;
		
		return isFirstOfYear && !didTransferThisYear && storedBalances;
	}
	
	/**
	 * Resets the system to the initial state.
	 */
	public static void reset() {
		setTotalNegativeInterestMap(new HashMap<String, Double>());
		setNegativeLowestDailyReachMap(new HashMap<String, Double>());
		setTotalPositiveInterestMap(new HashMap<String, Double>());
		setPositiveLowestDailyReachMap(new HashMap<String, Double>());
		
		setPreviousNegativeBalanceStoringDate();
		setPreviousNegativeInterestExecutionDate();
		setPreviousPositiveBalanceStoringDate();
		setPreviousPositiveInterestExecutionDate();
	}

}
