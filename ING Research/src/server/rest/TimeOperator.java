package server.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import accounts.BankAccount;
import accounts.CreditAccount;
import cards.CreditCard;
import database.DataManager;
import database.SQLiteDB;
import exceptions.IllegalAmountException;
import exceptions.ObjectDoesNotExistException;
import interesthandler.InterestHandler2;
import logging.Logger;

public class TimeOperator extends Thread {
	
	public TimeOperator() {
		start();
		BankSystemValue.init();
	}

	/**
	 * A timer that checks for certain events for specific dates/times AND when it is interrupted.
	 */
	@Override
	public void run() {		
		while (true) {
			Calendar c = ServerModel.getServerCalendar();

			// Update events on a daily basis
			SQLiteDB.connectionLock.lock();
			System.out.println("[INFO] updating system");
			updateSystem(c);
			SQLiteDB.connectionLock.unlock();
			
			try {
				// Sleep for a day
				Thread.sleep(3600 * 1000 * 24);
			} catch (InterruptedException e) {
				
			}	
		}
	}
	
	public static void simulateDays(int newlySimulatedDays) {
		SQLiteDB.connectionLock.lock();
		Calendar serverCalendar = ServerModel.getServerCalendar();
		for (int i = 1; i <= newlySimulatedDays; i++) {
			System.out.println("[INFO] Passing " + serverCalendar.getTime().toString());
			serverCalendar.add(Calendar.DATE, 1);
			System.out.println("[INFO] -> handling interest");
			InterestHandler2.handleInterest(serverCalendar);
			System.out.println("[INFO] -> updating system");
			updateSystem(serverCalendar);
		}	
		SQLiteDB.connectionLock.unlock();
	}
	
	/**
	 * Updates the system if there are any TimeEvents with the current given Calendar date.
	 * @param c the server date that will be used to check for TimeEvents
	 */
	private static void updateSystem(Calendar c) {
		String date = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE);
		long startMillis = 0;
		try {
			startMillis = Logger.parseDateToMillis(date, "yyyy-MM-dd");
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}
		long endMillis = startMillis + 1000 * 3600 * 24;
		
		ArrayList<TimeEvent> todaysUnfinishedEvents = getTodaysEvents(startMillis, endMillis);
		System.out.println("[INFO] Handling " + todaysUnfinishedEvents.size() + " event(s)");
		
