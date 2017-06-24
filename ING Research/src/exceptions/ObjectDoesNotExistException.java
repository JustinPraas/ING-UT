package exceptions;

public class ObjectDoesNotExistException extends Exception {

	private static final long serialVersionUID = 6514167236433722258L;
	String className, primaryKey;
	
	public ObjectDoesNotExistException(String className, String primaryKey) {
		this.className = className;
		this.primaryKey = primaryKey;
	}
	
	public String toString() {
		return "The object of class " + className + " with primary key " + primaryKey + " does not exist in the database.";
	}
}
