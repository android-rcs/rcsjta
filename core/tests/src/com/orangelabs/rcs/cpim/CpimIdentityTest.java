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

package com.orangelabs.rcs.cpim;

import android.test.InstrumentationTestCase;

import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimIdentity;

public class CpimIdentityTest extends InstrumentationTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCpimIdentity() {
        String test1 = "<sip:user@domain.com>";
        String test2 = "\"Winnie the Pooh\" <tel:+33674538159>";
        String test3 = "Winnie the Pooh <im:pooh@100akerwood.com>";
        String test4 = "im:pooh@100akerwood.com";

        CpimIdentity id = new CpimIdentity(test1);
        assertTrue("test failed with " + test1,
                id.getDisplayName() == null && id.getUri().equals("sip:user@domain.com"));
        id = new CpimIdentity(test2);
        assertTrue("test failed with " + test2, id.getDisplayName().equals("Winnie the Pooh")
                && id.getUri().equals("tel:+33674538159"));
        id = new CpimIdentity(test3);
        assertTrue("test failed with " + test3, id.getDisplayName().equals("Winnie the Pooh")
                && id.getUri().equals("im:pooh@100akerwood.com"));
        Throwable exception = null;
        try {
            new CpimIdentity(test4);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue("test failed with " + test4, exception instanceof IllegalArgumentException);
    }

}
