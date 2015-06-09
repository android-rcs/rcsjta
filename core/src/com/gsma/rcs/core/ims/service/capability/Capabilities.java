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

package com.gsma.rcs.core.ims.service.capability;

import java.util.HashSet;
import java.util.Set;

/**
 * Capabilities
 * 
 * @author jexa7410
 * @author YPLO6403
 */
public class Capabilities {

    /**
     * Invalid timestamp for capabilities
     */
    public static final long INVALID_TIMESTAMP = -1;

    private final boolean mImageSharing;
    private final boolean mVideoSharing;
    private final boolean mIpVoiceCall;
    private final boolean mIpVideoCall;
    private final boolean mImSession;
    private final boolean mFileTransfer;
    private final boolean mCsVideo;
    private final boolean mPresenceDiscovery;
    private final boolean mSocialPresence;
    private final boolean mFileTransferHttp;
    private final boolean mGeolocationPush;
    private final boolean mFileTransferThumbnail;
    private final boolean mFileTransferStoreForward;
    private final boolean mGroupChatStoreForward;
    /**
     * SIP automata (@see RFC 3840)
     */
    private final boolean mSipAutomata;

    /**
     * Set of supported extensions
     */
    private final Set<String> mExtensions;

    /**
     * Last timestamp capabilities was requested
     */
    private final long mTimestampOfLastRequest;

    /**
     * Timestamp of when this capabilities was received from network.
     */
    private final long mTimestampOfLastResponse;

    /**
     * The default capabilities applicable to non RCS contacts
     */
    public final static Capabilities sDefaultCapabilities = new Capabilities.CapabilitiesBuilder()
            .build();

    private Capabilities(CapabilitiesBuilder builder) {
        mImageSharing = builder.mImageSharing;
        mVideoSharing = builder.mVideoSharing;
        mIpVoiceCall = builder.mIpVoiceCall;
        mIpVideoCall = builder.mIpVideoCall;
        mImSession = builder.mImSession;
        mFileTransfer = builder.mFileTransfer;
        mCsVideo = builder.mCsVideo;
        mPresenceDiscovery = builder.mPresenceDiscovery;
        mSocialPresence = builder.mSocialPresence;
        mFileTransferHttp = builder.mFileTransferHttp;
        mGeolocationPush = builder.mGeolocationPush;
        mFileTransferThumbnail = builder.mFileTransferThumbnail;
        mFileTransferStoreForward = builder.mFileTransferStoreForward;
        mGroupChatStoreForward = builder.mGroupChatStoreForward;
        mSipAutomata = builder.mSipAutomata;
        mTimestampOfLastRequest = builder.mTimestampOfLastRequest;
        mTimestampOfLastResponse = builder.mTimestampOfLastResponse;
        mExtensions = new HashSet<String>(builder.mExtensions);
    }

    /**
     * Is image sharing supported
     * 
     * @return Boolean
     */
    public boolean isImageSharingSupported() {
        return mImageSharing;
    }

    /**
     * Is video sharing supported
     * 
     * @return Boolean
     */
    public boolean isVideoSharingSupported() {
        return mVideoSharing;
    }

    /**
     * Is IP voice call supported
     * 
     * @return Boolean
     */
    public boolean isIPVoiceCallSupported() {
        return mIpVoiceCall;
    }

    /**
     * Is IP video call supported
     * 
     * @return Boolean
     */
    public boolean isIPVideoCallSupported() {
        return mIpVideoCall;
    }

    /**
     * Is IM session supported
     * 
     * @return Boolean
     */
    public boolean isImSessionSupported() {
        return mImSession;
    }

    /**
     * Is file transfer supported
     * 
     * @return Boolean
     */
    public boolean isFileTransferSupported() {
        return mFileTransfer;
    }

    /**
     * Is CS video supported
     * 
     * @return Boolean
     */
    public boolean isCsVideoSupported() {
        return mCsVideo;
    }

    /**
     * Is presence discovery supported
     * 
     * @return Boolean
     */
    public boolean isPresenceDiscoverySupported() {
        return mPresenceDiscovery;
    }

    /**
     * Is social presence supported
     * 
     * @return Boolean
     */
    public boolean isSocialPresenceSupported() {
        return mSocialPresence;
    }

    /**
     * Is file transfer over HTTP supported
     * 
     * @return Boolean
     */
    public boolean isFileTransferHttpSupported() {
        return mFileTransferHttp;
    }

    /**
     * Is Geolocation Push supported
     * 
     * @return Boolean
     */
    public boolean isGeolocationPushSupported() {
        return mGeolocationPush;
    }

