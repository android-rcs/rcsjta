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
import com.gsma.services.rcs.contacts.RcsContact;

public class RcsContactTest extends AndroidTestCase {

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
	private String displayName;
	private long timestamp;
	private boolean valid;

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
		timestamp = random.nextLong();
		valid = random.nextBoolean();

		capabilities = new Capabilities(imageSharing, videoSharing, imSession, fileTransfer, geolocPush, ipVoiceCall, ipVideoCall,
				extensions, automata, timestamp, valid);
		registered = random.nextBoolean();
		ContactUtils contactUtils = ContactUtils.getInstance(getContext());
		contactId = contactUtils.formatContact("+33123456789");
		displayName = "displayName";
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRcsContactContactNull() {
		RcsContact rcsContact = new RcsContact(null, registered, capabilities, displayName);
		Parcel parcel = Parcel.obtain();
		rcsContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
		assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
	}
	
	public void testRcsContactCapabilitiesNull() {
		RcsContact rcsContact = new RcsContact(contactId, registered, null, displayName);
		Parcel parcel = Parcel.obtain();
		rcsContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
		assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
	}
	
	public void testRcsContactDisplayNameNull() {
		RcsContact rcsContact = new RcsContact(contactId, registered, capabilities, null);
		Parcel parcel = Parcel.obtain();
		rcsContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
		assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
	}
	
	public void testRcsContact() {
		RcsContact rcsContact = new RcsContact(contactId, registered, capabilities, displayName);
		Parcel parcel = Parcel.obtain();
		rcsContact.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
		assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
	}

	private boolean rcsContactIsEqual(RcsContact rcs1, RcsContact rcs2) {
		if (rcs1.isRegistered() != rcs2.isRegistered()) {
			return false;
		}
		if (rcs1.getContactId() != null) {
			if (!rcs1.getContactId().equals(rcs2.getContactId())) {
				return false;
			}
		} else {
			if (rcs2.getContactId() != null) {
				return false;
			}
		}
		if (rcs1.getCapabilities() != null) {
			if (!CapabilitiesTest.capabilitiesIsEqual(rcs1.getCapabilities(), rcs2.getCapabilities())) {
				return false;
			}
		} else {
			if (rcs2.getCapabilities() != null) {
				return false;
			}
		}
		if (rcs1.getDisplayName() != null) {
			if (!rcs1.getDisplayName().equals(rcs2.getDisplayName())) {
				return false;
			}
		} else {
			if (rcs2.getDisplayName() != null) {
				return false;
			}
		}
		return true;
	}
}
