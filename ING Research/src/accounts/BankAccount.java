package accounts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import database.DataManager;

import java.math.BigInteger;

import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;

/**
 * A simple model of a bank account in an abstract currency.
 * @author Andrei Cojocaru
 */
@Entity
@Table(name = "bankaccounts")
public class BankAccount implements database.DBObject {
	private final static String COUNTRY_CODE = "NL";
	private final static String BANK_CODE = "INGB";
	
	private float balance;
	private String IBAN;
	private String mainHolderBSN;
	private Set<CustomerAccount> owners = new HashSet<CustomerAccount>();
	boolean closed;
	public static final String CLASSNAME = "accounts.BankAccount";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	public String toString() {
		String output = "";
		String ownerBSNs = "";
		
		for (CustomerAccount key : this.getOwners()) {
			ownerBSNs += key.getBSN() + "; ";
		}
		
		output += "IBAN: " + IBAN;
		output += "\nHolder BSN: " + mainHolderBSN;
		output += "\nBalance: " + balance;
		output += "\nCustomers with access: " + ownerBSNs + "\n";
		return output;
	}
	
	public BankAccount() {
		
	}
	
	/**
	 * Create a new <code>BankAccount</code> with a specific account holder and an initial balance of 0.
	 * Adds it to the banking database with a newly-generated IBAN as primary key.
	 * @param holder The <code>CustomerAccount</code> considered to be the main holder of this <code>BankAccount</code>
	 */
	public BankAccount(String mainHolderBSN) {
		this.balance = 0;
		this.IBAN = generateIBAN(COUNTRY_CODE, BANK_CODE, randomPAN());
		this.mainHolderBSN = mainHolderBSN;
	}
	
	/**
	 * Constructs a new <code>BankAccount</code> with specific field values. Used to
	 * load a <code>BankAccount</code> from the SQLite database.
	 * @param mainHolderBSN The BSN of the main <code>CustomerAccount</code> this account is paired to
	 * @param balance The account's balance
	 * @param holders The list of all <code>CustomerAccounts</code> that can access this account
	 * @param IBAN The account's IBAN
	 */
	public BankAccount(String mainHolderBSN, float balance, String IBAN) {
		this.mainHolderBSN = mainHolderBSN;
		this.balance = balance;
		this.IBAN = IBAN;
	}
	
	public void addOwner(CustomerAccount owner) {
		owners.add(owner);
	}
	
	/**
	 * Generates a random personal account number of 10 digits long.
	 * @return personalAccountNumber.toString() The personal accountNumber for the IBAN
	 */
	private String randomPAN() {
		//The personalAccountNumber (last 10 digits) should be hierarchically distributed.
		//However, we do not keep track of used numbers yet, so for now assign 10 random digits
		StringBuilder personalAccountNumber = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			personalAccountNumber.append((int) (Math.random() * 10));
		}	
		
