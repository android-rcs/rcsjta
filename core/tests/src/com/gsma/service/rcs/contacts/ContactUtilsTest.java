/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.service.rcs.contacts;

import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;

public class ContactUtilsTest extends AndroidTestCase {

	private RcsSettings rcsSettings;
	private ContactUtils contactUtils;
	
	protected void setUp() throws Exception {
		super.setUp();
		RcsSettings.createInstance(getContext());
		rcsSettings = RcsSettings.getInstance();
		assertNotNull("Cannot instantiate RcsSettings", rcsSettings);
		contactUtils = ContactUtils.getInstance(getContext());
		assertNotNull("Cannot instantiate ContactUtils", contactUtils);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsValidContactNull() {
		assertFalse(contactUtils.isValidContact(null));
	}

	public void testIsValidContactInvalidLength() {
		assertFalse(contactUtils.isValidContact("+"));
	}

	public void testIsValidContactInvalidLength_min() {
		assertFalse(contactUtils.isValidContact(""));
	}

	public void testIsValidContactInvalidLength_max() {
		assertFalse(contactUtils.isValidContact("0123456789012345"));
	}

	public void testIsValidContactInvalidCharacter() {
		assertFalse(contactUtils.isValidContact("012345a"));
	}

	public void testIsValidContactNormalCase_1() {
		assertTrue(contactUtils.isValidContact("0123456789"));
	}

	public void testIsValidContactNormalCase_2() {
		assertTrue(contactUtils.isValidContact("+123456789"));
	}

	public void testIsValidContactNormalCase_3() {
		assertTrue(contactUtils.isValidContact("+012345678901234"));
	}

	public void testIsValidContactNormalCase_4() {
		assertTrue(contactUtils.isValidContact("012345678901234"));
	}

	public void testIsValidContactNormalCase_5() {
		assertTrue(contactUtils.isValidContact("0 1 2 3 4 5 6 7 8 9 0 1 2 3 4"));
	}

	public void testIsValidContactNormalCase_6() {
		assertTrue(contactUtils.isValidContact("0-1-2-3-4-5-6-7-8-9-0-1-2-3-4"));
	}

	public void testIsValidContactNormalCase_7() {
		assertTrue(contactUtils.isValidContact(" 0-1 2-3 4-5 6-7 8-9 0-1 2-3 4 "));
	}

	public void testIsValidContactNormalCase_8() {
		assertTrue(contactUtils.isValidContact("0    1--------2-3 4-5 6-7 8-9 0-1 2-3 4 "));
	}

	public void testFormatContactIdNull() {
		try {
			contactUtils.formatContact(null);
			fail("Expected RcsContactFormatException to be thrown");
		} catch (RcsContactFormatException e) {
			assertTrue(e instanceof RcsContactFormatException);
		}
	}

	public void testFormatContactIdWithInternationalNumbering() {
		try {
			ContactId cid = contactUtils.formatContact("+33612345678");
			assertTrue(cid.toString().equals("+33612345678"));
		} catch (RcsContactFormatException e) {
			fail("RcsContactFormatException thrown");
		}
	}
	
	public void testFormatContactIdWithLocalNumbering() {
		String cac = rcsSettings.getCountryAreaCode();
		if (TextUtils.isEmpty(cac)) 
			return;
		String cc = rcsSettings.getCountryCode();
		try {
			ContactId cid = contactUtils.formatContact(cac+"612345678");
			assertTrue(cid.toString().equals(cc+"612345678"));
		} catch (RcsContactFormatException e) {
			fail("RcsContactFormatException thrown");
		}
	}
	
	public void testFormatContactIdWithLocalNumberingButDifferentCountryAreaCode() {
		String cac = rcsSettings.getCountryAreaCode();
		if (TextUtils.isEmpty(cac)) 
			return;
		try {
			int cacInteger = Integer.parseInt(cac);
			contactUtils.formatContact((++cacInteger)+"612345678");
			fail("RcsContactFormatException expected");
		} catch (Exception e) {
			assertTrue(e instanceof RcsContactFormatException);
		}
	}
	
	public void testFormatContactIdWithPrefixedInternationalNumbering() {
		String cc = rcsSettings.getCountryCode();
		try {
			ContactId cid = contactUtils.formatContact("00"+cc.substring(1)+"612345678");
			assertTrue(cid.toString().equals(cc+"612345678"));
		} catch (RcsContactFormatException e) {
			fail("RcsContactFormatException thrown");
		}
	}
}
