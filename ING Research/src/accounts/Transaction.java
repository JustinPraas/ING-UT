package accounts;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import database.DBObject;
import database.DataManager;

@Entity
@Table(name = "transactions")
public class Transaction implements DBObject{
	private String sourceIBAN;
	private String destinationIBAN;
	private String dateTime;
	private float amount;
	private String description;
	private int id;
	
	public Transaction() {
		
	}
	
	public String toString() {
		return "UNFINISHED";
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
	public float getAmount() {
		return amount;
	}
	
	public void setAmount(float amount) {
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
	@GeneratedValue(strategy = GenerationType.AUTO)
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String getPrimaryKeyName() {
		return "id";
	}

	@Override
	public String getPrimaryKeyVal() {
		return Integer.toString(this.id);
	}

	@Override
	public String getClassName() {
		return "accounts.Transaction";
	}

	@Override
	public void saveToDB() {
		DataManager.save(this);
	}

	@Override
	public void deleteFromDB() {
		DataManager.removeEntryFromDB(this);
	}
}
