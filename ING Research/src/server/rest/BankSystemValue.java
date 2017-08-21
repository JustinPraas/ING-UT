package server.rest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import database.SQLiteDB;

public enum BankSystemValue {

	CREDIT_CARD_MONTHLY_FEE		(5.0),
	CREDIT_CARD_DEFAULT_CREDIT	(1000.0),
	CARD_EXPIRATION_LENGTH		(4.0),
	NEW_CARD_COST				(7.5),
	CARD_USAGE_ATTEMPTS			(3.0),
	MAX_OVERDRAFT_LIMIT			(5000.0),
	INTEREST_RATE_1				(0.0015),
	INTEREST_RATE_2				(0.0015),
	INTEREST_RATE_3				(0.0020),
	OVERDRAFT_INTEREST_RATE		(0.0100),
	DAILY_WITHDRAW_LIMIT		(250.0), 
	WEEKLY_TRANSFER_LIMIT		(2500.0), 
	CHILD_INTEREST_RATE			(0.02017);
	
	private double amount;
	
    BankSystemValue(double amount) {
        this.amount = amount;
    }

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public static void updateBankSystemValue(String key, String value) {
		double amount = Double.parseDouble(value);
		switch(key) {
		case "CREDIT_CARD_MONTHLY_FEE":
			CREDIT_CARD_MONTHLY_FEE.setAmount(amount);
			break;
		case "CREDIT_CARD_DEFAULT_CREDIT":
			CREDIT_CARD_DEFAULT_CREDIT.setAmount(amount);
			break;
		case "CARD_EXPIRATION_LENGTH":
			CARD_EXPIRATION_LENGTH.setAmount(amount);
			break;
		case "NEW_CARD_COST":
			NEW_CARD_COST.setAmount(amount);
			break;
		case "CARD_USAGE_ATTEMPTS":
			CARD_USAGE_ATTEMPTS.setAmount(amount);
			break;
		case "MAX_OVERDRAFT_LIMIT":
			MAX_OVERDRAFT_LIMIT.setAmount(amount);
			break;
		case "INTEREST_RATE_1":
			INTEREST_RATE_1.setAmount(amount);
			break;
		case "INTEREST_RATE_2":
			INTEREST_RATE_2.setAmount(amount);
			break;
		case "INTEREST_RATE_3":
			INTEREST_RATE_3.setAmount(amount);
			break;
		case "OVERDRAFT_INTEREST_RATE":
			OVERDRAFT_INTEREST_RATE.setAmount(amount);
			break;
		case "DAILY_WITHDRAW_LIMIT":
			DAILY_WITHDRAW_LIMIT.setAmount(amount);
			break;
		case "WEEKLY_TRANSFER_LIMIT":
			WEEKLY_TRANSFER_LIMIT.setAmount(amount);
			break;
		}
		System.out.println(key + " is updated to the new value: " + amount);
	}
	
	public static void reset() {
		CREDIT_CARD_MONTHLY_FEE.setAmount(5.0);
		CREDIT_CARD_DEFAULT_CREDIT.setAmount(1000.0);
		CARD_EXPIRATION_LENGTH.setAmount(4.0);
		NEW_CARD_COST.setAmount(7.5);
		CARD_USAGE_ATTEMPTS.setAmount(3.0);
		MAX_OVERDRAFT_LIMIT.setAmount(5000.0);
		INTEREST_RATE_1.setAmount(0.15);
		INTEREST_RATE_2.setAmount(0.15);
		INTEREST_RATE_3.setAmount(0.20);
		OVERDRAFT_INTEREST_RATE.setAmount(10.0);
		DAILY_WITHDRAW_LIMIT.setAmount(250.0);
		WEEKLY_TRANSFER_LIMIT.setAmount(2500.0);
	}
	
	public static void init() {
		System.out.println("Initializing bank system values.");
		Map<String, String> result = new LinkedHashMap<>();
    	Connection c = SQLiteDB.openConnection();	
		ResultSet rs;
		try {
			PreparedStatement s = c.prepareStatement("SELECT * FROM timeevents WHERE executed = 1 AND name = 'BANK_SYSTEM_VALUE_UPDATE' ORDER BY timestamp ASC");
			rs = s.executeQuery();
			while (rs.next()) {
				String[] descriptionArray = rs.getString("description").split(":");
				result.put(descriptionArray[0], descriptionArray[1]);
			}
		} catch (NumberFormatException | SQLException e) {
			e.printStackTrace();
		}
		
		for (Map.Entry<String, String> entry : result.entrySet()) {
			updateBankSystemValue(entry.getKey(), entry.getValue());
		}
	}
}
