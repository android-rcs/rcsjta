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

package com.gsma.rcs.cpim;

import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimIdentity;

import android.test.InstrumentationTestCase;

public class CpimIdentityTest extends InstrumentationTestCase {

    public void testInvalidUri() {
        String uri = "im:pooh@100akerwood.com";
        try {
            new CpimIdentity(uri);
            fail("Failed to detect unknown URI (" + uri + ")");

        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    public void testSipUri() {
        String uri = "sip:user@domain.com";
        CpimIdentity id = new CpimIdentity("<" + uri + ">");
        assertNull(id.getDisplayName());
        assertEquals(uri, id.getUri());
    }

    public void testTelUriWithDisplayName() {
        String displayName = "\"Winnie the Pooh\"";
        String uri = "tel:+33674538159";
        CpimIdentity id = new CpimIdentity(displayName + " <" + uri + ">");
        assertEquals("Winnie the Pooh", id.getDisplayName());
        assertEquals(uri, id.getUri());
    }

    public void testTelUriWithDisplayNameBis() {
        String displayName = "Winnie the Pooh";
        String uri = "tel:+33674538159";
        CpimIdentity id = new CpimIdentity(displayName + " <" + uri + ">");
        assertEquals(displayName, id.getDisplayName());
        assertEquals(uri, id.getUri());
    }

    public void testTelUriWithAcceptContact() {
        String displayName = "Winnie the Pooh";
        String uri = "tel:+33674538159";
        String acceptContact = "%2Bsip.instance%3D%22%3Curn%3Agsma%3Aimei%3A35824005-944763-1%3E%22";
        CpimIdentity id = new CpimIdentity(displayName + " <" + uri + "?Accept-Contact="
                + acceptContact + ">");
        assertEquals("Winnie the Pooh", id.getDisplayName());
        assertEquals("tel:+33674538159?Accept-Contact=" + acceptContact, id.getUri());
    }

}
