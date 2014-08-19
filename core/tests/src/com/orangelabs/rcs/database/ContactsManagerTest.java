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
package com.orangelabs.rcs.database;

import java.util.ArrayList;
import java.util.Set;

import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.eab.ContactsManagerException;
import com.orangelabs.rcs.provider.settings.RcsSettings;
//import com.gsma.services.rcs.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.presence.FavoriteLink;
import com.orangelabs.rcs.core.ims.service.presence.Geoloc;
//import com.orangelabs.rcs.core.ims.service.presence.PhotoIcon;
import com.orangelabs.rcs.core.ims.service.presence.PresenceInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

import com.orangelabs.rcs.utils.logger.Logger;

import android.content.Context;
import android.test.AndroidTestCase;

public class ContactsManagerTest extends AndroidTestCase {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private ContactsManager cm = null;
	private String contact = "+33987654321";
	// private String contacto = "+33633139785";
	private ContactUtils contactUtils = new ContactUtils();;
	// info.setContact(contact);
	private ContactId contactIdo = contactUtils.formatContactId("+33633139785");
	private long timestamp = 1354874203;
	Context ctx;

	protected void setUp() throws Exception {
		super.setUp();
		ContactsManager.createInstance(getContext());
		RcsSettings.createInstance(getContext());
		ctx = getContext();
		cm = ContactsManager.getInstance();
	}

	protected void tearDown() throws Exception {
		cm.cleanRCSEntries();
		super.tearDown();
	}

	public void testCreateMyContact() {
		// to end
		long myraw = cm.createMyContact();
		if (logger.isActivated()) {
			logger.debug("my rawId = " + myraw);
			// return -1 cause settings.isSocialPresenceSupported() and accounts
		}
	}

