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
package com.gsma.service.rcs.capabilities;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.capability.Capabilities;

public class CapabilitiesTest extends AndroidTestCase {

	private boolean imageSharing;
	private boolean videoSharing;
	private boolean imSession;
	private boolean fileTransfer;
	private boolean geolocPush;
	private boolean ipVoiceCall;
	private boolean ipVideoCall;
	private Set<String> extensions;
	private boolean automata;
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
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCapabilitiesNullSet() {
		Capabilities capabilities = new Capabilities(imageSharing, videoSharing, imSession, fileTransfer, geolocPush, ipVoiceCall,
				ipVideoCall, null, automata, timestamp, valid);
		Parcel parcel = Parcel.obtain();
		capabilities.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		Capabilities createFromParcel = Capabilities.CREATOR.createFromParcel(parcel);
		assertTrue(capabilitiesIsEqual(createFromParcel, capabilities));
	}

	public void testCapabilities() {
		Capabilities capabilities = new Capabilities(imageSharing, videoSharing, imSession, fileTransfer, geolocPush, ipVoiceCall,
				ipVideoCall, extensions, automata, timestamp, valid);
		Parcel parcel = Parcel.obtain();
		capabilities.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		Capabilities createFromParcel = Capabilities.CREATOR.createFromParcel(parcel);
		assertTrue(capabilitiesIsEqual(createFromParcel, capabilities));
	}

	public static boolean capabilitiesIsEqual(Capabilities cap1, Capabilities cap2) {
		if (cap1.isImageSharingSupported() != cap2.isImageSharingSupported()) {
			return false;
		}
		if (cap1.isVideoSharingSupported() != cap2.isVideoSharingSupported()) {
			return false;
		}
		if (cap1.isImSessionSupported() != cap2.isImSessionSupported()) {
			return false;
		}
		if (cap1.isFileTransferSupported() != cap2.isFileTransferSupported()) {
			return false;
		}
		if (cap1.isGeolocPushSupported() != cap2.isGeolocPushSupported()) {
			return false;
		}
		if (cap1.isIPVideoCallSupported() != cap2.isIPVideoCallSupported()) {
			return false;
		}
		if (cap1.isIPVoiceCallSupported() != cap2.isIPVoiceCallSupported()) {
			return false;
		}
		if (cap1.isAutomata() != cap2.isAutomata()) {
			return false;
		}
		if (cap1.getSupportedExtensions() != null) {
			if (!cap1.getSupportedExtensions().equals(cap2.getSupportedExtensions()))
				return false;
		} else {
			if (cap2.getSupportedExtensions() != null) {
				return false;
			}
		}
		if (cap1.getTimestamp() != cap2.getTimestamp()) {
			return false;
		}
		if (cap1.isValid() != cap2.isValid()) {
			return false;
		}
		return true;
	}
}