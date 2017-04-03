package userinterface;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.apache.commons.validator.routines.EmailValidator;

public class InputChecker {
	
	public static final String VALID_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyzˆÙÈËABCDEFGHIJKLMNOPQRSTUVWXYZ÷‘…»-'";
	public static final String UPPER_CASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ÷‘…»";
	public static final String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyzˆÙÈË";
	public static final String NUMERICALS = "0123456789";

	public static boolean isValidBSN(String bsn) {
		if (bsn.length() != 8 && bsn.length() != 9) {
			// TODO: Throw exception
			return false;
		}
		
		if (!isNumericalOnly(bsn)) {
			// TODO: Throw exception
			return false;
		}
		
		// TODO: Check if a customer with this bsn already exists
		
		// There are more requirements for a valid BSN, but for now this is sufficient
		return true;
	}
	
	public static boolean isValidName(String name) {
		if (name.length() < 1 && name.length() > 30) {
			// TODO: Throw exception
			return false;
		}
		
		for (int i = 0; i < name.length(); i++) {
			if (!VALID_NAME_CHARACTERS.contains(name.substring(i, i))) {
				// TODO: Throw exception
				return false;
			}
		}		
		return true;
	}
	
	public static boolean isValidStreetAddress(String streetAddress) {
		String[] streetAddressArray = streetAddress.split(" ");
		String number = streetAddressArray[1]; // Can include optional letters, for example: 13A, 462
		String street = streetAddress.substring(0, streetAddress.length() - number.length()); // 
		
		if (!isAlphabeticalOnly(street)) {
			// TODO: Returns false if a street with spaces is entered
			// TODO: Throw exception
			return false;
		}
		
		// TODO: For now assume that the number is valid
		return true;
	}
	
	public static boolean isValidEmailAddress(String email) {
		// Using the Apache Commons Validator library
		return EmailValidator.getInstance().isValid(email);
	}
	
	public static boolean isValidPhoneNumber(String phoneNumber) {
		if (phoneNumber.length() != 10) {
			// TODO: Throw exception
			return false;
		}
		
		if (!isNumericalOnly(phoneNumber)) {
			// TODO: Throw exception
			return false;
		}		
		return true;
	}
	
	public static boolean isValidBirthDate(String birthDate) {
		// Format: dd-mm-yyyy
		String[] birthDateArray = birthDate.split("-");
		int day = -1, month = -1, year = -1;
		
		try {
			day = Integer.parseInt(birthDateArray[0]);
			month = Integer.parseInt(birthDateArray[1]);
			year = Integer.parseInt(birthDateArray[2]);
			
			LocalDate dateOfBirth = LocalDate.parse(birthDate);
			LocalDate now = LocalDate.now();
			
			if (now.minusYears(18).isBefore(dateOfBirth)) {
				// TODO: Throw 'too young' exception
				return false;
			}
			
			// Triggers a DateTimeException if the given date can't exist (e.g. 2017-18-42)
			LocalDate.of(year, month, day); // http://stackoverflow.com/a/34718859/7133329
			
		} catch (NumberFormatException e) {
			// TODO: Throw exception (input is incorrect)
			return false;
		} catch (DateTimeException e) {
			// TODO: Throw an invalid date exception
			return false;
		}		
		return true;		
	}
	
	public static boolean isNumericalOnly(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (!NUMERICALS.contains(input.substring(i, i))) {
				// TODO: Throw exception
				return false;
			}
		}		
		return true;
	}
	
	public static boolean isAlphabeticalOnly(String input) {
		for (int i = 0; i < input.length(); i++) {
			if (!UPPER_CASE_LETTERS.contains(input.substring(i, i)) &&
					!LOWER_CASE_LETTERS.contains(input.substring(i, i))) {
				// TODO: Throw exception
				return false;
			}
		}
		return true;
	}

	public static boolean isValidCustomer(String BSN, String firstName, String surname, String streetAddress, String email,
			String phoneNumber, String birthDate) {
		return isValidBSN(BSN) && isValidName(firstName) && 
				isValidName (surname) && isValidStreetAddress(streetAddress) && 
				isValidEmailAddress(email) && isValidPhoneNumber(phoneNumber) && 
				isValidBirthDate(birthDate);
	}
}
