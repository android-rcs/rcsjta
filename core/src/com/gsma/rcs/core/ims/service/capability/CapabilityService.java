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
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Capability discovery service
 * 
 * @author jexa7410
 */
public class CapabilityService extends ImsService implements AddressBookEventListener {

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private OptionsManager optionsManager;

    private AnonymousFetchManager anonymousFetchManager;

    private PollingManager pollingManager;

    /**
     * Flag: set during the address book changed procedure, if we are notified of a change
     */
    private boolean isRecheckNeeded = false;

    /**
     * Flag indicating if a check procedure is in progress
     */
    private boolean isCheckInProgress = false;

    private final String[] PHONE_PROJECTION = {
            Phone.NUMBER, Phone.RAW_CONTACT_ID
    };

    private final static Logger sLogger = Logger.getLogger(CapabilityService.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactManager
     * @throws CoreException
     */
    public CapabilityService(ImsModule parent, RcsSettings rcsSettings,
            ContactManager contactsManager) throws CoreException {
        super(parent, true);
        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
        // Instantiate the polling manager
        pollingManager = new PollingManager(this, mRcsSettings, mContactManager);

        // Instantiate the options manager
        optionsManager = new OptionsManager(parent, mRcsSettings, mContactManager);

        // Instantiate the anonymous fetch manager
        anonymousFetchManager = new AnonymousFetchManager(parent, mRcsSettings, mContactManager);
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
     */
    public synchronized void requestContactCapabilities(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities to " + contact);
        }
        optionsManager.requestCapabilities(contact);
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contacts Set of contact identifiers
     */
    public void requestContactCapabilities(Set<ContactId> contacts) {
        if ((contacts != null) && (contacts.size() > 0)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Request capabilities for " + contacts.size() + " contacts");
            }
            optionsManager.requestCapabilities(contacts);
        }
    }

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     * @throws SipException
     */
    public void receiveCapabilityRequest(SipRequest options) throws SipException {
        optionsManager.receiveCapabilityRequest(options);
    }

    /**
     * Receive a notification (anonymous fecth procedure)
     * 
     * @param notify Received notify
     * @throws IOException
     */
    public void receiveNotification(SipRequest notify) throws IOException {
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
        Cursor phonesCursor = AndroidFactory.getApplicationContext().getContentResolver()
                .query(Phone.CONTENT_URI, PHONE_PROJECTION, null, null, null);
        /* TODO: Handle phonesCursor when null. */

        // List of unique number that will have to be queried for capabilities
        Set<ContactId> toBeTreatedNumbers = new HashSet<ContactId>();

        // List of unique number that have already been queried
        List<ContactId> alreadyInEabOrInvalidNumbers = new ArrayList<ContactId>();

        // We add "My number" to the numbers that are already RCS, so we don't query it if it is
        // present in the address book
        alreadyInEabOrInvalidNumbers.add(ImsModule.IMS_USER_PROFILE.getUsername());

        int columnIndexPhoneNumber = phonesCursor.getColumnIndexOrThrow(Phone.NUMBER);
        int columnIndexRawContactId = phonesCursor.getColumnIndexOrThrow(Phone.RAW_CONTACT_ID);

        while (phonesCursor.moveToNext()) {
            // Keep a trace of already treated row. Key is (phone number in international format)
            String phoneNumber = phonesCursor.getString(columnIndexPhoneNumber);
            PhoneNumber validatedNumber = ContactUtil.getValidPhoneNumberFromAndroid(phoneNumber);
            if (validatedNumber == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn(new StringBuilder("Cannot parse phone number '")
                            .append(phoneNumber).append("'").toString());
                }
                continue;
            }
            ContactId contact = ContactUtil.createContactIdFromValidatedData(validatedNumber);
            if (!alreadyInEabOrInvalidNumbers.contains(contact)) {
                // If this number is not considered RCS valid or has already an entry with RCS, skip
                // it
                if (!mContactManager.isContactIdAssociatedWithRcsContactProvider(contact)
                        && (!mContactManager.isOnlySimAssociated(contact) || (Build.VERSION.SDK_INT > 10))) {
                    // This entry is valid and not already has a RCS raw contact, it can be treated
                    // We exclude the number that comes from SIM only contacts, as those cannot be
                    // aggregated to RCS raw contacts only if OS version if gingerbread or fewer
                    toBeTreatedNumbers.add(contact);
                } else {
                    // This entry is either not valid or already RCS, this number is already done
                    alreadyInEabOrInvalidNumbers.add(contact);

                    // Remove the number from the treated list, if it is in it
                    toBeTreatedNumbers.remove(contact);
                }
            } else {
                // Remove the number from the treated list, it was already queried for another raw
                // contact on the same number
                toBeTreatedNumbers.remove(contact);

                // If it is a RCS contact and the raw contact is not associated with a RCS raw
                // contact,
                // then we have to create a new association for it
                long rawContactId = phonesCursor.getLong(columnIndexRawContactId);
                if ((!mContactManager.isSimAccount(rawContactId) || (Build.VERSION.SDK_INT > 10))
                        && (mContactManager.getAssociatedRcsRawContact(rawContactId, contact) == -1)) {
                    ContactInfo currentInfo = mContactManager.getContactInfo(contact);
                    if (currentInfo != null && currentInfo.isRcsContact()) {
                        mContactManager.createRcsContact(currentInfo, rawContactId);
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
        Capabilities capabilities = mContactManager.getContactCapabilities(contact);
        if (capabilities != null) {
            // Force a reset of content sharing capabilities
            capabilities.setImageSharingSupport(false);
            capabilities.setVideoSharingSupport(false);

            // Update the database capabilities
            mContactManager.setContactCapabilities(contact, capabilities);

            // Notify listener
            getImsModule().getCore().getListener()
                    .handleCapabilitiesNotification(contact, capabilities);
        }
    }
}
