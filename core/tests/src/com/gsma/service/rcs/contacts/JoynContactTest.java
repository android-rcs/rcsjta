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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.service.rcs.capabilities.CapabilitiesTest;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.JoynContact;

public class JoynContactTest extends AndroidTestCase {

	private boolean imageSharing;
	private boolean videoSharing;
	private boolean imSession;
	private boolean fileTransfer;
	private boolean geolocPush;
	private boolean ipVoiceCall;
	private boolean ipVideoCall;
	private Set<String> extensions;
	private boolean automata;
	private Capabilities capabilities;
	private boolean registered;
	private ContactId contactId;

	protected void setUp() throws Exception {
		super.setUp();
		Random random = new Random();
		imageSharing = random.nextBoolean();
		videoSharing = random.nextBoolean();
		imSession = random.nextBoolean();
		fileTransfer = random.nextBoolean();
		geolocPush = random.nextBoolean();
		ipVideoCall = random.nextBoolean();
		ipVoiceCall = random.nextBoolean();
		automata = random.nextBoolean();
		extensions = new HashSet<String>();
		extensions.add(String.valueOf(random.nextInt(96) + 32));
		extensions.add(String.valueOf(random.nextInt(96) + 32));
		capabilities = new Capabilities(imageSharing, videoSharing, imSession, fileTransfer, geolocPush, ipVoiceCall, ipVideoCall,
				extensions, automata);
		registered = random.nextBoolean();
		ContactUtils contactUtils = ContactUtils.getInstance(getContext());
		contactId = contactUtils.formatContactId("+33123456789");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testJoynContactContactNull() {
		JoynContact joynContact = new JoynContact(null, registered, capabilities);
		Parcel parcel = Parcel.obtain();
		joynContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		JoynContact createFromParcel = JoynContact.CREATOR.createFromParcel(parcel);
		assertTrue(joynContactIsEqual(createFromParcel, joynContact));
	}
	
	public void testJoynContactCapabilitiesNull() {
		JoynContact joynContact = new JoynContact(contactId, registered, null);
		Parcel parcel = Parcel.obtain();
		joynContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		JoynContact createFromParcel = JoynContact.CREATOR.createFromParcel(parcel);
		assertTrue(joynContactIsEqual(createFromParcel, joynContact));
	}
	
	public void testJoynContact() {
		JoynContact joynContact = new JoynContact(contactId, registered, capabilities);
		Parcel parcel = Parcel.obtain();
		joynContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		JoynContact createFromParcel = JoynContact.CREATOR.createFromParcel(parcel);
		assertTrue(joynContactIsEqual(createFromParcel, joynContact));
	}

	private boolean joynContactIsEqual(JoynContact joyn1, JoynContact joyn2) {
		if (joyn1.isRegistered() != joyn2.isRegistered())
			return false;
		if (joyn1.getContactId() != null) {
			if (!joyn1.getContactId().equals(joyn2.getContactId()))
				return false;
		} else {
			if (joyn2.getContactId() != null)
				return false;
		}
		if (joyn1.getCapabilities() != null) {
			if (!CapabilitiesTest.capabilitiesIsEqual(joyn1.getCapabilities(), joyn2.getCapabilities()))
				return false;
		} else {
			if (joyn2.getCapabilities() != null)
				return false;
		}
		return true;
	}
}