	public void testSetRcsContact() {
		// Init ContactInfo
		ContactInfo info = new ContactInfo();
		info.setRcsStatus(ContactInfo.RCS_ACTIVE);

		info.setRcsStatusTimestamp(timestamp);

		info.setRegistrationState(ContactInfo.REGISTRATION_STATUS_ONLINE);
		ContactUtils contactUtils = new ContactUtils();
		;
		// info.setContact(contact);
		ContactId contactId = contactUtils.formatContactId(contact);
		info.setContact(contactId);
		Capabilities capa = new Capabilities();
		capa.setCsVideoSupport(false);
		capa.setFileTransferSupport(false);
		capa.setImageSharingSupport(false);
		capa.setImSessionSupport(true);
		capa.setPresenceDiscoverySupport(true);
		capa.setSocialPresenceSupport(true);
		capa.setVideoSharingSupport(true);
		capa.setTimestamp(timestamp);
		capa.addSupportedExtension("MyRcsExtensionTag1");
		capa.addSupportedExtension("MyRcsExtensionTag2");
		info.setCapabilities(capa);

		PresenceInfo pres = new PresenceInfo();
		pres.setFavoriteLink(new FavoriteLink("fav_link_name", "http://fav_link_url"));
		pres.setFavoriteLinkUrl("http://fav_link_url");
		pres.setFreetext("free_text");
		pres.setGeoloc(new Geoloc(1, 2, 3));
		// TODO add photo
		// pres.setPhotoIcon(new PhotoIcon(content, width, height));
		pres.setPresenceStatus(PresenceInfo.ONLINE);
		pres.setTimestamp(timestamp);
		info.setPresenceInfo(pres);

		// Set RCS contact info
		try {
			cm.setContactInfo(info, null);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()) {
				logger.error("Could not save the contact modifications", e);
			}
		}
	}

	public void testGetRcsContactInfo() {
		// Get contact info
		contactoCreate();
		ContactInfo getInfo = cm.getContactInfo(contactIdo);

		// Compare getContactInfo informations and initial informations
		if (getInfo == null) {
			assertEquals(true, false);
			return;
		}
		Capabilities getCapa = getInfo.getCapabilities();
		if (getCapa == null) {
			assertEquals(true, false);
		} else {
			assertEquals(false, getCapa.isCsVideoSupported());
			assertEquals(false, getCapa.isFileTransferSupported());
			assertEquals(false, getCapa.isImageSharingSupported());
			assertEquals(true, getCapa.isImSessionSupported());
			assertEquals(true, getCapa.isPresenceDiscoverySupported());
			assertEquals(true, getCapa.isSocialPresenceSupported());
			assertEquals(true, getCapa.isVideoSharingSupported());
			// Timestamp not tested because it is automatically updated with the current time
			ArrayList<String> getExtraCapa = getCapa.getSupportedExtensions();
			if (getExtraCapa == null) {
				assertEquals(true, false);
			} else {
				assertEquals(true, getExtraCapa.contains("MyRcsExtensionTag1"));
				assertEquals(true, getExtraCapa.contains("MyRcsExtensionTag2"));
			}
		}
		PresenceInfo getPres = getInfo.getPresenceInfo();
		if (getPres == null) {
			assertEquals(true, false);
		} else {
			FavoriteLink getFav = getPres.getFavoriteLink();
			if (getFav == null) {
				assertEquals(true, false);
			} else {
				assertEquals("fav_link_name", getFav.getName());
				assertEquals("http://fav_link_url", getFav.getLink());
			}
			assertEquals("http://fav_link_url", getPres.getFavoriteLinkUrl());
			assertEquals("free_text", getPres.getFreetext());
			Geoloc getgeo = getPres.getGeoloc();
			if (getgeo == null) {
				assertEquals(true, false);
			} else {
				assertEquals(1, getgeo.getLatitude(), 0);
				assertEquals(2, getgeo.getLongitude(), 0);
				assertEquals(3, getgeo.getAltitude(), 0);
			}
			// TODO add photo
			assertEquals(PresenceInfo.ONLINE, getPres.getPresenceStatus());
			assertEquals(timestamp, getPres.getTimestamp());
		}
		assertEquals(ContactInfo.RCS_ACTIVE, getInfo.getRcsStatus());
		assertEquals(timestamp, getInfo.getRcsStatusTimestamp());
		assertEquals(ContactInfo.REGISTRATION_STATUS_ONLINE, getInfo.getRegistrationState());
	}

	public void contactoCreate() {
		ContactInfo info = new ContactInfo();
		info.setContact(contactIdo);
		info.setRcsStatus(ContactInfo.RCS_ACTIVE);
		info.setRcsStatusTimestamp(timestamp);
		info.setRegistrationState(ContactInfo.REGISTRATION_STATUS_ONLINE);

		Capabilities capa = new Capabilities();
		capa.setCsVideoSupport(false);
		capa.setFileTransferSupport(false);
		capa.setImageSharingSupport(false);
		capa.setImSessionSupport(true);
		capa.setPresenceDiscoverySupport(true);
		capa.setSocialPresenceSupport(true);
		capa.setVideoSharingSupport(true);
		capa.setTimestamp(timestamp);
		capa.addSupportedExtension("MyRcsExtensionTag1");
		capa.addSupportedExtension("MyRcsExtensionTag2");
		info.setCapabilities(capa);

		PresenceInfo pres = new PresenceInfo();
		pres.setFavoriteLink(new FavoriteLink("fav_link_name", "http://fav_link_url"));
		pres.setFavoriteLinkUrl("http://fav_link_url");
		pres.setFreetext("free_text");
		pres.setGeoloc(new Geoloc(1, 2, 3));
		// TODO add photo
		// pres.setPhotoIcon(new PhotoIcon(content, width, height));
		pres.setPresenceStatus(PresenceInfo.ONLINE);
		pres.setTimestamp(timestamp);
		info.setPresenceInfo(pres);

		// Set RCS contact info
		try {
			cm.setContactInfo(info, null);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()) {
				logger.error("Could not save the contact modifications", e);
			}
		}
	}

	public void testSetContactInfo() {
		// create then change a RCSContact into a basic Contact then return it to be a RCSContact
		Set<ContactId> rcscontacts = cm.getRcsContacts();
		if (logger.isActivated()) {
			for (ContactId rcs : rcscontacts) {
				logger.debug("RCS contact : " + rcs.toString());
			}
		}
		if (rcscontacts.isEmpty())
			contactoCreate();

		ContactInfo oldinfo = cm.getContactInfo(contactIdo);

		ContactInfo newInfo = new ContactInfo();

		Capabilities capa = new Capabilities();
		capa.setCsVideoSupport(false);
		capa.setFileTransferSupport(false);
		capa.setImageSharingSupport(false);

		capa.setImSessionSupport(false);
		capa.setPresenceDiscoverySupport(false);
		capa.setSocialPresenceSupport(false);
		capa.setVideoSharingSupport(false);

		capa.setTimestamp(timestamp);
		newInfo.setCapabilities(capa);

		newInfo.setContact(contactIdo);

		// newInfo.setPresenceInfo(null);
		// if (new)PresenceInfo is null, error on ContactManager line 504 so
		PresenceInfo prese = new PresenceInfo();
		prese.setFavoriteLink(new FavoriteLink("fav_link_name", "http://fav_link_url"));
		newInfo.setPresenceInfo(prese);
		newInfo.setRcsStatus(ContactInfo.NOT_RCS);
		newInfo.setRcsStatusTimestamp(timestamp);
		newInfo.setRegistrationState(ContactInfo.REGISTRATION_STATUS_UNKNOWN);
		// Set not RCS contact info and test
		try {
			cm.setContactInfo(newInfo, oldinfo);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()) {
				logger.error("Could not save the contact modifications", e);
			}
		}

		rcscontacts = cm.getRcsContacts();
		Set<ContactId> contacts = cm.getAllContacts();
		boolean contactChange = (contacts.contains(contactIdo) && (!(rcscontacts.contains(contactIdo))));
		assertEquals(contactChange, true);

		/*
		 * List<String> avails = cm.getAvailableContacts(); boolean contactChange = (contacts.contains(contacto) &&
		 * (!(rcscontacts.contains(contacto)))); if (logger.isActivated()){ if(rcscontacts.isEmpty()) {
		 * logger.debug("no RCS contact "); } else { for(String rcs : rcscontacts) { logger.debug("RCS contact : " + rcs); }
		 * for(String av : avails) { logger.debug("available contact : " + av); } } }
		 */

		oldinfo = cm.getContactInfo(contactIdo);
		newInfo.setRcsStatus(ContactInfo.RCS_ACTIVE);
		timestamp = 1354874212;
		newInfo.setRcsStatusTimestamp(timestamp);
		newInfo.setRegistrationState(ContactInfo.REGISTRATION_STATUS_ONLINE);
		capa.setImSessionSupport(true);
		capa.setPresenceDiscoverySupport(true);
		capa.setSocialPresenceSupport(true);
		capa.setVideoSharingSupport(true);
		capa.setTimestamp(timestamp);
		capa.addSupportedExtension("MyRcsExtensionTag3");
		capa.addSupportedExtension("MyRcsExtensionTag4");

		prese.setFavoriteLink(new FavoriteLink("favo_link_name", "http://favo_link_url"));
		prese.setFreetext("free_text");
		prese.setGeoloc(new Geoloc(1, 2, 4));
		// TODO add photo
		// pres.setPhotoIcon(new PhotoIcon(content, width, height));
		prese.setPresenceStatus(PresenceInfo.ONLINE);
		prese.setTimestamp(timestamp);
		newInfo.setPresenceInfo(prese);

		try {
			cm.setContactInfo(newInfo, oldinfo);
		} catch (ContactsManagerException e) {
			if (logger.isActivated()) {
				logger.error("Could not save the contact modifications", e);
			}
		}
		rcscontacts = cm.getRcsContacts();
		// avails = cm.getRcsContacts();
		contacts = cm.getAllContacts();
		boolean contactToRCS = (rcscontacts.contains(contactIdo) && contacts.contains(contactIdo));
		/*
		 * if (logger.isActivated()){ if(rcscontacts.isEmpty()) { logger.debug("no RCS contact "); } else { for(String rcs :
		 * rcscontacts) { logger.debug("RCS contact : " + rcs); } } for(String av : avails) { logger.debug("available contact : " +
		 * av); } }
		 */assertEquals(contactToRCS, true);
		cm.cleanRCSEntries();
	}

	public void testRemoveRcsContact() {
		cm.cleanRCSEntries();
	}

	/*
	 * private List<Long> TestGetRcsRawContactIdFromPhoneNumber(String phoneNumber) { }
	 * 
	 * public long TestGetAssociatedRcsRawContact(final long rawContactId, final String rcsNumber) { } public boolean
	 * TestIsOnlySimAssociated(final String phoneNumber) {} public boolean TestIsSimAssociated(final long rawContactId){} public
	 * boolean TestIsSimAccount(final long rawContactId){} private List<Long> TestGetRcsRawContactIdsFromContact(final String
	 * contact){} public long TestCreateRcsContact(final ContactInfo info, final long rawContactId) {} public void
	 * testSetContactPhotoIcon(String contact, PhotoIcon photoIcon) { } public long TestCreateMyContact() {} public void
	 * TestRemoveContactPhotoIcon(String contact) {} public ContactInfo TestGetContactInfo(String contact){} public void
	 * TestSetContactSharingStatus(String contact, String status, String reason){} public int TestGetContactSharingStatus(String
	 * contact){} public void TestRevokeContact(String contact){} public void TestUnrevokeContact(String contact) {} public void
	 * TestBlockContact(String contact) {} public void TestUnblockContact(String contact) {} public void
	 * TestFlushContactProvider(){} public void TestModifyRcsContactInProvider(String contact, int rcsStatus){} public List<String>
	 * TestGetRcsContactsWithSocialPresence(){ } public List<String> TestGetAllContacts(){} public List<String>
	 * TestGetRcsBlockedContacts(){} public List<String> TestGetRcsInvitedContacts(){} public List<String>
	 * TestGetRcsWillingContacts(){} public List<String> TestGetRcsCancelledContacts(){} public void
	 * TestRemoveCancelledPresenceInvitation(String contact){} public boolean TestIsNumberBlocked(String number){} public boolean
	 * TestIsNumberShared(String number){} public boolean TestIsNumberInvited(String number) {} public boolean
	 * TestIsNumberWilling(String number){} public boolean TestIsNumberCancelled(String number){} public boolean
	 * TestIsRcsValidNumber(String phoneNumber){} private ArrayList<ContentProviderOperation> TestModifyContactTypeForContact(long
	 * rawContactId, String rcsNumber, int newContactType, int oldContactType){} private ContentProviderOperation
	 * TestModifyMimeTypeForContact(long rawContactId, String rcsNumber, String mimeType, boolean newState, boolean oldState){}
	 * private ContentProviderOperation TestInsertMimeTypeForContact(long rawContactId, String rcsNumber, String mimeType){} private
	 * ContentProviderOperation TestDeleteMimeTypeForContact(long rawContactId, String rcsNumber, String mimeType){} private
	 * ArrayList<ContentProviderOperation> TestModifyContactRegistrationState(long rawContactId, String rcsNumber, int
	 * newRegistrationState, int oldRegistrationState, String newFreeText, String oldFreeText){}
	 * 
	 * private List<ContentProviderOperation> TestModifyExtensionsCapabilityForContact(long rawContactId, String rcsNumber,
	 * ArrayList<String> newExtensions, ArrayList<String> oldExtensions){}
	 * 
	 * private ArrayList<ContentProviderOperation> TestModifyPresenceForContact(long rawContactId, String rcsNumber, PresenceInfo
	 * newPresenceInfo, PresenceInfo oldPresenceInfo){}
	 * 
	 * private String TestGetMimeTypeDescription(String mimeType){} public List<String> TestGetImSessionCapableContacts() {} public
	 * List<String> TestGgetRichcallCapableContacts(){} public List<String> TestGetAvailableContacts() {} public boolean
	 * TestIsContactRcsActive(String contact){} public void TestSetContactCapabilities(String contact, Capabilities capabilities,
	 * int contactType, int registrationState) {} public void TestSetContactCapabilities(String contact, Capabilities capabilities)
	 * {} public Capabilities TestGetContactCapabilities(String contact){} public void TestSetContactCapabilitiesTimestamp(String
	 * contact, long timestamp){}
	 */

}
