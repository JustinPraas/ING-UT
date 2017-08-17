package accounts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import cards.Card;
import cards.CreditCard;
import cards.DebitCard;
import client.Client;
import database.DataManager;
import database.SQLiteDB;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import exceptions.ClosedAccountTransferException;
import exceptions.CreditCardNotActiveException;
import exceptions.ExceedLimitException;
import exceptions.ExceedLimitException.LimitType;
import exceptions.IllegalAccountCloseException;
import exceptions.IllegalAmountException;
import exceptions.IllegalTransferException;
import exceptions.InsufficientFundsTransferException;
import exceptions.ObjectDoesNotExistException;
import exceptions.PinCardBlockedException;
import exceptions.SameAccountTransferException;
import server.rest.BankSystemValue;
import server.rest.InterestHandler;
import server.rest.ServerModel;

/**
 * A simple model of a bank account in an abstract currency.
 * @author Andrei Cojocaru
 */
@Entity
@Table(name = "bankaccounts")
public class BankAccount extends Account {	

	public static final String CLASSNAME = "accounts.BankAccount";
	public static final String PRIMARYKEYNAME = "IBAN";
	
	/**
	 * The static country code for the Netherlands.
	 */
	private final static String COUNTRY_CODE = "NL";
	
	/**
	 * The bank code for ING.
	 */
	private final static String BANK_CODE = "INGB";
	
	/**
	 * A static IBAN for the ING bank account that will be used for 
	 * savings interest and negative interest.
	 */
	public final static String ING_BANK_ACCOUNT_IBAN = "NL36INGB8278309172";

	/*
	 * Properties of the bank account.
	 */
	private String mainHolderBSN;
	private String accountType;
	private Set<CustomerAccount> owners = new HashSet<CustomerAccount>();
	private SavingsAccount savingsAccount;
	private boolean frozen;
	private double overdraftLimit;
	private double transferLimit;

	/**
	 * Prints a representation of the bank account.
	 */
	public String toString() {
		String ownersBSNs = "";
		for (CustomerAccount key : this.getOwners()) {
			ownersBSNs += key.getBSN() + "; ";
		}
		ownersBSNs = ownersBSNs.substring(0, ownersBSNs.length() - 1);
		
		StringBuilder result = new StringBuilder();
		result.append(String.format("%25s- %s- %n", "IBAN", super.getIBAN()));
		result.append(String.format("%25s- %s- %n", "Holder BSN", mainHolderBSN));
		result.append(String.format("%25s- %s- %n", "Account type", accountType));
		result.append(String.format("%25s- %s- %n", "Balance", super.getBalance()));
		result.append(String.format("%25s- %s- %n", "Customers with access", ownersBSNs));
		result.append(String.format("%25s- %s- %n", "Overdraft limit", overdraftLimit));
		result.append(String.format("%25s- %s- %n", "Transfer limit", transferLimit));
		
		return result.toString();
	}

	public BankAccount() {

	}

	/**
	 * Create a new <code>BankAccount</code> with a specific account holder and
	 * an initial balance of 0. Adds it to the banking database with a
	 * newly-generated IBAN as primary key.
	 * 
	 * @param holder
	 *            The <code>CustomerAccount</code> considered to be the main
	 *            holder of this <code>BankAccount</code>
	 */
	public BankAccount(String mainHolderBSN) {
		super(generateIBAN(COUNTRY_CODE, BANK_CODE, randomPAN()), 0, false);
		this.mainHolderBSN = mainHolderBSN;
		this.overdraftLimit = 0;
		this.accountType = "regular";
		this.transferLimit = BankSystemValue.WEEKLY_TRANSFER_LIMIT.getAmount();
		this.savingsAccount = new SavingsAccount(this);
		this.savingsAccount.saveToDB();
		this.setFrozen(false);
	}

	/**
	 * Constructs a new <code>BankAccount</code> with specific field values.
	 * Used to load a <code>BankAccount</code> from the SQLite database.
	 * 
	 * @param mainHolderBSN
	 *            The BSN of the main <code>CustomerAccount</code> this account
	 *            is paired to
	 * @param balance
	 *            The account's balance
	 * @param holders
	 *            The list of all <code>CustomerAccounts</code> that can access
	 *            this account
	 * @param IBAN
	 *            The account's IBAN
	 */
	public BankAccount(String mainHolderBSN, float balance, String IBAN) {
		super(IBAN, balance, false);
		this.mainHolderBSN = mainHolderBSN;
		this.overdraftLimit = 0;
		this.transferLimit = BankSystemValue.WEEKLY_TRANSFER_LIMIT.getAmount();

		if (!IBAN.equals(ING_BANK_ACCOUNT_IBAN)) {
			this.savingsAccount = new SavingsAccount(this);
			this.savingsAccount.saveToDB();
		}
		this.setFrozen(false);
	}

