package interesthandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;

import accounts.Account;
import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.SavingsAccount;
import database.DataManager;
import database.SQLiteDB;
import exceptions.IllegalAmountException;
import exceptions.ObjectDoesNotExistException;
import server.rest.BankSystemValue;

public class InterestHandler2 {

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
	
	private static double calculateNegativeInterest(double balance, int nrOfDaysInMonth) {
		return balance * MONTHLY_INTEREST_RATE / nrOfDaysInMonth;
	}
	
	private static double calculatePositiveInterest(double balance, boolean isChild) {
		if (isChild) {
			if (balance < 2500) {
				return balance * CHILD_INTEREST_RATE;
			} else {
				return 2500 * CHILD_INTEREST_RATE;
			}			
		} else {
			if (balance < 25000) {
				return balance * DAILY_INTEREST_RATE_RANGE_1;
			} else if (balance >= 25000 && balance < 75000) {
				return balance * DAILY_INTEREST_RATE_RANGE_2;
			} else if (balance >= 75000 && balance < 1000000) {
				return balance * DAILY_INTEREST_RATE_RANGE_3;
			} else {
				return 0;
			}
		}
	}
	
	public static void handleInterest(Calendar serverCalendar) {
		updateNegativeInterest(serverCalendar);
		updatePositiveInterest(serverCalendar);
		
		if (serverCalendar.get(Calendar.DATE) == 1) {
			transferNegativeInterest();
		}
		
		if (serverCalendar.get(Calendar.DAY_OF_YEAR) == 1) {
			transferSavingsInterest();
			transferChildInterest();
		}
		
		initializeBalanceReachTable();
	}
	
