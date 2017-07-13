package server.rest;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
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

	private String name;
	private long timestamp;
	private String description;
	
	public TimeEvent() {
		
	}
	
	public TimeEvent(String name, long timestamp, String description) {
		this.name = name;
		this.timestamp = timestamp;
		this.description = description;
	}
	
	@Id
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
	
	@Transient
	public Calendar getCalendar() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(timestamp);
		return c;
	}

	public void setDescription(String description) {
		this.description = description;
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