	public void addOwner(CustomerAccount owner) {
		owners.add(owner);
	}

	/**
	 * Generates a random personal account number of 10 digits long. 
	 * @return The personal accountNumber for the IBAN
	 */
	private static String randomPAN() {
		// The personalAccountNumber (last 10 digits) should be hierarchically
		// distributed.
		// However, we do not keep track of used numbers yet, so for now assign
		// 10 random digits
		StringBuilder personalAccountNumber = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			personalAccountNumber.append((int) (Math.random() * 10));
		}
		return personalAccountNumber.toString();
	}

	/**
	 * Generates an IBAN for this BankAccount. 
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
			if (DataManager.isPrimaryKeyUnique(new BankAccount().getClassName(), new BankAccount().getPrimaryKeyName(),
					resultIBAN)) {
				unique = true;
			}
		}

		return resultIBAN;
	}

	/**
	 * Generates the controlNumber for the IBAN (3rd and 4th digit).	 * 
	 * @param countryCode The code that represents a country (e.g. NL, BE, DE, etc.)
	 * @param bankCode The code that represents the Bank (e.g. INGB, ABNA, etc.)
	 * @param pan The personal account number (the last 10 digits)
	 * @return controlNumber The controlNumber that belongs to the combination of countryCode, BankCode and pan
	 * @see <a href="http://www.ibannl.org/uitleg-over-iban/">Formula for the control number (Dutch)</a>
	 */
	public static int generateControlNumber(String countryCode, String bankCode, String pan) {
		/*
		 * Formula in short: String a = bank code converted to digits (e.g. 'A'
		 * = 10, 'B' = 11, 'Z' = 35, etc.) String b = personal account number
		 * (10 digits, prepend with 0's if needed) String c = country code
		 * converted to digits (see String a) BigInteger value = parse the
		 * following to a BigInteger: (a + b + c) int controlNumber = 98 -
		 * (value % 97)
		 */

		// Convert the bankCode to digits
		StringBuilder bankCodeInDigits = new StringBuilder();
		for (Character c : bankCode.toCharArray()) {
			bankCodeInDigits.append(Character.getNumericValue(c));
		}

		// Convert the countryCode to digits
		StringBuilder countryCodeInDigits = new StringBuilder();
		for (Character c : countryCode.toCharArray()) {
			countryCodeInDigits.append(Character.getNumericValue(c));
		}

		// A BigInteger is necessary for a value of that does not fit a long
		BigInteger computationValue = new BigInteger(bankCodeInDigits + pan + countryCodeInDigits + "00");

		// Get the remainder of computationValue % 97
		computationValue = computationValue.mod(new BigInteger("97"));

		// Subtract the computationValue from 98, this results in the
		// controlNumber of the debitcard
		int controlNumber = 98 - computationValue.intValue();

		return controlNumber;
	}

	/**
	 * Deposit a specific sum of money into the <code>BankAccount</code>.	 * 
	 * @param amount The amount of money to be deposited
	 * @throws ClosedAccountTransferException
	 * @throws PinCardBlockedException
	 * @throws CreditCardNotActiveException 
	 * @throws ObjectDoesNotExistException 
	 */
	public void deposit(double amount, String cardNum)
			throws IllegalAmountException, ClosedAccountTransferException, PinCardBlockedException, CreditCardNotActiveException, ObjectDoesNotExistException {
		Card card;
		boolean isCreditCard = cardNum.length() == 16;
		if (isCreditCard) {
			card = (CreditCard) DataManager.getObjectByPrimaryKey(CreditCard.CLASSNAME, cardNum);
			if (!((CreditCard) card).isActive()) {
				throw new CreditCardNotActiveException(cardNum);
			}
		} else {
			card = (DebitCard) DataManager.getObjectByPrimaryKey(DebitCard.CLASSNAME, cardNum);
			if (card.isBlocked()) {
				throw new PinCardBlockedException(cardNum);
			}
		}
		
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (super.isClosed()) {
			throw new ClosedAccountTransferException();
		}
		this.debit(amount);

		Calendar c = ServerModel.getServerCalendar();

		String date = c.getTime().toString();
		Transaction t = new Transaction();
		t.setDateTime(date);
		t.setDestinationIBAN(this.getIBAN());
		t.setPinTransaction(true);
		t.setAmount(amount);
		t.setDescription("Deposit from card " + cardNum);
		t.saveToDB();
		this.saveToDB();
	}

	/**
	 * Transfers money to the savings account 
	 * @param amount
	 * @throws IllegalAmountException
	 * @throws ExceedLimitException
	 * @throws ClosedAccountTransferException
	 */
	public void transfer(double amount)
			throws IllegalAmountException, ExceedLimitException, ClosedAccountTransferException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (exceedsLimit(amount, LimitType.OVERDRAFT_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.OVERDRAFT_LIMIT);
		} else if (super.isClosed() || savingsAccount.isClosed()) {
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
		t.setPinTransaction(false);
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
				boolean isChild = destination.getAccountType().equals("child");
				if (isChild) {
					destination.debit(amount);
				} else {
					destination.getSavingsAccount().debit(amount);
				}				
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
		t.setPinTransaction(false);
		t.setAmount(amount);
		t.setDescription("Interest on savings account.");
		t.saveToDB();
	}

	/**
	 * Transfers a specific amount of money from this <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @throws ExceedLimitException
	 */
	public void transfer(BankAccount destination, float amount)
			throws IllegalAmountException, IllegalTransferException, ExceedLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (exceedsLimit(amount, LimitType.OVERDRAFT_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.OVERDRAFT_LIMIT);
		} else if (exceedsLimit(amount, LimitType.TRANSFER_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.OVERDRAFT_LIMIT);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new SameAccountTransferException();
		} else if (super.isClosed() || destination.isClosed()) {
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
		t.setPinTransaction(false);
		t.setAmount(amount);
		t.setDescription("Transfer to " + destination.getIBAN() + ".");
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
		InterestHandler.setLowestNegativeDailyReachMapEntry(super.getIBAN(), super.getBalance());
	}

	/**
	 * Transfers money from a PIN transaction to a destination account. Also
	 * handles the negative interest credition. 
	 * @param destinationIBAN The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @param description An optional description that belongs to this transaction.
	 * @throws IllegalAmountException
	 * @throws InsufficientFundsTransferException
	 * @throws ClosedAccountTransferException
	 * @throws SameAccountTransferException
	 * @throws ExceedLimitException
	 */
	public void transfer(String destinationIBAN, double amount, String description)
			throws IllegalAmountException, InsufficientFundsTransferException, ClosedAccountTransferException,
			SameAccountTransferException, ExceedLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (exceedsLimit(amount, LimitType.OVERDRAFT_LIMIT) && !description.equals("Negative interest credit")) {
			throw new ExceedLimitException(amount, this, LimitType.OVERDRAFT_LIMIT);
		} else if (exceedsLimit(amount, LimitType.DEBIT_CARD_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.DEBIT_CARD_LIMIT);
		} else if (exceedsLimit(amount, LimitType.TRANSFER_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.TRANSFER_LIMIT);
		} else if (super.isClosed()) {
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
			if (destination.isClosed()) {
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
		t.setPinTransaction(true);
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		if (knownAccount) {
			destination.debit(amount);
			destination.saveToDB();
		}
		InterestHandler.setLowestNegativeDailyReachMapEntry(super.getIBAN(), super.getBalance());
	}

	/**
	 * Transfers, with a PIN transaction, a specific amount of money from this
	 * <code>BankAccount</code> to another.
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @param description Description of the transfer
	 * @throws ExceedLimitException
	 */
	public void transfer(BankAccount destination, double amount, String description)
			throws IllegalAmountException, IllegalTransferException, ExceedLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (exceedsLimit(amount, LimitType.OVERDRAFT_LIMIT) && !description.equals("Negative interest credit")) {
			throw new ExceedLimitException(amount, this, LimitType.OVERDRAFT_LIMIT);
		} else if (exceedsLimit(amount, LimitType.DEBIT_CARD_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.DEBIT_CARD_LIMIT);
		} else if (exceedsLimit(amount, LimitType.TRANSFER_LIMIT)) {
			throw new ExceedLimitException(amount, this, LimitType.TRANSFER_LIMIT);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new SameAccountTransferException();
		} else if (super.isClosed() || destination.isClosed()) {
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
		t.setPinTransaction(true);
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
		InterestHandler.setLowestNegativeDailyReachMapEntry(super.getIBAN(), super.getBalance());
	}

	/**
	 * Transfers money from one account to another account and is used to credit
	 * the fee for a new pin card.	 
	 * @param destination The <code>BankAccount</code> to which the transferred money should go
	 * @param amount The amount of money to be transferred from this <code>BankAccount</code> to the destination
	 * @param description Description of the transfer
	 * @param targetName The name of the receiver
	 * @throws IllegalAmountException
	 * @throws IllegalTransferException
	 * @throws ExceedLimitException
	 */
	public void transfer(BankAccount destination, double amount, String description, String targetName)
			throws IllegalAmountException, IllegalTransferException, ExceedLimitException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		} else if (exceedsLimit(amount, LimitType.OVERDRAFT_LIMIT) && !description.equals("Negative interest credit")) {
			throw new ExceedLimitException(amount, this, LimitType.OVERDRAFT_LIMIT);
		} else if (exceedsLimit(amount, LimitType.TRANSFER_LIMIT)
				&& !description.equals("Fee for new pincard")) {
			throw new ExceedLimitException(amount, this, LimitType.TRANSFER_LIMIT);
		} else if (destination.getIBAN().equals(this.getIBAN())) {
			throw new SameAccountTransferException();
		} else if (super.isClosed() || destination.isClosed()) {
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
		t.setPinTransaction(false);
		t.setAmount(amount);
		t.setDescription(description);
		t.saveToDB();
		this.saveToDB();
		destination.saveToDB();
		InterestHandler.setLowestNegativeDailyReachMapEntry(super.getIBAN(), super.getBalance());
	}

	/**
	 * Checks if the given limit of this account is exceeded.
	 * @param amount the amount that's attempted to transfer
	 * @return true if the amount exceeds the limit, false otherwise
	 */
	public boolean exceedsLimit(double amount, LimitType limitType) {
		if (limitType == LimitType.OVERDRAFT_LIMIT) {
			return super.getBalance() - amount < overdraftLimit * -1;
		} 
		
		if (limitType == LimitType.DEBIT_CARD_LIMIT) {
			return exceedsDebitCardLimit(amount);
		} 
		
		if (limitType == LimitType.TRANSFER_LIMIT) {
			return exceedsTransferLimit(amount);
		}
		return true;
	}

	/**
	 * Checks if the transfer limit of this account is exceeded for the previous 6 days.
	 * @param amount the amount that's attempted to transfer
	 * @return true if the amount exceeds the limit, false otherwise
	 */
	public boolean exceedsTransferLimit(double amount) {
		double currentSum;
		Calendar today = ServerModel.getServerCalendar();
		Calendar firstDay = ServerModel.getServerCalendar();
		firstDay.add(Calendar.DATE, -6);
		firstDay.set(Calendar.HOUR_OF_DAY, 0);
		firstDay.set(Calendar.MINUTE, 0);
		firstDay.set(Calendar.SECOND, 0);
		firstDay.set(Calendar.MILLISECOND, 0);
		Connection con;
		ResultSet result;
		PreparedStatement statement;
		try {
			SQLiteDB.connectionLock.lock();
			con = SQLiteDB.openConnection();
			statement = con.prepareStatement("SELECT sum(amount) FROM transactions WHERE source_IBAN = '" + super.getIBAN() + 
					"' AND date_time_milis >= " + firstDay.getTimeInMillis() + " AND date_time_milis < " + today.getTimeInMillis());
			result = statement.executeQuery();
			result.next();
			if (result.getString(1) == null) {
				currentSum = 0;
			} else {
				currentSum = Double.parseDouble(result.getString(1));
			}				
			double totalSum = currentSum + amount;
			
			statement.close();
			result.close();
			con.close();
			
			if (totalSum > transferLimit) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
		return false;
	}

	/**
	 * Checks if the debit card limit of this account is exceeded for today.
	 * @param amount the amount that's attempted to transfer
	 * @return true if the amount exceeds the limit, false otherwise
	 */
	public boolean exceedsDebitCardLimit(double amount) {
		double currentSum;
		Calendar today = ServerModel.getServerCalendar();
		String thisMonthDisplayName = today.getDisplayName(Calendar.MONTH, Calendar.SHORT_STANDALONE, Locale.UK);
		Connection con;
		ResultSet result;
		PreparedStatement statement;
		try {
			SQLiteDB.connectionLock.lock();
			con = SQLiteDB.openConnection();
			statement = con.prepareStatement("SELECT sum(amount) FROM transactions WHERE source_IBAN = '" + super.getIBAN() + 
					"' AND date_time LIKE '% " + thisMonthDisplayName + " " + today.get(Calendar.DAY_OF_MONTH) + " % " + today.get(Calendar.YEAR) + "'  AND pin_transaction = 1");
			result = statement.executeQuery();
			result.next();
			if (result.getString(1) == null) {
				currentSum = 0;
			} else {
				currentSum = Double.parseDouble(result.getString(1));
			}				
			double totalSum = currentSum + amount;
			
			statement.close();
			result.close();
			con.close();
			
			if (totalSum > BankSystemValue.DAILY_WITHDRAW_LIMIT.getAmount()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			SQLiteDB.connectionLock.unlock();
		}
		return false;
	}

	/**
	 * Credits a <code>BankAccount</code> with a specific amount of money. 
	 * @param amount The amount of money to credit the <code>BankAccount</code> with
	 * @throws IllegalAmountException Thrown when the specified amount is 0 or negative
	 */
	public void credit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		super.setBalance(super.getBalance() - (float)amount);
	}

	/**
	 * Debits a <code>BankAccount</code> with a specific amount of money. 
	 * @param amount The amount of money to debit the <code>BankAccount</code> with
	 * @throws Thrown when the specified amount is 0 or negative
	 */
	public void debit(double amount) throws IllegalAmountException {
		if (amount <= 0) {
			throw new IllegalAmountException(amount);
		}
		super.setBalance(super.getBalance() + (float)amount);
	}

	/**
	 * Removes one of the owners of this bank account.
	 * @param BSN the owner's BSN
	 */
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

	/**
	 * Closes this bank account if possible.
	 * @throws IllegalAccountCloseException
	 */
	public void close() throws IllegalAccountCloseException {
		CreditAccount creditAccount = null;
		try {
			creditAccount = (CreditAccount) DataManager.getObjectByPrimaryKey(CreditAccount.CLASSNAME, super.getIBAN());
		} catch (ObjectDoesNotExistException e) {
			// All fine if credit account does note exist, no need to close
		}
		if (super.getBalance() != 0) {
			throw new IllegalAccountCloseException(super.getIBAN(), "Bank account has a non-zero balance of " + super.getBalance() + ".");
		} else if (savingsAccount.getBalance() != 0) {
			throw new IllegalAccountCloseException(super.getIBAN(), "Savings account has a non-zero balance of " + savingsAccount.getBalance() + ".");
		} else if (creditAccount != null && !creditAccount.isClosed()) {
			throw new IllegalAccountCloseException(super.getIBAN(), "Credit account is not closed.");
		}

		setClosed(true);
		savingsAccount.setClosed(true);
		savingsAccount.saveToDB();
		
		DebitCard db = getDebitCards().get(0);
		db.setBlocked(true);
		db.saveToDB();

	}

	public void setOverdraftLimit(double overdraftLimit) {
		this.overdraftLimit = overdraftLimit;
	}

	public void setMainHolderBSN(String BSN) {
		mainHolderBSN = BSN;
	}

	public void setSavingsAccount(SavingsAccount savingsAccount) {
		this.savingsAccount = savingsAccount;
	}

	public void setTransferLimit(double transferLimit) {
		this.transferLimit = transferLimit;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	@Column(name = "frozen")
	public boolean isFrozen() {
		return frozen;
	}

	@Column(name = "accounttype")
	public String getAccountType() {
		return accountType;
	}

	@Column(name = "transferlimit")
	public double getTransferLimit() {
		return transferLimit;
	}

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "bankAccount", cascade = CascadeType.ALL)
	public SavingsAccount getSavingsAccount() {
		return savingsAccount;
	}

	@Column(name = "customer_BSN")
	public String getMainHolderBSN() {
		return mainHolderBSN;
	}

	@Column(name = "overdraftlimit")
	public double getOverdraftLimit() {
		return overdraftLimit;
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

	/**
	 * Sets up a static ING bank account that is used for interests and deposits.
	 */
	public static void setUpINGaccount() {
		System.out.println("Set up ING account");
		CustomerAccount ingAccount = new CustomerAccount("ING", "BANK", "I.B", "00000000", "ING Street 1", "0600000000",
				"ing@mail.com", "01-01-1950", "ing", "bank");
		BankAccount ingBankAccount = new BankAccount("00000000", 1000000f, ING_BANK_ACCOUNT_IBAN);
		ingBankAccount.setTransferLimit(1000000000);
		ingBankAccount.setOverdraftLimit(1000000000);
		ingBankAccount.setAccountType("regular");
		HashSet<BankAccount> bankAccountSet = new HashSet<>();
		bankAccountSet.add(ingBankAccount);
		ingAccount.setBankAccounts(bankAccountSet);
		ingAccount.saveToDB();
		ingBankAccount.saveToDB();
	}

	public void deleteFromDB() {
		for (DebitCard key : getDebitCards()) {
			key.deleteFromDB();
		}
		DataManager.removeEntryFromDB(this);
	}
}
