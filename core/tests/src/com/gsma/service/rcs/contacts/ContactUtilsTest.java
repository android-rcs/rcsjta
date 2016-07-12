/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

    public void testIsValidContactNull() {
        try {
            assertFalse(mContactUtils.isValidContact(null));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

    public void testIsValidContactInvalidLength() throws RcsPermissionDeniedException {
        assertFalse(mContactUtils.isValidContact("+"));
    }

    public void testIsValidContactInvalidLength_min() throws RcsPermissionDeniedException {
        assertFalse(mContactUtils.isValidContact(""));
    }

    public void testIsValidContactInvalidLength_max() throws RcsPermissionDeniedException {
        assertFalse(mContactUtils.isValidContact("0123456789012345"));
    }

    public void testIsValidContactInvalidCharacter() throws RcsPermissionDeniedException {
        assertFalse(mContactUtils.isValidContact("012345a"));
    }

    public void testIsValidContactWrongAreaCode() throws RcsPermissionDeniedException {
        assertFalse(mContactUtils.isValidContact(mNextCountryAreaCode.concat("123456789")));
    }

    public void testIsValidContactNormalCase_1() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                .concat("123456789")));
    }

    public void testIsValidContactNormalCase_2() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact("+123456789"));
    }

    public void testIsValidContactNormalCase_3() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact("+012345678901234"));
    }

    public void testIsValidContactNormalCase_4() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                .concat("12345678901234")));
    }

    public void testIsValidContactNormalCase_5() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                .concat("1 2 3 4 5 6 7 8 9 0 1 2 3 4")));
    }

    public void testIsValidContactNormalCase_6() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                .concat("-1-2-3-4-5-6-7-8-9-0-1-2-3-4")));
    }

    public void testIsValidContactNormalCase_7() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact(" " + ContactUtilMockContext.COUNTRY_AREA_CODE
                + "-1 2-3 4-5 6-7 8-9 0-1 2-3 4 "));
    }

    public void testIsValidContactNormalCase_8() throws RcsPermissionDeniedException {
        assertTrue(mContactUtils.isValidContact(ContactUtilMockContext.COUNTRY_AREA_CODE
                .concat("    1--------2-3 4-5 6-7 8-9 0-1 2-3 4 ")));
    }

    public void testFormatContactIdNull() throws RcsPermissionDeniedException {
        try {
            mContactUtils.formatContact(null);
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException ignore) {
        }
    }

    public void testFormatContactIdWithInternationalNumbering() throws RcsPermissionDeniedException {
        ContactId cid = mContactUtils.formatContact("+33612345678");
        assertEquals("+33612345678", cid.toString());
        cid = mContactUtils.formatContact("+34612345678");
        assertEquals("+34612345678", cid.toString());
    }

    public void testFormatContactIdWithLocalNumbering() throws RcsPermissionDeniedException {
        String cac = mContactUtils.getMyCountryAreaCode();
        if (TextUtils.isEmpty(cac))
            return;
        String cc = mContactUtils.getMyCountryCode();
        ContactId cid = mContactUtils.formatContact(cac + "612345678");
        assertEquals(cc + "612345678", cid.toString());
    }

    public void testFormatContactIdWithLocalNumberingButDifferentCountryAreaCode()
            throws RcsPermissionDeniedException {
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
    }

    public void testFormatContactIdWithPrefixedInternationalNumbering()
            throws RcsPermissionDeniedException {
        String cc = mContactUtils.getMyCountryCode();
        ContactId cid = mContactUtils.formatContact("00" + cc.substring(1) + "612345678");
        assertEquals(cc + "612345678", cid.toString());
        int ccInteger = Integer.parseInt(cc.substring(1)) + 1;
        cid = mContactUtils.formatContact("00" + ccInteger + "612345678");
        assertEquals("+" + ccInteger + "612345678", cid.toString());
    }

    public void testGetMyCountryAreaCode() throws RcsPermissionDeniedException {
        assertEquals(ContactUtilMockContext.COUNTRY_AREA_CODE, mContactUtils.getMyCountryAreaCode());
    }

}
