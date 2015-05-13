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

package com.gsma.rcs.database;

import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.presence.FavoriteLink;
import com.gsma.rcs.core.ims.service.presence.Geoloc;
import com.gsma.rcs.core.ims.service.presence.PresenceInfo;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.logger.Logger;

public class ContactManagerTest extends AndroidTestCase {

    private static final Logger logger = Logger.getLogger(ContactManagerTest.class.getName());
    private ContactManager mContactManager;
    private String mNumber = "+33987654321";
    private ContactUtil contactUtils;
    private ContactId mContact;
    private long timestamp = 1354874203;
    private Context mContext;
    private ContentResolver mContentResolver;
    private LocalContentResolver mLocalContentResolver;
    private RcsSettings mRcsSettings;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mContactManager = ContactManager.createInstance(mContext, mContentResolver,
                mLocalContentResolver, mRcsSettings);

        contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(mContext));

        // info.setContact(contact);
        mContact = contactUtils.formatContact("+33633139785");
    }

    protected void tearDown() throws Exception {
        mContactManager.cleanRCSEntries();
        super.tearDown();
    }

    public void testCreateMyContact() {
        // to end
        long myraw = mContactManager.createMyContact();
        if (logger.isActivated()) {
            logger.debug("my rawId = " + myraw);
            // return -1 cause settings.isSocialPresenceSupported() and accounts
        }
    }

    public void testSetRcsContact() {
        // Init ContactInfo
        ContactInfo info = new ContactInfo();
        info.setRcsStatus(RcsStatus.ACTIVE);

        info.setRcsStatusTimestamp(timestamp);

        info.setRegistrationState(RegistrationState.ONLINE);

        // info.setContact(contact);
        ContactId contactId = null;
        try {
            contactId = contactUtils.formatContact(mNumber);
        } catch (RcsPermissionDeniedException e1) {
            fail(e1.getMessage());
        }
        info.setContact(contactId);
        Capabilities capa = new Capabilities();
        capa.setCsVideoSupport(false);
        capa.setFileTransferSupport(false);
        capa.setImageSharingSupport(false);
        capa.setImSessionSupport(true);
        capa.setPresenceDiscoverySupport(true);
        capa.setSocialPresenceSupport(true);
        capa.setVideoSharingSupport(true);
        capa.setTimestampOfLastRequest(timestamp);
        capa.setSipAutomata(true);
        capa.addSupportedExtension("MyRcsExtensionTag1");
        capa.addSupportedExtension("MyRcsExtensionTag2");
        capa.setTimestampOfLastResponse(timestamp);
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
            mContactManager.setContactInfo(info, null);
        } catch (ContactManagerException e) {
            if (logger.isActivated()) {
                logger.error("Could not save the contact modifications", e);
            }
        }
    }

    public void testGetRcsContactInfo() {
        // Get contact info
        contactoCreate();
        ContactInfo getInfo = mContactManager.getContactInfo(mContact);

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
            assertEquals(true, getCapa.isSipAutomata());
            assertEquals(timestamp, getCapa.getTimestampOfLastResponse());
            assertEquals(timestamp, getCapa.getTimestampOfLastRequest());
            // Timestamp not tested because it is automatically updated with the current time
            Set<String> getExtraCapa = getCapa.getSupportedExtensions();
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
        assertEquals(RcsStatus.ACTIVE, getInfo.getRcsStatus());
        assertEquals(timestamp, getInfo.getRcsStatusTimestamp());
        assertEquals(RegistrationState.ONLINE, getInfo.getRegistrationState());
    }

    public void contactoCreate() {
        ContactInfo info = new ContactInfo();
        info.setContact(mContact);
        info.setRcsStatus(RcsStatus.ACTIVE);
        info.setRcsStatusTimestamp(timestamp);
        info.setRegistrationState(RegistrationState.ONLINE);

        Capabilities capa = new Capabilities();
        capa.setCsVideoSupport(false);
        capa.setFileTransferSupport(false);
        capa.setImageSharingSupport(false);
        capa.setImSessionSupport(true);
        capa.setPresenceDiscoverySupport(true);
        capa.setSocialPresenceSupport(true);
        capa.setVideoSharingSupport(true);
        capa.setTimestampOfLastRequest(timestamp);
        capa.addSupportedExtension("MyRcsExtensionTag1");
        capa.addSupportedExtension("MyRcsExtensionTag2");
        capa.setSipAutomata(true);
        capa.setTimestampOfLastResponse(timestamp);
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
            mContactManager.setContactInfo(info, null);
        } catch (ContactManagerException e) {
            if (logger.isActivated()) {
                logger.error("Could not save the contact modifications", e);
            }
        }
    }

    public void testSetContactInfo() {
        // create then change a RCSContact into a basic Contact then return it to be a RCSContact
        Set<ContactId> rcscontacts = mContactManager.getRcsContacts();
        if (logger.isActivated()) {
            for (ContactId rcs : rcscontacts) {
                logger.debug("RCS contact : " + rcs.toString());
            }
        }
        if (rcscontacts.isEmpty())
            contactoCreate();

        ContactInfo oldinfo = mContactManager.getContactInfo(mContact);

        ContactInfo newInfo = new ContactInfo();

        Capabilities capa = new Capabilities();
        capa.setCsVideoSupport(false);
        capa.setFileTransferSupport(false);
        capa.setImageSharingSupport(false);

        capa.setImSessionSupport(false);
        capa.setPresenceDiscoverySupport(false);
        capa.setSocialPresenceSupport(false);
        capa.setVideoSharingSupport(false);

        capa.setTimestampOfLastRequest(timestamp);
        newInfo.setCapabilities(capa);

        newInfo.setContact(mContact);

        capa.setTimestampOfLastResponse(timestamp);

        // newInfo.setPresenceInfo(null);
        // if (new)PresenceInfo is null, error on ContactManager line 504 so
        PresenceInfo prese = new PresenceInfo();
        prese.setFavoriteLink(new FavoriteLink("fav_link_name", "http://fav_link_url"));
        newInfo.setPresenceInfo(prese);
        newInfo.setRcsStatus(RcsStatus.NOT_RCS);
        newInfo.setRcsStatusTimestamp(timestamp);
        newInfo.setRegistrationState(RegistrationState.UNKNOWN);
        // Set not RCS contact info and test
        try {
            mContactManager.setContactInfo(newInfo, oldinfo);
        } catch (ContactManagerException e) {
            if (logger.isActivated()) {
                logger.error("Could not save the contact modifications", e);
            }
        }

        rcscontacts = mContactManager.getRcsContacts();
        Set<ContactId> contacts = mContactManager.getAllContacts();
        boolean contactChange = (contacts.contains(mContact) && (!(rcscontacts.contains(mContact))));
        assertEquals(contactChange, true);

        /*
         * List<String> avails = cm.getAvailableContacts(); boolean contactChange =
         * (contacts.contains(contacto) && (!(rcscontacts.contains(contacto)))); if
         * (logger.isActivated()){ if(rcscontacts.isEmpty()) { logger.debug("no RCS contact "); }
         * else { for(String rcs : rcscontacts) { logger.debug("RCS contact : " + rcs); } for(String
         * av : avails) { logger.debug("available contact : " + av); } } }
         */

        oldinfo = mContactManager.getContactInfo(mContact);
        newInfo.setRcsStatus(RcsStatus.ACTIVE);
        timestamp = 1354874212;
        newInfo.setRcsStatusTimestamp(timestamp);
        newInfo.setRegistrationState(RegistrationState.ONLINE);
        capa.setImSessionSupport(true);
        capa.setPresenceDiscoverySupport(true);
        capa.setSocialPresenceSupport(true);
        capa.setVideoSharingSupport(true);
        capa.setTimestampOfLastRequest(timestamp);
        capa.addSupportedExtension("MyRcsExtensionTag3");
        capa.addSupportedExtension("MyRcsExtensionTag4");
        capa.setTimestampOfLastResponse(timestamp);

        prese.setFavoriteLink(new FavoriteLink("favo_link_name", "http://favo_link_url"));
        prese.setFreetext("free_text");
        prese.setGeoloc(new Geoloc(1, 2, 4));
        // TODO add photo
        // pres.setPhotoIcon(new PhotoIcon(content, width, height));
        prese.setPresenceStatus(PresenceInfo.ONLINE);
        prese.setTimestamp(timestamp);
        newInfo.setPresenceInfo(prese);

        try {
            mContactManager.setContactInfo(newInfo, oldinfo);
        } catch (ContactManagerException e) {
            if (logger.isActivated()) {
                logger.error("Could not save the contact modifications", e);
            }
        }
        rcscontacts = mContactManager.getRcsContacts();
        // avails = cm.getRcsContacts();
        contacts = mContactManager.getAllContacts();
        boolean contactToRCS = (rcscontacts.contains(mContact) && contacts.contains(mContact));
        /*
         * if (logger.isActivated()){ if(rcscontacts.isEmpty()) { logger.debug("no RCS contact "); }
         * else { for(String rcs : rcscontacts) { logger.debug("RCS contact : " + rcs); } }
         * for(String av : avails) { logger.debug("available contact : " + av); } }
         */assertEquals(contactToRCS, true);
        mContactManager.cleanRCSEntries();
    }

    public void testRemoveRcsContact() {
        mContactManager.cleanRCSEntries();
    }

}
