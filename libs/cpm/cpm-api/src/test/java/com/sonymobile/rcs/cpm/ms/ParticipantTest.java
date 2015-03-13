/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.sonymobile.rcs.cpm.ms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;

public class ParticipantTest {

    @Test
    public void testParse() {
        Participant p = new Participant("the name", "user1@localhost");
        assertEquals("the name <user1@localhost>", p.toString());

        Participant alf = new Participant("Alfred", "alf@sonytest.com");

        Set<Participant> li = Participant.asSet("joe <joe@sony.com>", "Alfred <alf@sonytest.com>");

        assertEquals(2, li.size());
        assertTrue(li.contains(alf));

        li = Participant.asSet("joe <joe@sony.com> ; Alfred <alf@sonytest.com> ; toto <toto@titi>");

        assertEquals(3, li.size());
        assertTrue(li.contains(alf));

        Participant other = Participant.parseString("mr soneone from somwehre   < allo@allo>");
        assertEquals("mr soneone from somwehre", other.getName());
        assertEquals("allo@allo", other.getCommunicationAddress());

    }

}
