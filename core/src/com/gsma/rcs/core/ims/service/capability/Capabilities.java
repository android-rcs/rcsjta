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

import com.gsma.rcs.provider.settings.RcsSettings;

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

    /**
     * Image sharing support
     */
    private boolean mImageSharing = false;

    /**
     * Video sharing support
     */
    private boolean mVideoSharing = false;

    /**
     * IP voice call support
     */
    private boolean mIpVoiceCall = false;

    /**
     * IP video call support
     */
    private boolean mIpVideoCall = false;

    /**
     * IM session support
     */
    private boolean mImSession = false;

    /**
     * File transfer support
     */
    private boolean mFileTransfer = false;

    /**
     * CS video support
     */
    private boolean mCsVideo = false;

    /**
     * Presence discovery support
     */
    private boolean mPresenceDiscovery = false;

    /**
     * Social presence support
     */
    private boolean mSocialPresence = false;

    /**
     * File transfer over HTTP support
     */
    private boolean mFileTransferHttp = false;

    /**
     * Geolocation push support
     */
    private boolean mGeolocationPush = false;

    /**
     * File Transfer Thumbnail support
     */
    private boolean mFileTransferThumbnail = false;

    /**
     * File Transfer S&F
     */
    private boolean mFileTransferStoreForward = false;

    /**
     * Group chat S&F
     */
    private boolean mGroupChatStoreForward = false;

    /**
     * SIP automata (@see RFC 3840)
     */
    private boolean mSipAutomata = false;

    /**
     * Set of supported extensions
     */
    private Set<String> mExtensions = new HashSet<String>();

    /**
     * Last timestamp capabilities was requested
     */
    private long mTimestampOfLastRequest = INVALID_TIMESTAMP;

    /**
     * Last timestamp capabilities was refreshed
     */
    private long mTimestampOfLastRefresh = INVALID_TIMESTAMP;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public Capabilities(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
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
     * Set the image sharing support
     * 
     * @param supported Supported
     */
    public void setImageSharingSupport(boolean supported) {
        mImageSharing = supported;
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
     * Set the video sharing support
     * 
     * @param supported Supported
     */
    public void setVideoSharingSupport(boolean supported) {
        mVideoSharing = supported;
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
     * Set the IP voice call support
     * 
     * @param supported Supported
     */
    public void setIPVoiceCallSupport(boolean supported) {
        mIpVoiceCall = supported;
    }

    /**
     * Set the IP video call support
     * 
     * @param supported Supported
     */
    public void setIPVideoCallSupport(boolean supported) {
        mIpVideoCall = supported;
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
     * Set the IM session support
     * 
     * @param supported Supported
     */
    public void setImSessionSupport(boolean supported) {
        mImSession = supported;
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
     * Set the file transfer support
     * 
     * @param supported Supported
     */
    public void setFileTransferSupport(boolean supported) {
        mFileTransfer = supported;
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
     * Set the CS video support
     * 
     * @param supported Supported
     */
    public void setCsVideoSupport(boolean supported) {
        mCsVideo = supported;
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
     * Set the presence discovery support
     * 
     * @param supported Supported
     */
    public void setPresenceDiscoverySupport(boolean supported) {
        mPresenceDiscovery = supported;
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
     * Set the social presence support
     * 
     * @param supported Supported
     */
    public void setSocialPresenceSupport(boolean supported) {
        mSocialPresence = supported;
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
     * Set the file transfer over HTTP support
     * 
     * @param supported Supported
     */
    public void setFileTransferHttpSupport(boolean supported) {
        mFileTransferHttp = supported;
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
     * Set the Geolocation Push support
     * 
     * @param supported Supported
     */
    public void setGeolocationPushSupport(boolean supported) {
        mGeolocationPush = supported;
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
     * Set the file transfer thumbnail support
     * 
     * @param supported Supported
     */
    public void setFileTransferThumbnailSupport(boolean supported) {
        mFileTransferThumbnail = supported;
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
     * Set the file transfer S&F support
     * 
     * @param supported Supported
     */
    public void setFileTransferStoreForwardSupport(boolean supported) {
        mFileTransferStoreForward = supported;
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
     * Set the group chat S&F support
     * 
     * @param supported Supported
     */
    public void setGroupChatStoreForwardSupport(boolean supported) {
        mGroupChatStoreForward = supported;
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
     * Set the SIP automata feature tag
     * 
     * @param sipAutomata
     */
    public void setSipAutomata(boolean sipAutomata) {
        mSipAutomata = sipAutomata;
    }

    /**
     * Set supported extensions
     * 
     * @param extensions set of supported extensions
     */
    public void setSupportedExtensions(Set<String> extensions) {
        mExtensions = extensions;
    }

    /**
     * Add supported extension
     * 
     * @param serviceId Service ID
     */
    public void addSupportedExtension(String serviceId) {
        mExtensions.add(serviceId);
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

    /**
     * Set timestamp of the last request
     * 
     * @param timestampOfLastRequest (in milliseconds)
     */
    public void setTimestampOfLastRequest(long timestampOfLastRequest) {
        mTimestampOfLastRequest = timestampOfLastRequest;
    }

    /**
     * Returns a string representation of the object
     * 
     * @return String
     */
    public String toString() {
        return "Image_share=" + mImageSharing + ", Video_share=" + mVideoSharing
                + ", IP_voice_call="
                + mIpVoiceCall + ", IP_video_call=" + mIpVideoCall + ", File_transfer="
                + mFileTransfer + ", Chat=" + mImSession + ", FT_http=" + mFileTransferHttp
                + ", Geolocation_push=" + mGeolocationPush + ", Automata=" + mSipAutomata
                + ", TimestampLastRequest=" + mTimestampOfLastRequest + ", TimestampLastRefresh="
                + mTimestampOfLastRefresh;
    }

    /**
     * Check validity of capability
     * 
     * @return true if the capability is valid (no need to refresh it), otherwise False.
     */
    public boolean isValid() {
        // If no refresh of capabilities is required then capabilities are valid
        return !PollingManager.isCapabilityRefreshRequired(mTimestampOfLastRefresh, mRcsSettings);
    }

    /**
     * Get timestamp of last refresh
     * 
     * @return timestampOfLastRefresh (in milliseconds)
     */
    public long getTimestampOfLastRefresh() {
        return mTimestampOfLastRefresh;
    }

    /**
     * Set timestamp of last refresh
     * 
     * @param timestampOfLastRefresh (in milliseconds)
     */
    public void setTimestampOfLastRefresh(long timestampOfLastRefresh) {
        mTimestampOfLastRefresh = timestampOfLastRefresh;
    }

}
