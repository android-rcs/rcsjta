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

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.messaging.OriginatingSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.messaging.TerminatingSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.OriginatingSipRtpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.TerminatingSipRtpSession;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

import javax2.sip.message.Response;

/**
 * SIP service
 * 
 * @author Jean-Marc AUFFRET
 */
public class SipService extends ImsService {
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(SipService.class.getSimpleName());

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

    private final ContactsManager mContactManager;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param contactManager ContactsManager
     * @param rcsSettings
     * @throws CoreException
     */
    public SipService(ImsModule parent, ContactsManager contactManager, RcsSettings rcsSettings)
            throws CoreException {
        super(parent, true);

        mContactManager = contactManager;
        mRcsSettings = rcsSettings;
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
    public GenericSipMsrpSession initiateMsrpSession(ContactId contact, String featureTag) {
        if (logger.isActivated()) {
            logger.info("Initiate a MSRP session with contact " + contact);
        }

        // Create a new session
        long timestamp = System.currentTimeMillis();
        OriginatingSipMsrpSession session = new OriginatingSipMsrpSession(this, contact,
                featureTag, mRcsSettings, timestamp, mContactManager);

        return session;
    }

    /**
     * Receive a session invitation with MSRP media
     * 
     * @param sessionInvite Resolved intent
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveMsrpSessionInvitation(Intent sessionInvite, SipRequest invite, long timestamp) {
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                .getAssertedIdentity(invite));
        if (number == null) {
            if (logger.isActivated()) {
                logger.warn("Cannot process MSRP invitation: invalid SIP header");
            }
            sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
            return;
        }
        // Test if the contact is blocked
        ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
        if (mContactManager.isBlockedForContact(remote)) {
            if (logger.isActivated()) {
                logger.debug("Contact " + remote
                        + " is blocked: automatically reject the session invitation");
            }

            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            return;
        }

        // Create a new session
        TerminatingSipMsrpSession session = new TerminatingSipMsrpSession(this, invite, remote,
                sessionInvite, mRcsSettings, timestamp, mContactManager);

        getImsModule().getCore().getListener()
                .handleSipMsrpSessionInvitation(sessionInvite, session);

        session.startSession();
    }

    /**
     * Initiate a RTP session
     * 
     * @param contact Remote contact
     * @param featureTag Feature tag of the service
     * @return SIP session
     */
    public GenericSipRtpSession initiateRtpSession(ContactId contact, String featureTag) {
        if (logger.isActivated()) {
            logger.info("Initiate a RTP session with contact " + contact);
        }

        // Create a new session
        long timestamp = System.currentTimeMillis();
        OriginatingSipRtpSession session = new OriginatingSipRtpSession(this, contact, featureTag,
                mRcsSettings, timestamp, mContactManager);

        return session;
    }

    /**
     * Receive a session invitation with RTP media
     * 
     * @param sessionInvite Resolved intent
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveRtpSessionInvitation(Intent sessionInvite, SipRequest invite, long timestamp) {
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                .getAssertedIdentity(invite));
        if (number == null) {
            if (logger.isActivated()) {
                logger.warn("Cannot process RTP invitation: invalid SIP header");
            }
            sendErrorResponse(invite, Response.SESSION_NOT_ACCEPTABLE);
            return;
        }
        // Test if the contact is blocked
        ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
        if (mContactManager.isBlockedForContact(remote)) {
            if (logger.isActivated()) {
                logger.debug("Contact " + remote
                        + " is blocked: automatically reject the session invitation");
            }

            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            return;
        }

        // Create a new session
        TerminatingSipRtpSession session = new TerminatingSipRtpSession(this, invite, remote,
                sessionInvite, mRcsSettings, timestamp, mContactManager);

        getImsModule().getCore().getListener()
                .handleSipRtpSessionInvitation(sessionInvite, session);

        session.startSession();
    }

    public void addSession(GenericSipMsrpSession session) {
        final String sessionId = session.getSessionID();
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Add GenericSipMsrpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipMsrpSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GenericSipMsrpSession session) {
        final String sessionId = session.getSessionID();
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Remove GenericSipMsrpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads accessing
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mGenericSipMsrpSessionCache.remove(sessionId);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public GenericSipMsrpSession getGenericSipMsrpSession(String sessionId) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Get GenericSipMsrpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGenericSipMsrpSessionCache.get(sessionId);
        }
    }

    public void addSession(GenericSipRtpSession session) {
        final String sessionId = session.getSessionID();
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Add GenericSipRtpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGenericSipRtpSessionCache.put(sessionId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GenericSipRtpSession session) {
        final String sessionId = session.getSessionID();
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Remove GenericSipRtpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads accessing
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mGenericSipRtpSessionCache.remove(sessionId);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public GenericSipRtpSession getGenericSipRtpSession(String sessionId) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Get GenericSipRtpSession with sessionId '")
                    .append(sessionId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGenericSipRtpSessionCache.get(sessionId);
        }
    }

}
