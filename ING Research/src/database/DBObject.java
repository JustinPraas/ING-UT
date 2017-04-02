package database;

public interface DBObject {
	public String getPrimaryKeyName();
	public String getPrimaryKeyVal();
	public String getClassName();
	public void saveToDB();
	public void deleteFromDB();
}
