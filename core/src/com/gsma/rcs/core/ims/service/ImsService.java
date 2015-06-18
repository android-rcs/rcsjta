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

package com.gsma.rcs.core.ims.service;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract IMS service
 * 
 * @author jexa7410
 */
public abstract class ImsService {
    /**
     * IMS service enumerated type
     */
    public enum ImsServiceType {
        /**
         * Terms & conditions service
         */
        TERMS_CONDITIONS,
        /**
         * Capability service
         */
        CAPABILITY,
        /**
         * Instant Messaging service
         */
        INSTANT_MESSAGING,
        /**
         * IP call service
         */
        IPCALL,
        /**
         * Richcall service
         */
        RICHCALL,
        /**
         * Presence service
         */
        PRESENCE,
        /**
         * SIP service
         */
        SIP
    };

    /**
     * Activation flag
     */
    private boolean mActivated = true;

    /**
     * Service state
     */
    private boolean mStarted = false;

    private ImsModule mImsModule;

    /**
     * ImsServiceSessionCache with session dialog path's CallId as key
     */
    private Map<String, ImsServiceSession> mImsServiceSessionCache = new HashMap<String, ImsServiceSession>();

    /**
     * ImsServiceSessionWithoutDialogPathCache with session Id as key
     */
    private Map<String, ImsServiceSession> mImsServiceSessionWithoutDialogPathCache = new HashMap<String, ImsServiceSession>();

    private static final Logger sLogger = Logger.getLogger(ImsService.class.getSimpleName());

    protected final static class SharingDirection {

        public static final int UNIDIRECTIONAL = 1;

        public static final int BIDIRECTIONAL = 2;
    }

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param activated Activation flag
     */
    public ImsService(ImsModule parent, boolean activated) {
        mImsModule = parent;
        mActivated = activated;
    }

    /**
     * Is service activated
     * 
     * @return Boolean
     */
    public boolean isActivated() {
        return mActivated;
    }

    /**
     * Change the activation flag of the service
     * 
     * @param activated Activation flag
     */
    public void setActivated(boolean activated) {
        mActivated = activated;
    }

    /**
     * Returns the IMS module
     * 
     * @return IMS module
     */
    public ImsModule getImsModule() {
        return mImsModule;
    }

    /*
     * This method is by choice not synchronized here since the class extending this base-class will
     * need to handle the synchronization over a larger scope when calling this method anyway and we
     * would like to avoid double locks.
     */
    protected void addImsServiceSession(ImsServiceSession session) {
        mImsServiceSessionCache.put(session.getDialogPath().getCallId(), session);
    }

    /*
     * This method is by choice not synchronized here since the class extending this base-class will
     * need to handle the synchronization over a larger scope when calling this method anyway and we
     * would like to avoid double locks.
     */
    protected void removeImsServiceSession(ImsServiceSession session) {
        mImsServiceSessionCache.remove(session.getDialogPath().getCallId());
    }

    /**
     * Gets IMS session from callId
     * 
     * @param callId call ID
     * @return ImsServiceSession
     */
    public ImsServiceSession getImsServiceSession(String callId) {
        synchronized (getImsServiceSessionOperationLock()) {
            return mImsServiceSessionCache.get(callId);
        }
    }

    /*
     * This method is by choice not synchronized here since the class extending this base-class will
     * need to handle the synchronization over a larger scope when calling this method anyway and we
     * would like to avoid double locks.
     */
    protected void addImsServiceSessionWithoutDialogPath(ImsServiceSession session) {
        mImsServiceSessionWithoutDialogPathCache.put(session.getSessionID(), session);
    }

    /*
     * This method is by choice not synchronized here since the class extending this base-class will
     * need to handle the synchronization over a larger scope when calling this method anyway and we
     * would like to avoid double locks.
     */
    protected void removeImsServiceSessionWithoutDialogPath(ImsServiceSession session) {
        mImsServiceSessionWithoutDialogPathCache.remove(session.getSessionID());
    }

    protected Object getImsServiceSessionOperationLock() {
        return mImsServiceSessionCache;
    }

    /**
     * Is service started
     * 
     * @return Boolean
     */
    public boolean isServiceStarted() {
        return mStarted;
    }

    /**
     * Set service state
     * 
     * @param state State
     */
    public void setServiceStarted(boolean state) {
        mStarted = state;
    }

    /**
     * Start the IMS service
     */
    public abstract void start();

    /**
     * Stop the IMS service
     */
    public abstract void stop();

    /**
     * Check the IMS service
     */
    public abstract void check();

    /**
     * This function is used when all session needs to terminated in both invitation pending and
     * started state.
     * 
     * @param reason termination reason
     */
    public void terminateAllSessions(TerminationReason reason) {
        synchronized (getImsServiceSessionOperationLock()) {
            for (ImsServiceSession session : mImsServiceSessionCache.values()) {
                session.terminateSession(reason);
            }
            for (ImsServiceSession session : mImsServiceSessionWithoutDialogPathCache.values()) {
                session.terminateSession(reason);
            }
        }
    }

    /**
     * Send an error response to an invitation before to create a service session
     * 
     * @param invite Invite request
     * @param error Error code
     */
    public void sendErrorResponse(SipRequest invite, int error) {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Send error " + error);
            }
            SipResponse resp = SipMessageFactory.createResponse(invite,
                    IdGenerator.getIdentifier(), error);

            // Send response
            getImsModule().getSipManager().sendSipResponse(resp);
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send error " + error, e);
            }
        }
    }
}
