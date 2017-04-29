package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import accounts.BankAccount;
import accounts.CustomerAccount;
import accounts.DebitCard;
import database.DataManager;

/**
 * A simple class which purpose is storing a UI session.
 * @author Justin Praas
 * @version 2nd of April, 2017
 */
public class Session {
	
	/**
	 * Possible states of the UI
	 */
	public enum State {
		LOGGED_OUT,
		CUST_LOGGED_IN,
		BANK_LOGGED_IN;
	}

	public CustomerAccount customerAccount;
	public BankAccount bankAccount;
	public State state;	
	public List<BankAccount> bankAccountList;
	public List<DebitCard> debitCardList;

	/**
	 * Creates a session object, storing session details for the UI.
	 */
	public Session() {
		state = State.LOGGED_OUT;
		bankAccountList = new ArrayList<>();
		debitCardList = new ArrayList<>();
		bankAccount = null;
		customerAccount = null;
	}
	
	/**
	 * Resets the <code>Session</code> object to the original state.
	 */
	public void reset() {
		state = State.LOGGED_OUT;
		bankAccountList = new ArrayList<>();
		debitCardList = new ArrayList<>();
		bankAccount = null;
		customerAccount = null;
	}
	
	/**
	 * Signs in to the given <code>CustomerAccount</code>.
	 * @param customerAccount the <code>CustomerAccount</code> to be signed in
	 */
	public void loginCustomer(CustomerAccount customerAccount) {
		this.customerAccount = customerAccount;	
		this.state = State.CUST_LOGGED_IN;
		
		// Put this customer's bank-accounts in the session bankAccounts map.
		Iterator<BankAccount> it = customerAccount.getBankAccounts().iterator();
		while (it.hasNext()) {
			BankAccount bankAccount = it.next();
			if (!bankAccount.getClosed()) {
				bankAccountList.add(bankAccount);
			}
		}
	}
	
	/**
	 * Signs in to the given <code>BankAccount</code>.
	 * @param bankAccount the <code>BankAccount</code> to be signed in
	 */
	public void loginBank(BankAccount bankAccount) {
		this.bankAccount = bankAccount;
		this.state = State.BANK_LOGGED_IN;
		ArrayList<Criterion> criteria = new ArrayList<>();
		criteria.add(Restrictions.eq("bankAccountIBAN", bankAccount.getIBAN()));
		criteria.add(Restrictions.eq("holderBSN", customerAccount.getBSN()));
		debitCardList = DataManager.getObjectsFromDB(DebitCard.CLASSNAME, criteria);
	}
	
	/**
	 * Signs out the session's <code>CustomerAccount</code>.
	 */
	public void logoutCustomer() {
		reset();
	}
	
	/**
	 * Signs out the session's <code>BankAccount</code>.
	 */
	public void logoutBank() {
		this.bankAccount = null;
		this.state = State.CUST_LOGGED_IN;
	}

}
