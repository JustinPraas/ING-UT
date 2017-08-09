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

import accounts.BankAccount;
import database.DataManager;
import database.SQLiteDB;
import exceptions.ObjectDoesNotExistException;
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
			serverCalendar.add(Calendar.DATE, 1);		
			updateSystem(serverCalendar);
		}	
		SQLiteDB.connectionLock.unlock();
	}
	
	private static void updateSystem(Calendar c) {
		String date = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE);
		long startMillis = 0;
		try {
			startMillis = Logger.parseDateToMillis(date);
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}
		long endMillis = startMillis + 1000 * 3600 * 24;
		
		ArrayList<TimeEvent> todaysUnfinishedEvents = getTodaysEvents(startMillis, endMillis);
		
		for (TimeEvent t : todaysUnfinishedEvents) {
			switch (t.getName()) {
			case "BANK_SYSTEM_VALUE_UPDATE":
				updateBankSystemValue(t);
				break;
			case "TRANSFER_LIMIT_UPDATE":
				updateTransferLimit(t);
				break;
			}
		}
	}
	
	private static void updateBankSystemValue(TimeEvent t) {
		String[] descriptionArray = t.getDescription().split(":");
		String key = descriptionArray[0];
		String value = descriptionArray[1];
		BankSystemValue.updateBankSystemValue(key, value);
		t.setExecuted(true);
		t.saveToDB();
	}
	
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
