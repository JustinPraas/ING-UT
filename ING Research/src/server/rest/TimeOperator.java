package server.rest;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import accounts.BankAccount;
import database.DataManager;
import database.SQLiteDB;
import exceptions.ObjectDoesNotExistException;

public class TimeOperator extends Thread {
	
	public TimeOperator() {
		start();
	}

	/**
	 * A timer that checks for certain events for specific dates/times AND when it is interrupted.
	 */
	@Override
	public void run() {		
		while (true) {
			Calendar c = ServerModel.getServerCalendar();

			// Events on the first day of the month
			if (c.get(Calendar.DATE) == 1) {
				SQLiteDB.connectionLock.lock();
				updateTransferLimitsIfNeeded(c);
				SQLiteDB.connectionLock.unlock();
			}
			
			try {
				// Sleep for a day
				Thread.sleep(3600 * 1000 * 24);
			} catch (InterruptedException e) {
				
			}	
		}
	}

	/**
	 * @param newlySimulatedDays2 
	 * 
	 */
	public static void simulateDays(int newlySimulatedDays) {
		SQLiteDB.connectionLock.lock();
		Calendar serverCalendar = ServerModel.getServerCalendar();
		for (int i = 1; i <= newlySimulatedDays; i++) {
			serverCalendar.add(Calendar.DATE, 1);
			if (serverCalendar.get(Calendar.DATE) == 1 && !updatedTransferLimitsThisMonth(serverCalendar)) {
				updateTransferLimitsIfNeeded(serverCalendar);
				break;
			}			
		}	
		SQLiteDB.connectionLock.unlock();
	}

	/**
	 * Updates the transfer limits for all pending bank accounts. 
	 */
	private static void updateTransferLimitsIfNeeded(Calendar serverCalendar) {	
		if (serverCalendar.get(Calendar.DATE) == 1 && !updatedTransferLimitsThisMonth(serverCalendar)) {
			System.out.println("Updating transfer limits: " + serverCalendar.getTime().toString());
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
			ServerDataHandler.setUpdatedTransferLimitMap(new HashMap<>());
			new TimeEvent(TimeEvent.UPDATE_TRANSFER_LIMITS, serverCalendar.getTimeInMillis(), 
					"Updated the transfer limits on " + serverCalendar.getTime().toString()).saveToDB();
		}			
	}

	private static boolean updatedTransferLimitsThisMonth(Calendar serverCalendar) {
		Calendar lastExecution = getTimestampOf(TimeEvent.UPDATE_TRANSFER_LIMITS);
		if (lastExecution != null) {
			return lastExecution.get(Calendar.DATE) == 1 && lastExecution.get(Calendar.MONTH) == serverCalendar.get(Calendar.MONTH) && lastExecution.get(Calendar.YEAR) == serverCalendar.get(Calendar.YEAR);
		} else {
			return false;
		}		
	}

	private static Calendar getTimestampOf(String settingName) {
		TimeEvent timeEvent;
		try {
			timeEvent = (TimeEvent) DataManager.getObjectByPrimaryKey(TimeEvent.CLASSNAME, settingName);
			return timeEvent.getCalendar();
		} catch (ObjectDoesNotExistException e) {
			return null;
		}
	}

}
