package database;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;

import accounts.BankAccount;
import accounts.CreditAccount;
import accounts.CustomerAccount;
import accounts.SavingsAccount;
import cards.Card;
import cards.CreditCard;
import cards.DebitCard;
import exceptions.ObjectDoesNotExistException;
import logging.Logger;
import server.rest.TimeEvent;

/**
 * Provides utility methods to store/retrieve objects from DB
 * easily via the Hibernate ORM.
 * @author Andrei Cojocaru
 */
public class DataManager {
	public static final String CFGPATH = "hibernate.cfg.xml";
	private static Configuration cfg;
	private static SessionFactory factory;
	private static boolean initialized = false;

	/**
	 * Initializes hibernate configuration and database connection.
	 */
    public static synchronized void init() {
    	SQLiteDB.initializeDB();
    	cfg = new Configuration();
    	cfg.configure(CFGPATH);
    	cfg.addAnnotatedClass(DebitCard.class);
    	cfg.addAnnotatedClass(CreditCard.class);
    	cfg.addAnnotatedClass(Card.class);
    	cfg.addAnnotatedClass(BankAccount.class);
    	cfg.addAnnotatedClass(CustomerAccount.class);
    	cfg.addAnnotatedClass(SavingsAccount.class);
    	cfg.addAnnotatedClass(CreditAccount.class);
    	cfg.addAnnotatedClass(accounts.Transaction.class);
    	cfg.addAnnotatedClass(logging.Log.class);
    	cfg.addAnnotatedClass(server.rest.TimeEvent.class);
    	factory = cfg.buildSessionFactory();
    }
    
    /**
     * Initializes hibernate configuration and DB connection if
     * they are not already initialized. Sets up ING bank account in the database
     * for further use by the program. Resets the account on every server startup.
     */
	private static synchronized void initIfRequired() {
		if (!initialized) {
    		init();
    		initialized = true;
    	}	
	}
    
