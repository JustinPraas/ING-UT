package testing;

import static org.junit.Assert.*;
import org.junit.Test;

import client.InputChecker;

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
		
		assertFalse(InputChecker.isNumericalOnly(invalid1));
		assertFalse(InputChecker.isNumericalOnly(invalid2));
		assertFalse(InputChecker.isNumericalOnly(invalid3));
		assertFalse(InputChecker.isNumericalOnly(invalid4));
		assertTrue(InputChecker.isNumericalOnly(valid));
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

		assertTrue(InputChecker.isAlphabeticalOnly(valid1));
		assertTrue(InputChecker.isAlphabeticalOnly(valid2));
		assertTrue(InputChecker.isAlphabeticalOnly(valid3));
		assertTrue(InputChecker.isAlphabeticalOnly(valid4));
		assertFalse(InputChecker.isAlphabeticalOnly(invalid1));
		assertFalse(InputChecker.isAlphabeticalOnly(invalid2));
		assertFalse(InputChecker.isAlphabeticalOnly(invalid3));
		assertFalse(InputChecker.isAlphabeticalOnly(invalid4));
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

		assertTrue(InputChecker.isValidBSN(valid1));
		assertTrue(InputChecker.isValidBSN(valid2));
		assertFalse(InputChecker.isValidBSN(invalid1));
		assertFalse(InputChecker.isValidBSN(invalid2));
		assertFalse(InputChecker.isValidBSN(invalid3));
		assertFalse(InputChecker.isValidBSN(invalid4));
		assertFalse(InputChecker.isValidBSN(invalid5));
	}
	
	@Test
	public void isValidNameTest() {
		String valid1 = "Justin";
		String valid2 = "Ab";
		
		String invalid1 = "A"; // Too short
		String invalid2 = "3Praas";
		String invalid3 = "JustinPraasSteenanjerNijverdalTheNetherlands"; // Too long
		String invalid4 = "333";
		
		assertTrue(InputChecker.isValidName(valid1));
		assertTrue(InputChecker.isValidName(valid2));
		assertFalse(InputChecker.isValidName(invalid1));
		assertFalse(InputChecker.isValidName(invalid2));
		assertFalse(InputChecker.isValidName(invalid3));
		assertFalse(InputChecker.isValidName(invalid4));
	}
	
	@Test
	public void isValidStreetAddressTest() {
		String valid1 = "Steenanjer 3";
		String valid2 = "Van Dam Straat 3A";
		String valid3 = "Dr. Pieter-straat 18B";
		
		String invalid1 = "3Delige madeliefstraat";
		String invalid2 = "d";

		assertTrue(InputChecker.isValidStreetAddress(valid1));
		assertTrue(InputChecker.isValidStreetAddress(valid2));
		assertTrue(InputChecker.isValidStreetAddress(valid3));
		assertFalse(InputChecker.isValidStreetAddress(invalid1));
		assertFalse(InputChecker.isValidStreetAddress(invalid2));
	}
	
	@Test
	public void isValidEmailAddressTest() {
		String valid1 = "jw.praas@gmail.com";
		String valid2 = "a.h.r.jansen@co.uk.com";
		
		String invalid1 = "a.b.c.@gmail.com";
		String invalid2 = "a.jansen@.gmail.org";
		
		assertTrue(InputChecker.isValidEmailAddress(valid1));
		assertTrue(InputChecker.isValidEmailAddress(valid2));
		assertFalse(InputChecker.isValidEmailAddress(invalid1));
		assertFalse(InputChecker.isValidEmailAddress(invalid2));
	}
	
	@Test
	public void isValidPhoneNumberTest() {
		String valid1 = "0651023395";
		String valid2 = "0400083103";
		
		String invalid1 = "0651027d3395";
		String invalid2 = "065102779355";
		String invalid3 = "06510277";
		
		assertTrue(InputChecker.isValidPhoneNumber(valid1));
		assertTrue(InputChecker.isValidPhoneNumber(valid2));
		assertFalse(InputChecker.isValidPhoneNumber(invalid1));
		assertFalse(InputChecker.isValidPhoneNumber(invalid2));
		assertFalse(InputChecker.isValidPhoneNumber(invalid3));
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
		
		assertTrue(InputChecker.isValidBirthDate(valid1));
		assertTrue(InputChecker.isValidBirthDate(valid2));
		assertTrue(InputChecker.isValidBirthDate(valid3));
		assertFalse(InputChecker.isValidBirthDate(invalid1));
		assertFalse(InputChecker.isValidBirthDate(invalid2));
		assertFalse(InputChecker.isValidBirthDate(invalid3));
		assertFalse(InputChecker.isValidBirthDate(invalid4));
		assertFalse(InputChecker.isValidBirthDate(invalid5));
		assertFalse(InputChecker.isValidBirthDate(invalid6));		
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
		
		assertTrue(InputChecker.isValidIBAN(valid1));
		assertTrue(InputChecker.isValidIBAN(valid2));
		assertFalse(InputChecker.isValidIBAN(invalid1));
		assertFalse(InputChecker.isValidIBAN(invalid2));
		assertFalse(InputChecker.isValidIBAN(invalid3));
		assertFalse(InputChecker.isValidIBAN(invalid4));		
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
		
		assertTrue(InputChecker.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(invalidBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(validBSN, invalidFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(validBSN, validFirstName, 
				invalidSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(validBSN, validFirstName, 
				validSurname, invalidStreetAddress, validEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, invalidEmailAddress, validPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, invalidPhoneNumber, 
				validBirthDate));
		assertFalse(InputChecker.isValidCustomer(validBSN, validFirstName, 
				validSurname, validStreetAddress, validEmailAddress, validPhoneNumber, 
				invalidBirthDate));
	}
}
