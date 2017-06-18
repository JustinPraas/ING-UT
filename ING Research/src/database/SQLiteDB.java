package database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import client.Client;

/**
 * A class for dynamic DB generation.
 * @author Andrei Cojocaru
 */
public class SQLiteDB {
	public static final String DBName = Client.DESKTOP_ING_FOLDER_PATH + "banking.db";
	public static final String schema = Client.DESKTOP_ING_FOLDER_PATH + "bankingtables.txt";
	
	/**
	 * Creates a .db file if it does not already exist.
	 */
	public static void initializeDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			SQLiteDB.initializeTableStructure();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Fetches the table creation specifications from a certain schema file.
	 * @return A String containing table creation statements
	 */
	private static String getSchemaStatements() {
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(schema));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String statements = "";
		for (String key : lines) {
			statements += key;
		}
		
		return statements;
	}
	
	/**
	 * Creates the DB tables as instructed in the DBName.db file.
	 */
	private static void initializeTableStructure() {
		String tableStructure = getSchemaStatements();
		Connection conn;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + DBName);
			Statement statement = conn.createStatement();
			statement.executeUpdate(tableStructure);
			statement.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void executeStatement(String s) {
		Connection conn;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + DBName);
			Statement statement = conn.createStatement();
			statement.executeUpdate(s);
			statement.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static Connection openConnection() {
		Connection c = null;
		try {
			c = DriverManager.getConnection("jdbc:sqlite:" + DBName);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return c;
	}
}