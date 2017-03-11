package testing;

import static org.junit.Assert.*;

import org.junit.Test;

import accounts.BankAccount;

public class IBANTest {
	
	String countryCode1 = "NL";
	String countryCode2 = "GB";
	
	String bankCode1 = "INGB";
	String bankCode2 = "ABNA";
	String bankCode3 = "WEST"; 
	String bankCode4 = "MIDL";
	
	String pan1 = "0002135586";
	String pan2 = "0443691118";
	String pan3 = "12345698765432";
	String pan4 = "40051512345678";
	
	String IBAN1 = "NL10INGB0002135586";
	String IBAN2 = "NL46ABNA0443691118";
	String IBAN3 = "GB82WEST12345698765432";
	String IBAN4 = "GB15MIDL40051512345678";

	@Test
	public void controlNumberCalculationTest() {
		int controlNumber1 = BankAccount.generateControlNumber(countryCode1, bankCode1, pan1);
		assertEquals(10, controlNumber1);
		
		int controlNumber2 = BankAccount.generateControlNumber(countryCode1, bankCode2, pan2);
		assertEquals(46, controlNumber2);
		
		int controlNumber3 = BankAccount.generateControlNumber(countryCode2, bankCode3, pan3);
		assertEquals(82, controlNumber3);
		
		int controlNumber4 = BankAccount.generateControlNumber(countryCode2, bankCode4, pan4);
		assertEquals(15, controlNumber4);
	}
	
	@Test
	public void generateIBANTest() {
		assertEquals(IBAN1, BankAccount.generateIBAN(countryCode1, bankCode1, pan1));
		assertEquals(IBAN2, BankAccount.generateIBAN(countryCode1, bankCode2, pan2));
		assertEquals(IBAN3, BankAccount.generateIBAN(countryCode2, bankCode3, pan3));
		assertEquals(IBAN4, BankAccount.generateIBAN(countryCode2, bankCode4, pan4));
	}

}
