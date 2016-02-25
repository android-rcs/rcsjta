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

package com.gsma.rcs.core.ims.service.sip;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.capability.CapabilityUtils;
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.messaging.OriginatingSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.messaging.TerminatingSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.OriginatingSipRtpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.TerminatingSipRtpSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.MultimediaSessionServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax2.sip.message.Response;

/**
 * SIP service
 * 
 * @author Jean-Marc AUFFRET
 */
public class SipService extends ImsService {

    private static final String MM_MESSAGING_OPERATION_THREAD_NAME = "MmmOperations";

    private static final String MM_STREAMING_OPERATION_THREAD_NAME = "MmsOperations";

    private static final String MM_INSTANT_MESSAGE_OPERATION_THREAD_NAME = "ImmOperations";

    private final static Logger sLogger = Logger.getLogger(SipService.class.getSimpleName());

    /**
     * Default MIME-type for multimedia services
     */
    public final static String DEFAULT_MIME_TYPE = "application/*";

    /**
     * GenericSipMsrpSessionCache with SessionId as key
     */
    private Map<String, GenericSipMsrpSession> mGenericSipMsrpSessionCache = new HashMap<>();

    /**
     * GenericSipRtpSessionCache with SessionId as key
     */
    private Map<String, GenericSipRtpSession> mGenericSipRtpSessionCache = new HashMap<>();

    private final ContactManager mContactManager;

    private final RcsSettings mRcsSettings;

    private ImmManager mImmManager;

    private MultimediaSessionServiceImpl mMmSessionService;

    private final Handler mMultimediaMessagingOperationHandler;
    private final Handler mMultimediaStreamingOperationHandler;
    private final Handler mMultimediaMessageOperationHandler;

