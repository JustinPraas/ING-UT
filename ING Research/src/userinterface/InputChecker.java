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
			System.out.println("Invalid BSN.");
		}
		
		// There are more requirements for a valid BSN, but for now this is sufficient
		return isValid;
	}
	
	public static boolean isValidName(String name) {
		boolean isValid = true;
		
		if (name.length() < 1 && name.length() > 30) {
			// TODO: Throw exception
			isValid = false;;
		}
		
		for (int i = 0; i < name.length(); i++) {
			if (!VALID_NAME_CHARACTERS.contains(name.substring(i, i))) {
				// TODO: Throw exception
				isValid = false;
			}
		}	
		
		if (!isValid) {
			System.err.println("Invalid name.");
		}
		return isValid;
	}
	
	public static boolean isValidStreetAddress(String streetAddress) {
		boolean isValid = true;
		
		String[] streetAddressArray = streetAddress.split(" ");
		String number = streetAddressArray[1]; // Can include optional letters, for example: 13A, 462
		String street = streetAddress.substring(0, streetAddress.length() - number.length()); // 
		
		if (!isAlphabeticalOnly(street)) {
			// TODO: Returns false if a street with spaces is entered
			// TODO: Throw exception
			isValid = false;;
		}
		
		if (!isValid) {
			System.err.println("Invalid streetaddress.");
		}
		
		// TODO: For now assume that the number is valid
		return isValid;
	}
	
	public static boolean isValidEmailAddress(String email) {
		boolean isValid = EmailValidator.getInstance().isValid(email);
		// Using the Apache Commons Validator library
		if (!isValid) {
			System.out.println("Invalid emailaddress.");
		}
		
		return isValid;
	}
	
	public static boolean isValidPhoneNumber(String phoneNumber) {
		boolean isValid = true;
		if (phoneNumber.length() != 10) {
			// TODO: Throw exception
			isValid = false;
		}
		
		if (!isNumericalOnly(phoneNumber)) {
			// TODO: Throw exception
			isValid = false;
		}		
		
		if (!isValid) {
			System.out.println("Invalid phone number.");
		}
		return isValid;
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
				System.out.print("You're too young. ");
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
			System.out.println("Invalid birthdate.");
		}
		
		return isValid;		
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

	public static boolean isvalidIBAN(String toIBAN) {
		
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
		
		if (!isNumericalOnly(bankNumber)) {
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
