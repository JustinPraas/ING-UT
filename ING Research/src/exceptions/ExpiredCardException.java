package exceptions;

public class ExpiredCardException {
	String card, expDate;
	
	public ExpiredCardException(String card, String expDate) {
		this.card = card; this.expDate = expDate;
	}
	
	public String toString() {
		return "Card " + card + " expired on " + expDate + ".";
	}
}
