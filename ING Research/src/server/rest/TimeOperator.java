package server.rest;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import accounts.BankAccount;
import database.DataManager;
import database.SQLiteDB;
import exceptions.ObjectDoesNotExistException;

public class TimeOperator extends Thread {
	
	public int newlySimulatedDays;
	
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
				updateTransferLimitsIfNeeded(c);
			}
			
			try {
				// Sleep for a day
				Thread.sleep(3600 * 1000 * 24);
			} catch (InterruptedException e) {
				Calendar newCalendar = ServerModel.getServerCalendar();
				for (int i = 1; i <= newlySimulatedDays; i++) {
					newCalendar.add(Calendar.DATE, 1);
					updateTransferLimitsIfNeeded(newCalendar);
				}	
				newlySimulatedDays = 0;
			}	
		}
	}

	/**
	 * Updates the transfer limits for all pending bank accounts. 
	 */
	private void updateTransferLimitsIfNeeded(Calendar serverCalendar) {	
		SQLiteDB.connectionLock.lock();	
		if (serverCalendar.get(Calendar.DATE) == 1 && !updatedTransferLimitsThisMonth(serverCalendar)) {
			System.out.println("Updating transfer limits");
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
		
		SQLiteDB.connectionLock.unlock();			
	}

	private boolean updatedTransferLimitsThisMonth(Calendar serverCalendar) {
		Calendar lastExecution = getTimestampOf(TimeEvent.UPDATE_TRANSFER_LIMITS);
		if (lastExecution != null) {
			return lastExecution.get(Calendar.DATE) == 1 && lastExecution.get(Calendar.MONTH) == serverCalendar.get(Calendar.MONTH) && lastExecution.get(Calendar.YEAR) == serverCalendar.get(Calendar.YEAR);
		} else {
			return false;
		}		
	}

	private Calendar getTimestampOf(String settingName) {
		SQLiteDB.connectionLock.lock();	
		TimeEvent timeEvent;
		try {
			timeEvent = (TimeEvent) DataManager.getObjectByPrimaryKey(TimeEvent.CLASSNAME, settingName);
			SQLiteDB.connectionLock.unlock();	
			return timeEvent.getCalendar();
		} catch (ObjectDoesNotExistException e) {
			SQLiteDB.connectionLock.unlock();	
			return null;
		}
	}

}