    /**
     * Is file transfer thumbnail supported
     * 
     * @return Boolean
     */
    public boolean isFileTransferThumbnailSupported() {
        return mFileTransferThumbnail;
    }

    /**
     * Is file transfer S&F supported
     * 
     * @return Boolean
     */
    public boolean isFileTransferStoreForwardSupported() {
        return mFileTransferStoreForward;
    }

    /**
     * Is group chat S&F supported
     * 
     * @return Boolean
     */
    public boolean isGroupChatStoreForwardSupported() {
        return mGroupChatStoreForward;
    }

    /**
     * Is device an automata ?
     * 
     * @return True if automata
     */
    public boolean isSipAutomata() {
        return mSipAutomata;
    }

    /**
     * Get set of supported extensions
     * 
     * @return List
     */
    public Set<String> getSupportedExtensions() {
        return mExtensions;
    }

    /**
     * Get timestamp of last request
     * 
     * @return timetampOfLastRequest (in milliseconds)
     */
    public long getTimestampOfLastRequest() {
        return mTimestampOfLastRequest;
    }

    @Override
    public String toString() {
        return "Capabilities [mImageSharing=" + mImageSharing + ", mVideoSharing=" + mVideoSharing
                + ", mIpVoiceCall=" + mIpVoiceCall + ", mIpVideoCall=" + mIpVideoCall
                + ", mImSession=" + mImSession + ", mFileTransfer=" + mFileTransfer + ", mCsVideo="
                + mCsVideo + ", mPresenceDiscovery=" + mPresenceDiscovery + ", mSocialPresence="
                + mSocialPresence + ", mFileTransferHttp=" + mFileTransferHttp
                + ", mGeolocationPush=" + mGeolocationPush + ", mFileTransferThumbnail="
                + mFileTransferThumbnail + ", mFileTransferStoreForward="
                + mFileTransferStoreForward + ", mGroupChatStoreForward=" + mGroupChatStoreForward
                + ", mSipAutomata=" + mSipAutomata + ", mExtensions=" + mExtensions + "]";
    }

    /* The equals method does not consider the 2 timestamps.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Capabilities other = (Capabilities) obj;
        if (mCsVideo != other.mCsVideo)
            return false;
        if (mExtensions == null) {
            if (other.mExtensions != null)
                return false;
        } else if (!mExtensions.equals(other.mExtensions))
            return false;
        if (mFileTransfer != other.mFileTransfer)
            return false;
        if (mFileTransferHttp != other.mFileTransferHttp)
            return false;
        if (mFileTransferStoreForward != other.mFileTransferStoreForward)
            return false;
        if (mFileTransferThumbnail != other.mFileTransferThumbnail)
            return false;
        if (mGeolocationPush != other.mGeolocationPush)
            return false;
        if (mGroupChatStoreForward != other.mGroupChatStoreForward)
            return false;
        if (mImSession != other.mImSession)
            return false;
        if (mImageSharing != other.mImageSharing)
            return false;
        if (mIpVideoCall != other.mIpVideoCall)
            return false;
        if (mIpVoiceCall != other.mIpVoiceCall)
            return false;
        if (mPresenceDiscovery != other.mPresenceDiscovery)
            return false;
        if (mSipAutomata != other.mSipAutomata)
            return false;
        if (mSocialPresence != other.mSocialPresence)
            return false;
        if (mVideoSharing != other.mVideoSharing)
            return false;
        return true;
    }
    /**
     * Get timestamp of last response
     * 
     * @return timestampOfLastResponse (in milliseconds)
     */
    public long getTimestampOfLastResponse() {
        return mTimestampOfLastResponse;
    }

    /**
     * Capabilities builder class
     */
    public static class CapabilitiesBuilder {
        private boolean mImageSharing = false;
        private boolean mVideoSharing = false;
        private boolean mIpVoiceCall = false;
        private boolean mIpVideoCall = false;
        private boolean mImSession = false;
        private boolean mFileTransfer = false;
        private boolean mCsVideo = false;
        private boolean mPresenceDiscovery = false;
        private boolean mSocialPresence = false;
        private boolean mFileTransferHttp = false;
        private boolean mGeolocationPush = false;
        private boolean mFileTransferThumbnail = false;
        private boolean mFileTransferStoreForward = false;
        private boolean mGroupChatStoreForward = false;
        private boolean mSipAutomata = false;
        private Set<String> mExtensions = new HashSet<String>();
        private long mTimestampOfLastRequest = INVALID_TIMESTAMP;
        private long mTimestampOfLastResponse = INVALID_TIMESTAMP;

        /**
         * Default constructor
         */
        public CapabilitiesBuilder() {
        }

