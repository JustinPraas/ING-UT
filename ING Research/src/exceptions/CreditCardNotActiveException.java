package exceptions;

public class CreditCardNotActiveException extends Exception {

	private static final long serialVersionUID = -3475328137709696914L;
	String card;
	
	public CreditCardNotActiveException(String card) {
		this.card = card;
	}
	
	public String toString() {
		return "Creditcard with number " + card + " is inactive and cannot be used.";
	}
}