		for (TimeEvent t : todaysUnfinishedEvents) {
			switch (t.getName()) {
			case "BANK_SYSTEM_VALUE_UPDATE":
				updateBankSystemValue(t);
				break;
			case "TRANSFER_LIMIT_UPDATE":
				updateTransferLimit(t);
				break;
			case "ACCOUNT_TYPE_CHANGE":
				updateAccountType(t);
				break;
			case "ACTIVATE_CREDIT_CARD":
				activateCreditCard(t);
				break;
			case "CREDIT_CARD_RESET":
				updateCreditCardAndFee(t);
				break;
			}
		}
	}
	
	private static void activateCreditCard(TimeEvent t) {
		String[] descriptionArray = t.getDescription().split(":");
		String cardNumber = descriptionArray[1];
		
		try {
			CreditCard creditCard = (CreditCard) DataManager.getObjectByPrimaryKey(CreditCard.CLASSNAME, cardNumber);
			creditCard.setActive(true);
			creditCard.saveToDB();
			System.out.println("[UPDATE] Set credit card " + cardNumber + " to active.");
		} catch (ObjectDoesNotExistException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Updates the account type for a child bank account to a regular bank account.
	 * @param t The TimeEvent containing the date and description info needed to execute this 
	 * account type change
	 */
	private static void updateAccountType(TimeEvent t) {
		String[] descriptionArray = t.getDescription().split(":");
		String IBAN = descriptionArray[1];
		
		BankAccount ingBankAccount;
		try {
			Double positiveInterest = BankAccount.getAndRemoveChild18thBirthDayInteres(IBAN);
			
			// Transfer the money
			ingBankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, BankAccount.ING_BANK_ACCOUNT_IBAN);
			ingBankAccount.transfer(IBAN, positiveInterest);
			
			// TODO remove child from child interest
		} catch (ObjectDoesNotExistException e) {
			e.printStackTrace();
		}
		
		System.out.println("[UPDATE] Updating account type for " + IBAN + "; transfering built-up interest");
		
		BankAccount b = null;
		try {
			b = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
		} catch (ObjectDoesNotExistException e) {
			e.printStackTrace();
			return;
		}
		b.setAccountType("regular");
		b.saveToDB();
		t.setExecuted(true);
		t.saveToDB();		
	}

	/**
	 * Updates the BankSystemValue for the given description of the given TimeEvent
	 * @param t the TimeEvent for the update the bank system value.
	 */
	private static void updateBankSystemValue(TimeEvent t) {
		String[] descriptionArray = t.getDescription().split(":");
		
		String key = descriptionArray[0];
		String value = descriptionArray[1];
		BankSystemValue.updateBankSystemValue(key, value);
		System.out.println("[UPDATE] System setting '" + key + "' has been set to " + value);
		
		t.setExecuted(true);
		t.saveToDB();
	}
	
	/**
	 * Updates the transferLimit for each and every bank account that has requested such update.
	 * @param t The TimeEvent that indicates that the transfer limits should be updated
	 */
	private static void updateTransferLimit(TimeEvent t) {
		HashMap<String, Double> updatedTransferLimitMap = ServerDataHandler.getUpdatedTransferLimitMap();
		for (Map.Entry<String, Double> entry : updatedTransferLimitMap.entrySet()) {
			try {
				BankAccount bankAccount = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, entry.getKey());
				bankAccount.setTransferLimit(entry.getValue());
				bankAccount.saveToDB();
			} catch (ObjectDoesNotExistException e) {
				// Bank account does not exist.. do nothing with the entry
			}
		}
		
		// Update events
		t.setExecuted(true);
		t.saveToDB();
		
		System.out.println("[UPDATE] Transfer limits have been updated for " + updatedTransferLimitMap.size());
		
		TimeEvent nextMonthUpdate = new TimeEvent();
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(t.getTimestamp());
		c.add(Calendar.MONTH, 1);
		nextMonthUpdate.setName(t.getName());
		nextMonthUpdate.setDescription(t.getDescription());
		nextMonthUpdate.setTimestamp(c.getTimeInMillis());
		nextMonthUpdate.setExecuted(false);
		nextMonthUpdate.saveToDB();
	}
	
	/**
	 * Updates the transferLimit for each and every bank account that has requested such update.
	 * @param t The TimeEvent that indicates that the transfer limits should be updated
	 */
	private static void updateCreditCardAndFee(TimeEvent t) {
		ArrayList<Criterion> cr = new ArrayList<>();
		cr.add(Restrictions.eq("closed", false));
		@SuppressWarnings("unchecked")
		ArrayList<CreditAccount> creditCardUsers = (ArrayList<CreditAccount>) DataManager.getObjectsFromDB(CreditAccount.CLASSNAME, cr);
		
		for (CreditAccount c : creditCardUsers) {
			try {			
				// Equalize balance
				BankAccount b = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, c.getIBAN());
				double moneyToTransfer = BankSystemValue.CREDIT_CARD_DEFAULT_CREDIT.getAmount() - c.getBalance();
				b.credit(moneyToTransfer);
				c.debit(moneyToTransfer);

				// Subtract fee
				b.credit(BankSystemValue.CREDIT_CARD_MONTHLY_FEE.getAmount());
				b.saveToDB();
				c.saveToDB();
			} catch (IllegalAmountException | ObjectDoesNotExistException e) {
				e.printStackTrace();
			}
		}
		
		// Update events
		t.setExecuted(true);
		t.saveToDB();
		
		System.out.println("[UPDATE] Credit cards reset and fees subtracted for " + creditCardUsers.size() + " users.");
		
		TimeEvent nextMonthUpdate = new TimeEvent();
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(t.getTimestamp());
		c.add(Calendar.MONTH, 1);
		nextMonthUpdate.setName(t.getName());
		nextMonthUpdate.setDescription(t.getDescription());
		nextMonthUpdate.setTimestamp(c.getTimeInMillis());
		nextMonthUpdate.setExecuted(false);
		nextMonthUpdate.saveToDB();
	}

	/**
	 * Returns all TimeEvents which are not executed and are between 0:00 23:59 of a day.
	 * @param startMillis the start of milliseconds
	 * @param endMillis the end of milliseconds
	 * @return the list of TimeEvents which should be handled today
	 */
	private static ArrayList<TimeEvent> getTodaysEvents(long startMillis, long endMillis) {
		ArrayList<TimeEvent> result = new ArrayList<>();
    	Connection c = SQLiteDB.openConnection();	
		ResultSet rs;
		try {
			PreparedStatement s = c.prepareStatement("SELECT * FROM timeevents WHERE timestamp >= ? AND timestamp < ? AND executed = 0");
			s.setLong(1, startMillis);
			s.setLong(2, endMillis);
			rs = s.executeQuery();
			while (rs.next()) {
				TimeEvent timeEvent = new TimeEvent();
				timeEvent.setID(rs.getInt("ID"));
				timeEvent.setName(rs.getString("name"));
				timeEvent.setTimestamp(Long.parseLong(rs.getString("timestamp")));
				timeEvent.setDescription(rs.getString("description"));
				timeEvent.setExecuted(rs.getBoolean("executed"));
				result.add(timeEvent);
			}
		} catch (NumberFormatException | SQLException e) {
			e.printStackTrace();
		}
		
		return result;	
	}

}
