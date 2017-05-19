package server.core;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.apache.commons.validator.routines.EmailValidator;

/**
 * A class that checks user-input from the UI.
 * @author Justin Praas
 * @version 3rd of April, 2017
 */
public class InputValidator {
	
	public static final String VALID_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyzÎˆÙÈËABCDEFGHIJKLMNOPQRSTUVWXYZ÷‘…»-' ";
	public static final String UPPER_CASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZÀ÷‘…»";
	public static final String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyzÎˆÙÈË";
	public static final String NUMERICALS = "0123456789";

	/**
	 * Checks whether a given BSN is a valid BSN. 
	 * @param BSN the BSN to be checked
	 * @return true if the BSN is valid, otherwise false
	 */
	public static boolean isValidBSN(String BSN) {
		
		if (BSN.length() != 8 && BSN.length() != 9) {
			System.err.println("Invalid BSN. BSN length should be 8 to 9 digits.");
			return false;
		}
		
		if (!isNumericalOnly(BSN)) {
			System.err.println("Invalid BSN. BSN should be numerical only.");
			return false;
		}
		
		// There are more requirements for a valid BSN, but for now this is sufficient
		return true;
	}
	
	/**
	 * Checks whether a given name is (at a certain degree) valid.
	 * @param name the name to be checked
	 * @return true if the name is valid, otherwise false
	 */
	public static boolean isValidName(String name) {
		// Only allow names that are longer than 1 character and shorter than 31
		if (name.length() < 2 || name.length() > 30) {
			System.err.println("Invalid name. Name can only have a length of 2 to 30 characters.");
			return false;
		}
		
		for (int i = 0; i < name.length(); i++) {
			String character = "" + name.charAt(i);
			if (!VALID_NAME_CHARACTERS.contains(character)) {
				System.err.println("Invalid name. Name contains invalid characters.");
				return false;
			}
		}			
		return true;
	}
	
