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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.presence.PresenceError;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.text.ParseException;
import java.util.Vector;

import javax2.sip.InvalidArgumentException;
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
     * @param rcsSettings RCS settings accessor
     * @param contactManager Contact manager accessor
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
     * 
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    public void start() throws PayloadException, NetworkException, ContactManagerException {
        sendSubscribe();
    }

    /**
     * Send a SUBSCRIBE request
     * 
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    private void sendSubscribe() throws PayloadException, NetworkException, ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Send SUBSCRIBE request to " + mContact);
        }
        /* Create a dialog path */
        // @FIXME: This should be an URI instead of String
        String contactUri = PhoneUtils.formatContactIdToUri(mContact).toString();

        /* Set Call-Id */
        String callId = mImsModule.getSipManager().getSipStack().generateCallId();

        /* Set target */
        String target = contactUri;

        /* Set local party */
        String localParty = "sip:anonymous@".concat(ImsModule.getImsUserProfile().getHomeDomain());

        /* Set remote party */
        String remoteParty = contactUri;

        /* Set the route path */
        Vector<String> route = mImsModule.getSipManager().getSipStack().getServiceRoutePath();

        /* Create a dialog path */
        mDialogPath = new SipDialogPath(mImsModule.getSipManager().getSipStack(), callId, 1,
                target, localParty, remoteParty, route, mRcsSettings);
        sendSubscribe(createSubscribe());
    }

    /**
     * Create a SUBSCRIBE request
     * 
     * @return SIP request
     * @throws PayloadException
     */
    private SipRequest createSubscribe() throws PayloadException {
        try {
            SipRequest subscribe = SipMessageFactory.createSubscribe(mDialogPath, 0);
            subscribe.addHeader(SipUtils.HEADER_PRIVACY, "id");
            subscribe.addHeader(EventHeader.NAME, "presence");
            subscribe.addHeader(AcceptHeader.NAME, "application/pidf+xml");
            return subscribe;

        } catch (ParseException e) {
            throw new PayloadException("Failed to create subscribe request!", e);
        }
    }

    /**
     * Send SUBSCRIBE message
     * 
     * @param subscribe SIP SUBSCRIBE
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private void sendSubscribe(SipRequest subscribe) throws PayloadException, NetworkException,
            ContactManagerException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Send SUBSCRIBE, expire=")
                        .append(subscribe.getExpires()).append("ms").toString());
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
                    handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED,
                            ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No response received for SUBSCRIBE");
                }

                /* No response received: timeout */
                handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED));
            }
        } catch (FileAccessException e) {
            throw new PayloadException("Failed to send SUBSCRIBE!", e);

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
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private void handle407Authentication(SipTransactionContext ctx) throws PayloadException,
            NetworkException, ContactManagerException {
        try {
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
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);
        }
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
     * @throws ContactManagerException
     * @throws FileAccessException
     */
    private void handleUserNotFound(SipTransactionContext ctx) throws ContactManagerException,
            FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.info("User not found (" + ctx.getStatusCode() + " error)");
        }

        /* We update the database with default capabilities */
        mContactManager.setContactCapabilities(mContact, Capabilities.sDefaultCapabilities,
                RcsStatus.NOT_RCS, RegistrationState.UNKNOWN);
    }
}
