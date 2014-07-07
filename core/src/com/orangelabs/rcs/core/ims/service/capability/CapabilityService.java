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

package com.orangelabs.rcs.core.ims.service.capability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.addressbook.AddressBookEventListener;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Capability discovery service
 * 
 * @author jexa7410
 */
public class CapabilityService extends ImsService implements AddressBookEventListener {
	/**
	 * Capability refresh timeout in seconds
	 */
	private static final int CAPABILITY_REFRESH_PERIOD = RcsSettings.getInstance().getCapabilityRefreshTimeout();

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
     * Flag: set during the address book changed procedure, if we are notified
     * of a change
     */
	private boolean isRecheckNeeded = false;

	/**
     * Flag indicating if a check procedure is in progress
     */
	private boolean isCheckInProgress = false;

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @throws CoreException
     */
	public CapabilityService(ImsModule parent) throws CoreException {
        super(parent, true);

    	// Instanciate the polling manager
        pollingManager = new PollingManager(this);

    	// Instanciate the options manager
		optionsManager = new OptionsManager(parent);

    	// Instanciate the anonymous fetch manager
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
     * @param contactId Contact identifier
     * @return Capabilities
     */
	public synchronized Capabilities requestContactCapabilities(ContactId contactId) {
    	if (logger.isActivated()) {
    		logger.debug("Request capabilities to " + contactId);
    	}

		// Do not request capabilities for oneself
		if (PhoneUtils.compareNumbers(contactId.toString(),ImsModule.IMS_USER_PROFILE.getUsername())) {
			return null;
		}

		// Read capabilities from the database
		Capabilities capabilities = ContactsManager.getInstance().getContactCapabilities(contactId);
		if (capabilities == null) {
	    	if (logger.isActivated()) {
	    		logger.debug("No capability exist for " + contactId);
	    	}

            // New contact: request capabilities from the network
    		optionsManager.requestCapabilities(contactId);
		} else {
	    	if (logger.isActivated()) {
	    		logger.debug("Capabilities exist for " + contactId);
	    	}
			long delta = (System.currentTimeMillis()-capabilities.getTimestamp())/1000;
			if ((delta >= CAPABILITY_REFRESH_PERIOD) || (delta < 0)) {
		    	if (logger.isActivated()) {
		    		logger.debug("Capabilities have expired for " + contactId);
		    	}

		    	// Capabilities are too old: request capabilities from the network
	    		optionsManager.requestCapabilities(contactId);
			}
		}
		return capabilities;
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contactSet Set of contact identifiers
     */
	public void requestContactCapabilities(Set<ContactId> contactSet) {
    	if ((contactSet != null) && (contactSet.size() > 0)) {
        	if (logger.isActivated()) {
        		logger.debug("Request capabilities for " + contactSet.size() + " contacts");
        	}
    		optionsManager.requestCapabilities(contactSet);
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
        Cursor phonesCursor = AndroidFactory.getApplicationContext().getContentResolver().query(
                Phone.CONTENT_URI,
                projection,
                null,
                null,
                null);

		// List of unique number that will have to be queried for capabilities
		Set<ContactId> toBeTreatedNumbers = new HashSet<ContactId>();

		// List of unique number that have already been queried
		ArrayList<ContactId> alreadyInEabOrInvalidNumbers = new ArrayList<ContactId>();

		// We add "My number" to the numbers that are already RCS, so we don't query it if it is present in the address book
        try {
			alreadyInEabOrInvalidNumbers.add( ContactUtils.createContactId(ImsModule.IMS_USER_PROFILE.getUsername()));
		} catch (JoynContactFormatException e1) {
		}

		while(phonesCursor.moveToNext()) {
			// Keep a trace of already treated row. Key is (phone number in international format)
			ContactId phoneNumber;
			try {
				phoneNumber = ContactUtils.createContactId(phonesCursor.getString(1));
			} catch (JoynContactFormatException e) {
				if (logger.isActivated()) {
					logger.warn("Cannot parse phone number " + phonesCursor.getString(1));
				}
				continue;
			}
			if (!alreadyInEabOrInvalidNumbers.contains(phoneNumber)) {
				// If this number is not considered RCS valid or has already an entry with RCS, skip it
                if (!ContactsManager.getInstance().isRcsAssociated(phoneNumber)
						&& ( !ContactsManager.getInstance().isOnlySimAssociated(phoneNumber) || (Build.VERSION.SDK_INT > 10))) {
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
				// Remove the number from the treated list, it was already queried for another raw contact on the same number
				toBeTreatedNumbers.remove(phoneNumber);

                // If it is a RCS contact and the raw contact is not associated with a RCS raw contact,
                // then we have to create a new association for it
                long rawContactId = phonesCursor.getLong(2);
                if ((!ContactsManager.getInstance().isSimAccount(rawContactId) || (Build.VERSION.SDK_INT > 10))
                        && (ContactsManager.getInstance().getAssociatedRcsRawContact(rawContactId, phoneNumber) == -1)) {
                    ContactInfo currentInfo = ContactsManager.getInstance().getContactInfo(phoneNumber);
                    if (currentInfo != null && currentInfo.isRcsContact()) {
                        ContactsManager.getInstance().createRcsContact(currentInfo, rawContactId);
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
     * @param contactId Contact identifier
     */
	public void resetContactCapabilitiesForContentSharing(ContactId contactId) {
		Capabilities capabilities = ContactsManager.getInstance().getContactCapabilities(contactId);
		if (capabilities != null) {
            // Force a reset of content sharing capabilities
			capabilities.setImageSharingSupport(false);
			capabilities.setVideoSharingSupport(false);

		 	// Update the database capabilities
	        ContactsManager.getInstance().setContactCapabilities(contactId, capabilities);

		 	// Notify listener
		 	getImsModule().getCore().getListener().handleCapabilitiesNotification(contactId, capabilities);
		}
	 }
}
