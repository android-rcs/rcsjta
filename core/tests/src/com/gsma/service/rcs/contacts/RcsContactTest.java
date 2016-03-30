/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.service.rcs.contacts;

import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.service.rcs.capabilities.CapabilitiesTest;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.contact.RcsContact;

import android.os.Parcel;
import android.test.AndroidTestCase;

import java.util.HashSet;
import java.util.Random;

public class RcsContactTest extends AndroidTestCase {

    private Capabilities mCapabilities;

    private boolean mRegistered;

    private ContactId mContactId;

    private String mDisplayName;

    protected void setUp() throws Exception {
        super.setUp();
        Random random = new Random();
        boolean mBimageSharing = random.nextBoolean();
        boolean videoSharing = random.nextBoolean();
        boolean imSession = random.nextBoolean();
        boolean fileTransfer = random.nextBoolean();
        boolean geolocPush = random.nextBoolean();
        boolean automata = random.nextBoolean();
        HashSet<String> extensions = new HashSet<>();
        extensions.add(String.valueOf(random.nextInt(96) + 32));
        extensions.add(String.valueOf(random.nextInt(96) + 32));
        long timestamp = random.nextLong();

        mCapabilities = new Capabilities(mBimageSharing, videoSharing, imSession, fileTransfer,
                geolocPush, extensions, automata, timestamp);
        mRegistered = random.nextBoolean();
        ContactUtil contactUtils = ContactUtil
                .getInstance(new ContactUtilMockContext(getContext()));
        mContactId = contactUtils.formatContact("+33123456789");
        mDisplayName = "displayName";
    }

    public void testRcsContactContactNull() {
        RcsContact rcsContact = new RcsContact(null, mRegistered, mCapabilities, mDisplayName, false,
                -1L);
        Parcel parcel = Parcel.obtain();
        rcsContact.writeToParcel(parcel, 0);
        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);
        // finish round trip
        RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
        assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
    }

    public void testRcsContactCapabilitiesNull() {
        RcsContact rcsContact = new RcsContact(mContactId, mRegistered, null, mDisplayName, false, -1L);
        Parcel parcel = Parcel.obtain();
        rcsContact.writeToParcel(parcel, 0);
        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);
        // finish round trip
        RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
        assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
    }

    public void testRcsContactDisplayNameNull() {
        RcsContact rcsContact = new RcsContact(mContactId, mRegistered, mCapabilities, null, false,
                -1L);
        Parcel parcel = Parcel.obtain();
        rcsContact.writeToParcel(parcel, 0);
        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);
        // finish round trip
        RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
        assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
    }

    public void testRcsContact() {
        RcsContact rcsContact = new RcsContact(mContactId, mRegistered, mCapabilities, mDisplayName,
                false, -1L);
        Parcel parcel = Parcel.obtain();
        rcsContact.writeToParcel(parcel, 0);
        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);
        // finish round trip
        RcsContact createFromParcel = RcsContact.CREATOR.createFromParcel(parcel);
        assertTrue(rcsContactIsEqual(createFromParcel, rcsContact));
    }

    private boolean rcsContactIsEqual(RcsContact rcs1, RcsContact rcs2) {
        if (rcs1.isOnline() != rcs2.isOnline()) {
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
            if (!CapabilitiesTest.capabilitiesIsEqual(rcs1.getCapabilities(),
                    rcs2.getCapabilities())) {
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
