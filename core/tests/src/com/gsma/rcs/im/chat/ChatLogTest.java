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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.im.chat;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

public class ChatLogTest extends AndroidTestCase {

    private Context mContext;

    private ContactUtil mContactUtils;

    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();
        mContactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetParticipants() {
        Map<ContactId, ParticipantStatus> participants;
        try {
            participants = ChatLog.GroupChat.getParticipants(mContext, "+330123=3,+330124=5");
            assertNotNull(participants);
            assertEquals(2, participants.size());

            ContactId contact1 = mContactUtils.formatContact("+330123");
            AbstractMap.SimpleEntry<ContactId, ParticipantStatus> formatContactParticipant1 = new HashMap.SimpleEntry<ContactId, ParticipantStatus>(
                    contact1, ParticipantStatus.CONNECTED);
            AbstractMap.SimpleEntry<ContactId, ParticipantStatus> participantsParticipant1 = new HashMap.SimpleEntry<ContactId, ParticipantStatus>(
                    contact1, participants.get(contact1));

            assertTrue(formatContactParticipant1.equals(participantsParticipant1));

            ContactId contact2 = mContactUtils.formatContact("+330124");
            AbstractMap.SimpleEntry<ContactId, ParticipantStatus> formatContactParticipant2 = new HashMap.SimpleEntry<ContactId, ParticipantStatus>(
                    contact2, ParticipantStatus.DEPARTED);
            AbstractMap.SimpleEntry<ContactId, ParticipantStatus> participantsParticipant2 = new HashMap.SimpleEntry<ContactId, ParticipantStatus>(
                    contact2, participants.get(contact2));

            assertTrue(formatContactParticipant2.equals(participantsParticipant2));
        } catch (RcsPermissionDeniedException e) {
            fail(e.getMessage());
        }
    }

}
