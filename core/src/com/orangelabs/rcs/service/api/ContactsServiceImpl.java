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

package com.orangelabs.rcs.service.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.IContactsService;
import com.gsma.services.rcs.contacts.RcsContact;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.core.ims.service.ContactInfo.BlockingState;
import com.orangelabs.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Contacts service API implementation
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class ContactsServiceImpl extends IContactsService.Stub {
    /**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(ContactsServiceImpl.class.getSimpleName());

	/**
	 * Constructor
	 */
	public ContactsServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Contacts service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		if (logger.isActivated()) {
			logger.info("Contacts service API is closed");
		}
	}
    
    /**
     * Returns the RCS contact infos from its contact ID (i.e. MSISDN)
     * 
     * @param contact Contact ID
     * @return Contact
     * @throws ServerApiException
     */
	public RcsContact getRcsContact(ContactId contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get RCS contact " + contact);
		}
		// Read capabilities in the local database
		return getRcsContact(ContactsManager.getInstance().getContactInfo(contact));
	}
	
	/**
	 * Convert the com.orangelabs.rcs.core.ims.service.capability.Capabilities instance into a Capabilities instance
	 * 
	 * @param capabilities
	 *            com.orangelabs.rcs.core.ims.service.capability.Capabilities instance
	 * @return Capabilities instance
	 */
	/* package private */static Capabilities getCapabilities(com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities) {
		if (capabilities == null) {
			return null;
		}
		return new Capabilities(capabilities.isImageSharingSupported(), capabilities.isVideoSharingSupported(),
				capabilities.isImSessionSupported(), capabilities.isFileTransferSupported()
						|| capabilities.isFileTransferHttpSupported(), capabilities.isGeolocationPushSupported(),
				capabilities.isIPVoiceCallSupported(), capabilities.isIPVideoCallSupported(), capabilities.getSupportedExtensions(),
				capabilities.isSipAutomata(), capabilities.getTimestampOfLastRefresh(), capabilities.isValid());
	}
	
	/**
	 * Convert the ContactInfo instance into a RcsContact instance
	 * 
	 * @param contactInfo
	 *            the ContactInfo instance
	 * @return RcsContact instance
	 */
	private RcsContact getRcsContact(ContactInfo contactInfo) {
		// Discard if argument is null
		if (contactInfo == null) {
			return null;
		}
		Capabilities capaApi = getCapabilities(contactInfo.getCapabilities());
		boolean registered = RegistrationState.ONLINE.equals(contactInfo.getRegistrationState());
		boolean blocked = BlockingState.BLOCKED.equals(contactInfo.getBlockingState());
		return new RcsContact(contactInfo.getContact(), registered, capaApi, contactInfo.getDisplayName(),
				blocked, contactInfo.getBlockingTimestamp());
	}
	
	
	/**
	 * Interface to filter ContactInfo
	 * @author YPLO6403
	 *
	 */
	private interface FilterContactInfo {
		/**
		 * The filtering method
		 * 
		 * @param contactInfo
		 * @return true if contactInfo is in the scope
		 */
		boolean inScope(ContactInfo contactInfo);
	}
	
	/**
	 * Get a filtered list of RcsContact
	 * 
	 * @param filterContactInfo
	 *            the filter (or null if not applicable)
	 * @return the filtered list of RcsContact
	 */
	private List<RcsContact> getRcsContacts(FilterContactInfo filterContactInfo) {
		List<RcsContact> rcsContacts = new ArrayList<RcsContact>();
		// Read capabilities in the local database
		Set<ContactId> contacts = ContactsManager.getInstance().getRcsContacts();
		for (ContactId contact : contacts) {
			ContactInfo contactInfo = ContactsManager.getInstance().getContactInfo(contact);
			if (contactInfo != null) {
				if (filterContactInfo == null || filterContactInfo.inScope(contactInfo)) {
					RcsContact contact2add = getRcsContact(contactInfo);
					if (contact2add != null) {
						rcsContacts.add(getRcsContact(contactInfo));
					}
				}
			}
		}
		return rcsContacts;
	}
	
	/**
     * Returns the list of rcs contacts
     * 
     * @return List of contacts
     * @throws ServerApiException
     */
    public List<RcsContact> getRcsContacts() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get rcs contacts");
		}
		return getRcsContacts(null);
	}

    /**
     * Returns the list of online contacts (i.e. registered)
     * 
     * @return List of contacts
     * @throws ServerApiException
     */
	public List<RcsContact> getRcsContactsOnline() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get registered rcs contacts");
		}
		return getRcsContacts(new FilterContactInfo() {

			@Override
			public boolean inScope(ContactInfo contactInfo) {
				return RegistrationState.ONLINE.equals(contactInfo.getRegistrationState());
			}
		});
	}
    
    /**
     * Returns the list of contacts supporting a given extension (i.e. feature tag)
     * 
     * @param serviceId Service ID
     * @return List of contacts
     * @throws ServerApiException
     */
	public List<RcsContact> getRcsContactsSupporting(final String serviceId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get rcs contacts supporting " + serviceId);
		}

		return getRcsContacts(new FilterContactInfo() {

			@Override
			public boolean inScope(ContactInfo contactInfo) {
				com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities = contactInfo.getCapabilities();
				if (capabilities != null) {
					Set<String> supportedExtensions = capabilities.getSupportedExtensions();
					if (supportedExtensions != null) {
						for (String supportedExtension : supportedExtensions) {
							if (supportedExtension.equals(serviceId)) {
								return true;
							}
						}
					}
				}
				return false;
			}
		});
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return RcsService.Build.API_VERSION;
	}
	
	/**
	 * Returns the common service configuration
	 * 
	 * @return the common service configuration
	 */
	public ICommonServiceConfiguration getCommonConfiguration() {
		if (logger.isActivated()) {
			logger.debug("getCommonConfiguration");
		}
		return new CommonServiceConfigurationImpl();
	}
	
    /**
     * Block a contact. Any communication from the given contact will be
     * blocked and redirected to the corresponding spambox.
     * 
     * @param contact Contact ID
     * @throws ServerApiException
     */
    public void blockContact(ContactId contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Block contact " + contact);
		}
		try {
			ContactsManager.getInstance().setBlockingState(contact, BlockingState.BLOCKED);
		} catch (Exception e) {
			/*
			 * TODO: This is not the correct way to handle this exception, and
			 * will be fixed in CR037
			 */
			if (logger.isActivated()) {
				logger.error("Unexpected exception", e);
			}
			throw new ServerApiException(e);
		}
    }

    /**
     * Unblock a contact
     * 
     * @param contact Contact ID
     * @throws ServerApiException
     */
    public void unblockContact(ContactId contact) throws ServerApiException {
		try {
			if (logger.isActivated()) {
				logger.info("Unblock contact " + contact);
			}
			ContactsManager.getInstance().setBlockingState(contact, BlockingState.NONE);
		} catch (Exception e) {
			/*
			 * TODO: This is not the correct way to handle this exception, and
			 * will be fixed in CR037
			 */
			if (logger.isActivated()) {
				logger.error("Unexpected exception", e);
			}
			throw new ServerApiException(e);
		}
    }
}