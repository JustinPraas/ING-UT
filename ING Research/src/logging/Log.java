package logging;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DataManager;

@Entity
@Table(name = "log")
public class Log implements database.DBObject {
	
	private long timestamp;
	private String message;
	private Type type;
	
	public static final String CLASSNAME = "logging.Log";
	public static final String PRIMARYKEYNAME = "timestamp";
	
	public enum Type {SUCCESS, WARNING, ERROR, INFO}
	
	public Log() {
		
	}
	
	public Log(long timestamp, Type type, String message) {
		this.timestamp = timestamp;
		this.message = message;
		this.type = type;
	}

	@Column(name = "balance")
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Column(name = "message")
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Column(name = "type")
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}

	@Transient
	public Object getPrimaryKeyVal() {
		return timestamp;
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
