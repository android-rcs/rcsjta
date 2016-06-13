/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.richcall;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.gsm.CallManager;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.geoloc.OriginatingGeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.geoloc.TerminatingGeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.OriginatingImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.TerminatingImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.video.OriginatingVideoStreamingSession;
import com.gsma.rcs.core.ims.service.richcall.video.TerminatingVideoStreamingSession;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.GeolocSharingDeleteTask;
import com.gsma.rcs.provider.sharing.ImageSharingDeleteTask;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.provider.sharing.UpdateGeolocSharingStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.sharing.UpdateImageSharingStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.sharing.UpdateVideoSharingStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.sharing.VideoSharingDeleteTask;
import com.gsma.rcs.service.api.GeolocSharingServiceImpl;
import com.gsma.rcs.service.api.ImageSharingServiceImpl;
import com.gsma.rcs.service.api.VideoSharingServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;
import com.gsma.services.rcs.sharing.video.VideoSharing;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.HashMap;
import java.util.Map;

import javax2.sip.message.Response;

/**
 * Rich call service has in charge to monitor the GSM call in order to stop the current content
 * sharing when the call terminates, to process capability request from remote and to request remote
 * capabilities.
 * 
 * @author Jean-Marc AUFFRET
 */
public class RichcallService extends ImsService {

    private static final String ISH_OPERATION_THREAD_NAME = "IshOperations";

    private static final String ISH_DELETE_OPERATION_THREAD_NAME = "IshDeleteOperations";

    private static final String VSH_OPERATION_THREAD_NAME = "VshOperations";

    private static final String VSH_DELETE_OPERATION_THREAD_NAME = "VshDeleteOperations";

    private static final String GSH_OPERATION_THREAD_NAME = "GshOperations";

    private static final String GSH_DELETE_OPERATION_THREAD_NAME = "GshDeleteOperations";

    /**
     * Video share features tags
     */
    public final static String[] FEATURE_TAGS_VIDEO_SHARE = {
        FeatureTags.FEATURE_3GPP_VIDEO_SHARE
    };

    /**
     * Image share features tags
     */
    public final static String[] FEATURE_TAGS_IMAGE_SHARE = {
            FeatureTags.FEATURE_3GPP_VIDEO_SHARE, FeatureTags.FEATURE_3GPP_IMAGE_SHARE
    };

    /**
     * Geoloc share features tags
     */
    public final static String[] FEATURE_TAGS_GEOLOC_SHARE = {
            FeatureTags.FEATURE_3GPP_VIDEO_SHARE, FeatureTags.FEATURE_3GPP_LOCATION_SHARE
    };

    private final static Logger sLogger = Logger.getLogger(RichcallService.class.getSimpleName());

    /**
     * ImageTransferSessionCache with Session ID as key
     */
    private Map<String, ImageTransferSession> mImageTransferSessionCache = new HashMap<>();

    /**
     * VideoStreamingSessionCache with Session ID as key
     */
    private Map<String, VideoStreamingSession> mVideoStremaingSessionCache = new HashMap<>();

    /**
     * GeolocTransferSessionCache with Session ID as key
     */
    private Map<String, GeolocTransferSession> mGeolocTransferSessionCache = new HashMap<>();

    private final ContactManager mContactManager;

    private final RcsSettings mRcsSettings;

    private final CallManager mCallManager;

    private final RichCallHistory mRichCallHistory;

    private final LocalContentResolver mLocalContentResolver;

    private final Handler mImageSharingOperationHandler;

    private final Handler mImageSharingDeleteOperationHandler;

    private final Handler mVideoSharingOperationHandler;

    private final Handler mVideoSharingDeleteOperationHandler;

    private final Handler mGeolocSharingOperationHandler;

    private final Handler mGeolocSharingDeleteOperationHandler;

    private ImageSharingServiceImpl mImageSharingService;

    private VideoSharingServiceImpl mVideoSharingService;

