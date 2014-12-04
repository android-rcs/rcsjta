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

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

public class ParticipantInfoTest extends AndroidTestCase {

	private ContactId contact;

	protected void setUp() throws Exception {
		super.setUp();
		ContactUtils contactUtils = ContactUtils.getInstance(getContext());
		contact = contactUtils.formatContact("+33123456789");

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private void participantInfoisEqual(ParticipantInfo participantInfo1, ParticipantInfo participantInfo2) {
		assertEquals(participantInfo1.getStatus(), participantInfo2.getStatus());
		if (participantInfo1.getContact() != null) {
			assertEquals(participantInfo1.getContact(), participantInfo2.getContact());
		} else {
			if (participantInfo2.getContact() != null) {
				fail("Only one ParticipantInfo is null");
			}
		}
	}

	public void testParticipantInfoContactNull() {
		ParticipantInfo participant = new ParticipantInfo((ContactId) null);
		Parcel parcel = Parcel.obtain();
		participant.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		ParticipantInfo createFromParcel = ParticipantInfo.CREATOR.createFromParcel(parcel);
		participantInfoisEqual(createFromParcel, participant);
	}

	public void testParticipantInfo() {
		ParticipantInfo participant = new ParticipantInfo(contact);
		Parcel parcel = Parcel.obtain();
		participant.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		ParticipantInfo createFromParcel = ParticipantInfo.CREATOR.createFromParcel(parcel);
		participantInfoisEqual(createFromParcel, participant);
	}
}