	/**
	 * Checks whether a given street address is valid.
	 * @param streetAddress the street address to be checked
	 * @return true if the street address is valid, otherwise false
	 */
	public static boolean isValidStreetAddress(String streetAddress) {
		String[] streetAddressArray = streetAddress.split(" ");
		
		if (streetAddressArray.length < 2) {
			System.err.println("Invalid street address. Example: Steenanjer Straat 3B");
			return false;
		}
		
		String number = streetAddressArray[streetAddressArray.length - 1]; // Can include optional letters, for example: 13A, 462
		String street = streetAddress.substring(0, streetAddress.length() - number.length() - 1);
		
		for (int i = 0; i < street.length(); i++) {
			String character = "" + street.charAt(i);
			if (!(VALID_NAME_CHARACTERS + ".").contains(character)) {
				System.err.println("Invalid street address. Street address contains invalid characters.");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether an email address is valid.
	 * @param email the email address to be checked
	 * @return true if the email address is valid, otherwise false
	 */
	public static boolean isValidEmailAddress(String email) {
		boolean isValid = EmailValidator.getInstance().isValid(email);
		// Using the Apache Commons Validator library
		if (!isValid) {
			System.err.println("Invalid emailaddress.");
		}		
		return isValid;
	}
	
	/**
	 * Checks whether a given phone number is valid.
	 * @param phoneNumber the phone number to be checked.
	 * @return true if the phone number is valid, otherwise false
	 */
	public static boolean isValidPhoneNumber(String phoneNumber) {
		if (phoneNumber.length() != 10) {
			System.err.println("Invalid phone number. Phone number is not 10 digits long.");
			return false;
		}
		
		if (!isNumericalOnly(phoneNumber)) {
			System.err.println("Invalid phone number. Phone number is not numerical only.");
			return false;
		}
		return true;
	}
	
	/**
	 * Checks whether a given birth date is valid.
	 * @param birthDate the birth date to be checked
	 * @return true if the birth date is valid, otherwise false
	 */
	public static boolean isValidBirthDate(String birthDate) {
		boolean isValid = true;
		// Format: dd-MM-yyyy, 15 years and older only
		String[] birthDateArray = birthDate.split("-");
		int day = -1, month = -1, year = -1;
		
		try {
			day = Integer.parseInt(birthDateArray[0]);
			month = Integer.parseInt(birthDateArray[1]);
			year = Integer.parseInt(birthDateArray[2]);
			
			LocalDate dateOfBirth = LocalDate.of(year, month, day);
			LocalDate now = LocalDate.now();
			
			if (now.minusYears(15).isBefore(dateOfBirth)) {
				System.err.println("Invalid birth date. You're too young to create an account. ");
				return false;
			}
			
			// Triggers a DateTimeException if the given date can't exist (e.g. 2017-18-42)
			LocalDate.of(year, month, day); // http://stackoverflow.com/a/34718859/7133329
			
		} catch (NumberFormatException e) {
			System.err.println("Invalid birth date. The date is not numerical. The date format is dd-MM-yyyy.");
			isValid = false;
		} catch (DateTimeException e) {
			System.err.println("Invalid birth date. The date format is dd-MM-yyyy.");
			return false;
		}	
		
		return isValid;		
	}
	
	/**
	 * Checks whether a given IBAN is valid.
	 * @param toIBAN the IBAN to be checked
	 * @return true if the IBAN is valid, false otherwise
	 */
	public static boolean isValidIBAN(String toIBAN) {
		
		boolean isValid = true;
		
		String countryCode = toIBAN.substring(0, 1);
		String controlCode = toIBAN.substring(2, 3);
		String bankCode = toIBAN.substring(4,7);
		String bankNumber = toIBAN.substring(8);
		
		if (!isAlphabeticalOnly(countryCode)) {
			isValid = false;
			System.err.println("IBAN does not contain a valid country code.");
		}
		
		if (!isNumericalOnly(controlCode)) {
			isValid = false;
			System.err.println("IBAN does not contain a valid country code.");
		}
		
		if (!isNumericalOnly(bankNumber) || bankNumber.length() != 10) {
			isValid = false;
			System.err.println("IBAN does not contain a valid bank number.");
		}
		
		if (!isAlphabeticalOnly(bankCode)) {
			isValid = false;
			System.err.println("IBAN does not contain a valid bank code.");
		}
		return isValid;
	}

	/**
	 * Checks whether the given input can construct a valid <code>CustomerAccount</code>.
	 * @param BSN the customer's BSN
	 * @param firstName the customer's first name
	 * @param surname the customer's surname
	 * @param streetAddress the customer's street address
	 * @param email the customer's email address
	 * @param phoneNumber the customer's phone number
	 * @param birthDate the customer's birth date
	 * @return true if all of the input is valid, otherwise false.
	 */
	public static boolean isValidCustomer(String BSN, String firstName, String surname, String streetAddress, String email,
			String phoneNumber, String birthDate) {
		return isValidBSN(BSN) && isValidName(firstName) && 
				isValidName (surname) && isValidStreetAddress(streetAddress) && 
				isValidEmailAddress(email) && isValidPhoneNumber(phoneNumber) && 
				isValidBirthDate(birthDate);
	}
	
	/**
	 * Checks whether a given input is numerical only.
	 * @param input the input to be checked
	 * @return true if the input is numerical only, otherwise false
	 */
	public static boolean isNumericalOnly(String input) {
		boolean isNumerical = true;		
		for (int i = 0; i < input.length(); i++) {
			try {
				Integer.parseInt("" + input.charAt(i));
			} catch (NumberFormatException e) {
				isNumerical = false;
				break;
			}
		}		
		return isNumerical;
	}
	
	/**
	 * Checks whether the given input is alphabetical only.
	 * @param input the input to be checked
	 * @return true if the input is alphabetical only, otherwise false
	 */
	public static boolean isAlphabeticalOnly(String input) {		
		if (input.length() == 0) {
			System.err.println("Alphabetical checker: empty input");
			return false;
		}
		
		for (int i = 0; i < input.length(); i++) {
			String character = "" + input.charAt(i);
			if (!(UPPER_CASE_LETTERS + LOWER_CASE_LETTERS).contains(character)) {
				System.err.println("Alphabetical checker: input '" + input + "' is not alphabetical only.");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks whether a given String is a number.
	 * @param input The String to be checked
	 * @return true if the input is a valid number, false otherwise
	 */
	public static boolean isValidAmount(String input) {
		if (input.matches("(\\d+\\.\\d+|[1-9]\\d+|\\d)")) {
			return true;
		} else {
			System.err.println("Amount " + input + " invalid.");
			return false;
		}
	}
	
	/**
	 * Checks whether a given String is a valid debit card number.
	 * @param input The String to be checked
	 * @return true if the input is a valid debit card number, false otherwise
	 */
	public static boolean isValidCardNumber(String input) {
		if (input.matches("^\\d{3}+[a-zA-Z]\\d{3}$")) {
			return true;
		} else {
			System.err.print("Card number " + input + " is invalid.");
			return false;
		}
	}
	
	/**
	 * Checks whether a given String is a valid PIN.
	 * @param input The String to be checked
	 * @return true if the input is a 4-digit number, false otherwise
	 */
	public static boolean isValidPIN(String input) {
		if (input.matches("^\\d{4}$")) {
			return true;
		} else {
			System.err.println("PIN invalid.");
			return false;
		}
	}
}
