package accounts;

import java.util.Date;
import java.util.HashSet;

/**
 * A bank customer's main account, to which multiple <code>BankAccounts</code> may be tied.
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
		this.setName(name);
		this.setSurname(surname);
		this.setBSN(BSN);
		this.setStreetAddress(streetAddress);
		this.setPhoneNumber(phoneNumber);
		this.setEmail(email);
		this.setBirthdate(birthdate);
	}
	
	/**
	 * Open a new <code>BankAccount</code> in this holder's name.
	 */
	public void openBankAccount() {
		bankAccounts.add(new BankAccount(this));
	}
	
	public void addBankAccount(BankAccount account) {
		if (!bankAccounts.contains(account)) {
			bankAccounts.add(account);
		}
		//TODO: Throw exception if not?
	}
	
	public void removeBankAccount(BankAccount account) {
		if (bankAccounts.contains(account)) {
			bankAccounts.remove(account);
		} 
		//TODO: Maybe throw an exception in the other case?
		//TODO: Make sure this is not the sole account holder
	}
	
	public HashSet<BankAccount> getBankAccounts() {
		return bankAccounts;
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

	public Date getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}
}
