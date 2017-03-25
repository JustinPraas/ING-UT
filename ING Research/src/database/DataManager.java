package database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import accounts.CustomerAccount;

public class DataManager {
	public static final String CFGPATH = "hibernate.cfg.xml";
	private static Configuration cfg;
	private static SessionFactory factory;
	
    public static void init() {
    	cfg = new Configuration();
    	cfg.configure(CFGPATH);
    	factory = cfg.buildSessionFactory();
    }
    
    public static void main(String[] args) {
    	init();
    	Session session = factory.openSession();
    	Transaction t = session.beginTransaction();
    	CustomerAccount john = new CustomerAccount("John", "Smith", "TEST", "103 Testings Ave.", "000-TEST", "johntest@testing.test", "TESTDATE", false);
    	session.persist(john);
    	t.commit();
    	session.close();
    }
}
