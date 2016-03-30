/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.chat;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.event.ConferenceInfoDocument;
import com.gsma.rcs.core.ims.service.im.chat.event.ConferenceInfoParser;
import com.gsma.rcs.utils.logger.Logger;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

public class ConferenceInfoParserTest extends AndroidTestCase {
    private static Logger sLogger = Logger.getLogger(ConferenceInfoParserTest.class.getName());

    private static final String sXmlContentToParse1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<conference-info entity=\"sips:conf233@example.com\" state=\"full\" version=\"1\" xmlns=\"urn:ietf:params:xml:ns:conference-info\">\n"
            + "\t<!-- CONFERENCE INFO -->\n"
            + "\t<conference-description>\n"
            + "\t\t<subject>Agenda: This month's\n"
            + "     goals</subject>\n"
            + "\t\t<service-uris>\n"
            + "\t\t\t<entry>\n"
            + "\t\t\t\t<uri>http://sharepoint/salesgroup/</uri>\n"
            + "\t\t\t\t<purpose>web-page</purpose>\n"
            + "\t\t\t</entry>\n"
            + "\t\t</service-uris>\n"
            + "\t\t<maximum-user-count>50</maximum-user-count>\n"
            + "\t</conference-description>\n"
            + "\t<!-- CONFERENCE STATE-->\n"
            + "\t<conference-state>\n"
            + "\t\t<user-count>33</user-count>\n"
            + "\t</conference-state>\n"
            + "\t<!-- USERS -->\n"
            + "\t<users>\n"
            + "\t\t<!-- USER 1 -->\n"
            + "\t\t<user entity=\"sip:bob@example.com\" state=\"full\">\n"
            + "\t\t\t<display-text>Bob Hoskins</display-text>\n"
            + "\t\t\t<!-- ENDPOINTS -->\n"
            + "\t\t\t<endpoint entity=\"sip:bob@pc33.example.com\">\n"
            + "\t\t\t\t<display-text>Bob's Laptop</display-text>\n"
            + "\t\t\t\t<status>disconnected</status>\n"
            + "\t\t\t\t<disconnection-method>departed</disconnection-method>\n"
            + "\t\t\t\t<disconnection-info>\n"
            + "\t\t\t\t\t<when>2005-03-04T20:00:00Z</when>\n"
            + "\t\t\t\t\t<reason>bad voice quality</reason>\n"
            + "\t\t\t\t\t<by>sip:mike@example.com</by>\n"
            + "\t\t\t\t</disconnection-info>\n"
            + "\t\t\t\t<!-- MEDIA -->\n"
            + "\t\t\t\t<media id=\"1\">\n"
            + "\t\t\t\t\t<display-text>main audio</display-text>\n"
            + "\t\t\t\t\t<type>audio</type>\n"
            + "\t\t\t\t\t<label>34567</label>\n"
            + "\t\t\t\t\t<src-id>432424</src-id>\n"
            + "\t\t\t\t\t<status>sendrecv</status>\n"
            + "\t\t\t\t</media>\n"
            + "\t\t\t</endpoint>\n"
            + "\t\t</user>\n"
            + "\t\t<!-- USER 2-->\n"
            + "\t\t<user entity=\"sip:alice@example.com\" state=\"full\">\n"
            + "\t\t\t<display-text>Alice</display-text>\n"
            + "\t\t\t<!-- ENDPOINTS -->\n"
            + "\t\t\t<endpoint entity=\"sip:4kfk4j392jsu@example.com;grid=433kj4j3u\">\n"
            + "\t\t\t\t<status>connected</status>\n"
            + "\t\t\t\t<joining-method>dialed-out</joining-method>\n"
            + "\t\t\t\t<joining-info>\n"
            + "\t\t\t\t\t<when>2005-03-04T20:00:00Z</when>\n"
            + "\t\t\t\t\t<by>sip:mike@example.com</by>\n"
            + "\t\t\t\t</joining-info>\n"
            + "\t\t\t\t<!-- MEDIA-->\n"
            + "\t\t\t\t<media id=\"1\">\n"
            + "\t\t\t\t\t<display-text>main audio</display-text>\n"
            + "\t\t\t\t\t<type>audio</type>\n"
            + "\t\t\t\t\t<label>34567</label>\n"
            + "\t\t\t\t\t<src-id>534232</src-id>\n"
            + "\t\t\t\t\t<status>sendrecv</status>\n"
            + "\t\t\t\t</media>\n"
            + "\t\t\t</endpoint>\n"
            + "\t\t</user>\n"
            + "\t</users>\n"
            + "</conference-info>";


    public void testGetConferenceInfo() throws ParserConfigurationException, SAXException,
            IOException, ParseFailureException {

        ConferenceInfoParser parser = new ConferenceInfoParser(new InputSource(
                new ByteArrayInputStream(sXmlContentToParse1.getBytes())));
        parser.parse();
        ConferenceInfoDocument confInfoDoc = parser.getConferenceInfo();

        if (sLogger.isActivated()) {
            sLogger.info("conference info URI = " + confInfoDoc.getEntity());
            sLogger.info("conference info state = " + confInfoDoc.getState());
            sLogger.info("conference info users = " + confInfoDoc.getUserCount());
        }
        assertEquals(confInfoDoc.getEntity(), "sips:conf233@example.com");
        assertEquals(confInfoDoc.getState(), "full");
        assertEquals(confInfoDoc.getMaxUserCount(), 50);
        assertEquals(confInfoDoc.getUserCount(), 33);

    }
}
