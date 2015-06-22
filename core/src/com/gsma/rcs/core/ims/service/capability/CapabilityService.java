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

import com.gsma.rcs.addressbook.AddressBookEventListener;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Capability discovery service
 * 
 * @author jexa7410
 */
public class CapabilityService extends ImsService implements AddressBookEventListener {

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private OptionsManager mOptionsManager;

    private AnonymousFetchManager mAnonymousFetchManager;

    private PollingManager mPollingManager;

    /**
     * Flag: set during the address book changed procedure, if we are notified of a change
     */
    private boolean mRecheckNeeded = false;

    /**
     * Flag indicating if a check procedure is in progress
     */
    private boolean mCheckInProgress = false;

    private final static Logger sLogger = Logger.getLogger(CapabilityService.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactManager
     */
    public CapabilityService(ImsModule parent, RcsSettings rcsSettings,
            ContactManager contactsManager) {
        super(parent, true);
        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
        mPollingManager = new PollingManager(this, mRcsSettings, mContactManager);
        mOptionsManager = new OptionsManager(parent, mRcsSettings, mContactManager);
        mAnonymousFetchManager = new AnonymousFetchManager(parent, mRcsSettings, mContactManager);
    }

    /**
     * Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            /* Already started */
            return;
        }
        setServiceStarted(true);
        mOptionsManager.start();
        /* Listen to address book changes */
        getImsModule().getCore().getAddressBookManager().addAddressBookListener(this);
        mPollingManager.start();

