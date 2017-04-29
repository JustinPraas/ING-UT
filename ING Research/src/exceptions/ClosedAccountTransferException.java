package exceptions;

public class ClosedAccountTransferException extends IllegalTransferException {
	private static final long serialVersionUID = -3217088338389635565L;

	public String toString() {
		return "Source or destination account closed.";
	}
}
