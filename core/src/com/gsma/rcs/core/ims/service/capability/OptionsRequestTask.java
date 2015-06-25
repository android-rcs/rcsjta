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

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import javax2.sip.InvalidArgumentException;

/**
 * Options request task
 * 
 * @author Jean-Marc AUFFRET
 */
public class OptionsRequestTask implements Runnable {
    private final ImsModule mImsModule;

    private final ContactId mContact;

    private final String[] mFeatureTags;

    private SipDialogPath mDialogPath;

    private final SessionAuthenticationAgent mAuthenticationAgent;

    private final static Logger sLogger = Logger.getLogger(OptionsRequestTask.class.getName());

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final IOptionsRequestTaskListener mCallback;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param contact Remote contact identifier
     * @param featureTags Feature tags
     * @param rcsSettings accessor to RCS settings
     * @param contactManager accessor to contact manager
     * @param callback Callback to be executed at end of task
     */
    public OptionsRequestTask(ImsModule parent, ContactId contact, String[] featureTags,
            RcsSettings rcsSettings, ContactManager contactManager,
            IOptionsRequestTaskListener callback) {
        mImsModule = parent;
        mContact = contact;
        mFeatureTags = featureTags;
        mAuthenticationAgent = new SessionAuthenticationAgent(mImsModule);
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCallback = callback;
    }

    @Override
    public void run() {
        try {
            sendOptions();
        } catch (SipException e) {
            sLogger.error("Options request failed for contact " + mContact + " !", e);
            handleError(new CapabilityError(CapabilityError.OPTIONS_FAILED, e));
        } catch (ContactManagerException e) {
            sLogger.error("Options request failed for contact " + mContact + " !", e);
        } catch (RuntimeException e) {
            sLogger.error("Options request failed for contact " + mContact + " !", e);
        } finally {
            if (mCallback != null) {
                try {
                    mCallback.endOfOptionsRequestTask(mContact);
                } catch (RuntimeException e) {
                    sLogger.error("Failed to notify end of options request for contact " + mContact
                            + " !", e);
                }
            }
        }
    }

