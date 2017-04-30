package exceptions;

public class SameAccountTransferException extends IllegalTransferException {
	private static final long serialVersionUID = -8596089376122511816L;

	public String toString() {
		return "Attempted transfer from bank account to itself.";
	}
}
