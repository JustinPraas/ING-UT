package accounts;

import java.util.HashSet;
import java.util.Set;

/**
 * A bank customer's main account, to which multiple <code>BankAccounts</code> may be tied.
 * @author Andrei Cojocaru
 */

public class CustomerAccount implements database.DBObject {
	private String name;
	private String surname;
	private String BSN;
	private String streetAddress;
	private String phoneNumber;
	private String email;
	private String birthdate;
	private Set<BankAccount> bankAccounts = new HashSet<BankAccount>();
	
	/**
	 * Create a new <code>CustomerAccount</code> with the given customer information.
	 * @param name The customer's name
	 * @param surname The customer's surname
	 * @param BSN The customer's BSN
	 * @param streetAddress The customer's street address
	 * @param phoneNumber The customer's phone number
	 * @param email The customer's email
	 * @param birthdate The customer's date of birth
	 * @param addToDB Whether or not to add the newly-created customer account to the database
	 */
	public CustomerAccount(String name, String surname, String BSN, String streetAddress, String phoneNumber, 
			String email, String birthdate) {
		this.setName(name);
		this.setSurname(surname);
		this.setBSN(BSN);
		this.setStreetAddress(streetAddress);
		this.setPhoneNumber(phoneNumber);
		this.setEmail(email);
		this.setBirthdate(birthdate);
	}
	
	public CustomerAccount() {
		
	}
	
	/**
	 * Open a new <code>BankAccount</code> in this holder's name.
	 * Adds this and the respective association to the database.
	 */
	public void openBankAccount() {
		BankAccount newAccount = new BankAccount(getBSN());
		addBankAccount(newAccount);
	}
	
	/**
	 * Adds the <code>CustomerAccount</code> as an owner to a pre-existing
	 * <code>BankAccount</code>. Also adds the appropriate association to the
	 * database.
	 * @param account The <code>BankAccount</code> to be owned by the customer.
	 */
	public void addBankAccount(BankAccount account) {
		bankAccounts.add(account);
		account.addOwner(this);
	}
	
	/**
	 * Removes the <code>CustomerAccount</code>'s ownership of a given
	 * <code>BankAccount</code>.
	 * @param account The <code>BankAccount</code> to remove ownership of
	 */
	public void removeBankAccount(BankAccount account) {
		//TODO: Check if sole owner
		bankAccounts.remove(account);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getBSN() {
		return BSN;
	}

	public void setBSN(String bSN) {
		BSN = bSN;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}
	
	public String getPrimaryKeyName() {
		return "BSN";
	}
	
	public String getPrimaryKeyVal() {
		return BSN;
	}
	
	public String getClassName() {
		return "accounts.CustomerAccount";
	}

	public Set<BankAccount> getBankAccounts() {
		return bankAccounts;
	}

	public void setBankAccounts(Set<BankAccount> bankAccounts) {
		this.bankAccounts = bankAccounts;
	}
}

