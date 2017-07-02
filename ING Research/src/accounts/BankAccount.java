package accounts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import client.Client;
import database.DataManager;

import java.math.BigInteger;

import exceptions.ClosedAccountTransferException;
import exceptions.ExceedOverdraftLimitException;
import exceptions.IllegalAccountCloseException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InsufficientFundsTransferException;
import exceptions.ObjectDoesNotExistException;
import exceptions.PinCardBlockedException;
import exceptions.SameAccountTransferException;
import server.rest.InterestHandler;

/**
 * A simple model of a bank account in an abstract currency.
 * @author Andrei Cojocaru
 */
@Entity
@Table(name = "bankaccounts")
public class BankAccount implements database.DBObject {
	private final static String COUNTRY_CODE = "NL";
	private final static String BANK_CODE = "INGB";
	public final static String ING_BANK_ACCOUNT_IBAN = "NL36INGB8278309172";
	
	private float balance;
	private String IBAN;
	private String mainHolderBSN;
	private Set<CustomerAccount> owners = new HashSet<CustomerAccount>();
	private SavingsAccount savingsAccount;

	private boolean closed;
	private double overdraftLimit;
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
		this.overdraftLimit = 0;
		this.savingsAccount = new SavingsAccount(this);
		this.savingsAccount.saveToDB();
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
		this.overdraftLimit = 0;
		
		if (!IBAN.equals(ING_BANK_ACCOUNT_IBAN)) {
			this.savingsAccount = new SavingsAccount(this);
			this.savingsAccount.saveToDB();			
		}
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
	 * @throws ClosedAccountTransferException 
	 * @throws PinCardBlockedException 
	 */
	public void deposit(double amount, String cardNum) throws IllegalAmountException, ClosedAccountTransferException, PinCardBlockedException {
		try {
			if (((DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, cardNum)).isBlocked()) {
				throw new PinCardBlockedException(cardNum);
			} else if (amount <= 0) {
				throw new IllegalAmountException(amount);
			} else if (this.closed) {
				throw new ClosedAccountTransferException();
			}
		} catch (ObjectDoesNotExistException e) {
			System.err.println(e.toString());
			return;
		}
		this.debit(amount);
		
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDestinationIBAN(this.getIBAN());
		t.setAmount(amount);
		t.setDescription("Deposit from card " + cardNum);
		t.saveToDB();
		this.saveToDB();
	}
	