		return personalAccountNumber.toString();
	}

	/**
	 * Generates an IBAN for this BankAccount
	 * @param countryCode The code that represents a country (e.g. NL, BE, DE, etc.)
	 * @param bankCode The code that represents the Bank (e.g. INGB, ABNA, etc.)
	 * @param pan The personal account number of the BankAccount
	 * @return resultIBAN The IBAN 
	 */
	public static String generateIBAN(String countryCode, String bankCode, String pan) {
		boolean unique = false;
		String resultIBAN = null;
		while (!unique) {
			// Compute the controlNumber for this IBAN
			int controlNumber = generateControlNumber(countryCode, bankCode, pan.toString());
			
			// If the control number consists of 1 digit, prepend a 0
			String controlNumberString = controlNumber < 10 ? "0" + controlNumber : "" + controlNumber;	
			
			// Concatenate all parts of the IBAN to a complete IBAN	
			resultIBAN = countryCode + controlNumberString + bankCode + pan;
			
			// If the IBAN isn't already in use, we can continue
			if (DataManager.isPrimaryKeyUnique(new BankAccount().getClassName(), new BankAccount().getPrimaryKeyName(), resultIBAN)) {
				unique = true;
			}
		}
		
		return resultIBAN;
	}
	
	/**
	 * Generates the controlNumber for the IBAN (3rd and 4th digit).
	 * @param countryCode The code that represents a country (e.g. NL, BE, DE, etc.)
	 * @param bankCode The code that represents the Bank (e.g. INGB, ABNA, etc.)
	 * @param pan The personal account number (the last 10 digits)
	 * @return controlNumber The controlNumber that belongs to the combination of 
	 * countryCode, BankCode and pan
	 * @see <a href="http://www.ibannl.org/uitleg-over-iban/">Formula for the control number (Dutch)</a>
	 */
	public static int generateControlNumber(String countryCode, String bankCode, String pan) {
		/* 
		 * Formula in short:
		 * String a = bank code converted to digits (e.g. 'A' = 10, 'B' = 11, 'Z' = 35, etc.)
		 * String b = personal account number (10 digits, prepend with 0's if needed)
		 * String c = country code converted to digits (see String a)
		 * BigInteger value = parse the following to a BigInteger: (a + b + c)
		 * int controlNumber = 98 - (value % 97)
		 */
		
		//Convert the bankCode to digits
		StringBuilder bankCodeInDigits = new StringBuilder();
		for (Character c : bankCode.toCharArray()) {
			bankCodeInDigits.append(Character.getNumericValue(c));
		} 
		
		//Convert the countryCode to digits
		StringBuilder countryCodeInDigits = new StringBuilder();
		for (Character c : countryCode.toCharArray()) {
			countryCodeInDigits.append(Character.getNumericValue(c));
		}
		
		//A BigInteger is necessary for a value of that does not fit a long
		BigInteger computationValue = new BigInteger(bankCodeInDigits + pan + countryCodeInDigits + "00");
		
		//Get the remainder of computationValue % 97
		computationValue = computationValue.mod(new BigInteger("97"));
		
		//Subtract the computationValue from 98, this results in the controlNumber of the debitcard
		int controlNumber = 98 - computationValue.intValue();
		
		return controlNumber;
	}
	
	/**
	 * Deposit a specific sum of money into the <code>BankAccount</code>.
	 * @param amount The amount of money to be deposited
	 */
	public void deposit(float amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		this.debit(amount);
		
		Calendar c = Calendar.getInstance();
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDestinationIBAN(this.getIBAN());
		t.setAmount(amount);
		t.setDescription("Physical deposit.");
		//TODO: Only commit once all operations have succeeded
		t.saveToDB();
		this.saveToDB();
	}
	
	/**
	 * Transfers a specific amount of money from this <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 */
	public void transfer(BankAccount destination, float amount) throws IllegalAmountException, IllegalTransferException {
		//TODO: Only commit once all operations have succeeded
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance < amount || destination.getClosed()) {
			throw new IllegalTransferException(balance, IBAN, amount);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new IllegalTransferException(balance, IBAN, amount);
		}
		
		this.credit(amount);
		destination.debit(amount);
		Calendar c = Calendar.getInstance();
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(destination.getIBAN());
		t.setAmount(amount);
		t.setDescription("Transfer to " + destination.getIBAN() + ".");
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
	}
	
	/**
	 * Transfers a specific amount of money from this <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @param description Description of the transfer
	 */
	public void transfer(BankAccount destination, float amount, String description) throws IllegalAmountException, IllegalTransferException {
		//TODO: Only commit once all operations have succeeded
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance < amount || destination.getClosed()) {
			throw new IllegalTransferException(balance, IBAN, amount);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new IllegalTransferException(balance, IBAN, amount);
		}
		
		this.credit(amount);
		destination.debit(amount);
		Calendar c = Calendar.getInstance();
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(destination.getIBAN());
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
	}
	
	@Column(name = "balance")
	public float getBalance() {
		return balance;
	}
	
	/**
	 * Credits a <code>BankAccount</code> with a specific amount of money
	 * @param amount The amount of money to credit the <code>BankAccount</code> with
	 * @throws IllegalAmountException Thrown when the specified amount is 0 or negative
	 */
	public void credit(float amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance -= amount;
	}
	
	/**
	 * Debits a <code>BankAccount</code> with a specific amount of money
	 * @param amount The amount of money to debit the <code>BankAccount</code> with
	 * @throws Thrown when the specified amount is 0 or negative
	 */
	public void debit(float amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance += amount;
	}
	
	public void setIBAN(String IBAN) {
		this.IBAN = IBAN; 
	}
	
	public void setBalance(float balance) {
		this.balance = balance; 
	}
	
	public void setMainHolderBSN(String BSN) {
		mainHolderBSN = BSN;
	}
	
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	@Id
	@Column(name = "IBAN")
	public String getIBAN() {
		return IBAN;
	}
	
	@Column(name = "customer_BSN")
	public String getMainHolderBSN() {
		return mainHolderBSN;
	}
	
	@Column(name = "closed")
	public boolean getClosed() {
		return closed;
	}
	
	@Transient
	public String getPrimaryKeyName() {
		return PRIMARYKEYNAME;
	}
	
	@Transient
	public String getPrimaryKeyVal() {
		return IBAN;
	}
	
	@Transient
	public String getClassName() {
		return CLASSNAME;
	}

	@ManyToMany(fetch = FetchType.EAGER, mappedBy = "bankAccounts")
	public Set<CustomerAccount> getOwners() {
		return owners;
	}
	
	@SuppressWarnings("unchecked")
	@Transient
	public List<DebitCard> getDebitCards() {
		List<DebitCard> result = new ArrayList<>();
		ArrayList<Criterion> criteria = new ArrayList<>();
		criteria.add(Restrictions.eq("bankAccountIBAN", getPrimaryKeyVal()));
		result = DataManager.getObjectsFromDB(new DebitCard().getClassName(), criteria);
		return result;
	}

	public void setOwners(Set<CustomerAccount> owners) {
		this.owners = owners;
	}
	
	public void saveToDB() {
		DataManager.save(this);
	}
	
	public void deleteFromDB() {
		for (DebitCard key : getDebitCards()) {
			key.deleteFromDB();
		}
		DataManager.removeEntryFromDB(this);
	}
}
