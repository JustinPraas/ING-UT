package database;

import java.util.ArrayList;
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
import accounts.CustomerAccount;
import accounts.DebitCard;

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
    public static void init() {
    	SQLiteDB.initializeDB();
    	cfg = new Configuration();
    	cfg.configure(CFGPATH);
    	cfg.addAnnotatedClass(DebitCard.class);
    	cfg.addAnnotatedClass(BankAccount.class);
    	cfg.addAnnotatedClass(CustomerAccount.class);
    	cfg.addAnnotatedClass(accounts.Transaction.class);
    	factory = cfg.buildSessionFactory();
    }
    
	private static void initIfRequired() {
		if (!initialized) {
    		init();
    		initialized = true;
    	}
	}
    
	/**
	 * Add a new instance of a DBObject to the database.
	 * @param o The object to be added
	 */
    private static void addEntryToDB(DBObject o) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Transaction t = session.beginTransaction();
    	session.persist(o);
    	t.commit();
    	session.close();
    }

    /**
     * Remove a persistent object from the database.
     * @param o The object to be removed
     */
    public static void removeEntryFromDB(DBObject o) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Transaction t = session.beginTransaction();
    	String HQL = "delete " + o.getClassName() + " where " + o.getPrimaryKeyName() + " = '" + o.getPrimaryKeyVal() + "'";
    	Query query = session.createQuery(HQL);
    	query.executeUpdate();
    	t.commit();
    	session.close();
    }
    
    /**
     * Determines whether or not a given object already exists in the database.
     * @param o Object to be looked up
     * @return True or false, depending on whether or not the object is found
     */
    public static boolean objectExists(DBObject o) {
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
	public static void save(DBObject o) {
		initIfRequired();
		
		Session session = factory.openSession();
		Transaction t = session.beginTransaction();
		
		if (objectExists(o)) {
			session.merge(o);
		} else {
			addEntryToDB(o);
		}
		
		t.commit();
		session.close();
	}
    
	/**
	 * Locates all objects of the specified kind that meet the given criteria in the DB.
	 * @param className The name of the type of object being queried
	 * @param criteria The query criteria
	 * @return A List of all objects meeting the given criteria
	 */
    public static List getObjectsFromDB(String className, ArrayList<Criterion> criteria) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	for (Criterion c : criteria) {
    		cr.add(c);
    	}
    	List results = cr.list();
    	session.close();
    	return results;
    }
    
    /**
     * Returns a List of all objects of the specified type that exist in the DB.
     * @param className The name of the type of object being queried
     * @return A List of all objects of the given type
     */
    public static List getObjectsFromDB(String className) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	List results = cr.list();
    	session.close();
    	return results;
    }
    
    /**
     * Finds a persistent object in the database by primary key.
     * @param className The name of the type of object being queried
     * @param primaryKey The primary key of the desired object
     * @return The object with the given primary key, if found
     */
    public static Object getObjectByPrimaryKey(String className, Object primaryKey) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	Criterion c = Restrictions.idEq(primaryKey);
    	cr.add(c);
    	List results = cr.list();
    	session.close();
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
    public static boolean isPrimaryKeyUnique(String className, String primaryKeyName, String primaryKey) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	cr.add(Restrictions.eq(primaryKeyName, primaryKey));
    	List results = cr.list();
    	if (results.size() != 0) {
    		session.close();
    		return false;
    	}
    	session.close();
    	return true;
    }
}
