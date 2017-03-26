package database;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Criterion;
import org.hibernate.query.Query;

import accounts.CustomerAccount;

public class DataManager {
	public static final String CFGPATH = "hibernate.cfg.xml";
	private static Configuration cfg;
	private static SessionFactory factory;
	private static boolean initialized = false;
	
    public static void init() {
    	cfg = new Configuration();
    	cfg.configure(CFGPATH);
    	factory = cfg.buildSessionFactory();
    }
    
	private static void initIfRequired() {
		if (!initialized) {
    		init();
    		initialized = true;
    	}
	}
    
    public static void addEntryToDB(DBObject o) {
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
    	if (cr.list().size() != 0) {
    		return true;
    	}
    	return false;
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
    
    public static void main(String[] args) {
    	CustomerAccount john = new CustomerAccount("John", "Smith", "TEST", "103 Testings Ave.", "000-TEST", "johntest@testing.test", "TESTDATE", false);
    	System.out.println(objectExists(john));
    	addEntryToDB(john);
    	System.out.println(objectExists(john));
    	removeEntryFromDB(john);
    	System.out.println(objectExists(john));
    }
}
