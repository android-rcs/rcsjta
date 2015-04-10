/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.service.SessionIdGenerator;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.video.VideoSharingPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.provider.sharing.VideoSharingDeleteTask;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.service.broadcaster.VideoSharingEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;
import com.gsma.services.rcs.sharing.video.IVideoSharing;
import com.gsma.services.rcs.sharing.video.IVideoSharingListener;
import com.gsma.services.rcs.sharing.video.IVideoSharingService;
import com.gsma.services.rcs.sharing.video.IVideoSharingServiceConfiguration;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;

import android.os.IBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Rich call API service
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingServiceImpl extends IVideoSharingService.Stub {

    private final VideoSharingEventBroadcaster mBroadcaster = new VideoSharingEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final RichcallService mRichcallService;

    private final RichCallHistory mRichCallLog;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final Core mCore;

    private final LocalContentResolver mLocalContentResolver;

    private final ExecutorService mImOperationExecutor;

    private final Map<String, IVideoSharing> mVideoSharingCache = new HashMap<String, IVideoSharing>();

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private final Object mImsLock;

    private static final Logger sLogger = Logger.getLogger(VideoSharingServiceImpl.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param richcallService RichcallService
     * @param richCallLog RichCallHistory
     * @param rcsSettings RcsSettings
     * @param contactManager ContactManager
     * @param core Core
     * @param localContentResolver LocalContentResolver
     * @param imOperationExecutor IM ExecutorService
     * @param imsLock ims lock object
     */
    public VideoSharingServiceImpl(RichcallService richcallService, RichCallHistory richCallLog,
            RcsSettings rcsSettings, ContactManager contactManager, Core core,
            LocalContentResolver localContentResolver, ExecutorService imOperationExecutor,
            Object imsLock) {
        if (sLogger.isActivated()) {
            sLogger.info("Video sharing API is loaded");
        }
        mRichcallService = richcallService;
        mRichCallLog = richCallLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCore = core;
        mLocalContentResolver = localContentResolver;
        mImOperationExecutor = imOperationExecutor;
        mImsLock = imsLock;
    }

    /**
     * Close API
     */
    public void close() {
        // Clear list of sessions
        mVideoSharingCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("Video sharing service API is closed");
        }
    }

    /**
     * Add a video sharing in the list
     * 
     * @param videoSharing Video sharing
     */
    private void addVideoSharing(VideoSharingImpl videoSharing) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a video sharing");
        }

        mVideoSharingCache.put(videoSharing.getSharingId(), videoSharing);
    }

    /**
     * Remove a video sharing from the list
     * 
     * @param sharingId Sharing ID
     */
    public void removeVideoSharing(String sharingId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a video sharing");
        }

        mVideoSharingCache.remove(sharingId);
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     */
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Add a service listener");
        }
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
        }
    }

    /**
     * Unregisters a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    public void removeEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Remove a service listener");
        }
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Notifies registration event
     */
    public void notifyRegistration() {
        // Notify listeners
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
        }
    }

    /**
     * Notifies unregistration event
     * 
     * @param reasonCode for unregistration
     */
    public void notifyUnRegistration(RcsServiceRegistration.ReasonCode reasonCode) {
        // Notify listeners
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    public void setVideoSharingStateAndReasonCode(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode, long duration) {
        mRichCallLog.setVideoSharingStateReasonCodeAndDuration(sharingId, state, reasonCode,
                duration);
        mBroadcaster.broadcastStateChanged(contact, sharingId, state, reasonCode);
    }

    public void setVideoSharingStateAndReasonCode(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode) {
        mRichCallLog.setVideoSharingStateReasonCode(sharingId, state, reasonCode);
        mBroadcaster.broadcastStateChanged(contact, sharingId, state, reasonCode);
    }

    /**
     * Get the remote contact Id involved in the current call
     * 
     * @return ContactId or null if there is no call in progress
     * @throws ServerApiException
     */
    public ContactId getRemotePhoneNumber() throws ServerApiException {
        if (sLogger.isActivated()) {
            sLogger.info("Get remote phone number");
        }

        // Test core availability
        ServerApiUtils.testCore();

        try {
            return mCore.getImsModule().getCallManager().getContact();
        } catch (Exception e) {
            throw new ServerApiException(e);
        }
    }

    /**
     * Receive a new video sharing invitation
     * 
     * @param session Video sharing session
     */
    public void receiveVideoSharingInvitation(VideoStreamingSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Receive video sharing invitation from ")
                    .append(contact.toString()).append(" displayName=")
                    .append(session.getRemoteDisplayName()).toString());
        }

        // Update displayName of remote contact
        mContactManager.setContactDisplayName(contact, session.getRemoteDisplayName());
        String sharingId = session.getSessionID();
        VideoSharingPersistedStorageAccessor storageAccessor = new VideoSharingPersistedStorageAccessor(
                sharingId, mRichCallLog);
        VideoSharingImpl videoSharing = new VideoSharingImpl(sharingId, mRichcallService,
                mBroadcaster, storageAccessor, this);
        addVideoSharing(videoSharing);
        session.addListener(videoSharing);
    }

    /**
     * Returns the configuration of video sharing service
     * 
     * @return Configuration
     */
    public IVideoSharingServiceConfiguration getConfiguration() {
        return new IVideoSharingServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Shares a live video with a contact. The parameter renderer contains the video player provided
     * by the application. An exception if thrown if there is no ongoing CS call. The parameter
     * contact supports the following formats: MSISDN in national or international format, SIP
     * address, SIP-URI or Tel-URI. If the format of the contact is not supported an exception is
     * thrown.
     * 
     * @param contact Contact ID
     * @param player Video player
     * @return Video sharing
     * @throws ServerApiException
     */
    public IVideoSharing shareVideo(ContactId contact, IVideoPlayer player)
            throws ServerApiException {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a live video session with ".concat(contact.toString()));
        }

        // Test IMS connection
        ServerApiUtils.testIms();

        // Test if at least the audio media is configured
        if (player == null) {
            throw new ServerApiException("Missing video player");
        }

        try {
            long timestamp = System.currentTimeMillis();
            final VideoStreamingSession session = mRichcallService.initiateLiveVideoSharingSession(
                    contact, player, timestamp);

            String sharingId = session.getSessionID();
            VideoContent content = (VideoContent) session.getContent();
            mRichCallLog.addVideoSharing(sharingId, contact, Direction.OUTGOING, content,
                    VideoSharing.State.INITIATING, ReasonCode.UNSPECIFIED, timestamp);
            mBroadcaster.broadcastStateChanged(contact, sharingId, VideoSharing.State.INITIATING,
                    ReasonCode.UNSPECIFIED);

            VideoSharingPersistedStorageAccessor storageAccessor = new VideoSharingPersistedStorageAccessor(
                    sharingId, contact, Direction.OUTGOING, mRichCallLog, content.getEncoding(),
                    content.getHeight(), content.getWidth(), timestamp);
            VideoSharingImpl videoSharing = new VideoSharingImpl(sharingId, mRichcallService,
                    mBroadcaster, storageAccessor, this);
            addVideoSharing(videoSharing);
            session.addListener(videoSharing);

            new Thread() {
                public void run() {
                    session.startSession();
                }
            }.start();
            return videoSharing;

        } catch (Exception e) {
            throw new ServerApiException(e);
        }
    }

    /**
     * Returns a current video sharing from its unique ID
     * 
     * @param sharingId
     * @return Video sharing
     * @throws ServerApiException
     */
    public IVideoSharing getVideoSharing(String sharingId) throws ServerApiException {
        if (sLogger.isActivated()) {
            sLogger.info("Get video sharing ".concat(sharingId));
        }

        IVideoSharing videoSharing = mVideoSharingCache.get(sharingId);
        if (videoSharing != null) {
            return videoSharing;
        }
        VideoSharingPersistedStorageAccessor storageAccessor = new VideoSharingPersistedStorageAccessor(
                sharingId, mRichCallLog);
        return new VideoSharingImpl(sharingId, mRichcallService, mBroadcaster, storageAccessor,
                this);
    }

    /**
     * Returns the list of video sharings in progress
     * 
     * @return List of video sharings
     * @throws ServerApiException
     */
    public List<IBinder> getVideoSharings() throws ServerApiException {
        if (sLogger.isActivated()) {
            sLogger.info("Get video sharing sessions");
        }

        try {
            List<IBinder> videoSharings = new ArrayList<IBinder>(mVideoSharingCache.size());
            for (IVideoSharing videoSharing : mVideoSharingCache.values()) {
                videoSharings.add(videoSharing.asBinder());
            }
            return videoSharings;

        } catch (Exception e) {
            throw new ServerApiException(e);
        }
    }

    /**
     * Add and broadcast video sharing invitation rejections
     * 
     * @param contact Contact ID
     * @param content Video content
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     */
    public void addVideoSharingInvitationRejected(ContactId contact, VideoContent content,
            ReasonCode reasonCode, long timestamp) {
        String sessionId = SessionIdGenerator.getNewId();
        mRichCallLog.addVideoSharing(sessionId, contact, Direction.INCOMING, content,
                VideoSharing.State.REJECTED, reasonCode, timestamp);
        mBroadcaster.broadcastInvitation(sessionId);
    }

    /**
     * Adds a listener on video sharing events
     * 
     * @param listener Listener
     */
    public void addEventListener2(IVideoSharingListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Add a video sharing event listener");
        }
        synchronized (mLock) {
            mBroadcaster.addEventListener(listener);
        }
    }

    /**
     * Removes a listener from video sharing events
     * 
     * @param listener Listener
     */
    public void removeEventListener2(IVideoSharingListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Remove a video sharing event listener");
        }
        synchronized (mLock) {
            mBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @throws ServerApiException
     * @see VERSION_CODES
     */
    public int getServiceVersion() throws ServerApiException {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Deletes all video sharing from history and abort/reject any associated ongoing session if
     * such exists.
     */
    public void deleteVideoSharings() {
        mImOperationExecutor.execute(new VideoSharingDeleteTask(this, mRichcallService,
                mLocalContentResolver, mImsLock));
    }

    /**
     * Delete video sharing associated with a given contact from history and abort/reject any
     * associated ongoing session if such exists.
     * 
     * @param contact
     */
    public void deleteVideoSharings2(ContactId contact) {
        mImOperationExecutor.execute(new VideoSharingDeleteTask(this, mRichcallService,
                mLocalContentResolver, mImsLock, contact));
    }

    /**
     * Deletes a video sharing by its sharing ID from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId
     */
    public void deleteVideoSharing(String sharingId) {
        mImOperationExecutor.execute(new VideoSharingDeleteTask(this, mRichcallService,
                mLocalContentResolver, mImsLock, sharingId));
    }

    public void broadcastDeleted(ContactId contact, Set<String> sharingIds) {
        mBroadcaster.broadcastDeleted(contact, sharingIds);
    }
}