        /* Force a first capability check */
        new Thread() {
            public void run() {
                try {
                    handleAddressBookHasChanged();
                } catch (ContactManagerException e) {
                    // TODO CR037 exception handling
                    sLogger.error("Failed to process change in address book!", e);
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    // TODO CR037 exception handling
                    sLogger.error("Failed to process change in address book!", e);
                }
            }
        }.start();
    }

    /**
     * Stop the IMS service
     */
    public synchronized void stop() {
        if (!isServiceStarted()) {
            /* Already stopped */
            return;
        }
        setServiceStarted(false);
        mPollingManager.stop();
        /* Stop listening to address book changes */
        getImsModule().getCore().getAddressBookManager().removeAddressBookListener(this);
        mOptionsManager.stop();
    }

    /**
     * Check the IMS service
     */
    public void check() {
    }

    /**
     * Get the options manager
     * 
     * @return Options manager
     */
    public OptionsManager getOptionsManager() {
        return mOptionsManager;
    }

    /**
     * Get the options manager
     * 
     * @return Options manager
     */
    public AnonymousFetchManager getAnonymousFetchManager() {
        return mAnonymousFetchManager;
    }

    /**
     * Request contact capabilities
     * 
     * @param contact Contact identifier
     */
    public synchronized void requestContactCapabilities(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities for ".concat(contact.toString()));
        }
        mOptionsManager.requestCapabilities(contact);
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contacts Set of contact identifiers
     */
    public void requestContactCapabilities(Set<ContactId> contacts) {
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities for ".concat(Arrays.toString(contacts.toArray())));
        }
        mOptionsManager.requestCapabilities(contacts);
    }

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     * @throws SipException thrown if sending the capability response fails
     */
    public void receiveCapabilityRequest(SipRequest options) throws SipException {
        mOptionsManager.receiveCapabilityRequest(options);
    }

    /**
     * Receive a notification (anonymous fetch procedure)
     * 
     * @param notify Received notify
     * @throws IOException thrown if notification parsing fails
     */
    public void receiveNotification(SipRequest notify) throws IOException {
        mAnonymousFetchManager.receiveNotification(notify);
    }

    /**
     * Gets contacts not associated with RCS raw contact (i.e. existing in native address book but
     * without entry in RCS aggregation table)
     * 
     * @param nativeContacts map of contact IDs from the native address book
     * @param rcsContacts set of contact from the RCS contact provider
     * @return
     */
    private Set<ContactId> getContactsNotAssociatedWithRcsRawContact(
            Map<ContactId, Set<Long>> nativeContacts, Set<ContactId> rcsContacts) {
        Set<ContactId> result = new HashSet<ContactId>();
        for (Entry<ContactId, Set<Long>> nativeContactEntry : nativeContacts.entrySet()) {
            ContactId nativeContact = nativeContactEntry.getKey();
            if (rcsContacts.contains(nativeContact)) {
                Set<Long> nativeRawContactIds = nativeContactEntry.getValue();
                for (Long nativeRawContactId : nativeRawContactIds) {
                    if (!mContactManager.isAssociatedRcsRawContact(nativeRawContactId,
                            nativeContact)) {
                        /*
                         * Contact is known from RCS contact but without association between RCS raw
                         * contact and native row contact.
                         */
                        result.add(nativeContact);
                        break;
                    }
                }
            } else {
                /* Contact is not known from RCS contact */
                result.add(nativeContact);
            }
        }
        return result;
    }

    /**
     * Address book content has changed.<br>
     * This method requests update of capabilities for non RCS contacts (which capabilities are
     * unknown).<br>
     * This method set contact information for RCS contacts not yet aggregated.
     * 
     * @throws ContactManagerException thrown if RCS contact aggregation fails
     */
    @Override
    public void handleAddressBookHasChanged() throws ContactManagerException {
        if (mCheckInProgress) {
            mRecheckNeeded = true;
            return;
        }
        /* We are beginning the check procedure */
        mCheckInProgress = true;

        /* Reset re-check flag */
        mRecheckNeeded = false;

        Map<ContactId, Set<Long>> nativeContacts = mContactManager.getAllRawIdsInPhoneAddressBook();

        /*
         * Remove my contact since already created in native address book and no need to query for
         * capabilities.
         */
        nativeContacts.remove(ImsModule.IMS_USER_PROFILE.getUsername());

        Set<ContactId> rcsContacts = mContactManager.getAllContactsFromRcsContactProvider();

        boolean logActivated = sLogger.isActivated();

        /*
         * Gets contacts for which RCS contact aggregation is not done.
         */
        Set<ContactId> contactsWithNoRcsAggregation = getContactsNotAssociatedWithRcsRawContact(
                nativeContacts, rcsContacts);

        Set<ContactId> contactsOnlySimAssociated = mContactManager.getContactsOnlySimAssociated(nativeContacts);
        
        /* Remove contacts which are only SIM associated since they cannot be aggregated */
        contactsWithNoRcsAggregation.removeAll(contactsOnlySimAssociated);
        
        for (ContactId contact : contactsWithNoRcsAggregation) {
            ContactInfo contactInfo = mContactManager.getContactInfo(contact);
            if (!contactInfo.isRcsContact()) {
                /* Do not aggregate non RCS contact */
                continue;
            }
            if (logActivated) {
                sLogger.debug("handleAddressBookHasChanged: aggregate contact ".concat(contact
                        .toString()));
            }
            mContactManager.aggregateContactWithRcsRawContact(contactInfo);
        }

        Set<ContactId> unqueriedContacts = new HashSet<ContactId>(nativeContacts.keySet());
        /* Remove all contacts known from RCS contact provider to keep only unqueried contacts */
        unqueriedContacts.removeAll(rcsContacts);

        if (!unqueriedContacts.isEmpty()) {
            if (logActivated) {
                sLogger.debug("handleAddressBookHasChanged: request capabilities for contacts "
                        .concat(Arrays.toString(unqueriedContacts.toArray())));
            }
            mOptionsManager.requestCapabilities(unqueriedContacts);
        }

        /* End of the check procedure */
        mCheckInProgress = false;

        /* Check if we have to make another check */
        if (mRecheckNeeded) {
            handleAddressBookHasChanged();
        }
    }

    /**
     * Reset the content sharing capabilities for a given contact identifier
     * 
     * @param contact Contact identifier
     */
    public void resetContactCapabilitiesForContentSharing(ContactId contact) {
        Capabilities capabilities = mContactManager.getContactCapabilities(contact);
        if (capabilities == null
                || (!capabilities.isImageSharingSupported() && !capabilities
                        .isVideoSharingSupported())) {
            return;
        }
        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder(capabilities);
        /* Force a reset of content sharing capabilities */
        capaBuilder.setImageSharing(false);
        capaBuilder.setVideoSharing(false);
        capabilities = capaBuilder.build();
        mContactManager.setContactCapabilities(contact, capabilities);

        getImsModule().getCore().getListener()
                .handleCapabilitiesNotification(contact, capabilities);
    }
}
