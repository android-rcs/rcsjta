/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.im.chat;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.im.chat.resourcelist.ResourceListDocument;
import com.gsma.rcs.core.ims.service.im.chat.resourcelist.ResourceListParser;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Utilities for ParticipantInfo
 * 
 * @author YPLO6403
 */
public class ParticipantInfoUtils {

    private static final Logger sLogger = Logger.getLogger(ParticipantInfoUtils.class
            .getSimpleName());

    /**
     * Create a set of participant info from XML
     * 
     * @param xml Resource-list document in XML
     * @param status Participant info status
     * @return the set of participants
     * @throws PayloadException
     */
    public static Map<ContactId, ParticipantStatus> parseResourceList(String xml,
            ParticipantStatus status) throws PayloadException {
        Map<ContactId, ParticipantStatus> participants = new HashMap<ContactId, ParticipantStatus>();
        try {
            InputSource pidfInput = new InputSource(new ByteArrayInputStream(xml.getBytes(UTF8)));
            ResourceListParser listParser = new ResourceListParser(pidfInput).parse();
            ResourceListDocument resList = listParser.getResourceList();
            if (resList != null) {
                for (String entry : resList.getEntries()) {
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(entry);
                    if (number == null) {
                        continue;
                    }
                    ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                    if (!contact.equals(ImsModule.getImsUserProfile().getUsername())) {
                        participants.put(contact, status);
                        if (sLogger.isActivated()) {
                            sLogger.debug("Add participant " + contact + " to the list");
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            throw new PayloadException("Can't parse resource-list document!", e);

        } catch (SAXException e) {
            throw new PayloadException("Can't parse resource-list document!", e);

        } catch (ParseFailureException e) {
            throw new PayloadException("Can't parse resource-list document!", e);
        }
        return participants;
    }
}
