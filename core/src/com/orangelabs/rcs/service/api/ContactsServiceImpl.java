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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.IContactsService;
import com.gsma.services.rcs.contacts.JoynContact;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Contacts service API implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ContactsServiceImpl extends IContactsService.Stub {
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

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
     * Returns the joyn contact infos from its contact ID (i.e. MSISDN)
     * 
     * @param contact Contact ID
     * @return Contact
     * @throws ServerApiException
     */
	public JoynContact getJoynContact(ContactId contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get joyn contact " + contact);
		}

		// Read capabilities in the local database
		ContactInfo contactInfo = ContactsManager.getInstance().getContactInfo(contact);
		if (contactInfo !=  null) {
			com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities = contactInfo.getCapabilities();
    		Set<String> exts = new HashSet<String>();
    		List<String> listExts = capabilities.getSupportedExtensions();
    		for(int j=0; j < listExts.size(); j++) {
    			exts.add(listExts.get(j));
    		}
    		Capabilities capaApi = new Capabilities(
    				capabilities.isImageSharingSupported(),
    				capabilities.isVideoSharingSupported(),
    				capabilities.isImSessionSupported(),
    				capabilities.isFileTransferSupported(),
    				capabilities.isGeolocationPushSupported(),
    				capabilities.isIPVoiceCallSupported(),
    				capabilities.isIPVideoCallSupported(),
    				exts,
    				capabilities.isSipAutomata()); 
			boolean registered = (contactInfo.getRegistrationState() == ContactInfo.REGISTRATION_STATUS_ONLINE);
			return new JoynContact(contact, registered, capaApi);
		} else {
			return null;
		}
    }	
	
    /**
     * Returns the list of joyn contacts
     * 
     * @return List of contacts
     * @throws ServerApiException
     */
    public List<JoynContact> getJoynContacts() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get joyn contacts");
		}
		ArrayList<JoynContact> result = new ArrayList<JoynContact>();

		// Read capabilities in the local database
		Set<ContactId> contacts = ContactsManager.getInstance().getRcsContacts();
		for (ContactId contact : contacts) {
			ContactInfo contactInfo = ContactsManager.getInstance().getContactInfo(contact);
			com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities = contactInfo.getCapabilities();
			Capabilities capaApi = null;
			if (capabilities != null) {
	    		Set<String> exts = new HashSet<String>();
	    		List<String> listExts = capabilities.getSupportedExtensions();
	    		for(int j=0; j < listExts.size(); j++) {
	    			exts.add(listExts.get(j));
	    		}
				capaApi = new Capabilities(
	    				capabilities.isImageSharingSupported(),
	    				capabilities.isVideoSharingSupported(),
	    				capabilities.isImSessionSupported(),
	    				capabilities.isFileTransferSupported(),
	    				capabilities.isGeolocationPushSupported(),
	    				capabilities.isIPVoiceCallSupported(),
	    				capabilities.isIPVideoCallSupported(),
	    				exts,
	    				capabilities.isSipAutomata()); 
			}
			boolean registered = (contactInfo.getRegistrationState() == ContactInfo.REGISTRATION_STATUS_ONLINE);
			result.add(new JoynContact(contact, registered, capaApi));
		}
		
		return result;
	}

    /**
     * Returns the list of online contacts (i.e. registered)
     * 
     * @return List of contacts
     * @throws ServerApiException
     */
    public List<JoynContact> getJoynContactsOnline() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get registered joyn contacts");
		}
		ArrayList<JoynContact> result = new ArrayList<JoynContact>();

		// Read capabilities in the local database
		Set<ContactId> contacts = ContactsManager.getInstance().getRcsContacts();
		for (ContactId contact : contacts) {
			ContactInfo contactInfo = ContactsManager.getInstance().getContactInfo(contact);
			com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities = contactInfo.getCapabilities();
			if (contactInfo.getRegistrationState() == ContactInfo.REGISTRATION_STATUS_ONLINE) {			
				Capabilities capaApi = null;
				if (capabilities != null) {
		    		Set<String> exts = new HashSet<String>(capabilities.getSupportedExtensions());
					capaApi = new Capabilities(
		    				capabilities.isImageSharingSupported(),
		    				capabilities.isVideoSharingSupported(),
		    				capabilities.isImSessionSupported(),
		    				capabilities.isFileTransferSupported(),
		    				capabilities.isGeolocationPushSupported(),
		    				capabilities.isIPVoiceCallSupported(),
		    				capabilities.isIPVideoCallSupported(),
		    				exts,
		    				capabilities.isSipAutomata()); 
				}
				boolean registered = (contactInfo.getRegistrationState() == ContactInfo.REGISTRATION_STATUS_ONLINE);
				result.add(new JoynContact(contact, registered, capaApi));
			}
		}
		
		return result;
	}
    
    /**
     * Returns the list of contacts supporting a given extension (i.e. feature tag)
     * 
     * @param serviceId Service ID
     * @return List of contacts
     * @throws ServerApiException
     */
    public List<JoynContact> getJoynContactsSupporting(String serviceId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get joyn contacts supporting " + serviceId);
		}
		
		ArrayList<JoynContact> result = new ArrayList<JoynContact>();

		// Read capabilities in the local database
		Set<ContactId> contacts = ContactsManager.getInstance().getRcsContacts();
		for (ContactId contact : contacts) {
			ContactInfo contactInfo = ContactsManager.getInstance().getContactInfo(contact);
			com.orangelabs.rcs.core.ims.service.capability.Capabilities capabilities = contactInfo.getCapabilities();
			Capabilities capaApi = null;
			if (capabilities != null) {
				ArrayList<String> exts = capabilities.getSupportedExtensions();
				for (int j=0; j < exts.size(); j++) {
					String ext = exts.get(j);
					if (ext.equals(serviceId)) { 
						capaApi = new Capabilities(
			    				capabilities.isImageSharingSupported(),
			    				capabilities.isVideoSharingSupported(),
			    				capabilities.isImSessionSupported(),
			    				capabilities.isFileTransferSupported(),
			    				capabilities.isGeolocationPushSupported(),
			    				capabilities.isIPVoiceCallSupported(),
			    				capabilities.isIPVideoCallSupported(),
			    				new HashSet<String>(capabilities.getSupportedExtensions()),
			    				capabilities.isSipAutomata()); 
						boolean registered = (contactInfo.getRegistrationState() == ContactInfo.REGISTRATION_STATUS_ONLINE);
						result.add(new JoynContact(contact, registered, capaApi));
					}
				}
			}
		}
		
		return result;
    }

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see JoynService.Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return JoynService.Build.API_VERSION;
	}
}