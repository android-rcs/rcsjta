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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.addressbook.AddressBookEventListener;
import com.gsma.rcs.addressbook.AddressBookManager;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.capability.OptionsManager.IOptionsManagerListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A task to request options to new contacts.
 */
public class SyncContactTask implements Runnable {

    private final ContactManager mContactManager;

    private final AddressBookManager mAddressBookManager;

    private final PollingManager mPollingManager;

    private final OptionsManager mOptionsManager;

    private final AddressBookEventListener mAddressBookEventListener;

    private final ISyncContactTaskListener mSyncContactTaskListener;

    private static final int MAX_CONTACTS_TO_DISPLAY = 10;

    private final static Logger sLogger = Logger.getLogger(SyncContactTask.class.getSimpleName());

    /**
     * A task to synchronize capabilities for new contacts
     * 
     * @param syncContactTaskListener Listener on this task
     * @param addressBookEventListener Listener on address book events
     * @param contactManager Contact manager accessor
     * @param addressBookManager Address book manager instance
     * @param pollingManager Polling manager instance
     * @param optionsManager Options manager instance
     */
    public SyncContactTask(ISyncContactTaskListener syncContactTaskListener,
            AddressBookEventListener addressBookEventListener, ContactManager contactManager,
            AddressBookManager addressBookManager, PollingManager pollingManager,
            OptionsManager optionsManager) {
        super();
        mSyncContactTaskListener = syncContactTaskListener;
        mAddressBookEventListener = addressBookEventListener;
        mContactManager = contactManager;
        mAddressBookManager = addressBookManager;
        mPollingManager = pollingManager;
        mOptionsManager = optionsManager;
    }

    @Override
    public void run() {
        try {
            /* Stop listening to address book changes */
            mAddressBookManager.removeAddressBookListener(mAddressBookEventListener);

            mPollingManager.stop();

            final Set<ContactId> treatedContacts = new HashSet<ContactId>();
            do {
                final Set<ContactId> unqueriedContacts = aggregateNewContactsAndGetUnqueriedOnes();
                unqueriedContacts.removeAll(treatedContacts);
                if (unqueriedContacts.isEmpty()) {
                    /*
                     * All contacts are synchronized.
                     */
                    mSyncContactTaskListener.endOfSyncContactTask();
                    return;
                }

                if (sLogger.isActivated()) {
                    int nbOfContactsToQuery = unqueriedContacts.size();
                    if (nbOfContactsToQuery > MAX_CONTACTS_TO_DISPLAY) {
                        sLogger.debug("Synchronize capabilities for " + nbOfContactsToQuery
                                + " contacts");
                    } else {
                        sLogger.debug("Synchronize capabilities for contacts ".concat(Arrays
                                .toString(unqueriedContacts.toArray())));
                    }
                }
                mOptionsManager.requestCapabilities(unqueriedContacts,
                        new IOptionsManagerListener() {

                            @Override
                            public void endOfCapabilitiesRequest() {
                                synchronized (unqueriedContacts) {
                                    unqueriedContacts.notify();
                                }
                            }
                        });
                synchronized (unqueriedContacts) {
                    try {
                        unqueriedContacts.wait();
                        treatedContacts.addAll(unqueriedContacts);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            } while (true);
        } catch (ContactManagerException e) {
            sLogger.error("Failed to synchronize contacts!", e);
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to synchronize contacts!", e);
        }
    }

    /**
     * Interface listener for Sync contact task
     */
    public interface ISyncContactTaskListener {
        /**
         * Callback to notify end of contact synchronization
         */
        public void endOfSyncContactTask();
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

    private Set<ContactId> aggregateNewContactsAndGetUnqueriedOnes() throws ContactManagerException {
        Map<ContactId, Set<Long>> nativeContacts = mContactManager.getAllRawIdsInPhoneAddressBook();
        /*
         * Remove my contact since already created in native address book and no need to query for
         * capabilities.
         */
        nativeContacts.remove(ImsModule.IMS_USER_PROFILE.getUsername());

        Set<ContactId> rcsContacts = mContactManager.getAllContactsFromRcsContactProvider();

        /*
         * Get contacts for which RCS contact aggregation is not done.
         */
        Set<ContactId> contactsWithNoRcsAggregation = getContactsNotAssociatedWithRcsRawContact(
                nativeContacts, rcsContacts);

        Set<ContactId> contactsOnlySimAssociated = mContactManager
                .getContactsOnlySimAssociated(nativeContacts);

        /* Remove contacts which are only SIM associated since they cannot be aggregated */
        contactsWithNoRcsAggregation.removeAll(contactsOnlySimAssociated);

        for (ContactId contact : contactsWithNoRcsAggregation) {
            ContactInfo contactInfo = mContactManager.getContactInfo(contact);
            if (!contactInfo.isRcsContact()) {
                /* Do not aggregate non RCS contact */
                continue;
            }
            if (sLogger.isActivated()) {
                sLogger.debug("handleAddressBookHasChanged: aggregate contact ".concat(contact
                        .toString()));
            }
            mContactManager.aggregateContactWithRcsRawContact(contactInfo);
        }

        Set<ContactId> unqueriedContacts = nativeContacts.keySet();
        /* Remove all contacts known from RCS contact provider to keep only un-queried contacts */
        unqueriedContacts.removeAll(rcsContacts);
        return unqueriedContacts;
    }
}
