/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.presence;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.presence.watcherinfo.Watcher;
import com.gsma.rcs.core.ims.service.presence.watcherinfo.WatcherInfoDocument;
import com.gsma.rcs.core.ims.service.presence.watcherinfo.WatcherInfoParser;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.header.SubscriptionStateHeader;

/**
 * Subscribe manager for presence watcher info event
 * 
 * @author jexa7410
 */
public class WatcherInfoSubscribeManager extends SubscribeManager {
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings
     */
    public WatcherInfoSubscribeManager(ImsModule parent, RcsSettings rcsSettings) {
        super(parent, rcsSettings);
    }

    /**
     * Returns the presentity
     * 
     * @return Presentity
     */
    public String getPresentity() {
        return ImsModule.getImsUserProfile().getPublicUri();
    }

    /**
     * Create a SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period in milliseconds
     * @return SIP request
     * @throws PayloadException
     */
    @Override
    public SipRequest createSubscribe(SipDialogPath dialog, long expirePeriod)
            throws PayloadException {
        try {
            SipRequest subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod);
            subscribe.addHeader("Event", "presence.winfo");
            subscribe.addHeader("Accept", "application/watcherinfo+xml");
            return subscribe;

        } catch (ParseException e) {
            throw new PayloadException("Failed to create subscribe request!", e);
        }
    }

    /**
     * Receive a notification
     * 
     * @param notify Received notify
     * @throws PayloadException
     * @throws NetworkException
     */
    public void receiveNotification(SipRequest notify) throws PayloadException,
            NetworkException {
        // Check notification
        if (!isNotifyForThisSubscriber(notify)) {
            return;
        }

        if (logger.isActivated()) {
            logger.debug("New watcher-info notification received");
        }

        // Parse XML part
        byte[] content = notify.getContentBytes();
        if (content != null) {
            try {
                InputSource input = new InputSource(new ByteArrayInputStream(content));
                WatcherInfoParser parser = new WatcherInfoParser(input);
                WatcherInfoDocument watcherinfo = parser.getWatcherInfo();
                if (watcherinfo != null) {
                    for (Watcher watcher : watcherinfo.getWatcherList()) {
                        String uri = watcher.getUri();
                        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(uri);
                        if (number == null) {
                            if (logger.isActivated()) {
                                logger.warn("Invalid URI '" + uri + "'");
                            }
                            continue;
                        }
                        ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                        String status = watcher.getStatus();
                        String event = watcher.getEvent();

                        if ((status != null) && (event != null)) {
                            if (status.equalsIgnoreCase("pending")) {
                                // It's an invitation or a new status
                                getImsModule().getPresenceService()
                                        .handlePresenceSharingInvitation(contact);
                            }

                            // Notify listener
                            getImsModule().getPresenceService().handlePresenceSharingNotification(
                                    contact, status, event);
                        }
                    }
                }
            } catch (ParserConfigurationException e) {
                throw new PayloadException("Can't parse watcher-info notification!", e);

            } catch (SAXException e) {
                throw new PayloadException("Can't parse watcher-info notification!", e);

            } catch (IOException e) {
                throw new NetworkException("Can't parse watcher-info notification!", e);
            }
        }

        // Check subscription state
        SubscriptionStateHeader stateHeader = (SubscriptionStateHeader) notify
                .getHeader(SubscriptionStateHeader.NAME);
        if ((stateHeader != null) && stateHeader.getState().equalsIgnoreCase("terminated")) {
            if (logger.isActivated()) {
                logger.info("Watcher-info subscription has been terminated by server");
            }
            terminatedByServer();
        }
    }
}
