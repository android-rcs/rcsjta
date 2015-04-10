/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.capability;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

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

    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Thread pool to request capabilities in background
     */
    private ExecutorService mThreadPool;

    private final RcsSettings mRcsSettings;

    private final ContactsManager mContactManager;

    /**
     * The logger
     */
    private final static Logger sLogger = Logger.getLogger(OptionsManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings
     * @param contactManager
     */
    public OptionsManager(ImsModule parent, RcsSettings rcsSettings, ContactsManager contactManager) {
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
     * Request contact capabilities
     * 
     * @param contact Remote contact identifier
     * @return Returns true if success
     */
    public boolean requestCapabilities(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities in background for " + contact);
        }

        // Update capability time of last request
        mContactManager.updateCapabilitiesTimeLastRequest(contact);

        // Start request in background
        try {
            boolean richcall = mImsModule.getRichcallService().isCallConnectedWith(contact);
            OptionsRequestTask task = new OptionsRequestTask(mImsModule, contact,
                    CapabilityUtils.getSupportedFeatureTags(richcall, mRcsSettings), mRcsSettings,
                    mContactManager);
            mThreadPool.submit(task);
            return true;
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't submit task", e);
            }
            return false;
        }
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contacts Contact set
     */
    public void requestCapabilities(Set<ContactId> contacts) {
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities for " + contacts.size() + " contacts");
        }

        for (ContactId contact : contacts) {
            if (!requestCapabilities(contact)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Processing has been stopped");
                }
                break;
            }
        }
    }

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     */
    public void receiveCapabilityRequest(SipRequest options) {
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

        try {
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
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send 200 OK for OPTIONS", e);
            }
        }

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
}
