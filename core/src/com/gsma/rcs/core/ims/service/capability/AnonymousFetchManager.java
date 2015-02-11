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

package com.gsma.rcs.core.ims.service.capability;

import java.io.ByteArrayInputStream;

import org.xml.sax.InputSource;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.presence.PresenceInfo;
import com.gsma.rcs.core.ims.service.presence.PresenceUtils;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfParser;
import com.gsma.rcs.core.ims.service.presence.pidf.Tuple;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Capability discovery manager using anonymous fetch procedure
 * 
 * @author Jean-Marc AUFFRET
 */
public class AnonymousFetchManager implements DiscoveryManager {
    /**
     * IMS module
     */
    private ImsModule imsModule;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public AnonymousFetchManager(ImsModule parent) {
        this.imsModule = parent;
    }

    /**
     * Request contact capabilities
     * 
     * @param contact Remote contact identifier
     * @return Returns true if success
     */
    public boolean requestCapabilities(ContactId contact) {
        if (logger.isActivated()) {
            logger.debug("Request capabilities in background for " + contact);
        }
        AnonymousFetchRequestTask task = new AnonymousFetchRequestTask(imsModule, contact);
        task.start();
        return true;
    }

    /**
     * Receive a notification
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
        boolean isLogActivated = logger.isActivated();
        if (isLogActivated) {
            logger.debug("Anonymous fetch notification received");
        }

        // Parse XML part
        byte[] content = notify.getContentBytes();
        if (content != null) {
            if (isLogActivated) {
                logger.debug("Anonymous fetch notification with PIDF document");
            }
            try {
                InputSource pidfInput = new InputSource(new ByteArrayInputStream(content));
                PidfParser pidfParser = new PidfParser(pidfInput);
                PidfDocument presence = pidfParser.getPresence();
                if (presence == null) {
                    return;
                }
                // Extract capabilities
                Capabilities capabilities = new Capabilities();

                // We queried via anonymous fetch procedure, so set presence discovery to true
                capabilities.setPresenceDiscoverySupport(true);

                ContactId contact = ContactUtils.createContactId(presence.getEntity());
                for (Tuple tuple : presence.getTuplesList()) {
                    boolean state = false;
                    if (PresenceInfo.ONLINE.equals(tuple.getStatus().getBasic().getValue())) {
                        state = true;
                    }
                    String id = tuple.getService().getId();
                    if (PresenceUtils.FEATURE_RCS2_VIDEO_SHARE.equalsIgnoreCase(id)) {
                        capabilities.setVideoSharingSupport(state);
                    } else if (PresenceUtils.FEATURE_RCS2_IMAGE_SHARE.equalsIgnoreCase(id)) {
                        capabilities.setImageSharingSupport(state);
                    } else if (PresenceUtils.FEATURE_RCS2_FT.equalsIgnoreCase(id)) {
                        capabilities.setFileTransferSupport(state);
                    } else if (PresenceUtils.FEATURE_RCS2_CS_VIDEO.equalsIgnoreCase(id)) {
                        capabilities.setCsVideoSupport(state);
                    } else if (PresenceUtils.FEATURE_RCS2_CHAT.equalsIgnoreCase(id)) {
                        capabilities.setImSessionSupport(state);
                    }
                }

                // Update capabilities in database
                ContactsManager.getInstance().setContactCapabilities(contact, capabilities,
                        RcsStatus.RCS_CAPABLE, RegistrationState.UNKNOWN);

                // Notify listener
                imsModule.getCore().getListener()
                        .handleCapabilitiesNotification(contact, capabilities);

            } catch (Exception e) {
                if (isLogActivated) {
                    logger.error("Can't parse XML notification", e);
                }
            }
        } else {
            try {
                if (isLogActivated) {
                    logger.debug("Anonymous fetch notification is empty");
                }
                ContactId contact = ContactUtils.createContactId(SipUtils
                        .getAssertedIdentity(notify));

                // Notify content was empty
                Capabilities capabilities = new Capabilities();

                // Update capabilities in database
                ContactsManager.getInstance().setContactCapabilities(contact, capabilities,
                        RcsStatus.NO_INFO, RegistrationState.UNKNOWN);

                // Notify listener
                imsModule.getCore().getListener()
                        .handleCapabilitiesNotification(contact, capabilities);
            } catch (RcsContactFormatException e) {
                if (isLogActivated) {
                    logger.error("Cannot get contact from notify");
                }
            }
        }
    }
}