    private GeolocSharingServiceImpl mGeolocSharingService;

    private final CapabilityService mCapabilityService;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param richCallHistory RichCallHistory
     * @param contactsManager ContactManager
     * @param rcsSettings the RCS settings accessor
     * @param callManager the call manager
     * @param localContentResolver the local content resolver
     * @param capabilityService the capability service
     */
    public RichcallService(ImsModule parent, RichCallHistory richCallHistory,
            ContactManager contactsManager, RcsSettings rcsSettings, CallManager callManager,
            LocalContentResolver localContentResolver, CapabilityService capabilityService) {
        super(parent, true);
        mContactManager = contactsManager;
        mRcsSettings = rcsSettings;
        mCallManager = callManager;
        mRichCallHistory = richCallHistory;
        mLocalContentResolver = localContentResolver;
        mCapabilityService = capabilityService;
        mImageSharingOperationHandler = allocateBgHandler(ISH_OPERATION_THREAD_NAME);
        mImageSharingDeleteOperationHandler = allocateBgHandler(ISH_DELETE_OPERATION_THREAD_NAME);
        mVideoSharingOperationHandler = allocateBgHandler(VSH_OPERATION_THREAD_NAME);
        mVideoSharingDeleteOperationHandler = allocateBgHandler(VSH_DELETE_OPERATION_THREAD_NAME);
        mGeolocSharingOperationHandler = allocateBgHandler(GSH_OPERATION_THREAD_NAME);
        mGeolocSharingDeleteOperationHandler = allocateBgHandler(GSH_DELETE_OPERATION_THREAD_NAME);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public void register(ImageSharingServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mImageSharingService = service;
    }

    public void register(VideoSharingServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mVideoSharingService = service;
    }

    public void register(GeolocSharingServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mGeolocSharingService = service;
    }

    /**
     * Initializes richcall service
     */
    public void initialize() {
    }

    public void scheduleImageShareOperation(Runnable runnable) {
        mImageSharingOperationHandler.post(runnable);
    }

    public void scheduleVideoShareOperation(Runnable runnable) {
        mVideoSharingOperationHandler.post(runnable);
    }

    public void scheduleGeolocShareOperation(Runnable runnable) {
        mGeolocSharingOperationHandler.post(runnable);
    }

    public void onCoreLayerStarted() {
        scheduleImageShareOperation(new UpdateImageSharingStateAfterUngracefulTerminationTask(
                mRichCallHistory, mImageSharingService));
        scheduleVideoShareOperation(new UpdateVideoSharingStateAfterUngracefulTerminationTask(
                mRichCallHistory, mVideoSharingService));
        scheduleGeolocShareOperation(new UpdateGeolocSharingStateAfterUngracefulTerminationTask(
                mRichCallHistory, mGeolocSharingService));
    }

    @Override
    public synchronized void start() {
        if (isServiceStarted()) {
            /* Already started */
            return;
        }
        setServiceStarted(true);
    }

    @Override
    public synchronized void stop(TerminationReason reasonCode) {
        if (!isServiceStarted()) {
            /* Already stopped */
            return;
        }
        setServiceStarted(false);
        if (TerminationReason.TERMINATION_BY_SYSTEM == reasonCode) {
            mImageSharingOperationHandler.getLooper().quit();
            mVideoSharingOperationHandler.getLooper().quit();
            mGeolocSharingOperationHandler.getLooper().quit();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mImageSharingDeleteOperationHandler.getLooper().quitSafely();
                mVideoSharingDeleteOperationHandler.getLooper().quitSafely();
                mGeolocSharingDeleteOperationHandler.getLooper().quitSafely();
            } else {
                mImageSharingDeleteOperationHandler.getLooper().quit();
                mVideoSharingDeleteOperationHandler.getLooper().quit();
                mGeolocSharingDeleteOperationHandler.getLooper().quit();
            }
        }
    }

    @Override
    public void check() {
    }

