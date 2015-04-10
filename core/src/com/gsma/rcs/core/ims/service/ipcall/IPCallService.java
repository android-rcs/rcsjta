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

package com.gsma.rcs.core.ims.service.ipcall;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.ipcalldraft.IIPCallPlayer;
import com.gsma.rcs.service.ipcalldraft.IIPCallRenderer;
import com.gsma.rcs.service.ipcalldraft.IPCall.ReasonCode;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.HashMap;
import java.util.Map;

import javax2.sip.message.Response;

/**
 * IP call service offers one-to-on IP voice call and IP video call
 * 
 * @author opob7414
 */
public class IPCallService extends ImsService {
    /**
     * IP voice call features tags
     */
    public final static String[] FEATURE_TAGS_IP_VOICE_CALL = {
            FeatureTags.FEATURE_RCSE + "=\"" + FeatureTags.FEATURE_3GPP_IP_VOICE_CALL + "\"",
            FeatureTags.FEATURE_RCSE_IP_VOICE_CALL
    };

    /**
     * IP video call features tags
     */
    public final static String[] FEATURE_TAGS_IP_VIDEO_CALL = {
            FeatureTags.FEATURE_RCSE + "=\"" + FeatureTags.FEATURE_3GPP_IP_VOICE_CALL + "\"",
            FeatureTags.FEATURE_RCSE_IP_VOICE_CALL, FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL
    };

    private final RcsSettings mRcsSettings;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(IPCallService.class.getSimpleName());

    /**
     * IPCallSessionCache with Session ID (Call ID) as key
     */
    private Map<String, IPCallSession> mIPCallSessionCache = new HashMap<String, IPCallSession>();

    /**
     * Contacts manager
     */
    private final ContactsManager mContactManager;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactsManager
     * @throws CoreException
     */
    public IPCallService(ImsModule parent, RcsSettings rcsSettings, ContactsManager contactsManager)
            throws CoreException {
        super(parent, true);

        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
    }

    private void handleIPCallInvitationRejected(SipRequest invite, ContactId contact,
            ReasonCode reasonCode, long timestamp) {
        byte[] sessionDescriptionProtocol = invite.getSdpContent().getBytes(UTF8);
        AudioContent audioContent = ContentManager
                .createLiveAudioContentFromSdp(sessionDescriptionProtocol);
        VideoContent videoContent = ContentManager
                .createLiveVideoContentFromSdp(sessionDescriptionProtocol);
        getImsModule()
                .getCore()
                .getListener()
                .handleIPCallInvitationRejected(contact, audioContent, videoContent, reasonCode,
                        timestamp);
    }

    /**
     * Start the IMS service
     */
    public void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);
    }

    /**
     * Stop the IMS service
     */
    public void stop() {
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

    public void addSession(IPCallSession session) {
        String callId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add IPCallSession with call ID '").append(callId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mIPCallSessionCache.put(callId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final IPCallSession session) {
        final String callId = session.getSessionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove IPCallSession with call ID '").append(callId)
                    .append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads accessing
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mIPCallSessionCache.remove(callId);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public IPCallSession getIPCallSession(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get IPCallSession with call ID '").append(sessionId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mIPCallSessionCache.get(sessionId);
        }
    }

    protected void assertAvailableIpCallSession(String errorMessage) throws CoreException {
        synchronized (getImsServiceSessionOperationLock()) {
            int maxSessions = mRcsSettings.getMaxIPCallSessions();
            if (maxSessions != 0 && mIPCallSessionCache.size() >= maxSessions) {
                /*
                 * TODO : Exceptions will be handled better in CR037 implementation
                 */
                throw new CoreException(errorMessage);
            }
        }
    }

    private boolean isCurrentSharingUnidirectional() {
        synchronized (getImsServiceSessionOperationLock()) {
            return mIPCallSessionCache.size() >= SharingDirection.UNIDIRECTIONAL;
        }
    }

    /**
     * Initiate an IP call session
     * 
     * @param contact Remote contact identifier
     * @param video Video
     * @param player Player
     * @param renderer Renderer
     * @param timestamp Local timestamp when initiating the IPCall
     * @return IP call session
     * @throws CoreException
     */
    public IPCallSession initiateIPCallSession(ContactId contact, boolean video,
            IIPCallPlayer player, IIPCallRenderer renderer, long timestamp) throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an IP call session");
        }

        // Test number of sessions
        assertAvailableIpCallSession("Max sessions achieved");

        // Create content
        AudioContent audioContent = ContentManager.createGenericLiveAudioContent();
        VideoContent videoContent = null;
        if (video) {
            videoContent = ContentManager.createGenericLiveVideoContent();
        }

        // Create a new session
        OriginatingIPCallSession session = new OriginatingIPCallSession(this, contact,
                audioContent, videoContent, player, renderer, mRcsSettings, timestamp,
                mContactManager);

        return session;
    }

    /**
     * Receive a IP call invitation
     * 
     * @param invite Initial invite
     * @param audio
     * @param video
     */
    public void receiveIPCallInvitation(SipRequest invite, boolean audio, boolean video) {
        boolean logActivated = sLogger.isActivated();
        // Parse contact
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(SipUtils
                .getAssertedIdentity(invite));
        if (number == null) {
            if (logActivated) {
                sLogger.debug("Cannot parse contact: reject the invitation");
            }
            sendErrorResponse(invite, Response.NOT_ACCEPTABLE);
            return;

        }
        ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
        // Test if the contact is blocked
        if (mContactManager.isBlockedForContact(contact)) {
            if (logActivated) {
                sLogger.debug("Contact " + contact
                        + " is blocked: automatically reject the sharing invitation");
            }

            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            return;
        }

        long timestamp = System.currentTimeMillis();

        // Reject if there is already a call in progress
        if (isCurrentSharingUnidirectional()) {
            // Max session
            if (logActivated) {
                sLogger.debug("The max number of IP call sessions is achieved: reject the invitation");
            }
            handleIPCallInvitationRejected(invite, contact, ReasonCode.REJECTED_MAX_SESSIONS,
                    timestamp);
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        // Create a new session
        IPCallSession session = new TerminatingIPCallSession(this, invite, contact, mRcsSettings,
                timestamp, mContactManager);

        getImsModule().getCore().getListener().handleIPCallInvitation(session);

        session.startSession();
    }

    /**
     * Abort all pending sessions
     */
    public void abortAllSessions() {
        if (sLogger.isActivated()) {
            sLogger.debug("Abort all pending sessions");
        }
        abortAllSessions(TerminationReason.TERMINATION_BY_SYSTEM);
    }

    /**
     * Is call connected
     * 
     * @return Boolean
     */
    public boolean isCallConnected() {
        synchronized (getImsServiceSessionOperationLock()) {
            return (mIPCallSessionCache.size() > 0);
        }
    }

    /**
     * Is call connected with a given contact
     * 
     * @param contact Contact Id
     * @return Boolean
     */
    public boolean isCallConnectedWith(ContactId contact) {
        synchronized (getImsServiceSessionOperationLock()) {
            for (IPCallSession session : mIPCallSessionCache.values()) {
                if (contact.equals(session.getRemoteContact())) {
                    return true;
                }
            }
        }
        return false;
    }
}
