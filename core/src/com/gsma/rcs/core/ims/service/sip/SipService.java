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

package com.gsma.rcs.core.ims.service.sip;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
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
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.HashMap;
import java.util.Map;

import javax2.sip.message.Response;

/**
 * SIP service
 * 
 * @author Jean-Marc AUFFRET
 */
public class SipService extends ImsService {

    private static final String MM_MESSAGING_OPERATION_THREAD_NAME = "MmmOperations";

    private static final String MM_STREAMING_OPERATION_THREAD_NAME = "MmsOperations";

    private final static Logger sLogger = Logger.getLogger(SipService.class.getSimpleName());

    /**
     * MIME-type for multimedia services
     */
    public final static String MIME_TYPE = "application/*";

    /**
     * GenericSipMsrpSessionCache with SessionId as key
     */
    private Map<String, GenericSipMsrpSession> mGenericSipMsrpSessionCache = new HashMap<String, GenericSipMsrpSession>();

    /**
     * GenericSipRtpSessionCache with SessionId as key
     */
    private Map<String, GenericSipRtpSession> mGenericSipRtpSessionCache = new HashMap<String, GenericSipRtpSession>();

    private final ContactManager mContactManager;

    private final RcsSettings mRcsSettings;

    private MultimediaSessionServiceImpl mMmSessionService;

    private Handler mMultimediaMessagingOperationHandler;
    private Handler mMultimediaStreamingOperationHandler;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param contactManager ContactManager
     * @param rcsSettings
     */
    public SipService(ImsModule parent, ContactManager contactManager, RcsSettings rcsSettings) {
        super(parent, true);
        mContactManager = contactManager;
        mRcsSettings = rcsSettings;
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

    /**
     * /** Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);

        mMultimediaMessagingOperationHandler = allocateBgHandler(MM_MESSAGING_OPERATION_THREAD_NAME);
        mMultimediaStreamingOperationHandler = allocateBgHandler(MM_STREAMING_OPERATION_THREAD_NAME);
    }

    /**
     * Stop the IMS service
     */
    public synchronized void stop() {
        if (!isServiceStarted()) {
            // Already stopped
            return;
        }
        setServiceStarted(false);

        mMultimediaMessagingOperationHandler.getLooper().quit();
        mMultimediaMessagingOperationHandler = null;

        mMultimediaStreamingOperationHandler.getLooper().quit();
        mMultimediaStreamingOperationHandler = null;
    }

    /**
     * Check the IMS service
     */
    public void check() {
    }

    /**
     * Initiate a MSRP session
     * 
     * @param contact Remote contact Id
     * @param featureTag Feature tag of the service
     * @return SIP session
     */
    public GenericSipMsrpSession createMsrpSession(ContactId contact, String featureTag) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a MSRP session with contact " + contact);
        }
        return new OriginatingSipMsrpSession(this, contact, featureTag, mRcsSettings,
                System.currentTimeMillis(), mContactManager);
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
            mContactManager.setContactDisplayName(remote,
                    SipUtils.getDisplayNameFromUri(invite.getFrom()));

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

        } catch (PayloadException e) {
            sLogger.error("Failed to receive generic MSRP session invitation!", e);

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to receive generic MSRP session invitation!", e);
        }
    }

    /**
     * Initiate a RTP session
     * 
     * @param contact Remote contact
     * @param featureTag Feature tag of the service
     * @return SIP session
     */
    public GenericSipRtpSession createRtpSession(ContactId contact, String featureTag) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a RTP session with contact " + contact);
        }
        return new OriginatingSipRtpSession(this, contact, featureTag, mRcsSettings,
                System.currentTimeMillis(), mContactManager);
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
            mContactManager.setContactDisplayName(remote,
                    SipUtils.getDisplayNameFromUri(invite.getFrom()));

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

        } catch (PayloadException e) {
            sLogger.error("Failed to receive generic RTP session invitation!", e);

        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to receive generic RTP session invitation!", e);
        }
    }

    public void addSession(GenericSipMsrpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GenericSipMsrpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipMsrpSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GenericSipMsrpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GenericSipMsrpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipMsrpSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public GenericSipMsrpSession getGenericSipMsrpSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GenericSipMsrpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGenericSipMsrpSessionCache.get(sessionId);
        }
    }

    public void addSession(GenericSipRtpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GenericSipRtpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipRtpSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GenericSipRtpSession session) {
        final String sessionId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GenericSipRtpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipRtpSessionCache.remove(sessionId);
            removeImsServiceSession(session);
        }
    }

    public GenericSipRtpSession getGenericSipRtpSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GenericSipRtpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGenericSipRtpSessionCache.get(sessionId);
        }
    }

}
