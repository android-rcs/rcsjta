/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sonymobile.rcs.cpm.ms.Participant;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;

public class GroupStateImplTest {

    @Test
    public void test() throws IOException, XmlPullParserException, ParseException {
        String xml = "<groupstate timestamp=\"2012-06-13T16:39:57-05:00\" lastfocussessionid=\"da9274453@company.com\" group-type=\"closed\">"
                + "<participant name=\"bob\" comm-addr=\"tel:+16135551212\"/>"
                + "<participant name=\"alice\" comm-addr=\"tel:+15195551212\"/>"
                + "<participant name=\"bernie\" comm-addr=\"tel:+15145551212\"/>" + "</groupstate>";
        GroupStateImpl gs = GroupStateImpl.fromXml(1, null, xml);
        String xml2 = gs.toXml();
        assertEquals(3, gs.getParticipants().size());
        Participant p = new Participant("", "tel:+16135551212");
        assertTrue(gs.getParticipants().contains(p));
        p = new Participant("", "tel:+15145551212");
        assertTrue(gs.getParticipants().contains(p));

    }

}
