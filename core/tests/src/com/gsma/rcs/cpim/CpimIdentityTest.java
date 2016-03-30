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

    public void testCpimIdentity() {
        String test1 = "<sip:user@domain.com>";
        String test2 = "\"Winnie the Pooh\" <tel:+33674538159>";
        String test3 = "Winnie the Pooh <im:pooh@100akerwood.com>";
        String test4 = "im:pooh@100akerwood.com";

        CpimIdentity id;
        id = new CpimIdentity(test1);
        assertTrue("test failed with " + test1, id.getDisplayName() == null
                && "sip:user@domain.com".equals(id.getUri()));
        id = new CpimIdentity(test2);
        assertTrue("test failed with " + test2, "Winnie the Pooh".equals(id.getDisplayName())
                && "tel:+33674538159".equals(id.getUri()));
        id = new CpimIdentity(test3);
        assertTrue("test failed with " + test3, "Winnie the Pooh".equals(id.getDisplayName())
                && "im:pooh@100akerwood.com".equals(id.getUri()));
        Throwable exception = null;
        try {
            new CpimIdentity(test4);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue("test failed with " + test4, exception instanceof IllegalArgumentException);
    }

}
