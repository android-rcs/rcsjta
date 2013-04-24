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

package com.orangelabs.rcs.service.api.server.presence;

import java.util.List;

import android.content.Intent;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.service.api.client.presence.IPresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Presence API service
 * 
 * @author jexa7410
 */
public class PresenceApiService extends IPresenceApi.Stub {
    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 */
	public PresenceApiService() {
		if (logger.isActivated()) {
			logger.info("Presence API service is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
	}
    
    /**
	 * Set my presence info
	 * 
	 * @param info Presence info
	 * @return Boolean result
	 * @throws ServerApiException
	 */
	public boolean setMyPresenceInfo(PresenceInfo info) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Set my presence info");
		}

    	// Check permission
		ServerApiUtils.testPermission();

		if (Core.getInstance().getPresenceService().isPermanentState()) {
    		// Test core availability
    		ServerApiUtils.testCore();
    	} else {
			// Test IMS connection 
			ServerApiUtils.testIms();
    	}
    	
		try {
			// Publish presence info
			boolean result = Core.getInstance().getPresenceService().publishPresenceInfo(info);
			if (result) {
				// Update Contacts
				ContactsManager.getInstance().setMyInfo(info);
		
				// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
			}
			return result;
		} catch(Exception e) {
			throw new ServerApiException(e);
		}
	}
	
	/**
	 * Invite a contact to share its presence
	 * 
	 * @param contact Contact
	 * @return Boolean result
	 * @throws ServerApiException
	 */
	public boolean inviteContact(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Invite " + contact + " to share presence");
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			// Update presence server
			boolean result = Core.getInstance().getPresenceService().inviteContactToSharePresence(contact);
			if (result){
				// Put "pending_out" as presence status for contact in EAB content provider
				ContactsManager.getInstance().setContactSharingStatus(contact, PresenceInfo.RCS_PENDING_OUT, "");
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
		
	/**
	 * Accept sharing invitation
	 * 
	 * @param contact Contact
	 * @return Boolean result
	 * @throws ServerApiException
	 */
	public boolean acceptSharingInvitation(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Accept sharing invitation from " + contact);
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			// Update presence server
			boolean result = Core.getInstance().getPresenceService().acceptPresenceSharingInvitation(contact);
			if (result){
				// Set this contact presence status to "active"
				ContactsManager.getInstance().setContactSharingStatus(contact, PresenceInfo.RCS_ACTIVE, "");
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
	
	/**
	 * Reject sharing invitation
	 * 
	 * @param contact Contact
	 * @return Boolean result
	 * @throws ServerApiException
	 */
	public boolean rejectSharingInvitation(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Reject sharing invitation from " + contact);
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			// Update presence server
			boolean result = Core.getInstance().getPresenceService().blockPresenceSharingInvitation(contact);
			if (result){
				// Set this contact presence status to "blocked"
				ContactsManager.getInstance().setContactSharingStatus(contact, PresenceInfo.RCS_BLOCKED, "");
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
	
	/**
	 * Ignore sharing invitation
	 * 
	 * @param contact Contact
	 * @throws ServerApiException
	 */
	public void ignoreSharingInvitation(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Ignore sharing invitation from " + contact);
		}

    	// Check permission
		ServerApiUtils.testPermission();

		try {
			// Set this contact presence status to "pending"
			ContactsManager.getInstance().setContactSharingStatus(contact, PresenceInfo.RCS_PENDING, "");
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
	
	/**
	 * Revoke a contact
	 * 
	 * @param contact Contact
	 * @return Boolean result
	 * @throws ServerApiException
	 */
	public boolean revokeContact(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Revoke contact " + contact);
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			// Update presence server
			boolean result = Core.getInstance().getPresenceService().revokeSharedContact(contact);
			if (result){
				// Put contact in revoked contacts list of EAB content provider
				ContactsManager.getInstance().setContactSharingStatus(contact, PresenceInfo.RCS_REVOKED, "");
				
				// The contact should be automatically unrevoked after a given timeout. Here the
				// timeout period is 0, so the contact can receive invitations again now
				unrevokeContact(contact);			
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
     * Unrevoke a contact
     * 
     * @param contact Contact
	 * @return Boolean result
     * @throws ServerApiException
     */
	public boolean unrevokeContact(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Unrevoke contact " + contact);
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			// Update presence server
			boolean result = Core.getInstance().getPresenceService().removeRevokedContact(contact);
			if (result){
				// Remove contact from revoked contacts list of EAB content provider
				ContactsManager.getInstance().unrevokeContact(contact);
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Unblock a contact
     * 
     * @param contact Contact
	 * @return Boolean result
     * @throws ServerApiException
     */
	public boolean unblockContact(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Unblock contact " + contact);
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			// Update presence server
			boolean result = Core.getInstance().getPresenceService().removeBlockedContact(contact);
			if (result){
				// Remove contact from blocked contacts list of EAB content provider
				ContactsManager.getInstance().unblockContact(contact);
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
	
	/**
	 * Get the list of granted contacts
	 * 
	 * @return List of contacts
	 * @throws ServerApiException
	 */
	public List<String> getGrantedContacts() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get granted contacts");
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			return Core.getInstance().getPresenceService().getXdmManager().getGrantedContacts();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
	
	/**
	 * Get the list of revoked contacts
	 * 
	 * @return List of contacts
	 * @throws ServerApiException
	 */
	public List<String> getRevokedContacts() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get revoked contacts");
		}

    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			return Core.getInstance().getPresenceService().getXdmManager().getRevokedContacts();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

	/**
	 * Get the list of blocked contacts
	 * 
	 * @return List of contacts
	 * @throws ServerApiException
	 */
	public List<String> getBlockedContacts() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get blocked contacts");
		}
		
    	// Check permission
		ServerApiUtils.testPermission();

		// Test core availability
		ServerApiUtils.testCore();

		try {
			return Core.getInstance().getPresenceService().getXdmManager().getBlockedContacts();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}
}
