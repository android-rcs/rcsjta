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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.presence.PresenceError;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Vector;

import javax2.sip.header.AcceptHeader;
import javax2.sip.header.EventHeader;
import javax2.sip.message.Response;

/**
 * Anonymous fetch procedure which permits to request the capabilities for a given contact thanks to
 * a one shot subscribe.
 * 
 * @author Jean-Marc AUFFRET
 */
public class AnonymousFetchRequestTask {
    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Remote contact
     */
    private ContactId mContact;

    /**
     * Dialog path
     */
    private SipDialogPath mDialogPath;

    /**
     * Authentication agent
     */
    private SessionAuthenticationAgent mAuthenticationAgent;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(AnonymousFetchRequestTask.class
            .getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param contact Remote contact identifier
     * @param rcsSettings
     * @param contactManager
     */
    public AnonymousFetchRequestTask(ImsModule parent, ContactId contact, RcsSettings rcsSettings,
            ContactManager contactManager) {
        mImsModule = parent;
        mContact = contact;
        mAuthenticationAgent = new SessionAuthenticationAgent(mImsModule);
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
    }

    /**
     * Start task
     */
    public void start() {
        sendSubscribe();
    }

    /**
     * Send a SUBSCRIBE request
     */
    private void sendSubscribe() {
        if (sLogger.isActivated()) {
            sLogger.info("Send SUBSCRIBE request to " + mContact);
        }

        try {
            /* Create a dialog path */
            String contactUri = PhoneUtils.formatContactIdToUri(mContact);

            /* Set Call-Id */
            String callId = mImsModule.getSipManager().getSipStack().generateCallId();

            /* Set target */
            String target = contactUri;

            /* Set local party */
            String localParty = "sip:anonymous@" + ImsModule.IMS_USER_PROFILE.getHomeDomain();

            /* Set remote party */
            String remoteParty = contactUri;

            /* Set the route path */
            Vector<String> route = mImsModule.getSipManager().getSipStack().getServiceRoutePath();

            /* Create a dialog path */
            mDialogPath = new SipDialogPath(mImsModule.getSipManager().getSipStack(), callId, 1,
                    target, localParty, remoteParty, route, mRcsSettings);

            SipRequest subscribe = createSubscribe();

            sendSubscribe(subscribe);
        } catch (SipException e) {
            sLogger.error("Failed to send SUBSCRIBE request to".concat(mContact.toString()), e);
            handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED, e));
        } catch (CoreException e) {
            /* TODO: Remove CoreException in the future because it is too generic. */
            sLogger.error("Failed to send SUBSCRIBE request to".concat(mContact.toString()), e);
            handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED, e));
        }
    }

    /**
     * Create a SUBSCRIBE request
     * 
     * @return SIP request
     * @throws SipException
     * @throws CoreException
     */
    private SipRequest createSubscribe() throws SipException, CoreException {
        SipRequest subscribe = SipMessageFactory.createSubscribe(mDialogPath, 0);

        /* Set the Privacy header */
        subscribe.addHeader(SipUtils.HEADER_PRIVACY, "id");

        /* Set the Event header */
        subscribe.addHeader(EventHeader.NAME, "presence");

        /* Set the Accept header */
        subscribe.addHeader(AcceptHeader.NAME, "application/pidf+xml");

        return subscribe;
    }

    /**
     * Send SUBSCRIBE message
     * 
     * @param subscribe SIP SUBSCRIBE
     * @throws SipException
     * @throws CoreException
     */
    private void sendSubscribe(SipRequest subscribe) throws SipException, CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Send SUBSCRIBE, expire=" + subscribe.getExpires());
        }

        /* Send SUBSCRIBE request */
        SipTransactionContext ctx = mImsModule.getSipManager().sendSipMessageAndWait(subscribe);

        /* Analyze the received response */
        if (ctx.isSipResponse()) {
            /* A response has been received */
            if ((ctx.getStatusCode() >= Response.OK)
                    && (ctx.getStatusCode() < Response.MULTIPLE_CHOICES)) {
                handle200OK(ctx);
            } else if (Response.PROXY_AUTHENTICATION_REQUIRED == ctx.getStatusCode()) {
                /* 407 Proxy Authentication Required */
                handle407Authentication(ctx);
            } else if (Response.NOT_FOUND == ctx.getStatusCode()) {
                /* User not found */
                handleUserNotFound(ctx);
            } else {
                /* Other error response */
                handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED, ctx.getStatusCode()
                        + " " + ctx.getReasonPhrase()));
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("No response received for SUBSCRIBE");
            }

            /* No response received: timeout */
            handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED));
        }
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OK(SipTransactionContext ctx) {
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }
    }

    /**
     * Handle 407 response
     * 
     * @param ctx SIP transaction context
     * @throws SipException
     * @throws CoreException
     */
    private void handle407Authentication(SipTransactionContext ctx) throws SipException,
            CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("407 response received");
        }

        SipResponse resp = ctx.getSipResponse();

        /* Set the Proxy-Authorization header */
        mAuthenticationAgent.readProxyAuthenticateHeader(resp);

        /* Increment the Cseq number of the dialog path */
        mDialogPath.incrementCseq();

        /* Create a second SUBSCRIBE request with the right token */
        if (sLogger.isActivated()) {
            sLogger.info("Send second SUBSCRIBE");
        }
        SipRequest subscribe = createSubscribe();

        /* Set the Authorization header */
        mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);

        sendSubscribe(subscribe);
    }

    /**
     * Handle error response
     * 
     * @param error Error
     */
    private void handleError(PresenceError error) {
        /* On error don't modify the existing capabilities */
        if (sLogger.isActivated()) {
            sLogger.info("Subscribe has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }

        /* We update the database capabilities time of last request */
        mContactManager.updateCapabilitiesTimeLastRequest(mContact);
    }

    /**
     * Handle user not found
     * 
     * @param ctx SIP transaction context
     */
    private void handleUserNotFound(SipTransactionContext ctx) {
        if (sLogger.isActivated()) {
            sLogger.info("User not found (" + ctx.getStatusCode() + " error)");
        }

        /* We update the database with empty capabilities */
        Capabilities capabilities = new Capabilities();
        mContactManager.setContactCapabilities(mContact, capabilities, RcsStatus.NOT_RCS,
                RegistrationState.UNKNOWN);
    }
}
