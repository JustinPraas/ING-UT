package database;

import java.sql.SQLException;
import java.sql.Statement;

import accounts.BankAccount;
import accounts.CustomerAccount;

/**
 * An object facilitating a higher level of abstraction for the database functionality.
 * @author Andrei Cojocaru
 */
public class BankingLogger {
	//TODO: Test intensely
	private static boolean initialized = false;
	
	/**
	 * Adds a new <code>BankAccount</code> to the database.
	 * @param account The <code>BankAccount</code> to be added
	 */
	public static void addBankAccountEntry(BankAccount account) {
		//TODO: Ensure there is no duplicate IBAN
		if (initialized == false) {
			SQLiteDB.initializeDB();
			initialized = true;
		}
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO bankaccounts (IBAN, balance) VALUES ('" + account.getIBAN() + "', " + "0);";
			statement.executeUpdate(update);
			update = "INSERT INTO customerbankaccounts (customer_BSN, IBAN) VALUES ('" 
					+ account.getMainHolder().getBSN() + "', '" + account.getIBAN() + "');";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds a <code>CustomerAccount</code> to the database.
	 * @param account The <code>CustomerAccount</code> to be added
	 */
	public static void addCustomerAccountEntry(CustomerAccount account) {
		//TODO: Ensure there is no duplicate BSN
		if (initialized == false) {
			SQLiteDB.initializeDB();
			initialized = true;
		}
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO customeraccounts (customer_BSN, name, surname, street_address, email, phone_number, birth_date) " 
					+ "VALUES ('" + account.getBSN() + "', '" + account.getName() + "', '" + account.getSurname() + "', '" + account.getStreetAddress() 
					+ "', '" + account.getEmail() + "', '" + account.getPhoneNumber() + "', '" + account.getBirthdate() + ");";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
	}
	
	/**
	 * Logs a transfer of a certain amount of funds from a source <code>BankAccount</code>
	 * to a certain destination <code>BankAccount</code>. 
	 * @param source The source <code>BankAccount</code>
	 * @param destination The destination <code>BankAccount</code>
	 * @param amount The amount of funds transferred
	 */
	public static void logTransfer(BankAccount source, BankAccount destination, float amount) {
		if (initialized == false) {
			SQLiteDB.initializeDB();
			initialized = true;
		}
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO transfers (source_IBAN, destination_IBAN, amount) VALUES ('" + source.getIBAN() + "', '" 
					+ destination.getIBAN() + "', " + amount + ");";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
	}
}