    /**
     * Remove a persistent object from the database.
     * @param o The object to be removed
     */
    public static synchronized void removeEntryFromDB(DBObject o) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Transaction t = session.beginTransaction();
    	String HQL = "delete " + o.getClassName() + " where " + o.getPrimaryKeyName() + " = '" + o.getPrimaryKeyVal() + "'";
    	Query<?> query = session.createQuery(HQL);
    	query.executeUpdate();
    	t.commit();
    	session.close();
    }
    
    /**
     * Determines whether or not a given object already exists in the database.
     * @param o Object to be looked up
     * @return True or false, depending on whether or not the object is found
     */
    @SuppressWarnings("deprecation")
	public static synchronized boolean objectExists(DBObject o) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(o.getClassName());
    	cr.add(Restrictions.eq(o.getPrimaryKeyName(), o.getPrimaryKeyVal()));
    	if (cr.list().size() != 0) {
    		session.close();
    		return true;
    	}
    	session.close();
    	return false;
    }
    
    /**
     * Saves a given object to database. If the object exists in the DB, its entry
     * is updated. If the object does not exist in the DB, it is added.
     * @param o The object to save
     */
	public static synchronized void save(DBObject o) {
		initIfRequired();
		
		Session session = factory.openSession();
		Transaction t = session.beginTransaction();
		session.saveOrUpdate(o);
		t.commit();
		session.close();
	}
    
	/**
	 * Locates all objects of the specified kind that meet the given criteria in the DB.
	 * @param className The name of the type of object being queried
	 * @param criteria The query criteria
	 * @return A List of all objects meeting the given criteria
	 */
    @SuppressWarnings("deprecation")
	public static synchronized List<?> getObjectsFromDB(String className, ArrayList<Criterion> criteria) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	for (Criterion c : criteria) {
    		cr.add(c);
    	}
    	List<?> results = cr.list();
    	session.close();
    	return results;
    }
    
    /**
     * Returns a List of all objects of the specified type that exist in the DB.
     * @param className The name of the type of object being queried
     * @return A List of all objects of the given type
     */
    @SuppressWarnings("deprecation")
	public static synchronized List<?> getObjectsFromDB(String className) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	List<?> results = cr.list();
    	session.close();
    	return results;
    }
    
    /**
     * Finds a persistent object in the database by primary key.
     * @param className The name of the type of object being queried
     * @param primaryKey The primary key of the desired object
     * @return The object with the given primary key, if found
     * @throws ObjectDoesNotExistException 
     */
    @SuppressWarnings("deprecation")
	public static synchronized Object getObjectByPrimaryKey(String className, Object primaryKey) throws ObjectDoesNotExistException {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	Criterion c = Restrictions.idEq(primaryKey);
    	cr.add(c);
    	List<?> results = cr.list();
    	session.close();
    	if (results.size() == 0) {
    		throw new ObjectDoesNotExistException(className, primaryKey.toString());
    	}
    	return results.get(0);
    }
    
    /**
     * Determines whether or not a specified primary key is already in use for 
     * a given object type.
     * @param className The name corresponding to the object's class
     * @param primaryKeyName The name of the object's primary key field
     * @param primaryKey The value of the object's primary key
     * @return True or false, depending on whether or not the primary key is in use
     */
    @SuppressWarnings("deprecation")
	public static synchronized boolean isPrimaryKeyUnique(String className, String primaryKeyName, String primaryKey) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	cr.add(Restrictions.eq(primaryKeyName, primaryKey));
    	List<?> results = cr.list();
    	if (results.size() != 0) {
    		session.close();
    		return false;
    	}
    	session.close();
    	return true;
    }

	public static synchronized void wipeAllData() {
		initIfRequired();
		Session session = factory.openSession();
		Transaction t = session.beginTransaction();
		session.createNativeQuery("DROP TABLE customeraccounts").executeUpdate();
		session.createNativeQuery("DROP TABLE bankaccounts").executeUpdate();
		session.createNativeQuery("DROP TABLE customerbankaccounts").executeUpdate();
		session.createNativeQuery("DROP TABLE debitcards").executeUpdate();
		session.createNativeQuery("DROP TABLE creditcards").executeUpdate();
		session.createNativeQuery("DROP TABLE transactions").executeUpdate();
		session.createNativeQuery("DROP TABLE savingsaccounts").executeUpdate();
		session.createNativeQuery("DROP TABLE creditaccounts").executeUpdate();
		session.createNativeQuery("DROP TABLE logs").executeUpdate();
		session.createNativeQuery("DROP TABLE timeevents").executeUpdate();
		t.commit();
		session.close();
		init();
		
		BankAccount.setUpINGaccount();
		CustomerAccount.setUpAdminAccount();
		
		// Set up time events
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, 1);
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		String date = year + "-" + month + "-01";
		TimeEvent transferLimitUpdateEvent = new TimeEvent();
		TimeEvent creditCardMonthlyResetEvent = new TimeEvent();
		transferLimitUpdateEvent.setName("TRANSFER_LIMIT_UPDATE");
		creditCardMonthlyResetEvent.setName("CREDIT_CARD_RESET");
		try {
			creditCardMonthlyResetEvent.setTimestamp(Logger.parseDateToMillis(date, "yyyy-MM-dd"));
			transferLimitUpdateEvent.setTimestamp(Logger.parseDateToMillis(date, "yyyy-MM-dd"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		creditCardMonthlyResetEvent.setExecuted(false);
		transferLimitUpdateEvent.setExecuted(false);
		creditCardMonthlyResetEvent.setDescription("Update the credit cards the base balance and subtract fees.");
		transferLimitUpdateEvent.setDescription("Update the transfer limits for concerning bank accounts");
		creditCardMonthlyResetEvent.saveToDB();
		transferLimitUpdateEvent.saveToDB();
		
	}
}
