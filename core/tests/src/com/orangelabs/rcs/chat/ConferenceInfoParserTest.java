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
package com.orangelabs.rcs.chat;

import java.io.ByteArrayInputStream;

import org.xml.sax.InputSource;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceInfoParser;
import com.orangelabs.rcs.utils.logger.Logger;

public class ConferenceInfoParserTest extends AndroidTestCase {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String CRLF = "\r\n";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	// @formatter:off
	/*
	 * Conference-Info SAMPLE:
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <conference-info xmlns="urn:ietf:params:xml:ns:conference-info"
	 * entity="sips:conf233@example.com" state="full" version="1">
	 * <!-- CONFERENCE INFO -->
	 * <conference-description>
	 * <subject>Agenda: This month's goals</subject>
	 * <service-uris><entry>
	 * <uri>http://sharepoint/salesgroup/</uri> <purpose>web-page</purpose>
	 * </entry></service-uris>
	 * <maximum-user-count>50</maximum-user-count>
	 * </conference-description>
	 * 
	 * <!-- CONFERENCE STATE -->
	 * <conference-state> <user-count>33</user-count></conference-state>
	 * 
	 * <!-- USERS -->
	 * <users>
	 * <!-- USER 1 -->
	 * <user entity="sip:bob@example.com" state="full"> <display-text>Bob Hoskins</display-text>
	 * <!-- ENDPOINTS -->
	 * <endpoint entity="sip:bob@pc33.example.com"> <display-text>Bob's Laptop</display-text>
	 * <status>disconnected</status>
	 * <disconnection-method>departed</disconnection-method>
	 * <disconnection-info>
	 * <when>2005-03-04T20:00:00Z</when> <reason>bad voice quality</reason>
	 * <by>sip:mike@example.com</by> </disconnection-info>
	 * <!-- MEDIA -->
	 * <media id="1"> <display-text>main audio</display-text>
	 * <type>audio</type> <label>34567</label> <src-id>432424</src-id>
	 * <status>sendrecv</status></media></endpoint> </user>
	 * 
	 * <!-- USER 2 -->
	 * <user entity="sip:alice@example.com" state="full">
	 * <display-text>Alice</display-text>
	 * <!-- ENDPOINTS -->
	 * <endpoint entity="sip:4kfk4j392jsu@example.com;grid=433kj4j3u">
	 * <status>connected</status>
	 * <joining-method>dialed-out</joining-method>
	 * <joining-info><when>2005-03-04T20:00:00Z</when>
	 * <by>sip:mike@example.com</by></joining-info>
	 * <!-- MEDIA --> <media
	 * id="1"> <display-text>main audio</display-text> <type>audio</type>
	 * <label>34567</label> <src-id>534232</src-id> <status>sendrecv</status>
	 * </media> </endpoint> </user> </users> </conference-info>
	 */
	// @formatter:on

	public void testGetConferenceInfo() {
		StringBuffer sb = new StringBuffer("<?xml version=\"1.08\" encoding=\"UTF-8\"?>");
		sb.append(CRLF);
		sb.append("<conference-info xmlns=\"urn:ietf:params:xml:ns:conference-info\" entity=\"sips:conf233@example.com\" state=\"full\" version=\"1\">");
		sb.append(CRLF);
		sb.append("<conference-description>");
		sb.append("<subject>Agenda: This month's goals</subject>");
		sb.append("<service-uris>");
		sb.append("<entry> <uri>http://sharepoint/salesgroup/</uri> <purpose>web-page</purpose> </entry>");
		sb.append("</service-uris> <maximum-user-count>50</maximum-user-count>");
		sb.append("</conference-description>");
		sb.append(CRLF);
		sb.append("<conference-state>");
		sb.append("<user-count>33</user-count> </conference-state> ");
		sb.append(CRLF);
		sb.append("<users>");
		sb.append("<user entity=\"sip:bob@example.com\" state=\"full\"> ");
		sb.append("<display-text>Bob Hoskins</display-text> ");
		sb.append("<endpoint entity=\"sip:4kfk4j392jsu@example.com;grid=433kj4j3u\"> ");
		sb.append("<display-text>Bob's Laptop</display-text> ");
		sb.append("<status>disconnected</status> <disconnection-method>departed</disconnection-method> ");
		sb.append("<disconnection-info> <when>2005-03-04T20:00:00Z</when> ");
		sb.append("<by>sip:mike@example.com</by> </disconnection-info> ");
		sb.append("<media id=\"1\"> <display-text>main audio</display-text> ");
		sb.append("<type>audio</type> <label>34567</label> <src-id>534232</src-id> ");
		sb.append("<status>sendrecv</status> </media> </endpoint> </user> ");
		sb.append(CRLF);
		sb.append("<user entity=\"sip:alice@example.com\" state=\"full\"> ");
		sb.append("<display-text>Alice</display-text> ");
		sb.append("<endpoint entity=\"sip:4kfk4j392jsu@example.com;grid=433kj4j3u\"> ");
		sb.append("<status>connected</status> <joining-method>dialed-out</joining-method> ");
		sb.append("<joining-info> <when>2005-03-04T20:00:00Z</when> ");
		sb.append("<by>sip:mike@example.com</by> </joining-info> ");
		sb.append("<media id=\"1\"> <display-text>main audio</display-text> ");
		sb.append("<type>audio</type> <label>34567</label> <src-id>534232</src-id> ");
		sb.append("<status>sendrecv</status> </media> </endpoint> </user> ");
		sb.append("</users> </conference-info> ");
		String xml = sb.toString();
		try {
			InputSource inputso = new InputSource(new ByteArrayInputStream(xml.getBytes()));
			ConferenceInfoParser parser = new ConferenceInfoParser(inputso);
			ConferenceInfoDocument confInfoDoc = parser.getConferenceInfo();
			if (logger.isActivated()) {
				logger.info("conference info URI = " + confInfoDoc.getEntity());
				logger.info("conference info state = " + confInfoDoc.getState());
				logger.info("conference info users = " + confInfoDoc.getUserCount());
			}
			assertEquals(confInfoDoc.getEntity(), "sips:conf233@example.com");
			assertEquals(confInfoDoc.getState(), "full");
			assertEquals(confInfoDoc.getMaxUserCount(), 50);
			assertEquals(confInfoDoc.getUserCount(), 33);
		} catch (Exception e) {
			fail("no Conference info source parsed");
			e.printStackTrace();
		}
	}
}