        /**
         * Copy constructor
         * 
         * @param capabilities to copy or null if construct with default values
         */
        public CapabilitiesBuilder(Capabilities capabilities) {
            mImageSharing = capabilities.isImageSharingSupported();
            mVideoSharing = capabilities.isVideoSharingSupported();
            mIpVoiceCall = capabilities.isIPVoiceCallSupported();
            mIpVideoCall = capabilities.isIPVideoCallSupported();
            mImSession = capabilities.isImSessionSupported();
            mFileTransfer = capabilities.isFileTransferSupported();
            mCsVideo = capabilities.isCsVideoSupported();
            mPresenceDiscovery = capabilities.isPresenceDiscoverySupported();
            mSocialPresence = capabilities.isSocialPresenceSupported();
            mFileTransferHttp = capabilities.isFileTransferHttpSupported();
            mGeolocationPush = capabilities.isGeolocationPushSupported();
            mFileTransferThumbnail = capabilities.isFileTransferThumbnailSupported();
            mFileTransferStoreForward = capabilities.isFileTransferStoreForwardSupported();
            mGroupChatStoreForward = capabilities.isGroupChatStoreForwardSupported();
            mSipAutomata = capabilities.isSipAutomata();
            mTimestampOfLastRequest = capabilities.getTimestampOfLastRequest();
            mTimestampOfLastResponse = capabilities.getTimestampOfLastResponse();
            mExtensions = new HashSet<String>(capabilities.getSupportedExtensions());
        }

        /**
         * Sets image sharing support
         * 
         * @param support the image sharing support
         * @return the current instance
         */
        public CapabilitiesBuilder setImageSharing(boolean support) {
            mImageSharing = support;
            return this;
        }

        /**
         * Is video sharing supported
         * 
         * @return Boolean
         */
        public boolean isImageSharingSupported() {
            return mImageSharing;
        }

        /**
         * Sets video sharing support
         * 
         * @param support the video sharing support
         * @return the current instance
         */
        public CapabilitiesBuilder setVideoSharing(boolean support) {
            mVideoSharing = support;
            return this;
        }

        /**
         * Is video sharing supported
         * 
         * @return Boolean
         */
        public boolean isVideoSharingSupported() {
            return mVideoSharing;
        }

        /**
         * Sets IP voice call support
         * 
         * @param support the IP voice call support
         * @return the current instance
         */
        public CapabilitiesBuilder setIpVoiceCall(boolean support) {
            mIpVoiceCall = support;
            return this;
        }

        /**
         * Is IP voice call supported
         * 
         * @return Boolean
         */
        public boolean isIPVoiceCallSupported() {
            return mIpVoiceCall;
        }

        /**
         * Sets IP video call support
         * 
         * @param support the IP video call support
         * @return the current instance
         */
        public CapabilitiesBuilder setIpVideoCall(boolean support) {
            mIpVideoCall = support;
            return this;
        }

        /**
         * Is IP video call supported
         * 
         * @return Boolean
         */
        public boolean isIPVideoCallSupported() {
            return mIpVideoCall;
        }

        /**
         * Sets Instant Messaging support
         * 
         * @param support the Instant Messaging support
         * @return the current instance
         */
        public CapabilitiesBuilder setImSession(boolean support) {
            mImSession = support;
            return this;
        }

        /**
         * Is IM session supported
         * 
         * @return Boolean
         */
        public boolean isImSessionSupported() {
            return mImSession;
        }

        /**
         * Sets File Transfer support
         * 
         * @param support the File Transfer support
         * @return the current instance
         */
        public CapabilitiesBuilder setFileTransfer(boolean support) {
            mFileTransfer = support;
            return this;
        }

        /**
         * Is file transfer supported
         * 
         * @return Boolean
         */
        public boolean isFileTransferSupported() {
            return mFileTransfer;
        }

        /**
         * Sets CS video support
         * 
         * @param support the CS video support
         * @return the current instance
         */
        public CapabilitiesBuilder setCsVideo(boolean support) {
            mCsVideo = support;
            return this;
        }

        /**
         * Is CS video supported
         * 
         * @return Boolean
         */
        public boolean isCsVideoSupported() {
            return mCsVideo;
        }

        /**
         * Sets Presence Discovery support
         * 
         * @param support the Presence Discovery support
         * @return the current instance
         */
        public CapabilitiesBuilder setPresenceDiscovery(boolean support) {
            mPresenceDiscovery = support;
            return this;
        }

        /**
         * Is Presence Discovery supported
         * 
         * @return Boolean
         */
        public boolean isPresenceDiscovery() {
            return mPresenceDiscovery;
        }