    /**
     * Constructor
     *
     * @param parent IMS module
     * @param contactManager ContactManager
     * @param rcsSettings the RCS settings accessor
     */
    public SipService(ImsModule parent, ContactManager contactManager, RcsSettings rcsSettings) {
        super(parent, true);
        mContactManager = contactManager;
        mRcsSettings = rcsSettings;
        mMultimediaMessagingOperationHandler = allocateBgHandler(MM_MESSAGING_OPERATION_THREAD_NAME);
        mMultimediaStreamingOperationHandler = allocateBgHandler(MM_STREAMING_OPERATION_THREAD_NAME);
        mMultimediaMessageOperationHandler = allocateBgHandler(MM_INSTANT_MESSAGE_OPERATION_THREAD_NAME);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public void register(MultimediaSessionServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mMmSessionService = service;
    }

    public void scheduleMultimediaMessagingOperation(Runnable runnable) {
        mMultimediaMessagingOperationHandler.post(runnable);
    }

    public void scheduleMultimediaStreamingOperation(Runnable runnable) {
        mMultimediaStreamingOperationHandler.post(runnable);
    }

    public void scheduleMultimediaMessageOperation(Runnable runnable) {
        mMultimediaMessageOperationHandler.post(runnable);
    }

    @Override
    public synchronized void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);
        mImmManager = new ImmManager(this, mRcsSettings);
        mImmManager.start();
    }

    @Override
    public synchronized void stop(ImsServiceSession.TerminationReason reasonCode) {
        if (!isServiceStarted()) {
            // Already stopped
            return;
        }
        setServiceStarted(false);
        mImmManager.terminate();
        mImmManager = null;
        if (ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM == reasonCode) {
            mMultimediaMessagingOperationHandler.getLooper().quit();
            mMultimediaStreamingOperationHandler.getLooper().quit();
            mMultimediaMessageOperationHandler.getLooper().quit();
        }
    }

    @Override
    public void check() {
    }

    /**
     * Initiate a MSRP session
     *
     * @param contact Remote contact Id
     * @param featureTag Feature tag of the service
     * @param acceptTypes Accept-types related to exchanged messages
     * @param acceptWrappedTypes Accept-wrapped-types related to exchanged messages
     * @return SIP session
     */
    public GenericSipMsrpSession createMsrpSession(ContactId contact, String featureTag,
            String[] acceptTypes, String[] acceptWrappedTypes) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a MSRP session with contact " + contact);
        }
        return new OriginatingSipMsrpSession(this, contact, featureTag, mRcsSettings,
                System.currentTimeMillis(), mContactManager, acceptTypes, acceptWrappedTypes);
    }

    /**
     * Receive a session invitation with MSRP media
     *
     * @param sessionInvite Resolved intent
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onMsrpSessionInvitationReceived(final Intent sessionInvite, SipRequest invite,
            long timestamp) {
        try {
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                    .getAssertedIdentity(invite));
            if (number == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Cannot process MSRP invitation: invalid SIP header");
                }
                sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                return;
            }
            // Test if the contact is blocked
            ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
            mContactManager
                    .setContactDisplayName(remote, SipUtils.getDisplayNameFromInvite(invite));
            if (mContactManager.isBlockedForContact(remote)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + remote
                            + " is blocked: automatically reject the session invitation");
                }
                sendErrorResponse(invite, Response.DECLINE);
                return;
            }
            final TerminatingSipMsrpSession session = new TerminatingSipMsrpSession(this, invite,
                    getImsModule(), remote, sessionInvite, mRcsSettings, timestamp, mContactManager);
            mMultimediaMessagingOperationHandler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        mMmSessionService.receiveSipMsrpSessionInvitation(session);
                        session.startSession();

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Failed to receive generic MSRP session invitation!", e);
                    }
                }
            });
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Failed to receive generic MSRP session invitation! ("
                        + e.getMessage() + ")");
            }
        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Failed to receive generic MSRP session invitation!", e);
        }
    }

    /**
     * Initiate a RTP session
     *
     * @param contact Remote contact
     * @param featureTag Feature tag of the service
     * @param encoding Encoding
     * @return SIP session
     */
    public GenericSipRtpSession createRtpSession(ContactId contact, String featureTag,
            String encoding) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a RTP session with contact " + contact);
        }
        return new OriginatingSipRtpSession(this, contact, featureTag, mRcsSettings,
                System.currentTimeMillis(), mContactManager, encoding);
    }

    /**
     * Receive a session invitation with RTP media
     *
     * @param sessionInvite Resolved intent
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onRtpSessionInvitationReceived(final Intent sessionInvite, SipRequest invite,
            long timestamp) {
        try {
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                    .getAssertedIdentity(invite));
            if (number == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Cannot process RTP invitation: invalid SIP header");
                }
                sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
                return;
            }
            // Test if the contact is blocked
            ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
            mContactManager
                    .setContactDisplayName(remote, SipUtils.getDisplayNameFromInvite(invite));
            if (mContactManager.isBlockedForContact(remote)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + remote
                            + " is blocked: automatically reject the session invitation");
                }
                sendErrorResponse(invite, Response.DECLINE);
                return;
            }
            final TerminatingSipRtpSession session = new TerminatingSipRtpSession(this, invite,
                    getImsModule(), remote, sessionInvite, mRcsSettings, timestamp, mContactManager);
            mMultimediaStreamingOperationHandler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        mMmSessionService.receiveSipRtpSessionInvitation(session);
                        session.startSession();

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Failed to receive generic RTP session invitation!", e);
                    }
                }
            });
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Failed to receive generic RTP session invitation! ("
                        + e.getMessage() + ")");
            }
        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Failed to receive generic RTP session invitation!", e);
        }
    }

    public void addSession(GenericSipMsrpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Add GenericSipMsrpSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipMsrpSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GenericSipMsrpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Remove GenericSipMsrpSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipMsrpSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public GenericSipMsrpSession getGenericSipMsrpSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get GenericSipMsrpSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGenericSipMsrpSessionCache.get(sessionId);
        }
    }

    public void addSession(GenericSipRtpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Add GenericSipRtpSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipRtpSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GenericSipRtpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug("Remove GenericSipRtpSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipRtpSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public GenericSipRtpSession getGenericSipRtpSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Get GenericSipRtpSession with sessionId '" + sessionId + "'");
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGenericSipRtpSessionCache.get(sessionId);
        }
    }

    public void sendInstantMultimediaMessage(ContactId contact, String featureTag, byte[] content,
            String contentType) throws NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Send instant multimedia message to contact " + contact);
        }
        if (mImmManager == null) {
            throw new NetworkException("Cannot send multimedia message: SIP service not started!");
        }
        mImmManager.sendMessage(contact, featureTag, content, contentType);
    }

    /**
     * Receive a multimedia instant messsage
     *
     * @param intent Resolved intent
     * @param message Instant message
     */
    public void onInstantMessageReceived(final Intent intent, final SipRequest message) {
        try {
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                    .getAssertedIdentity(message));
            if (number == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Cannot process instant message: invalid SIP header");
                }
                sendErrorResponse(message, Response.SESSION_NOT_ACCEPTABLE);
                return;
            }
            // Test if the contact is blocked
            final ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
            mContactManager.setContactDisplayName(remote,
                    SipUtils.getDisplayNameFromInvite(message));
            if (mContactManager.isBlockedForContact(remote)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + remote
                            + " is blocked: automatically reject the message");
                }
                sendErrorResponse(message, Response.DECLINE);
                return;
            }
            /* Send automatically a 200 Ok */
            getImsModule().getSipManager().sendSipResponse(
                    SipMessageFactory.createResponse(message, IdGenerator.getIdentifier(),
                            Response.OK));
            Set<String> featureTags = message.getFeatureTags();
            String iariFeatureTag = GenericSipSession.getIariFeatureTag(featureTags);
            if (iariFeatureTag == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Cannot process instant message: no service ID");
                }
                sendErrorResponse(message, Response.SESSION_NOT_ACCEPTABLE);
                return;
            }
            final String serviceId = CapabilityUtils.extractServiceId(iariFeatureTag);
            mMultimediaMessageOperationHandler.post(new Runnable() {

                @Override
                public void run() {
                    try {
                        mMmSessionService.receiveSipInstantMessage(intent, remote,
                                message.getRawContent(), message.getContentType(), serviceId);
                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Failed to receive generic instant message!", e);
                    }
                }
            });
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Failed to receive generic instant message! (" + e.getMessage() + ")");
            }
        } catch (PayloadException | RuntimeException e) {
            sLogger.error("Failed to receive instant message!", e);
        }
    }
}
