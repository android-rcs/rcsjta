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

import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;
import android.text.TextUtils;

public class ContactUtilsTest extends AndroidTestCase {

    private ContactUtil mContactUtils;
    private String mNextCountryAreaCode;

    protected void setUp() throws Exception {
        super.setUp();
        mContactUtils = ContactUtil.getInstance(new ContactUtilMockContext(getContext()));
        mNextCountryAreaCode = String.valueOf(Integer
                .valueOf(ContactUtilMockContext.COUNTRY_AREA_CODE) + 1);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsValidContactNull() {
        try {
            assertFalse(mContactUtils.isValidContact(null));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactInvalidLength() {
        try {
            assertFalse(mContactUtils.isValidContact("+"));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactInvalidLength_min() {
        try {
            assertFalse(mContactUtils.isValidContact(""));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactInvalidLength_max() {
        try {
            assertFalse(mContactUtils.isValidContact("0123456789012345"));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactInvalidCharacter() {
        try {
            assertFalse(mContactUtils.isValidContact("012345a"));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactWrongAreaCode() {
        try {
            assertFalse(mContactUtils.isValidContact(mNextCountryAreaCode.concat("123456789")));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_1() {
        try {
            assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                    .concat("123456789")));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_2() {
        try {
            assertTrue(mContactUtils.isValidContact("+123456789"));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_3() {
        try {
            assertTrue(mContactUtils.isValidContact("+012345678901234"));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_4() {
        try {
            assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                    .concat("12345678901234")));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_5() {
        try {
            assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                    .concat("1 2 3 4 5 6 7 8 9 0 1 2 3 4")));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_6() {
        try {
            assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                    .concat("-1-2-3-4-5-6-7-8-9-0-1-2-3-4")));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_7() {
        try {
            assertTrue(mContactUtils.isValidContact(new StringBuilder(" ")
                    .append(ContactUtilMockContext.COUNTRY_AREA_CODE)
                    .append("-1 2-3 4-5 6-7 8-9 0-1 2-3 4 ").toString()));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactNormalCase_8() {
        try {
            assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                    .concat("    1--------2-3 4-5 6-7 8-9 0-1 2-3 4 ")));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testFormatContactIdNull() {
        try {
            mContactUtils.formatContact(null);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e instanceof IllegalArgumentException);
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testFormatContactIdWithInternationalNumbering() {
        try {
            ContactId cid = mContactUtils.formatContact("+33612345678");
            assertEquals("+33612345678", cid.toString());
            cid = mContactUtils.formatContact("+34612345678");
            assertEquals("+34612345678", cid.toString());
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testFormatContactIdWithLocalNumbering() {
        try {
            String cac = mContactUtils.getMyCountryAreaCode();
            if (TextUtils.isEmpty(cac))
                return;
            String cc = mContactUtils.getMyCountryCode();
            ContactId cid = mContactUtils.formatContact(cac + "612345678");
            assertEquals(cc + "612345678", cid.toString());
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testFormatContactIdWithLocalNumberingButDifferentCountryAreaCode() {
        try {
            String cac = mContactUtils.getMyCountryAreaCode();

            if (TextUtils.isEmpty(cac))
                return;
            try {
                int cacInteger = Integer.parseInt(cac);
                mContactUtils.formatContact((++cacInteger) + "612345678");
                fail("IllegalArgumentException expected");
            } catch (Exception e) {
                assertTrue(e instanceof IllegalArgumentException);
            }
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testFormatContactIdWithPrefixedInternationalNumbering() {
        try {
            String cc = mContactUtils.getMyCountryCode();
            try {
                ContactId cid = mContactUtils.formatContact("00" + cc.substring(1) + "612345678");
                assertEquals(cc + "612345678", cid.toString());
                int ccInteger = Integer.parseInt(cc.substring(1)) + 1;
                cid = mContactUtils.formatContact("00" + ccInteger + "612345678");
                assertEquals("+" + ccInteger + "612345678", cid.toString());
            } catch (IllegalArgumentException e) {
                fail("IllegalArgumentException thrown");
            }
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testGetMyCountryAreaCode() {
        try {
            assertEquals(ContactUtilMockContext.COUNTRY_AREA_CODE,
                    mContactUtils.getMyCountryAreaCode());
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testGetMyCountryCode() {
        try {
            assertEquals(ContactUtilMockContext.COUNTRY_CODE, mContactUtils.getMyCountryCode());
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }
}