        /**
         * Sets Social Presence support
         * 
         * @param support the Social Presence support
         * @return the current instance
         */
        public CapabilitiesBuilder setSocialPresence(boolean support) {
            mSocialPresence = support;
            return this;
        }

        /**
         * Is Social Presence supported
         * 
         * @return Boolean
         */
        public boolean isSocialPresence() {
            return mSocialPresence;
        }

        /**
         * Sets File Transfer HTTP support
         * 
         * @param support the File Transfer HTTP support
         * @return the current instance
         */
        public CapabilitiesBuilder setFileTransferHttp(boolean support) {
            mFileTransferHttp = support;
            return this;
        }

        /**
         * Is file transfer over HTTP supported
         * 
         * @return Boolean
         */
        public boolean isFileTransferHttpSupported() {
            return mFileTransferHttp;
        }

        /**
         * Sets Geolocation Push support
         * 
         * @param support the Geolocation Push support
         * @return the current instance
         */
        public CapabilitiesBuilder setGeolocationPush(boolean support) {
            mGeolocationPush = support;
            return this;
        }

        /**
         * Is Geolocation Push supported
         * 
         * @return Boolean
         */
        public boolean isGeolocationPushSupported() {
            return mGeolocationPush;
        }

        /**
         * Sets File Transfer Thumbnail support
         * 
         * @param support the File Transfer Thumbnail support
         * @return the current instance
         */
        public CapabilitiesBuilder setFileTransferThumbnail(boolean support) {
            mFileTransferThumbnail = support;
            return this;
        }

        /**
         * Is file transfer thumbnail supported
         * 
         * @return Boolean
         */
        public boolean isFileTransferThumbnailSupported() {
            return mFileTransferThumbnail;
        }

        /**
         * Sets File Transfer Thumbnail support
         * 
         * @param support the File Transfer Thumbnail support
         * @return the current instance
         */
        public CapabilitiesBuilder setFileTransferStoreForward(boolean support) {
            mFileTransferStoreForward = support;
            return this;
        }

        /**
         * Is file transfer S&F supported
         * 
         * @return Boolean
         */
        public boolean isFileTransferStoreForwardSupported() {
            return mFileTransferStoreForward;
        }

        /**
         * Sets Group Chat Store & Forward support
         * 
         * @param support the Group Chat Store & Forward support
         * @return the current instance
         */
        public CapabilitiesBuilder setGroupChatStoreForward(boolean support) {
            mGroupChatStoreForward = support;
            return this;
        }

        /**
         * Is group chat S&F supported
         * 
         * @return Boolean
         */
        public boolean isGroupChatStoreForwardSupported() {
            return mGroupChatStoreForward;
        }

        /**
         * Sets Sip Automata support
         * 
         * @param support the Sip Automata support
         * @return the current instance
         */
        public CapabilitiesBuilder setSipAutomata(boolean support) {
            mSipAutomata = support;
            return this;
        }

        /**
         * Is Sip Automata
         * 
         * @return Boolean
         */
        public boolean isSipAutomata() {
            return mSipAutomata;
        }

        /**
         * Sets extensions
         * 
         * @param extensions the supported extensions
         * @return the current instance
         */
        public CapabilitiesBuilder setExtensions(Set<String> extensions) {
            mExtensions = extensions;
            return this;
        }

        /**
         * Add extension
         * 
         * @param extension the extension to add
         * @return the current instance
         */
        public CapabilitiesBuilder addExtension(String extension) {
            mExtensions.add(extension);
            return this;
        }

        /**
         * Sets the timestamp of last request
         * 
         * @param time the Timestamp Of Last Request
         * @return the current instance
         */
        public CapabilitiesBuilder setTimestampOfLastRequest(long time) {
            mTimestampOfLastRequest = time;
            return this;
        }

        /**
         * Gets timestamp of last request
         * 
         * @return Boolean
         */
        public long getTimestampOfLastRequest() {
            return mTimestampOfLastRequest;
        }

        /**
         * Sets the timestamp of last response
         * 
         * @param time the File Transfer Thumbnail support
         * @return the current instance
         */
        public CapabilitiesBuilder setTimestampOfLastResponse(long time) {
            mTimestampOfLastResponse = time;
            return this;
        }

        /**
         * Gets timestamp of last response
         * 
         * @return Boolean
         */
        public long getTimestampOfLastResponse() {
            return mTimestampOfLastResponse;
        }

        /**
         * Build the capabilities
         * 
         * @return the built capabilities instance
         */
        public Capabilities build() {
            return new Capabilities(this);
        }

    }

    
}
