package database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashSet;

import accounts.BankAccount;
import accounts.CustomerAccount;
import exceptions.IllegalAccountDeletionException;

/**
 * An object facilitating a higher level of abstraction for the banking database functionality.
 * @author Andrei Cojocaru
 */
public class BankingLogger {
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
			// Add the bank account entry into the bankaccounts table
			String update = "INSERT INTO bankaccounts (IBAN, customer_BSN, balance) VALUES ('" + account.getIBAN() + "', '" 
					+ account.getMainHolder() + "', " + "0);";
			statement.executeUpdate(update);
			// Create a pairing between the bankaccount and its designated main customeraccount
			update = "INSERT INTO customerbankaccounts (customer_BSN, IBAN) VALUES ('" 
					+ account.getMainHolder() + "', '" + account.getIBAN() + "');";
			statement.executeUpdate(update);
			// Commit the changes to database after all statements were successfully executed
			SQLiteDB.getConn().commit();
			SQLiteDB.getConn().setAutoCommit(true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Generates the database plus tables and opens a connection,
	 * as needed.
	 */
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
	public static void removeCustomerBankAccountPairing(String BSN, String IBAN) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String update = "DELETE FROM customerbankaccounts WHERE customer_BSN='" + BSN 
				+ "' AND IBAN='" + IBAN + "';";
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
	
	/**
	 * Creates a database entry for a payment. Payments differ from transfers in that they
	 * do not necessarily have a destination account (e.g. physical deposits).
	 * @param account The <code>BankAccount</code> the payment is going to/from
	 * @param amount The amount paid
	 * @param type The type of payment (debit or credit)
	 * @param dateTime The timestamp of the payment
	 * @param description A description of why the payment occurred
	 */
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
	
	/**
	 * Delete a specific <code>BankAccount</code> by IBAN.
	 * @param IBAN The <code>BankAccount</code>'s IBAN
	 */
	public static void removeBankAccount(String IBAN) {
		initIfRequired();
		//TODO: Delete related transfers and payments, or not? Should pairings also be deleted?
		try {
			SQLiteDB.getConn().setAutoCommit(false);
			Statement statement = SQLiteDB.getConn().createStatement();
			
			// Find the bank account with the given IBAN
			String query = "SELECT balance FROM bankaccounts WHERE IBAN='" + IBAN + "';";
			ResultSet rs = statement.executeQuery(query);
			if (!rs.next()) {
				return;
			}
			
			// Make sure the bank account has a non-negative balance before closing
			if (rs.getFloat("balance") < 0) {
				throw new IllegalAccountDeletionException(IBAN, rs.getFloat("balance"));
			}
			
			// Delete the bank account
			String delete = "DELETE FROM bankaccounts WHERE IBAN='" + IBAN + "';";
			statement.executeUpdate(delete);
			
			// Delete any pairings between customer accounts and the bank account
			delete = "DELETE FROM customerbankaccounts WHERE IBAN='" + IBAN + "';";
			statement.executeUpdate(delete);
			
			// Commit the changes after all necessary operations are successful
			SQLiteDB.getConn().commit();
			SQLiteDB.getConn().setAutoCommit(true);
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		} catch (IllegalAccountDeletionException e) {
			//TODO: Handle
			e.printStackTrace();
		}
	}
	
	/**
	 * Retrieves a bank account from the database and creates a 
	 * <code>BankAccount</code> instance corresponding to it.
	 * @param IBAN The target <code>BankAccount</code>'s IBAN
	 * @return A newly-created <code>BankAccount</code> object
	 * corresponding to the target account. 
	 */
	public static BankAccount getBankAccountByIBAN(String IBAN) {
		initIfRequired();
		
		try {
			BankAccount result;
			Statement statement = SQLiteDB.getConn().createStatement();
			// Find the bank account associated with the IBAN in the DB
			String query = "SELECT * FROM bankaccounts WHERE IBAN='" + IBAN + "';";
			ResultSet rs = statement.executeQuery(query);
			// If the bank account exists, get its balance and main holder BSN
			if (rs.next()) {
				float balance = rs.getFloat("balance");
				String customerBSN = rs.getString("customer_BSN");
				// Create a BankAccount instance with the retrieved characteristics and return it
				result = new BankAccount(customerBSN, balance, IBAN);
			// If the bank account does not exist, return null	
			} else {
				result = null;
			}
			return result;
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Retrieves all <code>BankAccounts</code> paired with a specific <code>CustomerAccount</code>
	 * in the database.
	 * @param BSN The BSN of the customer whose <code>BankAccount</code>s should be retrieved
	 * @return A HashSet<BankAccount> of all <code>BankAccounts</code> the given
	 * customer has permission to use.
	 */
	public static HashSet<BankAccount> getBankAccountsByBSN(String BSN) {
		initIfRequired();
		HashSet<BankAccount> result = null;
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			// Find all bank accounts paired to the specified BSN
			String query = "SELECT * FROM customerbankaccounts WHERE customer_BSN='" + BSN + "';";
			ResultSet rs = statement.executeQuery(query);
			// Add each bank account found to the HashSet
			result = new HashSet<>();
			while (rs.next()) {
				String IBAN = rs.getString("IBAN");
				BankAccount newAccount = getBankAccountByIBAN(IBAN);
				if (newAccount != null) {
					result.add(newAccount);
				}
			}
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
		// Return all bank accounts found
		return result;
	}
	
	/**
	 * Remove a <code>CustomerAccount</code> from the database,
	 * along with all associated pairings and <code>BankAccounts</code>.
	 * @param BSN The <code>CustomerAccount</code>'s BSN
	 */
	public static void removeCustomerAccount(String BSN) {
		initIfRequired();
		//TODO: Make sure this plays nicely with transaction atomicity
		//TODO: Make more robust
		// Get all bank accounts paired to this customer account
		HashSet<BankAccount> bankAccounts = getBankAccountsByBSN(BSN);
		// Make sure the customer HAS bank accounts
		if (bankAccounts != null) {
			for (BankAccount key : bankAccounts) {
				// If this is the main holder of the bank account, delete the bank account
				if (key.getMainHolder().equals(BSN)) {
					removeBankAccount(key.getIBAN());
				}
				// Remove the pairing between this customer account and bank account
				removeCustomerBankAccountPairing(BSN, key.getIBAN());
			}
		}
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String delete = "DELETE FROM customeraccounts WHERE customer_BSN='" + BSN + "';";
			statement.executeUpdate(delete);
		} catch (SQLException e) {
			//TODO: Handle
			e.printStackTrace();
		}
	}
	
	/**
	 * Determines whether a <code>CustomerAccount</code> exists in the database
	 * with the given BSN.
	 * @param BSN The <code>CustomerAccount</code>'s BSN
	 * @return True or false, depending on whether a <code>CustomerAccount</code> with
	 * the given BSN is found or not
	 */
	public static boolean customerAccountExists(String BSN) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String query = "SELECT * FROM customeraccounts WHERE customer_BSN='" + BSN + "';";
			ResultSet rs = statement.executeQuery(query);
			if (!rs.next()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Determines whetehr a </code>BankAccount</code> exists in the database
	 * with the given IBAN.
	 * @param IBAN The <code>BankAccount</code>'s IBAN
	 * @return True or false, depending on whether a <code>BankAccount</code> with
	 * the given IBAN is found in the database or not
	 */
	public static boolean bankAccountExists(String IBAN) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String query = "SELECT * FROM bankaccounts WHERE IBAN='" + IBAN + "';";
			ResultSet rs = statement.executeQuery(query);
			if (!rs.next()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Determines whether a given <code>CustomerAccount</code> is paired with a
	 * given <code>BankAccount</code> in the database.
	 * @param BSN The <code>CustomerAccount</code>'s BSN
	 * @param IBAN The <code>BankAccount</code>'s IBAN
	 * @return True or false, depending on whether a matching pairing is found or not
	 */
	public static boolean customerBankAccountPairingExists(String BSN, String IBAN) {
		initIfRequired();
		
		try {
			Statement statement = SQLiteDB.getConn().createStatement();
			String query = "SELECT * FROM customerbankaccounts WHERE customer_BSN='" + BSN +"' AND IBAN='" + IBAN + "';";
			ResultSet rs = statement.executeQuery(query);
			if (!rs.next()) {
				return false;
			} else {
				return true;
			}
		} catch (SQLException e) {
			// TODO: Handle
			e.printStackTrace();
		}
		return false;
	}
}
