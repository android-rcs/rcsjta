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

public class ContactUtilsTest extends AndroidTestCase {

	private ContactUtils mContactUtils;

	protected void setUp() throws Exception {
		super.setUp();
		mContactUtils = ContactUtils.getInstance(getContext());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsValidContactNull() {
		assertFalse(mContactUtils.isValidContact(null));
	}

	public void testIsValidContactInvalidLength() {
		assertFalse(mContactUtils.isValidContact("+"));
	}

	public void testIsValidContactInvalidLength_min() {
		assertFalse(mContactUtils.isValidContact(""));
	}

	public void testIsValidContactInvalidLength_max() {
		assertFalse(mContactUtils.isValidContact("0123456789012345"));
	}

	public void testIsValidContactInvalidCharacter() {
		assertFalse(mContactUtils.isValidContact("012345a"));
	}

	public void testIsValidContactNormalCase_1() {
		assertTrue(mContactUtils.isValidContact("0123456789"));
	}

	public void testIsValidContactNormalCase_2() {
		assertTrue(mContactUtils.isValidContact("+123456789"));
	}

	public void testIsValidContactNormalCase_3() {
		assertTrue(mContactUtils.isValidContact("+012345678901234"));
	}

	public void testIsValidContactNormalCase_4() {
		assertTrue(mContactUtils.isValidContact("012345678901234"));
	}

	public void testIsValidContactNormalCase_5() {
		assertTrue(mContactUtils.isValidContact("0 1 2 3 4 5 6 7 8 9 0 1 2 3 4"));
	}

	public void testIsValidContactNormalCase_6() {
		assertTrue(mContactUtils.isValidContact("0-1-2-3-4-5-6-7-8-9-0-1-2-3-4"));
	}

	public void testIsValidContactNormalCase_7() {
		assertTrue(mContactUtils.isValidContact(" 0-1 2-3 4-5 6-7 8-9 0-1 2-3 4 "));
	}

	public void testIsValidContactNormalCase_8() {
		assertTrue(mContactUtils.isValidContact("0    1--------2-3 4-5 6-7 8-9 0-1 2-3 4 "));
	}

	public void testFormatContactIdNull() {
		try {
			mContactUtils.formatContact(null);
			fail("Expected RcsContactFormatException to be thrown");
		} catch (RcsContactFormatException e) {
			assertTrue(e instanceof RcsContactFormatException);
		}
	}

	public void testFormatContactIdWithInternationalNumbering() {
		try {
			ContactId cid = mContactUtils.formatContact("+33612345678");
			assertEquals("+33612345678", cid.toString());
			cid = mContactUtils.formatContact("+34612345678");
			assertEquals("+34612345678", cid.toString());
		} catch (RcsContactFormatException e) {
			fail("RcsContactFormatException thrown");
		}
	}

	public void testFormatContactIdWithLocalNumbering() {
		String cac = mContactUtils.getMyCountryAreaCode();
		if (TextUtils.isEmpty(cac))
			return;
		String cc = mContactUtils.getMyCountryCode();
		try {
			ContactId cid = mContactUtils.formatContact(cac + "612345678");
			assertEquals(cc + "612345678", cid.toString());
		} catch (RcsContactFormatException e) {
			fail("RcsContactFormatException thrown");
		}
	}

	public void testFormatContactIdWithLocalNumberingButDifferentCountryAreaCode() {
		String cac = mContactUtils.getMyCountryAreaCode();
		if (TextUtils.isEmpty(cac))
			return;
		try {
			int cacInteger = Integer.parseInt(cac);
			mContactUtils.formatContact((++cacInteger) + "612345678");
			fail("RcsContactFormatException expected");
		} catch (Exception e) {
			assertTrue(e instanceof RcsContactFormatException);
		}
	}

	public void testFormatContactIdWithPrefixedInternationalNumbering() {
		String cc = mContactUtils.getMyCountryCode();
		try {
			ContactId cid = mContactUtils.formatContact("00" + cc.substring(1) + "612345678");
			assertEquals(cc + "612345678", cid.toString());
			int ccInteger = Integer.parseInt(cc.substring(1)) + 1;
			cid = mContactUtils.formatContact("00" + ccInteger + "612345678");
			assertEquals("+" + ccInteger + "612345678", cid.toString());
		} catch (RcsContactFormatException e) {
			fail("RcsContactFormatException thrown");
		}
	}
}
