/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfParser;
import com.gsma.rcs.core.ims.service.presence.rlmi.ResourceInstance;
import com.gsma.rcs.core.ims.service.presence.rlmi.RlmiDocument;
import com.gsma.rcs.core.ims.service.presence.rlmi.RlmiParser;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.header.AcceptHeader;
import javax2.sip.header.EventHeader;
import javax2.sip.header.SubscriptionStateHeader;
import javax2.sip.header.SupportedHeader;

/**
 * Subscribe manager for presence event
 * 
 * @author jexa7410
 */
public class PresenceSubscribeManager extends SubscribeManager {
    /**
     * The logger
     */
    private static final Logger sLogger = Logger
            .getLogger(PresenceSubscribeManager.class.getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings
     */
    public PresenceSubscribeManager(ImsModule parent, RcsSettings rcsSettings) {
        super(parent, rcsSettings);
    }

    /**
     * Returns the presentity
     * 
     * @return Presentity
     */
    public String getPresentity() {
        return ImsModule.getImsUserProfile().getPublicUri().concat(";pres-list=rcs");
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
            subscribe.addHeader(EventHeader.NAME, "presence");
            subscribe.addHeader(AcceptHeader.NAME,
                    "application/pidf+xml, application/rlmi+xml, multipart/related");
            subscribe.addHeader(SupportedHeader.NAME, "eventlist");
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
    public void receiveNotification(SipRequest notify) throws PayloadException, NetworkException {
        if (!isNotifyForThisSubscriber(notify)) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("New presence notification received");
        }
        String content = notify.getContent();
        if (TextUtils.isEmpty(content)) {
            throw new PayloadException("Presence notification content should not be null or empty!");
        }
        try {
            String boundary = notify.getBoundaryContentType();
            Multipart multi = new Multipart(content, boundary);
            if (!multi.isMultipart()) {
                throw new PayloadException("Presence notification content not multipart!");
            }
            String rlmiPart = multi.getPart("application/rlmi+xml");
            if (rlmiPart != null) {
                InputSource rlmiInput = new InputSource(new ByteArrayInputStream(
                        rlmiPart.getBytes(UTF8)));
                RlmiParser rlmiParser = new RlmiParser(rlmiInput).parse();
                RlmiDocument rlmiInfo = rlmiParser.getResourceInfo();
                Vector<ResourceInstance> list = rlmiInfo.getResourceList();
                for (ResourceInstance res : list) {
                    String uri = res.getUri();
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(uri);
                    if (number == null) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Invalid uri '" + uri + "'");
                        }
                        continue;
                    }
                    ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                    String state = res.getState();
                    String reason = res.getReason();

                    if ((state != null) && (reason != null)) {
                        if (state.equalsIgnoreCase("terminated")
                                && reason.equalsIgnoreCase("rejected")) {
                            /*
                             * It's a "terminated" event with status "rejected" the contact should
                             * be removed from the "rcs" list
                             */
                            getImsModule().getPresenceService().getXdmManager()
                                    .removeContactFromGrantedList(contact);
                        }
                        getImsModule().getPresenceService().handlePresenceSharingNotification(
                                contact, state, reason);
                    }
                }
            }
            String pidfPart = multi.getPart("application/pidf+xml");
            InputSource pidfInput = new InputSource(new ByteArrayInputStream(
                    pidfPart.getBytes(UTF8)));
            PidfParser pidfParser = new PidfParser(pidfInput).parse();
            PidfDocument presenceInfo = pidfParser.getPresence();
            String entity = presenceInfo.getEntity();
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(entity);
            if (number == null) {
                throw new PayloadException(new StringBuilder("Invalid entity :").append(entity)
                        .toString());
            }
            ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
            getImsModule().getPresenceService().handlePresenceInfoNotification(contact,
                    presenceInfo);
        } catch (ParserConfigurationException e) {
            throw new PayloadException("Can't parse presence notification!", e);

        } catch (SAXException e) {
            throw new PayloadException("Can't parse presence notification!", e);

        } catch (ParseFailureException e) {
            throw new PayloadException("Can't parse presence notification!", e);
        }

        SubscriptionStateHeader stateHeader = (SubscriptionStateHeader) notify
                .getHeader(SubscriptionStateHeader.NAME);
        if ((stateHeader != null) && stateHeader.getState().equalsIgnoreCase("terminated")) {
            if (sLogger.isActivated()) {
                sLogger.info("Presence subscription has been terminated by server");
            }
            terminatedByServer();
        }
    }
}
