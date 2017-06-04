package testing;

import static org.junit.Assert.*;
import org.junit.Test;

import server.core.InputValidator;

/**
 * A simple test class for the InputChecker.
 * @author Justin Praas
 * @version 4th of April, 2017
 */
public class InputCheckerTest {
	
	@Test
	public void isNumericalOnlyTest() {		
		String valid = "35113561";
		
		String invalid1 = "351da351";
		String invalid2 = "-1311351";
		String invalid3 = "3b";
		String invalid4 = "'313`3&35";
		
		assertFalse(InputValidator.isNumericalOnly(invalid1));
		assertFalse(InputValidator.isNumericalOnly(invalid2));
		assertFalse(InputValidator.isNumericalOnly(invalid3));
		assertFalse(InputValidator.isNumericalOnly(invalid4));
		assertTrue(InputValidator.isNumericalOnly(valid));
	}
	
	@Test
	public void isAlphabeticalOnlyTest() {		
		String valid1 = "abcdefg";
		String valid2 = "ÎÙÈË";
		String valid3 = "ABCDEFG";
		String valid4 = "À‘…»";
		
		String invalid1 = "`abcdefg";
		String invalid2 = "b342ald";
		String invalid3 = "-";
		String invalid4 = "4";

		assertTrue(InputValidator.isAlphabeticalOnly(valid1));
		assertTrue(InputValidator.isAlphabeticalOnly(valid2));
		assertTrue(InputValidator.isAlphabeticalOnly(valid3));
		assertTrue(InputValidator.isAlphabeticalOnly(valid4));
		assertFalse(InputValidator.isAlphabeticalOnly(invalid1));
		assertFalse(InputValidator.isAlphabeticalOnly(invalid2));
		assertFalse(InputValidator.isAlphabeticalOnly(invalid3));
		assertFalse(InputValidator.isAlphabeticalOnly(invalid4));
	}
	
	@Test
	public void isValidBSNTest() {
		String valid1 = "135153331";
		String valid2 = "23052011";
		
		String invalid1 = "0351323";
		String invalid2 = "2";
		String invalid3 = "2352744412";
		String invalid4 = "2305201d";
		String invalid5 = "2305201dd";

		assertTrue(InputValidator.isValidBSN(valid1));
		assertTrue(InputValidator.isValidBSN(valid2));
		assertFalse(InputValidator.isValidBSN(invalid1));
		assertFalse(InputValidator.isValidBSN(invalid2));
		assertFalse(InputValidator.isValidBSN(invalid3));
		assertFalse(InputValidator.isValidBSN(invalid4));
		assertFalse(InputValidator.isValidBSN(invalid5));
	}
	
	@Test
	public void isValidNameTest() {
		String valid1 = "Justin";
		String valid2 = "Ab";
		
		String invalid1 = "A"; // Too short
		String invalid2 = "3Praas";
		String invalid3 = "JustinPraasSteenanjerNijverdalTheNetherlands"; // Too long
		String invalid4 = "333";
		
		assertTrue(InputValidator.isValidName(valid1));
		assertTrue(InputValidator.isValidName(valid2));
		assertFalse(InputValidator.isValidName(invalid1));
		assertFalse(InputValidator.isValidName(invalid2));
		assertFalse(InputValidator.isValidName(invalid3));
		assertFalse(InputValidator.isValidName(invalid4));
	}
	
	@Test
	public void isValidStreetAddressTest() {
		String valid1 = "Steenanjer 3";
		String valid2 = "Van Dam Straat 3A";
		String valid3 = "Dr. Pieter-straat 18B";
		
		String invalid1 = "3Delige madeliefstraat";
		String invalid2 = "d";

		assertTrue(InputValidator.isValidStreetAddress(valid1));
		assertTrue(InputValidator.isValidStreetAddress(valid2));
		assertTrue(InputValidator.isValidStreetAddress(valid3));
		assertFalse(InputValidator.isValidStreetAddress(invalid1));
		assertFalse(InputValidator.isValidStreetAddress(invalid2));
	}
	
	@Test
	public void isValidEmailAddressTest() {
		String valid1 = "jw.praas@gmail.com";
		String valid2 = "a.h.r.jansen@co.uk.com";
		
		String invalid1 = "a.b.c.@gmail.com";
		String invalid2 = "a.jansen@.gmail.org";
		
		assertTrue(InputValidator.isValidEmailAddress(valid1));
		assertTrue(InputValidator.isValidEmailAddress(valid2));
		assertFalse(InputValidator.isValidEmailAddress(invalid1));
		assertFalse(InputValidator.isValidEmailAddress(invalid2));
	}
	
