package com.orangelabs.rcs.utils;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.provider.settings.RcsSettings;

public class PhoneUtilsTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
		
		RcsSettings.createInstance(getContext());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFranceNumber() {
		RcsSettings.getInstance().setCountryCode("+33");
		RcsSettings.getInstance().setCountryAreaCode("0");
		PhoneUtils.initialize(getContext());

		assertEquals(PhoneUtils.formatNumberToInternational("0033121345678"), "+33121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("0121345678"), "+33121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("+33121345678"), "+33121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("33121345678"), "+3333121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("123"), "+33123");
		assertEquals(PhoneUtils.formatNumberToInternational("+33004010001"), "+33004010001");
	}

	public void testSpainNumber() {
		RcsSettings.getInstance().setCountryCode("+34");
		RcsSettings.getInstance().setCountryAreaCode("");
		PhoneUtils.initialize(getContext());

		assertEquals(PhoneUtils.formatNumberToInternational("0034121345678"), "+34121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("121345678"), "+34121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("+34121345678"), "+34121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("34121345678"),	"+3434121345678");
		assertEquals(PhoneUtils.formatNumberToInternational("123"), "+34123");
	}

}
