package database;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * A "static" class used to interface with the banking database.
 * @author Andrei Cojocaru
 */
public class SQLiteDB {
	public static final String DBName = "banking";
	private static Connection conn = null;
	
	/**
	 * Opens a connection to the database, creates a .db file if it does not already exist.
	 */
	public static void initializeDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + DBName + ".db");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