	@Test
	public void isValidPhoneNumberTest() {
		String valid1 = "0651023395";
		String valid2 = "0400083103";
		
		String invalid1 = "0651027d3395";
		String invalid2 = "065102779355";
		String invalid3 = "06510277";
		
		assertTrue(InputValidator.isValidPhoneNumber(valid1));
		assertTrue(InputValidator.isValidPhoneNumber(valid2));
		assertFalse(InputValidator.isValidPhoneNumber(invalid1));
		assertFalse(InputValidator.isValidPhoneNumber(invalid2));
		assertFalse(InputValidator.isValidPhoneNumber(invalid3));
	}
	
	@Test
	public void isValidBirthdateTest() {
		String valid1 = "27-11-1997";
		String valid2 = "20-03-2001";
		String valid3 = "12-03-2000";
		
		String invalid1 = "32-11-1997";
		String invalid2 = "22-03-2005"; // Too young
		String invalid3 = "1999-30-01";
		String invalid4 = "1999-01-23";
		String invalid5 = "30-2000-11";
		String invalid6 = "11-2000-30";
		
		assertTrue(InputValidator.isValidBirthDate(valid1));
		assertTrue(InputValidator.isValidBirthDate(valid2));
		assertTrue(InputValidator.isValidBirthDate(valid3));
		assertFalse(InputValidator.isValidBirthDate(invalid1));
		assertFalse(InputValidator.isValidBirthDate(invalid2));
		assertFalse(InputValidator.isValidBirthDate(invalid3));
		assertFalse(InputValidator.isValidBirthDate(invalid4));
		assertFalse(InputValidator.isValidBirthDate(invalid5));
		assertFalse(InputValidator.isValidBirthDate(invalid6));		
	}
	
	@Test
	public void isValidIBANTest() {
		// The method InputChecker.isValidIBAN doesn't actually test if the control number 
		// is generated from the rest of the IBAN, i.e. this test only checks the format.
		String valid1 = "NL10INGB0002323355";
		String valid2 = "NL35ABNA0003255223";
		
		String invalid1 = "NL35ABNA00032552";
		String invalid2 = "NL35ABNA000325D223";
		String invalid3 = "3L35ABNA000325D223";
		String invalid4 = "NLB5ABNA000325D223";
		
		assertTrue(InputValidator.isValidIBAN(valid1));
		assertTrue(InputValidator.isValidIBAN(valid2));
		assertFalse(InputValidator.isValidIBAN(invalid1));
		assertFalse(InputValidator.isValidIBAN(invalid2));
		assertFalse(InputValidator.isValidIBAN(invalid3));
		assertFalse(InputValidator.isValidIBAN(invalid4));		
	}
	
	@Test
	public void isValidCustomerTest() {
		String validBSN = "20325555";
		String invalidBSN = "20352d34";
		
		String validFirstName = "Justin";
		String invalidFirstName = "Ju5tin";
		
		String validSurname = "Praas";
		String invalidSurname = "Pr4as";
		
		String validStreetAddress = "Dr. Steenanjer 3B";
		String invalidStreetAddress = "D";
		
		String validEmailAddress = "jw.praas@gmail.com";
		String invalidEmailAddress = "jw.praas.@gmail.com";
		
		String validPhoneNumber = "0651023235";
		String invalidPhoneNumber = "063231d";
		
		String validBirthDate = "22-03-1998";
		String invalidBirthDate = "33-02-2000";
		
		assertTrue(InputValidator.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(invalidBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(validBSN, invalidFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(validBSN, validFirstName, 
				invalidSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(validBSN, validFirstName, 
				validSurname, invalidStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, invalidEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, invalidPhoneNumber, 
				validBirthDate));
		assertFalse(InputValidator.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				invalidBirthDate));
	}
	
	@Test
	public void isValidAmountTest() {
		String nonAmount1 = "342T2";
		String nonAmount2 = "10.5.3";
		String nonAmount3 = "0214";
		String amount1 = "0.5";
		String amount2 = "135";
		String amount3 = "30204.05";
		
		assert(InputValidator.isValidAmount(amount1));
		assert(InputValidator.isValidAmount(amount2));
		assert(InputValidator.isValidAmount(amount3));
		assert(!InputValidator.isValidAmount(nonAmount1));
		assert(!InputValidator.isValidAmount(nonAmount2));
		assert(!InputValidator.isValidAmount(nonAmount3));
	}
	
	@Test
	public void testIsValidCardNumber() {
		String cardNum = "3420244";
		String nonNum1 = "343T4453";
		String nonNum2 = "345T453";
		
		assert(InputValidator.isValidCardNumber(cardNum));
		assert(!InputValidator.isValidCardNumber(nonNum1));
		assert(!InputValidator.isValidCardNumber(nonNum2));
	}
	
	@Test
	public void testIsValidPIN() {
		String PIN = "4444";
		String nonPIN1 = "4A44";
		String nonPIN2 = "ABCD";
		
		assert(InputValidator.isValidPIN(PIN));
		assert(!InputValidator.isValidPIN(nonPIN1));
		assert(!InputValidator.isValidPIN(nonPIN2));
	}
}
