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

package com.gsma.rcs.core.ims.service.presence;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
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

import java.io.ByteArrayInputStream;

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
        return ImsModule.IMS_USER_PROFILE.getPublicUri();
    }

    /**
     * Create a SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period
     * @return SIP request
     * @throws SipException
     */
    public SipRequest createSubscribe(SipDialogPath dialog, int expirePeriod) throws SipException {
        // Create SUBSCRIBE message
        SipRequest subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod);

        // Set the Event header
        subscribe.addHeader("Event", "presence.winfo");

        // Set the Accept header
        subscribe.addHeader("Accept", "application/watcherinfo+xml");

        return subscribe;
    }

    /**
     * Receive a notification
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
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
                                getImsModule().getCore().getListener()
                                        .handlePresenceSharingInvitation(contact);
                            }

                            // Notify listener
                            getImsModule().getCore().getListener()
                                    .handlePresenceSharingNotification(contact, status, event);
                        }
                    }
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't parse watcher-info notification", e);
                }
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
