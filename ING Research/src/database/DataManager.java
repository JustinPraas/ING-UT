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
	
    public static void init() {
    	cfg = new Configuration();
    	cfg.configure(CFGPATH);
    	cfg.addAnnotatedClass(DebitCard.class);
    	cfg.addAnnotatedClass(BankAccount.class);
    	cfg.addAnnotatedClass(CustomerAccount.class);
    	factory = cfg.buildSessionFactory();
    }
    
	private static void initIfRequired() {
		if (!initialized) {
    		init();
    		initialized = true;
    	}
	}
    
    private static void addEntryToDB(DBObject o) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Transaction t = session.beginTransaction();
    	session.persist(o);
    	t.commit();
    	session.close();
    }

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
    
    public static boolean objectExists(DBObject o) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(o.getClassName());
    	cr.add(Restrictions.eq(o.getPrimaryKeyName(), o.getPrimaryKeyVal()));
    	if (cr.list().size() != 0) {
    		return true;
    	}
    	return false;
    }
    
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
    
    public static List getObjectsFromDB(String className, ArrayList<Criterion> criteria) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	for (Criterion c : criteria) {
    		cr.add(c);
    	}
    	List results = cr.list();
    	return results;
    }
    
    public static List getObjectsFromDB(String className) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	List results = cr.list();
    	return results;
    }
    
    public static boolean isPrimaryKeyUnique(String className, String primaryKeyName, String primaryKey) {
    	initIfRequired();
    	
    	Session session = factory.openSession();
    	Criteria cr = session.createCriteria(className);
    	cr.add(Restrictions.eq(primaryKeyName, primaryKey));
    	List results = cr.list();
    	
    	if (results.size() != 0) {
    		return false;
    	}
    	
    	return true;
    }
    
//    public static void main(String[] args) {
//    	CustomerAccount john = new CustomerAccount("John", "Test", "TEST", "103 Testings Ave.", "000-TEST", "johntest@testing.test", "TESTDATE");
//    	CustomerAccount jane = new CustomerAccount("Jane", "Test", "TEST2", "104 Testings Ave.", "001-TEST", "janetest@testing.test", "TESTDATE2");
//    	BankAccount bAcc = new BankAccount("TEST", 100, "TESTIBAN");
//    	BankAccount bAcc2 = new BankAccount("TEST2", 100, "TESTIBAN2");
//    	DebitCard card = new DebitCard("TEST", "TESTIBAN");
//    	john.saveToDB();
//    	jane.saveToDB();
//    	card.saveToDB();
//    	jane.addBankAccount(bAcc);
//    	john.addBankAccount(bAcc);
//    	jane.addBankAccount(bAcc2);
//    	save(john);
//    	save(jane);
//    	System.out.println(bAcc.getDebitCards().size());
//    	System.out.println(bAcc2.getDebitCards().size());
//    	john.deleteFromDB();
//    	jane.deleteFromDB();
//    	System.out.println(bAcc.getDebitCards().size());
//    	System.out.println(bAcc2.getDebitCards().size());
//    }
}
