/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs.capability;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Capabilities of a contact. This class encapsulates the different capabilities which may be
 * supported by the local user or a remote contact.
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class Capabilities implements Parcelable {
    /**
     * Image sharing support
     */
    private boolean mImageSharing = false;

    /**
     * Video sharing support
     */
    private boolean mVideoSharing = false;

    /**
     * IM session support
     */
    private boolean mImSession = false;

    /**
     * File transfer support
     */
    private boolean mFileTransfer = false;

    /**
     * Geolocation push support
     */
    private boolean mGeolocPush = false;

    /**
     * List of supported extensions
     */
    private Set<String> mExtensions = new HashSet<>();

    /**
     * Automata flag
     */
    private boolean mAutomata = false;

    /**
     * The timestamp of the last capability response
     */
    private long mTimestamp;

    /**
     * Indicates the file transfer capability is supported.
     */
    public static final int CAPABILITY_FILE_TRANSFER = 0x00000001;

    /**
     * Indicates the IM capability is supported.
     */
    public static final int CAPABILITY_IM = 0x00000002;

    /**
     * Indicates the geoloc push capability is supported.
     */
    public static final int CAPABILITY_GEOLOC_PUSH = 0x00000004;

    /**
     * Indicates the image sharing capability is supported.
     */
    public static final int CAPABILITY_IMAGE_SHARING = 0x00000008;

    /**
     * Indicates the video sharing capability is supported.
     */
    public static final int CAPABILITY_VIDEO_SHARING = 0x00000010;

    /**
     * Constructor
     *
     * @param imageSharing Image sharing support
     * @param videoSharing Video sharing support
     * @param imSession IM/Chat support
     * @param fileTransfer File transfer support
     * @param geolocPush Geolocation push support
     * @param extensions Set of supported extensions
     * @param automata Automata flag
     * @param timestamp timestamp of last capability response
     * @hide
     */
    public Capabilities(boolean imageSharing, boolean videoSharing, boolean imSession,
            boolean fileTransfer, boolean geolocPush, Set<String> extensions, boolean automata,
            long timestamp) {
        mImageSharing = imageSharing;
        mVideoSharing = videoSharing;
        mImSession = imSession;
        mFileTransfer = fileTransfer;
        mGeolocPush = geolocPush;
        mExtensions = extensions;
        mAutomata = automata;
        mTimestamp = timestamp;
    }

    /**
     * Constructor
     *
     * @param source Parcelable source
     * @hide
     */
    public Capabilities(Parcel source) {
        mImageSharing = source.readInt() != 0;
        mVideoSharing = source.readInt() != 0;
        mImSession = source.readInt() != 0;
        mFileTransfer = source.readInt() != 0;

        boolean containsExtension = source.readInt() != 0;
        if (containsExtension) {
            List<String> exts = new ArrayList<>();
            source.readStringList(exts);
            mExtensions = new HashSet<>(exts);
        } else {
            mExtensions = null;
        }
        mGeolocPush = source.readInt() != 0;
        mAutomata = source.readInt() != 0;
        mTimestamp = source.readLong();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation
     *
     * @return Integer
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Write parcelable object
     *
     * @param dest The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mImageSharing ? 1 : 0);
        dest.writeInt(mVideoSharing ? 1 : 0);
        dest.writeInt(mImSession ? 1 : 0);
        dest.writeInt(mFileTransfer ? 1 : 0);
        if (mExtensions != null) {
            dest.writeInt(1);
            List<String> exts = new ArrayList<String>(mExtensions);
            dest.writeStringList(exts);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mGeolocPush ? 1 : 0);
        dest.writeInt(mAutomata ? 1 : 0);
        dest.writeLong(mTimestamp);
    }

    /**
     * Parcelable creator
     *
     * @hide
     */
    public static final Parcelable.Creator<Capabilities> CREATOR = new Parcelable.Creator<Capabilities>() {
        public Capabilities createFromParcel(Parcel source) {
            return new Capabilities(source);
        }

        public Capabilities[] newArray(int size) {
            return new Capabilities[size];
        }
    };

    /**
     * Is image sharing supported
     *
     * @deprecated Use {@link #hasCapabilities(int capabilities)} instead.
     * @return true if supported else returns false
     */
    @Deprecated
    public boolean isImageSharingSupported() {
        return mImageSharing;
    }

    /**
     * Is video sharing supported
     *
     * @deprecated Use {@link #hasCapabilities(int capabilities)} instead.
     * @return true if supported else returns false
     */
    @Deprecated
    public boolean isVideoSharingSupported() {
        return mVideoSharing;
    }

    /**
     * Is IM session supported
     *
     * @deprecated Use {@link #hasCapabilities(int capabilities)} instead.
     * @return true if supported else returns false
     */
    @Deprecated
    public boolean isImSessionSupported() {
        return mImSession;
    }

    /**
     * Is file transfer supported
     *
     * @deprecated Use {@link #hasCapabilities(int capabilities)} instead.
     * @return true if supported else returns false
     */
    @Deprecated
    public boolean isFileTransferSupported() {
        return mFileTransfer;
    }

    /**
     * Is geolocation push supported
     *
     * @deprecated Use {@link #hasCapabilities(int capabilities)} instead.
     * @return true if supported else returns false
     */
    @Deprecated
    public boolean isGeolocPushSupported() {
        return mGeolocPush;
    }

    /**
     * Is extension supported
     *
     * @param tag Feature tag
     * @return true if supported else returns false
     */
    public boolean isExtensionSupported(String tag) {
        return mExtensions.contains(tag);
    }

    /**
     * Gets the set of supported extensions
     *
     * @return Set of feature tags
     */
    public Set<String> getSupportedExtensions() {
        return mExtensions;
    }

    /**
     * Is automata
     *
     * @return true if it's an automata else returns false
     */
    public boolean isAutomata() {
        return mAutomata;
    }

    /**
     * Timestamp of the last capability response (in milliseconds)
     *
     * @return the timestamp of the last capability response
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    private int isCapabilitySupported(boolean supported, int capabilityFlag) {
        return (supported) ? capabilityFlag : 0;
    }

    /**
     * Gets the combination of supported capabilities.
     * 
     * @return the combination of supported capabilities
     */
    private int getSupportedCapabilities() {
        int result = isCapabilitySupported(mFileTransfer, CAPABILITY_FILE_TRANSFER);
        result |= isCapabilitySupported(mImSession, CAPABILITY_IM);
        result |= isCapabilitySupported(mGeolocPush, CAPABILITY_GEOLOC_PUSH);
        result |= isCapabilitySupported(mImageSharing, CAPABILITY_IMAGE_SHARING);
        result |= isCapabilitySupported(mVideoSharing, CAPABILITY_VIDEO_SHARING);
        return result;
    }

    /**
     * Tests for the support of capabilities on this instance.
     * 
     * @return True if the capabilities are supported.
     */
    public boolean hasCapabilities(int capabilities) {
        return (getSupportedCapabilities() & capabilities) == capabilities;
    }
}
