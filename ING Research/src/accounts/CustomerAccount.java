package accounts;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import database.DataManager;

/**
 * A bank customer's main account, to which multiple <code>BankAccounts</code> may be tied.
 * @author Andrei Cojocaru
 */
@Entity
@Table(name = "customeraccounts")
public class CustomerAccount implements database.DBObject {
	private String name;
	private String surname;
	private String BSN;
	private String streetAddress;
	private String phoneNumber;
	private String email;
	private String birthdate;
	private String initials;
	private String username;
	private String password;
	private Set<BankAccount> bankAccounts = new HashSet<BankAccount>();
	public static final String CLASSNAME = "accounts.CustomerAccount";
	public static final String PRIMARYKEYNAME = "BSN";
	
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
	public CustomerAccount(String name, String surname, String initials, String BSN, String streetAddress, String phoneNumber, 
			String email, String birthdate, String username, String password) {
		this.setName(name);
		this.setSurname(surname);
		this.setBSN(BSN);
		this.setStreetAddress(streetAddress);
		this.setPhoneNumber(phoneNumber);
		this.setEmail(email);
		this.setBirthdate(birthdate);
		this.setInitials(initials);
		this.setUsername(username);
		this.setPassword(password);
	}
	
	public CustomerAccount() {
		
	}
	
	/**
	 * Open a new <code>BankAccount</code> in this holder's name.
	 * Adds this and the respective association to the database.
	 */
	public BankAccount openBankAccount() {
		BankAccount newAccount = new BankAccount(getBSN());
		addBankAccount(newAccount);
		return newAccount;
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
		bankAccounts.remove(account);
	}

	@Column(name = "initials")
	public String getInitials() {
		return initials;
	}
	
	public void setInitials(String initials) {
		this.initials = initials;
	}
	
	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "surname")
	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	@Id
	@Column(name = "customer_BSN")
	public String getBSN() {
		return BSN;
	}

	public void setBSN(String bSN) {
		BSN = bSN;
	}

	@Column(name = "street_address")
	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	@Column(name = "phone_number")
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Column(name = "email")
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Column(name = "birth_date")
	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}
	
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}
	
	@Transient
	public String getPrimaryKeyVal() {
		return BSN;
	}
	
	@Transient
	public String getClassName() {
		return CLASSNAME;
	}

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "customerbankaccounts", joinColumns = {
			@JoinColumn(name = "customer_BSN", nullable = false, updatable = false)}, inverseJoinColumns = {
					@JoinColumn(name = "IBAN", nullable = false, updatable = false)})
	public Set<BankAccount> getBankAccounts() {
		return bankAccounts;
	}

	public void setBankAccounts(Set<BankAccount> bankAccounts) {
		this.bankAccounts = bankAccounts;
	}
	
	public void saveToDB() {
		DataManager.save(this);
	}
	
	public void deleteFromDB() {
		for (BankAccount key : getBankAccounts()) {
			key.deleteFromDB();
		}
		DataManager.removeEntryFromDB(this);
	}

	@Column (name = "username")
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Column (name = "password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
