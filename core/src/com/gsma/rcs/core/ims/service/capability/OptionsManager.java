/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.capability.CapabilityService.IOptionsManagerListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capability discovery manager using options procedure
 * 
 * @author jexa7410
 */
public class OptionsManager implements DiscoveryManager {
    /**
     * Max number of threads for background processing
     */
    private final static int MAX_PROCESSING_THREADS = 15;

    private ImsModule mImsModule;

    /**
     * Thread pool to request capabilities in background
     */
    private ExecutorService mThreadPool;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final static Logger sLogger = Logger.getLogger(OptionsManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RCS settings accessor
     * @param contactManager Contact manager accessor
     */
    public OptionsManager(ImsModule parent, RcsSettings rcsSettings, ContactManager contactManager) {
        mImsModule = parent;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
    }

    /**
     * Start the manager
     */
    public void start() {
        mThreadPool = Executors.newFixedThreadPool(MAX_PROCESSING_THREADS);
    }

    /**
     * Stop the manager
     */
    public void stop() {
        try {
            mThreadPool.shutdown();
        } catch (SecurityException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Could not stop all threads");
            }
        }
    }

    /**
     * Interface listener for OptionRequestTask
     */
    public interface IOptionsRequestTaskListener {
        /**
         * Callback notify end of task
         * 
         * @param contact ID
         */
        public void endOfTask(ContactId contact);
    }

    /**
     * Request capabilities in background
     * 
     * @param contact Contact ID
     * @param listener callback to execute when response is received
     */
    private void requestCapabilitiesInBackground(ContactId contact,
            IOptionsRequestTaskListener listener) {
        if (mThreadPool.isShutdown()) {
            if (sLogger.isActivated()) {
                sLogger.warn("Request capabilities in background for " + contact
                        + " rejected: manager is stopped!");
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities in background for ".concat(contact.toString()));
        }

        boolean richcall = mImsModule.getRichcallService().isCallConnectedWith(contact);
        OptionsRequestTask task = new OptionsRequestTask(mImsModule, contact,
                CapabilityUtils.getSupportedFeatureTags(richcall, mRcsSettings), mRcsSettings,
                mContactManager, listener);
        mThreadPool.submit(task);
    }

    /**
     * Check if refresh of capability is authorized
     * 
     * @param timestampOfLastRequest timestamp of last capability request in milliseconds
     * @return true if capability request is authorized
     */
    private boolean isCapabilityRefreshAuthorized(long timestampOfLastRequest) {
        long currentTime = System.currentTimeMillis();
        /*
         * Is current time before last capability request ? (may occur if current time has been
         * updated)
         */
        if (currentTime < timestampOfLastRequest) {
            return true;
        }
        return (currentTime > (timestampOfLastRequest + mRcsSettings.getCapabilityRefreshTimeout()));
    }

    /**
     * Request contact capabilities
     * 
     * @param contact Remote contact identifier
     */
    public void requestCapabilities(ContactId contact) {
        boolean logActivated = sLogger.isActivated();
        if (contact == null || contact.equals(ImsModule.IMS_USER_PROFILE.getUsername())) {
            return;
        }
        Capabilities capabilities = mContactManager.getContactCapabilities(contact);
        if (capabilities == null) {
            if (logActivated) {
                sLogger.debug("No capability exist for ".concat(contact.toString()));
            }
            requestCapabilitiesInBackground(contact, null);
            mContactManager.updateCapabilitiesTimeLastRequest(contact);
        } else {
            if (logActivated) {
                sLogger.debug("Capabilities exist for ".concat(contact.toString()));
            }
            if (isCapabilityRefreshAuthorized(capabilities.getTimestampOfLastRequest())) {
                if (logActivated) {
                    sLogger.debug("Request capabilities for ".concat(contact.toString()));
                }
                requestCapabilitiesInBackground(contact, null);
                mContactManager.updateCapabilitiesTimeLastRequest(contact);
            }
        }
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contacts Contact set
     */
    public void requestCapabilities(Set<ContactId> contacts) {
        for (ContactId contact : contacts) {
            requestCapabilities(contact);
        }
    }

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     * @throws SipException thrown if capability request fails
     */
    public void receiveCapabilityRequest(SipRequest options) throws SipException {
        String sipId = SipUtils.getAssertedIdentity(options);
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(sipId);
        if (number == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Invalid contact from capability request '" + sipId + "'");
            }
            return;
        }
        ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
        if (sLogger.isActivated()) {
            sLogger.debug("OPTIONS request received from ".concat(contact.toString()));
        }

        // Create 200 OK response
        String ipAddress = mImsModule.getCurrentNetworkInterface().getNetworkAccess()
                .getIpAddress();
        boolean richcall = mImsModule.getRichcallService().isCallConnectedWith(contact);
        SipResponse resp = SipMessageFactory.create200OkOptionsResponse(options, mImsModule
                .getSipManager().getSipStack().getContact(),
                CapabilityUtils.getSupportedFeatureTags(richcall, mRcsSettings),
                CapabilityUtils.buildSdp(ipAddress, richcall, mRcsSettings));

        // Send 200 OK response
        mImsModule.getSipManager().sendSipResponse(resp);
        // Read features tag in the request
        Capabilities capabilities = CapabilityUtils.extractCapabilities(options);

        // Update capabilities in database
        if (capabilities.isImSessionSupported()) {
            // RCS-e contact
            mContactManager.setContactCapabilities(contact, capabilities, RcsStatus.RCS_CAPABLE,
                    RegistrationState.ONLINE);
        } else {
            // Not a RCS-e contact
            mContactManager.setContactCapabilities(contact, capabilities, RcsStatus.NOT_RCS,
                    RegistrationState.UNKNOWN);
        }

        // Notify listener
        mImsModule.getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
    }

    /**
     * Requests capabilities for a set of contacts
     * 
     * @param contacts Set of contacts to query.
     * @param callback Callback to execute once all contacts have been queried or null if caller
     *            does need to be notified
     */
    public void requestCapabilities(Set<ContactId> contacts, final IOptionsManagerListener callback) {
        IOptionsRequestTaskListener listener = null;
        final Set<ContactId> contactsToQuery = new HashSet<ContactId>(contacts);
        if (callback != null) {
            listener = new IOptionsRequestTaskListener() {

                @Override
                public void endOfTask(ContactId contact) {
                    synchronized (contactsToQuery) {
                        contactsToQuery.remove(contact);
                        if (contactsToQuery.isEmpty()) {
                            callback.endOfSynchronization();
                        }
                    }
                }
            };
        }
        for (ContactId contact : contacts) {
            requestCapabilitiesInBackground(contact, listener);
        }
    }

}
