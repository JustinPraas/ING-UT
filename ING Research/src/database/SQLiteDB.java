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
 * A class for dynamic DB generation.
 * @author Andrei Cojocaru
 */
public class SQLiteDB {
	public static final String DBName = "banking";
	public static final String schema = "bankingtables";
	
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
	
	/**
	 * Creates the DB tables as instructed in the DBName.db file.
	 */
	private static void initializeTableStructure() {
		String tableStructure = getSchemaStatements();
		Connection conn;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + DBName + ".db");
			Statement statement = conn.createStatement();
			statement.executeUpdate(tableStructure);
			statement.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}