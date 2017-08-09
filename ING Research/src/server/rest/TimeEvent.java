package server.rest;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DataManager;

@Entity
@Table(name = "timeevents")
public class TimeEvent implements database.DBObject {

	public static final String CLASSNAME = "server.rest.TimeEvent";
	public static final String PRIMARYKEYNAME = "name";
	
	public static final String UPDATE_TRANSFER_LIMITS = "transferLimitUpdate";

	private int ID;
	private String name;
	private long timestamp;
	private String description;
	private boolean executed;
	
	public TimeEvent() {
		
	}
	
	public TimeEvent(String name, long timestamp, String description, boolean executed) {
		this.name = name;
		this.timestamp = timestamp;
		this.description = description;
		this.executed = executed;
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name = "ID")
	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "timestamp")
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Column(name = "description")
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "executed")
	public boolean isExecuted() {
		return executed;
	}

	public void setExecuted(boolean executed) {
		this.executed = executed;
	}

	@Transient
	public Calendar getCalendar() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timestamp);
		return c;
	}
	
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Transient
	public Object getPrimaryKeyVal() {
		return name;
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
