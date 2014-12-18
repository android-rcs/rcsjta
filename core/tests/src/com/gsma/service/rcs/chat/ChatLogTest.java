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
package com.gsma.service.rcs.chat;

import java.util.Set;

import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

public class ChatLogTest extends AndroidTestCase {

	private Context mContext;
	private ContactUtils mContactUtils;

	protected void setUp() throws Exception {
		super.setUp();

		mContext = getContext();
		mContactUtils = ContactUtils.getInstance(mContext);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetParticipantInfoNull() {
		Set<ParticipantInfo> participantInfos = ChatLog.GroupChat.getParticipantInfo(mContext, null);
		assertNull(participantInfos);
	}

	public void testGetParticipantInfo() {
		Set<ParticipantInfo> participantInfos = ChatLog.GroupChat.getParticipantInfo(mContext, "+330123=1,+330124=3");
		assertNotNull(participantInfos);
		assertEquals(2, participantInfos.size());
		ContactId participant1 = mContactUtils.formatContact("+330123");
		ParticipantInfo info1 = new ParticipantInfo(participant1, ParticipantInfo.Status.CONNECTED);
		assertTrue(participantInfos.contains(info1));
		ContactId participant2 = mContactUtils.formatContact("+330124");
		ParticipantInfo info2 = new ParticipantInfo(participant2, ParticipantInfo.Status.DEPARTED);
		assertTrue(participantInfos.contains(info2));
	}

	public void testgetGeolocNull() {
		try {
			ChatLog.getGeoloc(null);
			fail("Exception is expected");
		} catch (Exception e) {
			assertTrue(e instanceof RuntimeException);
		}
	}

	public void testgetGeolocInvalidData() {
		Geoloc geoloc = ChatLog.getGeoloc("abcd,2,3,4.0");
		assertTrue(geoloc == null);
	}

	public void testgetGeolocMissingData() {
		Geoloc geoloc = ChatLog.getGeoloc("label,1,2,3");
		assertTrue(geoloc == null);
	}

	public void testgetGeoloc() {
		Geoloc geoloc = ChatLog.getGeoloc("label,1,2,3,4.0");
		assertNotNull(geoloc);
		assertEquals("label", geoloc.getLabel());
		assertEquals(1.0d, geoloc.getLatitude());
		assertEquals(2.0d, geoloc.getLongitude());
		assertEquals(3L, geoloc.getExpiration());
		assertEquals(4.0f, geoloc.getAccuracy());
	}

	public void testgetGeolocNoLabel() {
		Geoloc geoloc = ChatLog.getGeoloc("1,2,3,4.0");
		assertNotNull(geoloc);
		assertEquals(null, geoloc.getLabel());
		assertEquals(1.0d, geoloc.getLatitude());
		assertEquals(2.0d, geoloc.getLongitude());
		assertEquals(3L, geoloc.getExpiration());
		assertEquals(4.0f, geoloc.getAccuracy());
	}

}