    private ImageTransferSession getUnidirectionalImageSharingSession() {
        if (sLogger.isActivated()) {
            sLogger.debug("Get Unidirection ImageTransferSession ");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mImageTransferSessionCache.values().iterator().next();
        }
    }

    private boolean isCurrentlyImageSharingUniDirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mImageTransferSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
        }
    }

    private boolean isCurrentlyImageSharingBiDirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mImageTransferSessionCache.size() >= SharingDirection.BIDIRECTIONAL;
        }
    }

    private void assertMaximumImageTransferSize(long size, String errorMessage)
            throws CoreException {
        long maxSize = ImageTransferSession.getMaxImageSharingSize(mRcsSettings);
        if (maxSize > 0 && size > maxSize) {
            if (sLogger.isActivated()) {
                sLogger.error(errorMessage);
            }
            throw new CoreException(errorMessage);
        }
    }

    private VideoStreamingSession getUnidirectionalVideoSharingSession() {
        if (sLogger.isActivated()) {
            sLogger.debug("Get Unidirection VideoStreamingSession ");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mVideoStremaingSessionCache.values().iterator().next();
        }
    }

    private boolean isCurrentlyVideoSharingUniDirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mVideoStremaingSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
        }
    }

    private boolean isCurrentlyVideoSharingBiDirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mVideoStremaingSessionCache.size() >= SharingDirection.BIDIRECTIONAL;
        }
    }

    private GeolocTransferSession getUnidirectionalGeolocSharingSession() {
        if (sLogger.isActivated()) {
            sLogger.debug("Get Unidirection GeolocTransferSession ");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGeolocTransferSessionCache.values().iterator().next();
        }
    }

    private boolean isCurrentlyGeolocSharingUniDirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mGeolocTransferSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
        }
    }

    private boolean isCurrentlyGeolocSharingBiDirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mGeolocTransferSessionCache.size() >= SharingDirection.BIDIRECTIONAL;
        }
    }

    public void addSession(ImageTransferSession session) {
        String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Add ImageTransferSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mImageTransferSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final ImageTransferSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Remove ImageTransferSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mImageTransferSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public ImageTransferSession getImageTransferSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get ImageTransferSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mImageTransferSessionCache.get(sessionId);
        }
    }

    public void addSession(VideoStreamingSession session) {
        String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Add VideoStreamingSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mVideoStremaingSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final VideoStreamingSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Remove VideoStreamingSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mVideoStremaingSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public VideoStreamingSession getVideoSharingSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get VideoStreamingSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mVideoStremaingSessionCache.get(sessionId);
        }
    }

    public void addSession(GeolocTransferSession session) {
        String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Add GeolocTransferSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGeolocTransferSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GeolocTransferSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Remove GeolocTransferSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGeolocTransferSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public GeolocTransferSession getGeolocTransferSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get GeolocTransferSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGeolocTransferSessionCache.get(sessionId);
        }
    }

    /**
     * Is call connected with a given contact
     * 
     * @param contact Contact Id
     * @return Boolean
     */
    public boolean isCallConnectedWith(ContactId contact) {
        return (mCallManager.isCallConnectedWith(contact));
    }

    /**
     * Initiate an image sharing session
     * 
     * @param contact Remote contact identifier
     * @param content The file content to share
     * @param thumbnail The thumbnail content
     * @param timestamp Local timestamp
     * @return CSh session
     * @throws CoreException
     */
    public ImageTransferSession createImageSharingSession(ContactId contact, MmContent content,
            MmContent thumbnail, long timestamp) throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate image sharing session with contact " + contact + ", file "
                    + content.toString());
        }
        /* Test if call is established */
        if (!isCallConnectedWith(contact)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Rich call not established: cancel the initiation");
            }
            throw new CoreException("Call not established");
        }

        assertMaximumImageTransferSize(content.getSize(), "File exceeds max size.");

        /* Reject if there are already 2 bidirectional sessions with a given contact */
        boolean rejectInvitation = false;
        if (isCurrentlyImageSharingBiDirectional()) {
            /* Already a bidirectional session */
            if (sLogger.isActivated()) {
                sLogger.debug("Max sessions reached");
            }
            rejectInvitation = true;
        } else if (isCurrentlyImageSharingUniDirectional()) {
            ImageTransferSession currentSession = getUnidirectionalImageSharingSession();
            if (isSessionOriginating(currentSession)) {
                /* Originating session already used */
                if (sLogger.isActivated()) {
                    sLogger.debug("Max originating sessions reached");
                }
                rejectInvitation = true;
            } else if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
                /* Not the same contact */
                if (sLogger.isActivated()) {
                    sLogger.debug("Only bidirectional session with same contact authorized");
                }
                rejectInvitation = true;
            }
        }
        if (rejectInvitation) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of sharing sessions is achieved: cancel the initiation");
            }
            throw new CoreException("Max content sharing sessions achieved");
        }
        return new OriginatingImageTransferSession(this, content, contact, thumbnail, mRcsSettings,
                timestamp, mContactManager, mCapabilityService);
    }

    /**
     * Receive an image sharing invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onImageSharingInvitationReceived(final SipRequest invite, final long timestamp) {
        final RichcallService richCallService = this;
        scheduleImageShareOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    if (logActivated) {
                        sLogger.info("Receive an image sharing session invitation");
                    }

                    /* Check validity of contact */
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                            .getAssertedIdentity(invite));
                    if (number == null) {
                        if (logActivated) {
                            sLogger.debug("Rich call not established: cannot parse contact");
                        }
                        sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                        return;

                    }
                    ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                    mContactManager.setContactDisplayName(contact,
                            SipUtils.getDisplayNameFromInvite(invite));

                    /* Test if call is established */
                    if (!isCallConnectedWith(contact)) {
                        if (logActivated) {
                            sLogger.debug("Rich call not established: reject the invitation");
                        }
                        sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                        return;
                    }

                    /* Test if the contact is blocked */
                    if (mContactManager.isBlockedForContact(contact)) {
                        if (logActivated) {
                            sLogger.debug("Contact " + contact
                                    + " is blocked: automatically reject the sharing invitation");
                        }
                        MmContent content = ContentManager.createMmContentFromSdp(invite,
                                mRcsSettings);
                        addImageSharingInvitationRejected(contact, content,
                                ImageSharing.ReasonCode.REJECTED_SPAM, timestamp);
                        sendErrorResponse(invite, Response.DECLINE);
                        return;
                    }

                    MmContent content = ContentManager.createMmContentFromSdp(invite, mRcsSettings);
                    /* Reject if there are already 2 bidirectional sessions with a given contact */
                    boolean rejectInvitation = false;
                    if (isCurrentlyImageSharingBiDirectional()) {
                        /* Already a bidirectional session */
                        if (logActivated) {
                            sLogger.debug("Max sessions reached");
                        }
                        rejectInvitation = true;
                        addImageSharingInvitationRejected(contact, content,
                                ImageSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS, timestamp);
                    } else if (isCurrentlyImageSharingUniDirectional()) {
                        ImageTransferSession currentSession = getUnidirectionalImageSharingSession();
                        if (isSessionTerminating(currentSession)) {
                            /* Terminating session already used */
                            if (logActivated) {
                                sLogger.debug("Max terminating sessions reached");
                            }
                            rejectInvitation = true;
                        } else if (contact == null
                                || !contact.equals(currentSession.getRemoteContact())) {
                            /* Not the same contact */
                            if (logActivated) {
                                sLogger.debug("Only bidirectional session with same contact authorized");
                            }
                            rejectInvitation = true;
                            addImageSharingInvitationRejected(contact, content,
                                    ImageSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS,
                                    timestamp);
                        }
                    }
                    if (rejectInvitation) {
                        if (logActivated) {
                            sLogger.debug("Reject the invitation");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }

                    /* Auto reject if file too big or if storage capacity is too small */
                    ContentSharingError error = ImageTransferSession.isImageCapacityAcceptable(
                            content.getSize(), mRcsSettings);
                    if (error != null) {
                        sendErrorResponse(invite, Response.DECLINE);
                        int errorCode = error.getErrorCode();
                        switch (errorCode) {
                            case ContentSharingError.MEDIA_SIZE_TOO_BIG:
                                addImageSharingInvitationRejected(contact, content,
                                        ImageSharing.ReasonCode.REJECTED_MAX_SIZE, timestamp);
                                break;
                            case ContentSharingError.NOT_ENOUGH_STORAGE_SPACE:
                                addImageSharingInvitationRejected(contact, content,
                                        ImageSharing.ReasonCode.REJECTED_LOW_SPACE, timestamp);
                                break;
                            default:
                                if (sLogger.isActivated()) {
                                    sLogger.error("Unexpected error while receiving image share invitation"
                                            .concat(Integer.toString(errorCode)));
                                }
                        }
                        return;
                    }

                    ImageTransferSession session = new TerminatingImageTransferSession(
                            richCallService, invite, contact, mRcsSettings, timestamp,
                            mContactManager, mCapabilityService);

                    mImageSharingService.receiveImageSharingInvitation(session);

                    session.startSession();

                } catch (FileAccessException | RuntimeException | PayloadException e) {
                    sLogger.error("Failed to receive image share invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive image share invitation! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);
                }
            }
        });
    }

    /**
     * Initiate a live video sharing session
     * 
     * @param contact Remote contact Id
     * @param player Media player
     * @param timestamp Local timestamp
     * @return CSh session
     * @throws CoreException
     */
    public VideoStreamingSession createLiveVideoSharingSession(ContactId contact,
            IVideoPlayer player, long timestamp) throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a live video sharing session");
        }

        /* Test if call is established */
        if (!isCallConnectedWith(contact)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Rich call not established: cancel the initiation");
            }
            throw new CoreException("Call not established");
        }

        /* Reject if there are already 2 bidirectional sessions with a given contact */
        boolean rejectInvitation = false;
        if (isCurrentlyVideoSharingBiDirectional()) {
            /* Already a bidirectional session */
            if (sLogger.isActivated()) {
                sLogger.debug("Max sessions reached");
            }
            rejectInvitation = true;

        } else if (isCurrentlyVideoSharingUniDirectional()) {
            VideoStreamingSession currentSession = getUnidirectionalVideoSharingSession();
            if (isSessionOriginating(currentSession)) {
                /* Originating session already used */
                if (sLogger.isActivated()) {
                    sLogger.debug("Max originating sessions reached");
                }
                rejectInvitation = true;

            } else if (contact == null || !contact.equals(currentSession.getRemoteContact())) {
                /* Not the same contact */
                if (sLogger.isActivated()) {
                    sLogger.debug("Only bidirectional session with same contact authorized");
                }
                rejectInvitation = true;
            }
        }
        if (rejectInvitation) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of sharing sessions is achieved: cancel the initiation");
            }
            throw new CoreException("Max content sharing sessions achieved");
        }
        return new OriginatingVideoStreamingSession(this, player,
                ContentManager.createGenericLiveVideoContent(), contact, mRcsSettings, timestamp,
                mContactManager, mCapabilityService);
    }

    /**
     * Receive a video sharing invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onVideoSharingInvitationReceived(final SipRequest invite, final long timestamp) {
        final RichcallService richCallService = this;
        scheduleVideoShareOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    if (logActivated) {
                        sLogger.info("Receive a video sharing invitation");
                    }
                    /* Check validity of contact */
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                            .getAssertedIdentity(invite));
                    if (number == null) {
                        if (logActivated) {
                            sLogger.debug("Rich call not established: cannot parse contact");
                        }
                        sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                        return;

                    }
                    ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                    mContactManager.setContactDisplayName(contact,
                            SipUtils.getDisplayNameFromInvite(invite));

                    /* Test if call is established */
                    if (!isCallConnectedWith(contact)) {
                        if (logActivated) {
                            sLogger.debug("Rich call not established: reject the invitation");
                        }
                        sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                        return;
                    }

                    String remoteSdp = invite.getSdpContent();
                    SipUtils.assertContentIsNotNull(remoteSdp, invite);
                    VideoContent content = ContentManager.createLiveVideoContentFromSdp(remoteSdp
                            .getBytes(UTF8));

                    /* Test if the contact is blocked */
                    if (mContactManager.isBlockedForContact(contact)) {
                        if (logActivated) {
                            sLogger.debug("Contact " + contact
                                    + " is blocked: automatically reject the sharing invitation");
                        }
                        addVideoSharingInvitationRejected(contact, content,
                                VideoSharing.ReasonCode.REJECTED_SPAM, timestamp);
                        sendErrorResponse(invite, Response.DECLINE);
                        return;
                    }

                    /* Reject if there are already 2 bidirectional sessions with a given contact */
                    boolean rejectInvitation = false;
                    if (isCurrentlyVideoSharingBiDirectional()) {
                        /* Already a bidirectional session */
                        if (logActivated) {
                            sLogger.debug("Max sessions reached");
                        }
                        rejectInvitation = true;
                        addVideoSharingInvitationRejected(contact, content,
                                VideoSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS, timestamp);

                    } else if (isCurrentlyVideoSharingUniDirectional()) {
                        VideoStreamingSession currentSession = getUnidirectionalVideoSharingSession();
                        if (isSessionTerminating(currentSession)) {
                            // Terminating session already used
                            if (logActivated) {
                                sLogger.debug("Max terminating sessions reached");
                            }
                            rejectInvitation = true;
                            addVideoSharingInvitationRejected(contact, content,
                                    VideoSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS,
                                    timestamp);

                        } else if (contact == null
                                || !contact.equals(currentSession.getRemoteContact())) {
                            // Not the same contact
                            if (logActivated) {
                                sLogger.debug("Only bidirectional session with same contact authorized");
                            }
                            rejectInvitation = true;
                            addVideoSharingInvitationRejected(contact, content,
                                    VideoSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS,
                                    timestamp);
                        }
                    }
                    if (rejectInvitation) {
                        if (logActivated) {
                            sLogger.debug("Reject the invitation");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }

                    /* Create a new session */
                    VideoStreamingSession session = new TerminatingVideoStreamingSession(
                            richCallService, invite, contact, mRcsSettings, timestamp,
                            mContactManager, mCapabilityService);

                    mVideoSharingService.receiveVideoSharingInvitation(session);

                    session.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive video share invitation! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (PayloadException | RuntimeException e) {
                    sLogger.error("Failed to receive video share invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Initiate a geoloc sharing session
     * 
     * @param contact Remote contact
     * @param content Content to be shared
     * @param geoloc Geolocation
     * @param timestamp Local timesatmp
     * @return GeolocTransferSession
     * @throws CoreException
     */
    public GeolocTransferSession createGeolocSharingSession(ContactId contact, MmContent content,
            Geoloc geoloc, long timestamp) throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate geoloc sharing session with contact " + contact + ".");
        }
        if (!isCallConnectedWith(contact)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Rich call not established: cancel the initiation.");
            }
            throw new CoreException("Call not established");
        }
        return new OriginatingGeolocTransferSession(this, content, contact, geoloc, mRcsSettings,
                timestamp, mContactManager, mCapabilityService);
    }

    /**
     * Receive a geoloc sharing invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onGeolocSharingInvitationReceived(final SipRequest invite, final long timestamp) {
        final RichcallService richCallService = this;
        scheduleGeolocShareOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    if (logActivated) {
                        sLogger.info("Receive a geoloc sharing session invitation");
                    }

                    /* Check validity of contact */
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                            .getAssertedIdentity(invite));
                    if (number == null) {
                        if (logActivated) {
                            sLogger.debug("Rich call not established: cannot parse contact");
                        }
                        sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                        return;

                    }
                    ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                    mContactManager.setContactDisplayName(contact,
                            SipUtils.getDisplayNameFromInvite(invite));

                    /* Test if call is established */
                    if (!isCallConnectedWith(contact)) {
                        if (logActivated) {
                            sLogger.debug("Rich call not established: reject the invitation");
                        }
                        sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                        return;
                    }

                    /* Test if the contact is blocked */
                    if (mContactManager.isBlockedForContact(contact)) {
                        if (logActivated) {
                            sLogger.debug("Contact " + contact
                                    + " is blocked: automatically reject the sharing invitation");
                        }
                        addGeolocSharingInvitationRejected(contact,
                                GeolocSharing.ReasonCode.REJECTED_SPAM, timestamp);
                        sendErrorResponse(invite, Response.DECLINE);
                        return;
                    }

                    /* Reject if there are already 2 bidirectional sessions with a given contact */
                    boolean rejectInvitation = false;
                    if (isCurrentlyGeolocSharingBiDirectional()) {
                        /* Already a bidirectional session */
                        if (logActivated) {
                            sLogger.debug("Max sessions reached");
                        }
                        addGeolocSharingInvitationRejected(contact,
                                GeolocSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS, timestamp);
                        rejectInvitation = true;

                    } else if (isCurrentlyGeolocSharingUniDirectional()) {
                        GeolocTransferSession currentSession = getUnidirectionalGeolocSharingSession();
                        if (isSessionTerminating(currentSession)) {
                            /* Terminating session already used */
                            if (logActivated) {
                                sLogger.debug("Max terminating sessions reached");
                            }
                            addGeolocSharingInvitationRejected(contact,
                                    GeolocSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS,
                                    timestamp);
                            rejectInvitation = true;

                        } else if (contact == null
                                || !contact.equals(currentSession.getRemoteContact())) {
                            /* Not the same contact */
                            if (logActivated) {
                                sLogger.debug("Only bidirectional session with same contact authorized");
                            }
                            addGeolocSharingInvitationRejected(contact,
                                    GeolocSharing.ReasonCode.REJECTED_MAX_SHARING_SESSIONS,
                                    timestamp);
                            rejectInvitation = true;
                        }
                    }
                    if (rejectInvitation) {
                        if (logActivated) {
                            sLogger.debug("Reject the invitation");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }

                    /* Create a new session */
                    GeolocTransferSession session = new TerminatingGeolocTransferSession(
                            richCallService, invite, contact, mRcsSettings, timestamp,
                            mContactManager, mCapabilityService);

                    mGeolocSharingService.receiveGeolocSharingInvitation(session);

                    session.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive geoloc share invitation! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (PayloadException | RuntimeException e) {
                    sLogger.error("Failed to receive geoloc share invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * This function is used when all session needs to terminated in both invitation pending and
     * started state.
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void terminateAllSessions() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Terminate all sessions");
        }
        terminateAllSessions(TerminationReason.TERMINATION_BY_SYSTEM);
    }

    /**
     * Is the current session an originating one
     * 
     * @param session the session instance
     * @return true if session is an originating content sharing session (image or video)
     */
    private boolean isSessionOriginating(ContentSharingSession session) {
        return (session instanceof OriginatingImageTransferSession || session instanceof OriginatingVideoStreamingSession);
    }

    /**
     * Is the current session a terminating one
     * 
     * @param session the session instance
     * @return true if session is an terminating content sharing session (image or video)
     */
    private boolean isSessionTerminating(ContentSharingSession session) {
        return (session instanceof TerminatingImageTransferSession || session instanceof TerminatingVideoStreamingSession);
    }

    /**
     * Handles image sharing rejection
     * 
     * @param remoteContact Remote contact
     * @param content Multimedia content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got image sharing invitation
     */
    public void addImageSharingInvitationRejected(ContactId remoteContact, MmContent content,
            ImageSharing.ReasonCode reasonCode, long timestamp) {
        mImageSharingService.addImageSharingInvitationRejected(remoteContact, content, reasonCode,
                timestamp);
    }

    /**
     * Handle the case of rejected video sharing
     * 
     * @param remoteContact Remote contact
     * @param content Video content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got video sharing invitation
     */
    public void addVideoSharingInvitationRejected(ContactId remoteContact, VideoContent content,
            VideoSharing.ReasonCode reasonCode, long timestamp) {
        mVideoSharingService.addVideoSharingInvitationRejected(remoteContact, content, reasonCode,
                timestamp);
    }

    /**
     * Handle the case of rejected geoloc sharing
     * 
     * @param remoteContact Remote contact
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got geoloc sharing invitation
     */
    public void addGeolocSharingInvitationRejected(ContactId remoteContact,
            GeolocSharing.ReasonCode reasonCode, long timestamp) {
        mGeolocSharingService.addGeolocSharingInvitationRejected(remoteContact, reasonCode,
                timestamp);
    }

    /**
     * Try to delete all image sharing from history and abort/reject any associated ongoing session
     * if such exists.
     */
    public void tryToDeleteImageSharings() {
        mImageSharingDeleteOperationHandler.post(new ImageSharingDeleteTask(mImageSharingService,
                this, mLocalContentResolver));
    }

    /**
     * Try to delete image sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact the remote contact
     */
    public void tryToDeleteImageSharings(ContactId contact) {
        mImageSharingDeleteOperationHandler.post(new ImageSharingDeleteTask(mImageSharingService,
                this, mLocalContentResolver, contact));
    }

    /**
     * Try to delete a image sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId the sharing ID
     */
    public void tryToDeleteImageSharing(String sharingId) {
        mImageSharingDeleteOperationHandler.post(new ImageSharingDeleteTask(mImageSharingService,
                this, mLocalContentResolver, sharingId));
    }

    /**
     * Try to delete all video sharing from history and abort/reject any associated ongoing session
     * if such exists.
     */
    public void tryToDeleteVideoSharings() {
        mVideoSharingDeleteOperationHandler.post(new VideoSharingDeleteTask(mVideoSharingService,
                this, mLocalContentResolver));
    }

    /**
     * Try to delete video sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact the remote contact
     */
    public void tryToDeleteVideoSharings(ContactId contact) {
        mVideoSharingDeleteOperationHandler.post(new VideoSharingDeleteTask(mVideoSharingService,
                this, mLocalContentResolver, contact));
    }

    /**
     * Try to delete a video sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId the sharing ID
     */
    public void tryToDeleteVideoSharing(String sharingId) {
        mVideoSharingDeleteOperationHandler.post(new VideoSharingDeleteTask(mVideoSharingService,
                this, mLocalContentResolver, sharingId));
    }

    /**
     * Try to delete all geoloc sharing from history and abort/reject any associated ongoing session
     * if such exists.
     */
    public void tryToDeleteGeolocSharings() {
        mGeolocSharingDeleteOperationHandler.post(new GeolocSharingDeleteTask(
                mGeolocSharingService, this, mLocalContentResolver));
    }

    /**
     * Try to delete geoloc sharing with a given contact from history and abort/reject any
     * associated ongoing session if such exists.
     * 
     * @param contact the remote contact
     */
    public void tryToDeleteGeolocSharings(ContactId contact) {
        mGeolocSharingDeleteOperationHandler.post(new GeolocSharingDeleteTask(
                mGeolocSharingService, this, mLocalContentResolver, contact));
    }

    /**
     * Try to delete a geoloc sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId the sharing ID
     */
    public void tryToDeleteGeolocSharing(String sharingId) {
        mGeolocSharingDeleteOperationHandler.post(new GeolocSharingDeleteTask(
                mGeolocSharingService, this, mLocalContentResolver, sharingId));
    }

}
