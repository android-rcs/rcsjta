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

package com.gsma.service.rcs.capabilities;

import com.gsma.services.rcs.capability.Capabilities;

import android.os.Parcel;
import android.test.AndroidTestCase;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CapabilitiesTest extends AndroidTestCase {

    private boolean mBimageSharing;

    private boolean mBvideoSharing;

    private boolean mBimSession;

    private boolean fileTransfer;

    private boolean mBgeolocPush;

    private Set<String> mExtensions = new HashSet<>();

    private boolean mBautomata;

    private long mTimestamp;

    protected void setUp() throws Exception {
        super.setUp();
        Random random = new Random();
        mBimageSharing = random.nextBoolean();
        mBvideoSharing = random.nextBoolean();
        mBimSession = random.nextBoolean();
        fileTransfer = random.nextBoolean();
        mBgeolocPush = random.nextBoolean();
        mBautomata = random.nextBoolean();
        mExtensions.add(String.valueOf(random.nextInt(96) + 32));
        mExtensions.add(String.valueOf(random.nextInt(96) + 32));
        mTimestamp = random.nextLong();
    }


    public void testCapabilitiesNullSet() {
        Capabilities capabilities = new Capabilities(mBimageSharing, mBvideoSharing, mBimSession,
                fileTransfer, mBgeolocPush, null, mBautomata, mTimestamp);
        Parcel parcel = Parcel.obtain();
        capabilities.writeToParcel(parcel, 0);
        // done writing, now reset parcel for reading
        parcel.setDataPosition(0);
        // finish round trip
        Capabilities createFromParcel = Capabilities.CREATOR.createFromParcel(parcel);
        assertTrue(capabilitiesIsEqual(createFromParcel, capabilities));
    }

    public void testCapabilities() {
        Capabilities capabilities = new Capabilities(mBimageSharing, mBvideoSharing, mBimSession,
                fileTransfer, mBgeolocPush, mExtensions, mBautomata, mTimestamp);
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
        return cap1.getTimestamp() == cap2.getTimestamp();
    }
}
