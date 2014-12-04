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

import java.util.Random;

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

public class GeolocMessageTest extends AndroidTestCase {

	private String messageId;
	private long receiptAt;
	private long sentAt;
	private ContactId remote;
	private double longitude;
	private long expiration;
	private String label;
	private double latitude;
	private Geoloc geoloc;

	private void geolocMessageIsEqual(GeolocMessage geolocMessage1, GeolocMessage geolocMessage2) {
		assertEquals(geolocMessage1.getId(), geolocMessage2.getId());
		assertEquals(geolocMessage1.getRemoteContact(), geolocMessage2.getRemoteContact());
		// TODO These methods will be implemented in CR018
		// assertEquals(geolocMessage1.getTimestamp(), geolocMessage2.getTimestamp());
		// assertEquals(geolocMessage1.getTimestampSent(), geolocMessage2.getTimestampSent());

		if (geolocMessage1.getGeoloc() != null) {
			if (geolocMessage2.getGeoloc() == null) {
				fail("One geoloc message is null");
			}
			assertEquals(geolocMessage1.getGeoloc().getExpiration(), geolocMessage2.getGeoloc().getExpiration());
			assertEquals(geolocMessage1.getGeoloc().getLabel(), (geolocMessage2.getGeoloc().getLabel()));
			assertEquals(geolocMessage1.getGeoloc().getLatitude(), geolocMessage2.getGeoloc().getLatitude());
			assertEquals(geolocMessage1.getGeoloc().getLongitude(), geolocMessage2.getGeoloc().getLongitude());
		} else {
			if (geolocMessage2.getGeoloc() != null) {
				fail("One geoloc message is null");
			}
		}
	}

	protected void setUp() throws Exception {
		super.setUp();
		Random random = new Random();
		messageId = String.valueOf(random.nextInt(96) + 32);
		receiptAt = random.nextLong();
		sentAt = random.nextLong();
		ContactUtils contactUtils = ContactUtils.getInstance(getContext());
		remote = contactUtils.formatContact("+33123456789");
		longitude = random.nextDouble();
		expiration = random.nextLong();
		label = String.valueOf(random.nextInt(96) + 32);
		latitude = random.nextDouble();
		geoloc = new Geoloc(label, latitude, longitude, expiration);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testChatMessageContactNull() {
		GeolocMessage geolocMessage = new GeolocMessage(messageId, null, geoloc, receiptAt, sentAt);
		Parcel parcel = Parcel.obtain();
		geolocMessage.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		GeolocMessage createFromParcel = GeolocMessage.CREATOR.createFromParcel(parcel);
		geolocMessageIsEqual(createFromParcel, geolocMessage);
	}

	public void testChatMessageGeolocNull() {
		GeolocMessage geolocMessage = new GeolocMessage(messageId, remote, null, receiptAt, sentAt);
		Parcel parcel = Parcel.obtain();
		geolocMessage.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		GeolocMessage createFromParcel = GeolocMessage.CREATOR.createFromParcel(parcel);
		geolocMessageIsEqual(createFromParcel, geolocMessage);
	}

	public void testChatMessage() {
		GeolocMessage geolocMessage = new GeolocMessage(messageId, remote, geoloc, receiptAt, sentAt);
		Parcel parcel = Parcel.obtain();
		geolocMessage.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		GeolocMessage createFromParcel = GeolocMessage.CREATOR.createFromParcel(parcel);
		geolocMessageIsEqual(createFromParcel, geolocMessage);
	}
}