    /**
     * Send an OPTIONS request
     * 
     * @throws SipPayloadException
     * @throws ContactManagerException
     * @throws SipNetworkException
     */
    private void sendOptions() throws SipPayloadException, SipNetworkException,
            ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Send an options request to ".concat(mContact.toString()));
        }
        if (!mImsModule.getCurrentNetworkInterface().isRegistered()) {
            if (sLogger.isActivated()) {
                sLogger.debug("IMS not registered, do nothing");
            }
            return;
        }
        String contactUri = PhoneUtils.formatContactIdToUri(mContact);
        mDialogPath = new SipDialogPath(mImsModule.getSipManager().getSipStack(), mImsModule
                .getSipManager().getSipStack().generateCallId(), 1, contactUri,
                ImsModule.IMS_USER_PROFILE.getPublicUri(), contactUri, mImsModule.getSipManager()
                        .getSipStack().getServiceRoutePath(), mRcsSettings);

        if (sLogger.isActivated()) {
            sLogger.debug("Send first OPTIONS");
        }
        SipRequest options = SipMessageFactory.createOptions(mDialogPath, mFeatureTags);

        sendAndWaitOptions(options);
    }

    /**
     * Sends OPTIONS message and waits for response
     * 
     * @param options SIP OPTIONS
     * @throws SipPayloadException
     * @throws SipNetworkException
     * @throws ContactManagerException
     */
    private void sendAndWaitOptions(SipRequest options) throws SipPayloadException,
            SipNetworkException, ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Send OPTIONS");
        }

        // Send OPTIONS request
        SipTransactionContext ctx = mImsModule.getSipManager().sendSipMessageAndWait(options);

        // Analyze the received response
        if (ctx.isSipResponse()) {
            // A response has been received
            if (ctx.getStatusCode() == 200) {
                // 200 OK
                handle200OK(ctx);
            } else if (ctx.getStatusCode() == 407) {
                // 407 Proxy Authentication Required
                handle407Authentication(ctx);
            } else if ((ctx.getStatusCode() == 480) || (ctx.getStatusCode() == 408)) {
                // User not registered
                handleUserNotRegistered(ctx);
            } else if (ctx.getStatusCode() == 404) {
                // User not found
                handleUserNotFound(ctx);
            } else {
                // Other error response
                handleError(new CapabilityError(CapabilityError.OPTIONS_FAILED, ctx.getStatusCode()
                        + " " + ctx.getReasonPhrase()));
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("No response received for OPTIONS");
            }

            // No response received: timeout
            handleError(new CapabilityError(CapabilityError.OPTIONS_FAILED, ctx.getStatusCode()
                    + " " + ctx.getReasonPhrase()));
        }
    }

    /**
     * Handle user not registered
     * 
     * @param ctx SIP transaction context
     */
    private void handleUserNotRegistered(SipTransactionContext ctx) {
        /* 408 or 480 response received */
        if (sLogger.isActivated()) {
            sLogger.info("User " + mContact + " is not registered");
        }
        ContactInfo info = mContactManager.getContactInfo(mContact);
        if (RcsStatus.NO_INFO.equals(info.getRcsStatus())) {
            /*
             * If there is no info on this contact: update the database with default capabilities
             */
            mContactManager.setContactCapabilities(mContact, Capabilities.sDefaultCapabilities,
                    RcsStatus.NO_INFO, RegistrationState.OFFLINE);
        } else {
            /*
             * There are info on this contact: update the database with its previous info and set
             * the registration state to offline.
             */
            mContactManager.setContactCapabilities(mContact, info.getCapabilities(),
                    info.getRcsStatus(), RegistrationState.OFFLINE);

            mImsModule.getCore().getListener()
                    .handleCapabilitiesNotification(mContact, info.getCapabilities());
        }
    }

    /**
     * Handle user not found
     * 
     * @param ctx SIP transaction context
     */
    private void handleUserNotFound(SipTransactionContext ctx) {
        /* 404 response received */
        if (sLogger.isActivated()) {
            sLogger.info("User " + mContact + " is not found");
        }
        /* The contact is not RCS */
        mContactManager.setContactCapabilities(mContact, Capabilities.sDefaultCapabilities,
                RcsStatus.NOT_RCS, RegistrationState.UNKNOWN);
        mImsModule.getCore().getListener()
                .handleCapabilitiesNotification(mContact, Capabilities.sDefaultCapabilities);
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OK(SipTransactionContext ctx) {
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received for " + mContact);
        }

        /* Read capabilities */
        SipResponse resp = ctx.getSipResponse();
        Capabilities capabilities = CapabilityUtils.extractCapabilities(resp);

        /* Update capability time of last response */
        mContactManager.updateCapabilitiesTimeLastResponse(mContact);

        /* Update the database capabilities */
        if (capabilities.isImSessionSupported()) {
            /* The contact is RCS capable */

            /*
             * Note RCS5.1 chapter 2.7.1.1: "a user shall be considered as unregistered when ... a
             * response that included the automata tag defined in [RFC3840]".
             */
            if (capabilities.isSipAutomata()) {
                mContactManager.setContactCapabilities(mContact, capabilities,
                        RcsStatus.RCS_CAPABLE, RegistrationState.OFFLINE);
            } else {
                mContactManager.setContactCapabilities(mContact, capabilities,
                        RcsStatus.RCS_CAPABLE, RegistrationState.ONLINE);
            }
        } else {
            /* The contact is not RCS */
            mContactManager.setContactCapabilities(mContact, capabilities, RcsStatus.NOT_RCS,
                    RegistrationState.UNKNOWN);
        }
        mImsModule.getCore().getListener().handleCapabilitiesNotification(mContact, capabilities);
    }

    /**
     * Handle 407 response
     * 
     * @param ctx SIP transaction context
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle407Authentication(SipTransactionContext ctx) throws SipPayloadException,
            SipNetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("407 response received");
            }

            SipResponse resp = ctx.getSipResponse();

            mAuthenticationAgent.readProxyAuthenticateHeader(resp);

            mDialogPath.incrementCseq();

            /* Create a second OPTIONS request with the right token */
            if (sLogger.isActivated()) {
                sLogger.info("Send second OPTIONS");
            }
            SipRequest options = SipMessageFactory.createOptions(mDialogPath, mFeatureTags);

            mAuthenticationAgent.setProxyAuthorizationHeader(options);

            sendAndWaitOptions(options);
        } catch (InvalidArgumentException e) {
            throw new SipPayloadException("Unable to fetch Authorization header!", e);
        } catch (ContactManagerException e) {
            throw new SipPayloadException("Unable to fetch Authorization header!", e);
        }
    }

    /**
     * Handle error response
     * 
     * @param error Error
     */
    private void handleError(CapabilityError error) {
        if (sLogger.isActivated()) {
            sLogger.info("Options has failed for contact " + mContact + ": " + error.getErrorCode()
                    + ", reason=" + error.getMessage());
        }
        ContactInfo info = mContactManager.getContactInfo(mContact);
        if (RcsStatus.NO_INFO.equals(info.getRcsStatus())) {
            /*
             * If there is no info on this contact: update the database with default capabilities
             */
            mContactManager.setContactCapabilities(mContact, Capabilities.sDefaultCapabilities,
                    RcsStatus.NO_INFO, RegistrationState.OFFLINE);
        } else {
            /*
             * There are info on this contact: update the database capabilities time of last request
             */
            mContactManager.updateCapabilitiesTimeLastRequest(mContact);
        }
    }

    /**
     * Interface listener for OptionRequestTask
     */
    public interface IOptionsRequestTaskListener {
        /**
         * Callback to notify end of options request task
         * 
         * @param contact ID
         */
        public void endOfOptionsRequestTask(ContactId contact);
    }
}