	private static void transferSavingsInterest() {
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs1 = s.executeQuery("SELECT IBAN, balance FROM savingsinterest;");
			while (rs1.next()) {
				String IBAN = rs1.getString("IBAN");
				double balance = rs1.getDouble("balance");
				try {
					SavingsAccount savingsAccount = (SavingsAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
					savingsAccount.debit(balance);
					savingsAccount.saveToDB();
				} catch (ObjectDoesNotExistException e) {
					// Account does no longer exist, no problem, do nothing.
				} catch (IllegalAmountException e) {
					e.printStackTrace();
				}	
				s.executeUpdate("DELETE FROM savingsinterest");			
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
	}
	
	private static void transferChildInterest() {
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM childinterest;");
			while (rs.next()) {
				String IBAN = rs.getString("IBAN");
				double balance = rs.getDouble("balance");
				try {
					BankAccount bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
					bankAccount.debit(balance);
					bankAccount.saveToDB();
				} catch (ObjectDoesNotExistException e) {
					// Account does no longer exist, no problem, do nothing.
				} catch (IllegalAmountException e) {
					e.printStackTrace();
				}
				s.executeUpdate("DELETE FROM childinterest");				
			}
			rs.close();
			s.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
	}

	private static void transferNegativeInterest() {
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM negativeinterest;");
			while (rs.next()) {
				String IBAN = rs.getString("IBAN");
				double balance = rs.getDouble("balance");
				try {
					BankAccount bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
					bankAccount.credit(balance);
					bankAccount.saveToDB();
				} catch (ObjectDoesNotExistException e) {
					// Account does no longer exist, no problem, do nothing.
				} catch (IllegalAmountException e) {
					e.printStackTrace();
				}				
			}
			s.executeUpdate("DELETE FROM negativeinterest");
			rs.close();
			s.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
	}

	private static void updateNegativeInterest(Calendar serverCalendar) {
		// The result query for updating all bank accounts
		String updateQuery = "";
		// Fetch all normal bank accounts from the balancereach map
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM balancereach WHERE savings == 0 AND child == 0;");
			while (rs.next()) {
				String IBAN = rs.getString("IBAN");
				double newInterest = calculateNegativeInterest(rs.getDouble("balance"), serverCalendar.getActualMaximum(Calendar.DATE));
//				updateQuery += "INSERT INTO negativeinterest (IBAN, balance) VALUES ('" + IBAN + "', " + newInterest + ") "
//								+ "ON DUPLICATE KEY UPDATE balance = balance + " + newInterest + "; ";
				updateQuery += "INSERT OR REPLACE INTO negativeinterest(IBAN, balance) VALUES ('" 
						+ IBAN + "', 0, 0, (SELECT CASE WHEN exists(SELECT * FROM negativeinterest WHERE IBAN = '" + IBAN 
						+ "') THEN balance + " + newInterest + " ELSE " + newInterest + " END ));";
			
			}
			s.executeUpdate(updateQuery);
			rs.close();
			s.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
	}
	
	private static void updatePositiveInterest(Calendar serverCalendar) {
		// The result query for updating all bank accounts
		String updateQuery = "";
		// Fetch all normal bank accounts from the balancereach map
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM balancereach WHERE savings == 1 OR child == 1;");
			while (rs.next()) {
				boolean isChild = rs.getBoolean("child");
				String IBAN = rs.getString("IBAN");
				double newInterest = calculatePositiveInterest(rs.getDouble("balance"), isChild);				
//				updateQuery += "INSERT INTO " + (isChild ? "childinterest" : "savingsinterest") + " (IBAN, balance) VALUES "
//						+ "('" + IBAN + "', " + newInterest + ") ON DUPLICATE KEY UPDATE balance = balance " + newInterest + ";";
				updateQuery += "INSERT OR REPLACE INTO " + (isChild ? "childinterest" : "savingsinterest") + " (IBAN, balance) VALUES ('" 
						+ IBAN + "', 0, 0, (SELECT CASE WHEN exists(SELECT * FROM " + (isChild ? "childinterest" : "savingsinterest") + " WHERE IBAN = '" + IBAN 
						+ "') THEN balance + " + newInterest + " ELSE " + newInterest + " END ));";
			}
			s.executeUpdate(updateQuery);
			rs.close(); 
			s.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
	}

	public static void updateBalanceReachEntry(Account account) {
		CustomerAccount customerAccount;
		boolean isBankAccount = account.getClassName() == BankAccount.CLASSNAME;
		boolean isSavingsAccount = account.getClassName() == SavingsAccount.CLASSNAME;
		boolean isChild = false;
		if (isBankAccount) {
			try {
				customerAccount = (CustomerAccount) DataManager.getObjectByPrimaryKey(CustomerAccount.CLASSNAME, ((BankAccount) account).getMainHolderBSN());
				isChild = CustomerAccount.isYoungerThan(18, customerAccount);
			} catch (ObjectDoesNotExistException e) {
				e.printStackTrace();
				return;
			}
		}		
		double balance = account.getBalance();
		try {
			SQLiteDB.connectionLock.lock();
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("SELECT IBAN, balance FROM balancereach WHERE savings == " 
								+ (isSavingsAccount ? "1" : "0") +" AND child == " + (isChild ? "1" : "0") 
								+ " AND IBAN = '" + account.getIBAN() + "';");
			while (rs.next()) {
				double oldReach = rs.getDouble("balance");			
				if (balance < oldReach) {
					s.executeUpdate("UPDATE balancereach SET balance = " + balance + " WHERE IBAN = '" + account.getIBAN() + "';");
				}
			}
			rs.close(); 
			s.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void initializeBalanceReachTable() {
		String insertStatement = "";
		
		ArrayList<BankAccount> bankAccounts = (ArrayList<BankAccount>) DataManager.getObjectsFromDB(BankAccount.CLASSNAME);
		ArrayList<SavingsAccount> savingsAccounts = (ArrayList<SavingsAccount>) DataManager.getObjectsFromDB(SavingsAccount.CLASSNAME);
		
		// Add all bank accounts to the balance reach table
		for (BankAccount b : bankAccounts) {
			try {
				boolean isChild = BankAccount.isMainHolderChild(b.getMainHolderBSN());
				if (b.getBalance() < 0) {
					insertStatement += "INSERT INTO balancereach (IBAN, savings, child, balance) VALUES ('" 
							+ b.getIBAN() + "', 0, " + (isChild ? "1" : "0") + ", " + b.getBalance() + ");";
				}
			} catch (ObjectDoesNotExistException e) {
				// Main holder does not exist...
				e.printStackTrace();
				
			}
		}
		
		for (SavingsAccount s : savingsAccounts) {
			insertStatement += "INSERT INTO balancereach (IBAN, savings, child, balance) VALUES ('" 
					+ s.getIBAN() + "', 1, 0, " + s.getBalance() + ");";
		}
		
		try {
			Connection c = SQLiteDB.openConnection();
			Statement s = c.createStatement();
			s.executeUpdate(insertStatement);
			s.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
