package database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * A "static" class used to interface with the banking database.
 * Note: Due to the nature of the project, hashing and security will not
 * be implemented until specifically requested.
 * @author Andrei Cojocaru
 */
public class SQLiteDB {
	public static final String DBName = "banking";
	public static final String schema = "bankingtables";
	private static Connection conn = null;
	
	/**
	 * Opens a connection to the database, creates a .db file if it does not already exist.
	 */
	public static void initializeDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + DBName + ".db");
			SQLiteDB.initializeTableStructure();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConn() {
		return conn;
	}
	
	/**
	 * Fetches the table creation specifications from a certain schema file.
	 * @return A String containing table creation statements
	 */
	private static String getSchemaStatements() {
		//TODO: Make this more robust
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(schema));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String statements = "";
		for (String key : lines) {
			statements += key;
		}
		
		return statements;
	}
	
	private static void initializeTableStructure() {
		String tableStructure = getSchemaStatements();
		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate(tableStructure);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
