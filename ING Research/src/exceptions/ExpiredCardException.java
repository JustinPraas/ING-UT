package exceptions;

public class ExpiredCardException extends Exception {
	private static final long serialVersionUID = -5804804620134879308L;
	String card, expDate;
	
	public ExpiredCardException(String card, String expDate) {
		this.card = card; this.expDate = expDate;
	}
	
	public String toString() {
		return "Card " + card + " expired on " + expDate + ".";
	}
}
