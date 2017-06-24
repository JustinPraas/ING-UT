package exceptions;

public class PinCardBlockedException extends Exception {

	private static final long serialVersionUID = -6464757375808369414L;
	String card;
	
	public PinCardBlockedException(String card) {
		this.card = card;
	}
	
	public String toString() {
		return "Pincard with number " + card + " is blocked and cannot be used.";
	}
}
