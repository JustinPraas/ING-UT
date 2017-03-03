package banking;

import java.util.Date;
import java.util.HashSet;

/**
 * A bank customer's main account, to which multiple bank accounts may be tied.
 * @author Andrei Cojocaru
 */

public class CustomerAccount {
	private String name;
	private String surname;
	private String BSN;
	private String streetAddress;
	private String phoneNumber;
	private String email;
	private Date birthdate;
	private HashSet<BankAccount> bankAccounts;
	
	/**
	 * Create a new <code>CustomerAccount</code> with the given customer information.
	 * @param name The customer's name
	 * @param surname The customer's surname
	 * @param BSN The customer's BSN
	 * @param streetAddress The customer's street address
	 * @param phoneNumber The customer's phone number
	 * @param email The customer's email
	 * @param birthdate The customer's date of birth
	 */
	public CustomerAccount(String name, String surname, String BSN, String streetAddress, String phoneNumber, 
			String email, Date birthdate) {
		bankAccounts = new HashSet<BankAccount>();
		this.name = name;
		this.surname = surname;
		this.BSN = BSN;
		this.streetAddress = streetAddress;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.birthdate = birthdate;
	}
	
	/**
	 * Open a new <code>BankAccount</code> in this holder's name.
	 */
	public void openBankAccount() {
		bankAccounts.add(new BankAccount(this));
	}
}
