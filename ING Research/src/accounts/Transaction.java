package accounts;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DBObject;
import database.DataManager;

/**
 * A transaction model for logging purposes.
 * @author Andrei Cojocaru
 */
@Entity
@Table(name = "transactions")
public class Transaction implements DBObject, Comparable<Object> {
	private String sourceIBAN;
	private String destinationIBAN;
	private String dateTime;
	private long dateTimeMilis;
	private double amount;
	private String description;
	private int id;
	private String targetName;
	public static final String CLASSNAME = "accounts.Transaction";
	public static final String PRIMARYKEYNAME = "id";
	
	public Transaction() {
		
	}
	
	
	
	public String toString() {
		String output = "";
		output += "==================================";
		output += "\nFROM: " + sourceIBAN;
		output += "\nTO: " + destinationIBAN;
		output += "\nAMOUNT: " + amount;
		output += "\nDESCRIPTION: " + description + "\n";
		output += "==================================";
		output += "\n";
		return output;
	}
	
	@Column(name = "source_IBAN")
	public String getSourceIBAN() {
		return sourceIBAN;
	}
	
	public void setSourceIBAN(String sourceIBAN) {
		this.sourceIBAN = sourceIBAN;
	}
	
	@Column(name = "destination_IBAN")
	public String getDestinationIBAN() {
		return destinationIBAN;
	}
	
	public void setDestinationIBAN(String destinationIBAN) {
		this.destinationIBAN = destinationIBAN;
	}
	
	@Column(name = "date_time")
	public String getDateTime() {
		return dateTime;
	}
	
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
	@Column(name = "amount")
	public double getAmount() {
		return amount;
	}
	
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	@Column(name = "description")
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	@Override
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Override
	@Transient
	public Integer getPrimaryKeyVal() {
		return new Integer(this.id);
	}

	@Override
	@Transient
	public String getClassName() {
		return CLASSNAME;
	}

	@Override
	public void saveToDB() {
		DataManager.save(this);
	}

	@Override
	public void deleteFromDB() {
		DataManager.removeEntryFromDB(this);
	}

	@Column(name = "target_name")
	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	@Column(name = "date_time_milis")
	public long getDateTimeMilis() {
		return dateTimeMilis;
	}

	public void setDateTimeMilis(long dateTimeMilis) {
		this.dateTimeMilis = dateTimeMilis;
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof Transaction)) {
			return 0;
		}
		
		Transaction t = (Transaction) o;
		
		if (this.dateTimeMilis < t.dateTimeMilis) {
			return -1;
		} else if (this.dateTimeMilis > t.dateTimeMilis) {
			return 1;
		} else {
			return 0;
		}
	}
}
