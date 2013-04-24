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

package com.orangelabs.rcs.service.api.client.contacts;

import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.server.ServerApiException;

/**
 * Contacts API
 */
public class ContactsApi {
    /**
     * Constructor
     * 
     * @param ctx Application context
     */
    public ContactsApi(Context ctx) {
    	// Initialize contacts provider
    	ContactsManager.createInstance(ctx);
    }

    /**
     * Get list of supported MIME types associated to RCS contacts
     * 
     * @return MIME types
     */
    public String[] getRcsMimeTypes(){
    	return ContactsManager.getInstance().getRcsMimeTypes();
    }
    
    /**
     * Get contact info
     * 
     * @param contact Contact
     * @return ContactInfo
     */
    public ContactInfo getContactInfo(String contact) {
    	return ContactsManager.getInstance().getContactInfo(contact);
    }

    /**
     * Get a list of all RCS contacts with social presence
     * 
     * @return list of all RCS contacts
     */
    public List<String> getRcsContactsWithSocialPresence(){
    	return ContactsManager.getInstance().getRcsContactsWithSocialPresence();
    }

    /**
     * Get a list of all contacts that have been at least queried once for capabilities
     *
     * @return list of all contacts checked for capabilities
     */
    public List<String> getAllContacts(){
        return ContactsManager.getInstance().getAllContacts();
    }

    /**
     * Get a list of all RCS contacts
     * 
     * @return list of all RCS contacts
     */
    public List<String> getRcsContacts(){
    	return ContactsManager.getInstance().getRcsContacts();
    }
    
    /**
     * Get a list of RCS contacts which are available
     * 
     * @return list of available contacts
     */
    public List<String> getRcsContactsAvailable(){
    	return ContactsManager.getInstance().getAvailableContacts();
    }

    /**
     * Get a list of all RCS blocked contacts
     * 
     * @return list of all RCS contacts
     */
    public List<String> getRcsBlockedContacts(){
    	return ContactsManager.getInstance().getRcsBlockedContacts();
    }
    
    /**
     * Get a list of all RCS invited contacts
     * 
     * @return list of all RCS contacts
     */
    public List<String> getRcsInvitedContacts(){
    	return ContactsManager.getInstance().getRcsInvitedContacts();
    }
    
    /**
     * Get a list of all RCS willing contacts
     * 
     * @return list of all RCS contacts
     */
    public List<String> getRcsWillingContacts(){
    	return ContactsManager.getInstance().getRcsWillingContacts();
    }
    
    /**
     * Get a list of all RCS cancelled contacts
     * 
     * @return list of all RCS contacts
     */
    public List<String> getRcsCancelledContacts(){
    	return ContactsManager.getInstance().getRcsCancelledContacts();
    }

    /**
     * Check if the given number is valid to be treated as an RCS contact
     * 
     * @param phoneNumber Number to be checked
     * @return true if the number is valid, false otherwise
     */
    public boolean isRcsValidNumber(String phoneNumber){
        return ContactsManager.getInstance().isRcsValidNumber(phoneNumber);
    }
    
	/**
	 * Is the number in the RCS blocked list
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberBlocked(String number) {
		return ContactsManager.getInstance().isNumberBlocked(number);
	}
	
	/**
	 * Is the number in the RCS buddy list
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberShared(String number) {
		return ContactsManager.getInstance().isNumberShared(number);
	}

	/**
	 * Has the number been invited to RCS
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberInvited(String number) {
		return ContactsManager.getInstance().isNumberInvited(number);
	}

	/**
	 * Has the number invited us to RCS
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberWilling(String number) {
		return ContactsManager.getInstance().isNumberWilling(number);
	}
	
	/**
	 * Has the number invited us to RCS then be cancelled
	 * 
	 * @param number Number to check
	 * @return boolean
	 */
	public boolean isNumberCancelled(String number) {
		return ContactsManager.getInstance().isNumberCancelled(number);
	}
    
    /**
     * Set the IM-blocked status of a contact
     * 
     * @param contact
     * @param status of the IM-blocked
     */
    public void setImBlockedForContact(String contact, boolean status){
    	ContactsManager.getInstance().setImBlockedForContact(contact, status);
    }
    
    /**
     * Get the IM-blocked status of a contact
     * 
     * @param contact
     */
    public boolean isContactImBlocked(String contact){
    	return ContactsManager.getInstance().isImBlockedForContact(contact);
    }    
    
	/**
	 * Get list of blocked contacts for IM sessions
	 *  
	 * @return List of contacts
	 * @throws ClientApiException
	 */
	public List<String> getBlockedContactsForIm(){
		return ContactsManager.getInstance().getImBlockedContacts();
	}
	
    /**
     * Set the FT-blocked status of a contact
     * 
     * @param contact
     * @param status of the FT-blocked
     */
    public void setFtBlockedForContact(String contact, boolean status){
    	ContactsManager.getInstance().setFtBlockedForContact(contact, status);
    }
    
    /**
     * Get the FT-blocked status of a contact
     * 
     * @param contact
     */
    public boolean isContactFtBlocked(String contact){
    	return ContactsManager.getInstance().isFtBlockedForContact(contact);
    }
    
	/**
	 * Get list of blocked contacts for FT sessions
	 *  
	 * @return List of contacts
	 * @throws ClientApiException
	 */
	public List<String> getBlockedContactsForFt(){
		return ContactsManager.getInstance().getFtBlockedContacts();
	}
	
	/**
	 * Get list of contacts that can use IM sessions
	 *  
	 * @return List of contacts
	 * @throws ServerApiException
	 */
	public List<String> getImSessionCapableContacts(){
		return ContactsManager.getInstance().getImSessionCapableContacts();
	}
	
	/**
	 * Get list of contacts that can do use rich call features
	 *  
	 * @return List of contacts
	 * @throws ServerApiException
	 */
	public List<String> getRichcallCapableContacts(){
		return ContactsManager.getInstance().getRichcallCapableContacts();
	}

	/**
	 * Remove a cancelled presence invitation
	 * 
	 * @param contact
	 */
	public void removeCancelledPresenceInvitation(String contact){
		ContactsManager.getInstance().removeCancelledPresenceInvitation(contact);
	}
    
    /**
     * Get the vCard file associated to a contact
     *
     * @param uri Contact URI in database
     * @return vCard filename
     */
    public String getVisitCard(Uri uri) {
    	return ContactsManager.getInstance().getVisitCard(uri);
    }
}
