package exceptions;

public class PinCardBlockedException extends Exception {
	private static final long serialVersionUID = -6904621156696972537L;
	String card;
	
	public PinCardBlockedException(String card) {
		this.card = card;
	}
	
	public String toString() {
		return "Pincard with number " + card + " is blocked and cannot be used.";
	}
}
