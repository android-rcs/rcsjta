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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import com.gsma.rcs.addressbook.AddressBookEventListener;
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Capability discovery service
 * 
 * @author jexa7410
 */
public class CapabilityService extends ImsService implements AddressBookEventListener {

    private final RcsSettings mRcsSettings;

    private final ContactsManager mContactsManager;

    /**
     * Options manager
     */
    private OptionsManager optionsManager;

    /**
     * Anonymous fetch manager
     */
    private AnonymousFetchManager anonymousFetchManager;

    /**
     * Polling manager
     */
    private PollingManager pollingManager;

    /**
     * Flag: set during the address book changed procedure, if we are notified of a change
     */
    private boolean isRecheckNeeded = false;

    /**
     * Flag indicating if a check procedure is in progress
     */
    private boolean isCheckInProgress = false;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(CapabilityService.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactsManager
     * @throws CoreException
     */
    public CapabilityService(ImsModule parent, RcsSettings rcsSettings,
            ContactsManager contactsManager) throws CoreException {
        super(parent, true);
        mRcsSettings = rcsSettings;
        mContactsManager = contactsManager;
        // Instantiate the polling manager
        pollingManager = new PollingManager(this);

        // Instantiate the options manager
        optionsManager = new OptionsManager(parent);

        // Instantiate the anonymous fetch manager
        anonymousFetchManager = new AnonymousFetchManager(parent);
    }

    /**
     * Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);

        // Start options manager
        optionsManager.start();

        // Listen to address book changes
        getImsModule().getCore().getAddressBookManager().addAddressBookListener(this);

        // Start polling
        pollingManager.start();

        // Force a first capability check
        Thread t = new Thread() {
            public void run() {
                handleAddressBookHasChanged();
            }
        };
        t.start();
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

        // Stop options manager
        optionsManager.stop();

        // Stop polling
        pollingManager.stop();

        // Stop listening to address book changes
        getImsModule().getCore().getAddressBookManager().removeAddressBookListener(this);
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
        return optionsManager;
    }

    /**
     * Get the options manager
     * 
     * @return Options manager
     */
    public AnonymousFetchManager getAnonymousFetchManager() {
        return anonymousFetchManager;
    }

    /**
     * Request contact capabilities
     * 
     * @param contact Contact identifier
     * @return Capabilities
     */
    public synchronized Capabilities requestContactCapabilities(ContactId contact) {
        if (logger.isActivated()) {
            logger.debug("Request capabilities to " + contact);
        }

        // Do not request capabilities for oneself
        if (contact == null || contact.equals(ImsModule.IMS_USER_PROFILE.getUsername())) {
            return null;
        }

        // Read capabilities from the database
        Capabilities capabilities = mContactsManager.getContactCapabilities(contact);
        if (capabilities == null) {
            if (logger.isActivated()) {
                logger.debug("No capability exist for " + contact);
            }

            // New contact: request capabilities from the network
            optionsManager.requestCapabilities(contact);
        } else {
            if (logger.isActivated()) {
                logger.debug("Capabilities exist for " + contact);
            }
            if (isCapabilityRefreshAuthorized(capabilities.getTimestampOfLastRequest())) {
                if (logger.isActivated()) {
                    logger.debug("Request capabilities for " + contact);
                }

                // Capabilities are too old: request capabilities from the network
                optionsManager.requestCapabilities(contact);
            }
        }
        return capabilities;
    }

    /**
     * Check if refresh of capability is authorized
     * 
     * @param timestampOfLastRequest timestamp of last capability request in milliseconds
     * @return true if capability request is authorized
     */
    private boolean isCapabilityRefreshAuthorized(long timestampOfLastRequest) {
        // Do not request capability refresh too often
        long now = System.currentTimeMillis();
        // Is current time before last capability request ? (may occur if current time has been
        // updated)
        if (now < timestampOfLastRequest) {
            return true;
        }
        // Is current time after capability refresh timeout ?
        return (now > (timestampOfLastRequest + mRcsSettings.getCapabilityRefreshTimeout() * 1000));
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contacts Set of contact identifiers
     */
    public void requestContactCapabilities(Set<ContactId> contacts) {
        if ((contacts != null) && (contacts.size() > 0)) {
            if (logger.isActivated()) {
                logger.debug("Request capabilities for " + contacts.size() + " contacts");
            }
            optionsManager.requestCapabilities(contacts);
        }
    }

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     */
    public void receiveCapabilityRequest(SipRequest options) {
        optionsManager.receiveCapabilityRequest(options);
    }

    /**
     * Receive a notification (anonymous fecth procedure)
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
        anonymousFetchManager.receiveNotification(notify);
    }

    /**
     * Address book content has changed
     */
    public void handleAddressBookHasChanged() {
        // Update capabilities for the contacts that have never been queried
        if (isCheckInProgress) {
            isRecheckNeeded = true;
            return;
        }

        // We are beginning the check procedure
        isCheckInProgress = true;

        // Reset recheck flag
        isRecheckNeeded = false;

        // Check all phone numbers and query only the new ones
        String[] projection = {
                Phone._ID, Phone.NUMBER, Phone.RAW_CONTACT_ID
        };
        Cursor phonesCursor = AndroidFactory.getApplicationContext().getContentResolver()
                .query(Phone.CONTENT_URI, projection, null, null, null);

        // List of unique number that will have to be queried for capabilities
        Set<ContactId> toBeTreatedNumbers = new HashSet<ContactId>();

        // List of unique number that have already been queried
        List<ContactId> alreadyInEabOrInvalidNumbers = new ArrayList<ContactId>();

        // We add "My number" to the numbers that are already RCS, so we don't query it if it is
        // present in the address book
        alreadyInEabOrInvalidNumbers.add(ImsModule.IMS_USER_PROFILE.getUsername());

        while (phonesCursor.moveToNext()) {
            // Keep a trace of already treated row. Key is (phone number in international format)
            ContactId phoneNumber;
            try {
                phoneNumber = ContactUtils.createContactId(phonesCursor.getString(1));
            } catch (RcsContactFormatException e) {
                if (logger.isActivated()) {
                    logger.warn("Cannot parse phone number " + phonesCursor.getString(1));
                }
                continue;
            }
            if (!alreadyInEabOrInvalidNumbers.contains(phoneNumber)) {
                // If this number is not considered RCS valid or has already an entry with RCS, skip
                // it
                if (!mContactsManager
                        .isContactIdAssociatedWithContactInRichAddressBook(phoneNumber)
                        && (!mContactsManager.isOnlySimAssociated(phoneNumber) || (Build.VERSION.SDK_INT > 10))) {
                    // This entry is valid and not already has a RCS raw contact, it can be treated
                    // We exclude the number that comes from SIM only contacts, as those cannot be
                    // aggregated to RCS raw contacts only if OS version if gingerbread or fewer
                    toBeTreatedNumbers.add(phoneNumber);
                } else {
                    // This entry is either not valid or already RCS, this number is already done
                    alreadyInEabOrInvalidNumbers.add(phoneNumber);

                    // Remove the number from the treated list, if it is in it
                    toBeTreatedNumbers.remove(phoneNumber);
                }
            } else {
                // Remove the number from the treated list, it was already queried for another raw
                // contact on the same number
                toBeTreatedNumbers.remove(phoneNumber);

                // If it is a RCS contact and the raw contact is not associated with a RCS raw
                // contact,
                // then we have to create a new association for it
                long rawContactId = phonesCursor.getLong(2);
                if ((!mContactsManager.isSimAccount(rawContactId) || (Build.VERSION.SDK_INT > 10))
                        && (mContactsManager.getAssociatedRcsRawContact(rawContactId, phoneNumber) == -1)) {
                    ContactInfo currentInfo = mContactsManager.getContactInfo(phoneNumber);
                    if (currentInfo != null && currentInfo.isRcsContact()) {
                        mContactsManager.createRcsContact(currentInfo, rawContactId);
                    }
                }
            }
        }
        phonesCursor.close();

        // Get the capabilities for the numbers that haven't got a RCS associated contact
        requestContactCapabilities(toBeTreatedNumbers);

        // End of the check procedure
        isCheckInProgress = false;

        // Check if we have to make another check
        if (isRecheckNeeded) {
            handleAddressBookHasChanged();
        }
    }

    /**
     * Reset the content sharing capabilities for a given contact identifier
     * 
     * @param contact Contact identifier
     */
    public void resetContactCapabilitiesForContentSharing(ContactId contact) {
        Capabilities capabilities = mContactsManager.getContactCapabilities(contact);
        if (capabilities != null) {
            // Force a reset of content sharing capabilities
            capabilities.setImageSharingSupport(false);
            capabilities.setVideoSharingSupport(false);

            // Update the database capabilities
            mContactsManager.setContactCapabilities(contact, capabilities);

            // Notify listener
            getImsModule().getCore().getListener()
                    .handleCapabilitiesNotification(contact, capabilities);
        }
    }
}
