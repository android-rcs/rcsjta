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

package com.gsma.rcs.contact;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ContactInfo.BlockingState;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.test.AndroidTestCase;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ContactManagerTest extends AndroidTestCase {

    private static final Logger sLogger = Logger.getLogger(ContactManagerTest.class.getName());
    private ContactManager mContactManager;
    private ContactId mContact;
    private Random mRandom;
    private RcsSettings mRcsSettings;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        mRcsSettings = RcsSettings.createInstance(localContentResolver);
        mContactManager = ContactManager.createInstance(context, contentResolver,
                localContentResolver, mRcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(new ContactUtilMockContext(context));
        mContact = contactUtils.formatContact("+33633139785");
        mRandom = new Random();
    }

    protected void tearDown() throws Exception {
        mContactManager.cleanRCSEntries();
        super.tearDown();
    }

    public void testCreateMyContact() throws ContactManagerException {
        long myraw = mContactManager.createMyContact();
        if (sLogger.isActivated()) {
            sLogger.debug("my rawId = ".concat(Long.toString(myraw)));
        }
        if (mRcsSettings.isSocialPresenceSupported()) {
            assertTrue(ContactManager.INVALID_ID != myraw);
        }
    }

    public void testGetRcsContactInfo() throws ContactManagerException, FileAccessException {
        CapabilitiesBuilder expectedCapa = createRcsContact();
        ContactInfo getInfo = mContactManager.getContactInfo(mContact);
        assertNotNull(getInfo);
        /* Compare getContactInfo informations and initial informations */
        Capabilities getCapa = getInfo.getCapabilities();
        assertNotNull(getCapa);
        assertEquals(expectedCapa.isCsVideoSupported(), getCapa.isCsVideoSupported());
        assertEquals(expectedCapa.isFileTransferMsrpSupported(),
                getCapa.isFileTransferMsrpSupported());
        assertEquals(expectedCapa.isFileTransferHttpSupported(),
                getCapa.isFileTransferHttpSupported());
        assertEquals(expectedCapa.isImageSharingSupported(), getCapa.isImageSharingSupported());
        assertEquals(expectedCapa.isImSessionSupported(), getCapa.isImSessionSupported());
        assertEquals(expectedCapa.isPresenceDiscovery(), getCapa.isPresenceDiscoverySupported());
        assertEquals(expectedCapa.isSocialPresence(), getCapa.isSocialPresenceSupported());
        assertEquals(expectedCapa.isVideoSharingSupported(), getCapa.isVideoSharingSupported());
        assertEquals(expectedCapa.isSipAutomata(), getCapa.isSipAutomata());
        assertEquals(expectedCapa.getTimestampOfLastRequest(), getCapa.getTimestampOfLastResponse());
        assertEquals(expectedCapa.getTimestampOfLastRequest(), getCapa.getTimestampOfLastRequest());
        Set<String> extensions = getCapa.getSupportedExtensions();
        assertNotNull(extensions);
        assertTrue(extensions.contains("MyRcsExtensionTag1"));
        assertTrue(extensions.contains("MyRcsExtensionTag2"));
        assertEquals(RcsStatus.RCS_CAPABLE, getInfo.getRcsStatus());
        assertEquals(RegistrationState.ONLINE, getInfo.getRegistrationState());
    }

    public void testGetContactCapabilities() throws ContactManagerException, FileAccessException {
        CapabilitiesBuilder expectedCapa = createRcsContact();
        Capabilities getCapa = mContactManager.getContactCapabilities(mContact);
        assertNotNull(getCapa);
        assertEquals(expectedCapa.isCsVideoSupported(), getCapa.isCsVideoSupported());
        assertEquals(expectedCapa.isFileTransferMsrpSupported(),
                getCapa.isFileTransferMsrpSupported());
        assertEquals(expectedCapa.isFileTransferHttpSupported(),
                getCapa.isFileTransferHttpSupported());
        assertEquals(expectedCapa.isImageSharingSupported(), getCapa.isImageSharingSupported());
        assertEquals(expectedCapa.isImSessionSupported(), getCapa.isImSessionSupported());
        assertEquals(expectedCapa.isPresenceDiscovery(), getCapa.isPresenceDiscoverySupported());
        assertEquals(expectedCapa.isSocialPresence(), getCapa.isSocialPresenceSupported());
        assertEquals(expectedCapa.isVideoSharingSupported(), getCapa.isVideoSharingSupported());
        assertEquals(expectedCapa.isSipAutomata(), getCapa.isSipAutomata());
        assertEquals(expectedCapa.getTimestampOfLastRequest(), getCapa.getTimestampOfLastResponse());
        assertEquals(expectedCapa.getTimestampOfLastRequest(), getCapa.getTimestampOfLastRequest());
        Set<String> extensions = getCapa.getSupportedExtensions();
        assertNotNull(extensions);
        assertTrue(extensions.contains("MyRcsExtensionTag1"));
        assertTrue(extensions.contains("MyRcsExtensionTag2"));
    }

    public void testDisplayName() throws ContactManagerException, FileAccessException {
        createRcsContact();
        String displayName = UUID.randomUUID().toString();
        mContactManager.setContactDisplayName(mContact, displayName);
        assertEquals(displayName, mContactManager.getContactDisplayName(mContact));
    }

    public void testUpdateCapabilitiesTimeLastRequest() throws InterruptedException,
            ContactManagerException, FileAccessException {
        createRcsContact();
        Capabilities oldCapa = mContactManager.getContactCapabilities(mContact);
        /*
         * A timer greater than 1 second is set because some emulator have only an accuracy of 1
         * second.
         */
        Thread.sleep(1010);
        mContactManager.updateCapabilitiesTimeLastRequest(mContact);
        Capabilities newCapa = mContactManager.getContactCapabilities(mContact);
        assertNotNull(newCapa);
        assertNotNull(oldCapa);
        assertTrue(newCapa.getTimestampOfLastRequest() > oldCapa.getTimestampOfLastRequest());
    }

    public void testUpdateCapabilitiesTimeLastResponse() throws InterruptedException,
            ContactManagerException, FileAccessException {
        createRcsContact();
        Capabilities oldCapa = mContactManager.getContactCapabilities(mContact);
        /*
         * A timer greater than 1 second is set because some emulator have only an accuracy of 1
         * second.
         */
        Thread.sleep(1010);
        mContactManager.updateCapabilitiesTimeLastResponse(mContact, System.currentTimeMillis());
        Capabilities newCapa = mContactManager.getContactCapabilities(mContact);
        assertNotNull(newCapa);
        assertNotNull(oldCapa);
        assertTrue(newCapa.getTimestampOfLastResponse() > oldCapa.getTimestampOfLastResponse());
    }

    public void testBlockedContact() throws ContactManagerException, FileAccessException {
        createRcsContact();
        mContactManager.setBlockingState(mContact, BlockingState.BLOCKED);
        assertTrue(mContactManager.isBlockedForContact(mContact));
    }

    public CapabilitiesBuilder createRcsContact() throws ContactManagerException, FileAccessException {
        long now = System.currentTimeMillis();
        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder();
        /*
         * For capabilities which do not depend on provisioning, support is chosen randomly.
         */
        capaBuilder.setCsVideo(false);
        capaBuilder.setGeolocationPush(mRandom.nextBoolean());
        capaBuilder.setGeolocationPush(mRandom.nextBoolean());
        capaBuilder.setIpVideoCall(mRandom.nextBoolean());
        capaBuilder.setIpVoiceCall(mRandom.nextBoolean());
        capaBuilder.setGroupChatStoreForward(mRandom.nextBoolean());
        capaBuilder.setFileTransferThumbnail(true);
        capaBuilder.setFileTransferHttp(true);
        capaBuilder.setFileTransferMsrp(true);
        capaBuilder.setImageSharing(mRandom.nextBoolean());
        capaBuilder.setImSession(true);
        capaBuilder.setPresenceDiscovery(mRandom.nextBoolean());
        capaBuilder.setSocialPresence(mRandom.nextBoolean());
        capaBuilder.setVideoSharing(mRandom.nextBoolean());
        capaBuilder.setTimestampOfLastRequest(now);
        capaBuilder.addExtension("MyRcsExtensionTag1");
        capaBuilder.addExtension("MyRcsExtensionTag2");
        capaBuilder.setSipAutomata(mRandom.nextBoolean());
        capaBuilder.setTimestampOfLastResponse(now);
        /* This will create a RCS contact with default presence information */
        mContactManager.setContactCapabilities(mContact, capaBuilder.build(),
                RcsStatus.RCS_CAPABLE, RegistrationState.ONLINE);
        return capaBuilder;
    }

    public void testDeleteRCSEntries() throws ContactManagerException, FileAccessException {
        createRcsContact();
        Set<ContactId> contacts = mContactManager.getAllContactsFromRcsContactProvider();
        assertTrue(!contacts.isEmpty());
        if (sLogger.isActivated()) {
            sLogger.debug("All contacts=".concat(Arrays.toString(contacts.toArray())));
        }
        mContactManager.deleteRCSEntries();
        assertTrue(mContactManager.getAllContactsFromRcsContactProvider().isEmpty());
    }

}
