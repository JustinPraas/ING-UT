package interesthandler;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DataManager;

@Entity
@Table(name = "balancereach")
public class BalanceReachObject implements database.DBObject {
	
	public static final String CLASSNAME = "interesthandler.BalanceReachObject";
	public static final String PRIMARYKEYNAME = "ID";

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name = "ID")
	private int ID;
	
	@Column(name = "type")
	private String type;
	
	@Column(name = "IBAN")
	private String IBAN;
	
	@Column(name = "balance")
	private double balance;
	
	public BalanceReachObject() {}
	
	public BalanceReachObject(String IBAN, String type, double balance) {
		this.IBAN = IBAN;
		this.type = type;
		this.balance = balance;
	}

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIBAN() {
		return IBAN;
	}

	public void setIBAN(String iBAN) {
		IBAN = iBAN;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Transient
	public Object getPrimaryKeyVal() {
		return ID;
	}

	@Transient
	public String getClassName() {
		return CLASSNAME;
	}

	public void saveToDB() {
		DataManager.save(this);
	}

	public void deleteFromDB() {
		DataManager.removeEntryFromDB(this);
	}
}
