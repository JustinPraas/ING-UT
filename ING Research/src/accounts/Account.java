package accounts;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import database.DataManager;

@MappedSuperclass
public abstract class Account implements database.DBObject {

	private String IBAN;
	private float balance; 
	private boolean closed;
	public static final String CLASSNAME = "accounts.Account";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	public Account() {
		
	}
	
	public Account(String IBAN, float balance, boolean closed) {
		this.IBAN = IBAN;
		this.balance = balance;
		this.closed = closed;
	}
	@Id
	@Column(name = "IBAN")
	public String getIBAN() {
		return IBAN;
	}

	public void setIBAN(String iBAN) {
		IBAN = iBAN;
	}

	@Column(name = "balance")
	public float getBalance() {
		return balance;
	}

	public void setBalance(float balance) {
		this.balance = balance;
	}

	@Column(name = "closed")
	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Transient
	public Object getPrimaryKeyVal() {
		return IBAN;
	}

	@Transient
	public abstract String getClassName();

	@Override
	public void saveToDB() {
		DataManager.save(this);;		
	}

	@Override
	public void deleteFromDB() {
		DataManager.removeEntryFromDB(this);
	}

}
