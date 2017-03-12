package database;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * An object facilitating a higher level of abstraction for the database functionality.
 * @author Andrei Cojocaru
 */
public class BankingLogger {
	private static boolean initialized = false;
	
	/**
	 * Adds a new <code>BankAccount</code> to the database.
	 * @param IBAN The <code>BankAccount</code>'s IBAN
	 */
	public static void addBankAccountEntry(String IBAN) {
		//TODO: Ensure there is no duplicate IBAN
		if (initialized == false) {
			SQLiteDB.initializeDB();
			initialized = true;
		}
		
		Statement statement = null;
		try {
			statement = SQLiteDB.getConn().createStatement();
			String update = "INSERT INTO bankaccounts (IBAN, balance) VALUES ('" + IBAN + "', " + "0);";
			statement.executeUpdate(update);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
