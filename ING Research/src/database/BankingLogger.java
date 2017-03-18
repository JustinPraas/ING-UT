package database;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import accounts.BankAccount;
import accounts.CustomerAccount;

/**
 * An object facilitating a higher level of abstraction for the banking database functionality.
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
		initIfRequired();
		
		try {
			SQLiteDB.getConn().setAutoCommit(false);
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO bankaccounts (IBAN, customer_BSN, balance) VALUES ('" + account.getIBAN() + "', '" 
					+ account.getMainHolder().getBSN() + "', " + "0);";
			statement.executeUpdate(update);
			update = "INSERT INTO customerbankaccounts (customer_BSN, IBAN) VALUES ('" 
					+ account.getMainHolder().getBSN() + "', '" + account.getIBAN() + "');";
			statement.executeUpdate(update);
			SQLiteDB.getConn().commit();
			SQLiteDB.getConn().setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void initIfRequired() {
		if (initialized == false) {
			SQLiteDB.initializeDB();
			initialized = true;
		}
	}
	
	/**
	 * Associates a <code>BankAccount</code> with a <code>CustomerAccount</code> in the database.
	 * @param customerAccount The <code>CustomerAccount</code> to associate the <code>BankAccount</code> with
	 * @param bankAccount The <code>BankAccount</code> to associate the <code>CustomerAccount</code> with
	 */
	public static void addCustomerBankAccountPairing(CustomerAccount customerAccount, BankAccount bankAccount) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO customerbankaccounts (customer_BSN, IBAN) VALUES ('" 
					+ customerAccount.getBSN() + "', '" + bankAccount.getIBAN() + "');";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Removes a <code>BankAccount</code>-<code>CustomerAccount</code> pairing from the database.
	 * @param customerAccount The <code>CustomerAccount</code> associated with the <code>BankAccount</code>
	 * @param bankAccount The <code>BankAccount</code> associated with the <code>CustomerAccount</code>
	 */
	public static void removeCustomerBankAccountPairing(CustomerAccount customerAccount, BankAccount bankAccount) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "DELETE FROM customerbankaccounts WHERE customer_BSN='" + customerAccount.getBSN() 
				+ "' AND IBAN='" + bankAccount.getIBAN() + "';";
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
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO customeraccounts (customer_BSN, name, surname, street_address, email, phone_number, birth_date) " 
					+ "VALUES ('" + account.getBSN() + "', '" + account.getName() + "', '" + account.getSurname() + "', '" + account.getStreetAddress() 
					+ "', '" + account.getEmail() + "', '" + account.getPhoneNumber() + "', '" + account.getBirthdate().toString() + "');";
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
	public static void logTransfer(BankAccount source, BankAccount destination, float amount, Timestamp dateTime) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO transfers (source_IBAN, destination_IBAN, amount, date_time) VALUES ('" + source.getIBAN() + "', '" 
					+ destination.getIBAN() + "', " + amount + ", '" + dateTime.toString() + "');";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
	}
	
	public static void logPayment(BankAccount account, float amount, String type, Timestamp dateTime, String description) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO payments (IBAN, amount, date_time, payment_type, description) VALUES ('" + account.getIBAN() + "', " 
					+ amount + ", '" + dateTime.toString() + "', '" + type + "', '" + description + "');";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
	}
}
