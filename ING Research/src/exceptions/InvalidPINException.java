package exceptions;

public class InvalidPINException extends Exception {
	private static final long serialVersionUID = -6904621156696972537L;
	String PIN, card;
	
	public InvalidPINException(String PIN, String card) {
		this.PIN = PIN; this.card = card;
	}
	
	public String toString() {
		return "PIN " + PIN + " is not valid for card " + card + ".";
	}
}
