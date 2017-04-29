package exceptions;

/**
 * Thrown when a transfer is attempted with a source <code>BankAccount</code> that does not have 
 * a sufficient balance to cover the chosen transfer amount, or a transfer from a bank account to itself/a closed bank account.
 * @author Andrei Cojocaru
 */
public class IllegalTransferException extends Exception {
	private static final long serialVersionUID = -8284837193637155915L;
	
	public IllegalTransferException() {
		
	}
	
	public String toString() {
		return "Attempted illegal transfer.";
	}
}
