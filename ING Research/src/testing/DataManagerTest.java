package testing;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;
import database.DataManager;

public class DataManagerTest {
	public static CustomerAccount cust1;
	public static CustomerAccount cust2;
	public static BankAccount bAcc1;
	public static BankAccount bAcc2;
	public static DebitCard card1;
	public static DebitCard card2;
	public static String customer1BSN = "TEST";
	public static String customer2BSN = "TEST2";
	
	
	@Before
	public void setUp() throws Exception {
		cust1 = new CustomerAccount("John", "Test", customer1BSN, "103 Testings Ave.", "000-TEST", "johntest@testing.test", "TESTDATE");
		cust2 = new CustomerAccount("Jane", "Test", customer2BSN, "104 Testings Ave.", "001-TEST", "janetest@testing.test", "TESTDATE2");
		bAcc1 = new BankAccount(customer1BSN);
		bAcc2 = new BankAccount(customer2BSN);
		card1 = new DebitCard(customer1BSN, bAcc2.getIBAN());
		card2 = new DebitCard(customer2BSN, bAcc2.getIBAN());
		cust1.addBankAccount(bAcc1);
		cust2.addBankAccount(bAcc2);
		cust1.saveToDB();
		cust2.saveToDB();
		card1.saveToDB();
		card2.saveToDB();
	}

	@After
	public void tearDown() throws Exception {
		cust1.deleteFromDB();
		cust2.deleteFromDB();
	}
	
	@Test
	public void testIsPrimaryKeyUnique() {
		assertFalse(DataManager.isPrimaryKeyUnique(cust1.getClassName(), cust1.getPrimaryKeyName(), cust1.getPrimaryKeyVal()));
		assertFalse(DataManager.isPrimaryKeyUnique(cust2.getClassName(), cust2.getPrimaryKeyName(), cust2.getPrimaryKeyVal()));
		assertFalse(DataManager.isPrimaryKeyUnique(bAcc1.getClassName(), bAcc1.getPrimaryKeyName(), bAcc1.getPrimaryKeyVal()));
		assertFalse(DataManager.isPrimaryKeyUnique(bAcc2.getClassName(), bAcc2.getPrimaryKeyName(), bAcc2.getPrimaryKeyVal()));
		assertFalse(DataManager.isPrimaryKeyUnique(card1.getClassName(), card1.getPrimaryKeyName(), card1.getPrimaryKeyVal()));
		assertFalse(DataManager.isPrimaryKeyUnique(card2.getClassName(), card2.getPrimaryKeyName(), card2.getPrimaryKeyVal()));
	}
	
	@Test
	public void testObjectExists() {
		assertTrue(DataManager.objectExists(cust1));
		assertTrue(DataManager.objectExists(cust2));
		assertTrue(DataManager.objectExists(bAcc1));
		assertTrue(DataManager.objectExists(bAcc2));
		assertTrue(DataManager.objectExists(card1));
		assertTrue(DataManager.objectExists(card2));
		assertFalse(DataManager.objectExists(new DebitCard()));
	}
	
	@Test
	public void testDeleteObject() {
		assertTrue(DataManager.objectExists(cust2));
		assertTrue(DataManager.objectExists(bAcc2));
		assertTrue(DataManager.objectExists(card1));
		assertTrue(DataManager.objectExists(card2));
		cust2.deleteFromDB();
		assertFalse(DataManager.objectExists(cust2));
		assertFalse(DataManager.objectExists(bAcc2));
		assertFalse(DataManager.objectExists(card1));
		assertFalse(DataManager.objectExists(card2));
	}
	
	// TODO Below test works only with an empty database. Fix later
//	@Test
//	public void getObjectsFromDBTest() {
//		assertTrue(DataManager.getObjectsFromDB(cust1.getClassName()).size() == 2);
//		Criterion c = Restrictions.idEq(cust1.getPrimaryKeyVal());
//		ArrayList<Criterion> criteria = new ArrayList<>();
//		criteria.add(c);
//		assertTrue(DataManager.getObjectsFromDB(cust1.getClassName(), criteria).size() == 1);
//		cust1.deleteFromDB();
//		assertTrue(DataManager.getObjectsFromDB(cust1.getClassName()).size() == 1);
//		assertTrue(DataManager.getObjectsFromDB(cust1.getClassName(), criteria).size() == 0);
//		cust2.deleteFromDB();
//		assertTrue(DataManager.getObjectsFromDB(cust1.getClassName()).size() == 0);
//	}
}
