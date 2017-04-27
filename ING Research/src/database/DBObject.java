package database;

/**
 * A common interface for all persistent objects.
 * @author Andrei Cojocaru
 */
public interface DBObject {
	public String getPrimaryKeyName();
	public Object getPrimaryKeyVal();
	public String getClassName();
	public void saveToDB();
	public void deleteFromDB();
}