	/**
	 * Transfers money to the savings account
	 * @param amount
	 * @throws IllegalAmountException 
	 * @throws ExceedOverdraftLimitException 
	 * @throws ClosedAccountTransferException 
	 */
	public void transfer(double amount) throws IllegalAmountException, ExceedOverdraftLimitException, ClosedAccountTransferException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance - amount < overdraftLimit * -1) {
			throw new ExceedOverdraftLimitException(overdraftLimit);
		} else if (this.closed || savingsAccount.isClosed()) {
			throw new ClosedAccountTransferException();
		}
		
		this.credit(amount);
		savingsAccount.debit(amount);
		
		Calendar c = Calendar.getInstance();		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(savingsAccount.getIBAN() + "S");
		t.setAmount(amount);
		t.setDescription("Transfer from main account to savings account.");
		t.saveToDB();
		this.saveToDB();
		savingsAccount.saveToDB();
		
	}

	/**
	 * Used by the ING bank account to transfer interest to savings accounts.
	 * @param IBAN
	 * @param amount
	 */
	public void transfer(String IBAN, double amount) {
		BankAccount destination = null;
		if (!DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, IBAN)) {
			try {
				destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, IBAN);
				destination.getSavingsAccount().debit(amount);
				destination.saveToDB();
			} catch (ObjectDoesNotExistException | IllegalAmountException e) {
				System.err.println(e.toString());
				return;
			}
		}		
		
		Calendar c = Calendar.getInstance();		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(IBAN + "S");
		t.setAmount(amount);
		t.setDescription("Interest on savings account.");
		t.saveToDB();
	}

	/**
	 * Transfers a specific amount of money from this <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @throws ExceedOverdraftLimitException 
	 */
	public void transfer(BankAccount destination, float amount) throws IllegalAmountException, IllegalTransferException, ExceedOverdraftLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance - amount < overdraftLimit * -1) {
			throw new ExceedOverdraftLimitException(overdraftLimit);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new SameAccountTransferException();
		} else if (this.closed || destination.getClosed()) {
			throw new ClosedAccountTransferException();
		}
		
		this.credit(amount);
		destination.debit(amount);
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(destination.getIBAN());
		t.setAmount(amount);
		t.setDescription("Transfer to " + destination.getIBAN() + ".");
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
		InterestHandler.setLowestNegativeDailyReachMapEntry(IBAN, balance);
	}

	public void transfer(String destinationIBAN, double amount, String description) throws IllegalAmountException, InsufficientFundsTransferException,
		ClosedAccountTransferException, SameAccountTransferException, ExceedOverdraftLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance - amount < overdraftLimit * -1 && !description.equals("Negative interest credit")) {
			throw new ExceedOverdraftLimitException(overdraftLimit);
		} else if (this.closed) {
			throw new ClosedAccountTransferException();
		}
		
		boolean knownAccount = false;
		BankAccount destination = null;
		
		if (!DataManager.isPrimaryKeyUnique(BankAccount.CLASSNAME, BankAccount.PRIMARYKEYNAME, destinationIBAN)) {
			knownAccount = true;
			try {
				destination = (BankAccount) DataManager.getObjectByPrimaryKey(BankAccount.CLASSNAME, destinationIBAN);
			} catch (ObjectDoesNotExistException e) {
				System.err.println(e.toString());
				return;
			}
		}
		
		if (knownAccount) {
			if (destination.getClosed()) {
				throw new ClosedAccountTransferException();
			} else if (destination.getIBAN().equals(this.getIBAN())) {
				throw new SameAccountTransferException();
			}
		}
		
		this.credit(amount);
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(destinationIBAN);
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		if (knownAccount) {
			destination.debit(amount);
			destination.saveToDB();
		}
		InterestHandler.setLowestNegativeDailyReachMapEntry(IBAN, balance);
	}
	
	/**
	 * Transfers a specific amount of money from this <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @param description Description of the transfer
	 * @throws ExceedOverdraftLimitException 
	 */
	public void transfer(BankAccount destination, double amount, String description) throws IllegalAmountException, IllegalTransferException, ExceedOverdraftLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance - amount < overdraftLimit * -1 && !description.equals("Negative interest credit")) {
			throw new ExceedOverdraftLimitException(overdraftLimit);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new SameAccountTransferException();
		} else if (this.closed || destination.getClosed()) {
			throw new ClosedAccountTransferException();
		}
		
		this.credit(amount);
		destination.debit(amount);
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(destination.getIBAN());
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
		InterestHandler.setLowestNegativeDailyReachMapEntry(IBAN, balance);
	}
	
	public void transfer(BankAccount destination, double amount, String description, String targetName) throws IllegalAmountException, IllegalTransferException, ExceedOverdraftLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (balance - amount < overdraftLimit * -1 && !description.equals("Negative interest credit")) {
			throw new ExceedOverdraftLimitException(overdraftLimit);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new SameAccountTransferException();
		} else if (this.closed || destination.getClosed()) {
			throw new ClosedAccountTransferException();
		}
		
		this.credit(amount);
		destination.debit(amount);
		Calendar c = Calendar.getInstance();
		
		// Add simulated days 
		c.add(Calendar.DATE, Client.getSimulatedDays());
		
		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDateTimeMilis(c.getTimeInMillis());
		t.setTargetName(targetName);
		t.setSourceIBAN(this.getIBAN());
		t.setDestinationIBAN(destination.getIBAN());
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
		InterestHandler.setLowestNegativeDailyReachMapEntry(IBAN, balance);
	}
	
	/**
	 * Credits a <code>BankAccount</code> with a specific amount of money
	 * @param amount The amount of money to credit the <code>BankAccount</code> with
	 * @throws IllegalAmountException Thrown when the specified amount is 0 or negative
	 */
	public void credit(double amount) throws IllegalAmountException {
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
	public void debit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		balance += amount;
	}
	
	public void removeOwner(String BSN) {
		CustomerAccount cAcc = null;
		for (CustomerAccount c : owners) {
			if (c.getBSN().equals(BSN)) {
				cAcc = c;
				break;
			}
		}
		
		if (cAcc != null) {
			owners.remove(cAcc);
		}
	}
	
	public void close() throws IllegalAccountCloseException {
		if (balance != 0) {
			throw new IllegalAccountCloseException(IBAN, balance, false);
		} else if (savingsAccount.getBalance() != 0) {
			throw new IllegalAccountCloseException(IBAN, balance, true);
		}
		
		setClosed(true);
		savingsAccount.setClosed(true);
		
	}

	public void setIBAN(String IBAN) {
		this.IBAN = IBAN; 
	}

	public void setOverdraftLimit(double overdraftLimit) {
		this.overdraftLimit = overdraftLimit;
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

	public void setSavingsAccount(SavingsAccount savingsAccount) {
		this.savingsAccount = savingsAccount;
	}
	
	@OneToOne(fetch = FetchType.LAZY, mappedBy = "bankAccount", cascade = CascadeType.ALL)
	public SavingsAccount getSavingsAccount() {
		return savingsAccount;
	}

	@Column(name = "balance")
	public float getBalance() {
		return balance;
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
	
	@Column(name = "overdraftlimit")
	public double getOverdraftLimit() {
		return overdraftLimit;
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
		result = (List<DebitCard>) DataManager.getObjectsFromDB(new DebitCard().getClassName(), criteria);
		return result;
	}

	public void setOwners(Set<CustomerAccount> owners) {
		this.owners = owners;
	}
	
	public static void setUpINGaccount() {
		System.out.println("Set up ING account");
		CustomerAccount ingAccount = new CustomerAccount("ING", "BANK", "I.B", "00000000", "ING Street 1", "0600000000",
				"ing@mail.com", "01-01-1950", "ing", "bank");
		BankAccount ingBankAccount = new BankAccount("00000000", 1000000f, ING_BANK_ACCOUNT_IBAN);
		HashSet<BankAccount> bankAccountSet = new HashSet<>();
		bankAccountSet.add(ingBankAccount);
		ingAccount.setBankAccounts(bankAccountSet);
		ingAccount.saveToDB();
		ingBankAccount.saveToDB();
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
