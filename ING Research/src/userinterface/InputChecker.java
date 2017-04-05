package userinterface;

import java.time.DateTimeException;
import java.time.LocalDate;
import org.apache.commons.validator.routines.EmailValidator;

/**
 * A class that checks user-input from the UI.
 * @author Justin Praas
 * @version 3rd of April 2017
 */
public class InputChecker {
	
	public static final String VALID_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyzÎˆÙÈËABCDEFGHIJKLMNOPQRSTUVWXYZ÷‘…»-' ";
	public static final String UPPER_CASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZÀ÷‘…»";
	public static final String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyzÎˆÙÈË";
	public static final String NUMERICALS = "0123456789";

	public static boolean isValidBSN(String bsn) {
		boolean isValid = true;
		
		if (bsn.length() != 8 && bsn.length() != 9) {
			// TODO: Throw exception
			isValid = false;;
		}
		
		if (!isNumericalOnly(bsn)) {
			// TODO: Throw exception
			isValid = false;;
		}
		
		// TODO: Check if a customer with this bsn already exists
		
		if (!isValid) {
			System.err.println("Invalid BSN.");
		}
		
		// There are more requirements for a valid BSN, but for now this is sufficient
		return isValid;
	}
	
	public static boolean isValidName(String name) {
		// Only allow names that are longer than 1 character and shorter than 31
		if (name.length() < 2 || name.length() > 30) {
			// TODO: Throw exception
			System.err.println("Invalid name.");
			return false;
		}
		
		for (int i = 0; i < name.length(); i++) {
			String character = "" + name.charAt(i);
			if (!VALID_NAME_CHARACTERS.contains(character)) {
				// TODO: Throw exception
				System.err.println("Invalid name.");
				return false;
			}
		}			
		return true;
	}
	
	public static boolean isValidStreetAddress(String streetAddress) {
		String[] streetAddressArray = streetAddress.split(" ");
		
		if (streetAddressArray.length < 2) {
			System.err.println("Invalid streetaddress.");
			return false;
		}
		
		String number = streetAddressArray[streetAddressArray.length - 1]; // Can include optional letters, for example: 13A, 462
		String street = streetAddress.substring(0, streetAddress.length() - number.length() - 1);
		
		for (int i = 0; i < street.length(); i++) {
			String character = "" + street.charAt(i);
			if (!(VALID_NAME_CHARACTERS + ".").contains(character)) {
				// TODO: Throw exception
				System.err.println("Invalid streetaddress.");
				return false;
			}
		}
		
		// TODO: For now assume that the number is valid
		return true;
	}
	
	public static boolean isValidEmailAddress(String email) {
		boolean isValid = EmailValidator.getInstance().isValid(email);
		// Using the Apache Commons Validator library
		if (!isValid) {
			System.err.println("Invalid emailaddress.");
		}
		
		return isValid;
	}
	
	public static boolean isValidPhoneNumber(String phoneNumber) {
		if (phoneNumber.length() != 10) {
			System.err.println("Invalid phone number.");
			// TODO: Throw exception
			return false;
		}
		
		if (!isNumericalOnly(phoneNumber)) {
			System.err.println("Invalid phone number.");
			// TODO: Throw exception
			return false;
		}
		return true;
	}
	
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
				// TODO: Throw 'too young' exception
				isValid = false; 
				System.err.print("You're too young. ");
			}
			
			// Triggers a DateTimeException if the given date can't exist (e.g. 2017-18-42)
			LocalDate.of(year, month, day); // http://stackoverflow.com/a/34718859/7133329
			
		} catch (NumberFormatException e) {
			// TODO: Throw exception (input is incorrect)
			isValid = false;
		} catch (DateTimeException e) {
			// TODO: Throw an invalid date exception
			isValid = false;
		}	
		
		if (!isValid) {
			System.err.println("Invalid birthdate.");
		}
		
		return isValid;		
	}
	
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
	
	public static boolean isAlphabeticalOnly(String input) {
		boolean isAlphabetical = true;
		
		if (input.length() == 0) {
			isAlphabetical = false;
		}
		
		for (int i = 0; i < input.length(); i++) {
			String character = "" + input.charAt(i);
			if (!(UPPER_CASE_LETTERS + LOWER_CASE_LETTERS).contains(character)) {
				isAlphabetical = false;
				break;
			}
		}
		return isAlphabetical;
	}

	public static boolean isValidCustomer(String BSN, String firstName, String surname, String streetAddress, String email,
			String phoneNumber, String birthDate) {
		return isValidBSN(BSN) && isValidName(firstName) && 
				isValidName (surname) && isValidStreetAddress(streetAddress) && 
				isValidEmailAddress(email) && isValidPhoneNumber(phoneNumber) && 
				isValidBirthDate(birthDate);
	}

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
}
