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

package com.gsma.rcs.core.ims.service.im.chat.event;

import com.gsma.rcs.utils.logger.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Conference-Info parser
 * 
 * @author jexa7410
 */
public class ConferenceInfoParser extends DefaultHandler {

    /*
     * Conference-Info SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <conference-info
     * xmlns="urn:ietf:params:xml:ns:conference-info" entity="sips:conf233@example.com" state="full"
     * version="1"> <!-- CONFERENCE INFO --> <conference-description> <subject>Agenda: This month's
     * goals</subject> <service-uris> <entry> <uri>http://sharepoint/salesgroup/</uri>
     * <purpose>web-page</purpose> </entry> </service-uris>
     * <maximum-user-count>50</maximum-user-count> </conference-description> <!-- CONFERENCE STATE
     * --> <conference-state> <user-count>33</user-count> </conference-state> <!-- USERS --> <users>
     * <!-- USER 1 --> <user entity="sip:bob@example.com" state="full"> <display-text>Bob
     * Hoskins</display-text> <!-- ENDPOINTS --> <endpoint entity="sip:bob@pc33.example.com">
     * <display-text>Bob's Laptop</display-text> <status>disconnected</status>
     * <disconnection-method>departed</disconnection-method> <disconnection-info>
     * <when>2005-03-04T20:00:00Z</when> <reason>bad voice quality</reason>
     * <by>sip:mike@example.com</by> </disconnection-info> <!-- MEDIA --> <media id="1">
     * <display-text>main audio</display-text> <type>audio</type> <label>34567</label>
     * <src-id>432424</src-id> <status>sendrecv</status> </media> </endpoint> </user> <!-- USER 2
     * --> <user entity="sip:alice@example.com" state="full"> <display-text>Alice</display-text>
     * <!-- ENDPOINTS --> <endpoint entity="sip:4kfk4j392jsu@example.com;grid=433kj4j3u">
     * <status>connected</status> <joining-method>dialed-out</joining-method> <joining-info>
     * <when>2005-03-04T20:00:00Z</when> <by>sip:mike@example.com</by> </joining-info> <!-- MEDIA
     * --> <media id="1"> <display-text>main audio</display-text> <type>audio</type>
     * <label>34567</label> <src-id>534232</src-id> <status>sendrecv</status> </media> </endpoint>
     * </user> </users> </conference-info>
     */

    private StringBuffer mAccumulator;

    private ConferenceInfoDocument mConference = null;

    private String mEntity;

    private boolean mMe;

    private String mStatus;

    private String mDisplayName;

    private String mDisconnectionMethod;

    private String mFailureReason;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @throws Exception
     */
    public ConferenceInfoParser(InputSource inputSource) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
    }

    public ConferenceInfoDocument getConferenceInfo() {
        return mConference;
    }

    public void startDocument() {
        if (logger.isActivated()) {
            logger.debug("Start document");
        }
        mAccumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);

        if (localName.equals("conference-info")) {
            String entity = attr.getValue("entity").trim();
            String state = attr.getValue("state").trim();
            mConference = new ConferenceInfoDocument(entity, state);
        } else if (localName.equals("user")) {
            mEntity = attr.getValue("entity").trim();
            String yourown = attr.getValue("yourown");
            mMe = false;
            mStatus = null;
            mDisplayName = null;
            mDisconnectionMethod = null;
            mFailureReason = null;
            if (yourown != null) {
                try {
                    mMe = Boolean.parseBoolean(yourown);
                    /* TODO: Implement proper exception handling in CR037 */
                } catch (Exception e) {
                }
            }
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("user")) {
            if (mConference != null) {
                User user = new User(mEntity, mMe, mStatus, mDisplayName, mDisconnectionMethod,
                        mFailureReason);
                mConference.addUser(user);
            }
        } else if (localName.equals("display-text")) {
            mDisplayName = mAccumulator.toString().trim();
        } else if (localName.equals("status")) {
            mStatus = mAccumulator.toString().trim();
        } else if (localName.equals("maximum-user-count")) {
            if (mConference != null) {
                mConference.setMaxUserCount(Integer.parseInt(mAccumulator.toString().trim()));
            }
        } else if (localName.equals("user-count")) {
            if (mConference != null) {
                mConference.setUserCount(Integer.parseInt(mAccumulator.toString().trim()));
            }
        } else if (localName.equals("conference-info")) {
            if (logger.isActivated()) {
                logger.debug("Conference-Info document complete");
            }
        } else if (localName.equals("disconnection-method")) {
            mDisconnectionMethod = mAccumulator.toString().trim();
        } else if (localName.equals("reason")) {
            mFailureReason = mAccumulator.toString().trim();
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}
