package testing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import accounts.BankAccount;
import accounts.Transaction;
import cards.DebitCard;
import database.DataManager;
import exceptions.ExceedLimitException;
import exceptions.ExpiredCardException;
import exceptions.InvalidPINException;
import exceptions.ObjectDoesNotExistException;

public class DebitCardTest {
	public DebitCard card;
	
	@Before
	public void createDebitCard() {
		card = new DebitCard("TESTBSN", "TESTIBAN");
		System.out.println("Generated:");
		System.out.println(card.toString());
	}
	
	@Test
	public void testExpiration() {
		assert(!card.isExpired());
		DebitCard card2 = new DebitCard("TEST1", "TEST2", "17-02-1995", "999", "4444");
		assert(card2.isExpired());
	}
	
	@Test
	public void testPINValidation() {
		DebitCard card2 = new DebitCard("TEST1", "TEST2", "17-02-1995", "999", "4444");
		assert(card2.isValidPIN("4444"));
		assert(!card2.isValidPIN("4443"));
	}
	
	@Test
	public void testPinMachineCharge() {
		BankAccount bAcc = new BankAccount("TEST1", 10000, "TEST2");
		BankAccount bAcc2 = new BankAccount("TEST3", 0, "TEST4");
		bAcc.saveToDB();
		bAcc2.saveToDB();
		DebitCard card2 = new DebitCard("TEST1", "TEST2", "2022-04-30", "999", "4444");
		try {
			card2.pinPayment(1000, "3444", bAcc2);
		} catch (InvalidPINException e) {
			
		} catch (ExpiredCardException e) {
			
		} catch (ExceedLimitException e) {

		}
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAcc.getIBAN());
			bAcc2 = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAcc2.getIBAN());
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
		}
		
		assert(bAcc.getBalance() == 10000 && bAcc2.getBalance() == 0);
		try {
			card2.pinPayment(1000, "4444", bAcc2);
		} catch (InvalidPINException e) {
			
		} catch (ExpiredCardException e) {
			
		} catch (ExceedLimitException e) {

		}
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAcc.getIBAN());
			bAcc2 = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAcc2.getIBAN());
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
		}

		assertEquals(10000, bAcc.getBalance(), 0.00000000001);
		assertEquals(0, bAcc2.getBalance(), 0.00000000001);
		
		try {
			card2.pinPayment(250, "4444", bAcc2);
		} catch (InvalidPINException e) {
			
		} catch (ExpiredCardException e) {
			
		} catch (ExceedLimitException e) {

		}
		try {
			bAcc = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAcc.getIBAN());
			bAcc2 = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, bAcc2.getIBAN());
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
		}		

		assert(bAcc.getBalance() == 9750 && bAcc2.getBalance() == 250);
		
		bAcc.deleteFromDB();
		bAcc2.deleteFromDB();
	}
	
	@AfterClass
	public static void cleanup() {
		@SuppressWarnings("unchecked")
		ArrayList<Transaction> ts = (ArrayList<Transaction>) DataManager.getObjectsFromDB(Transaction.CLASSNAME);
		
		for (Transaction t : ts) {
			t.deleteFromDB();
		}
	}
}
