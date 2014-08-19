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

import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.orangelabs.rcs.utils.logger.Logger;

public class ImdnParserTest extends AndroidTestCase {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String CRLF = "\r\n";

	// @formatter:off
	/* IMDN SAMPLE:
	   <?xml version="1.0" encoding="UTF-8"?>
	   <imdn xmlns="urn:ietf:params:xml:ns:imdn">
		<message-id>34jk324j</message-id>
		<datetime>2008-04-04T12:16:49-05:00</datetime>
		<display-notification>
		    <status>
		       <displayed/>
		    </status>
		</display-notification>
    </imdn>
	*/
	// @formatter:on

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetImdnDocument() {
		/**
		 * Parse a delivery report
		 * 
		 * @param xml
		 *            XML document
		 * @return IMDN document
		 */
		StringBuffer sb = new StringBuffer("<?xml version=\"1.08\" encoding=\"UTF-8\"?>");
		sb.append("<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">");
		sb.append("<message-id>34jk324j</message-id>");
		sb.append("DateTime: 2008-12-13T13:40:00-08:00");
		sb.append("<display-notification>");
		sb.append(CRLF);
		sb.append("<status>");
		sb.append(CRLF);
		sb.append("<displayed/>");
		sb.append(CRLF);
		sb.append("</status>");
		sb.append(CRLF);
		sb.append("</display-notification>");
		sb.append(CRLF);
		sb.append("</imdn>");
		String xml = sb.toString();
		try {
			InputSource inputso = new InputSource(new ByteArrayInputStream(xml.getBytes()));
			ImdnParser parser = new ImdnParser(inputso);
			ImdnDocument imdnDoc = parser.getImdnDocument();
			if (logger.isActivated()) {
				logger.info("MsgId=" + imdnDoc.getMsgId() + "  status=" + imdnDoc.getStatus());
			}
			assertEquals(imdnDoc.getMsgId(), "34jk324j");
			assertEquals(imdnDoc.getStatus(), "displayed");
		} catch (Exception e) {
			fail("no Imdn source parsed");
			e.printStackTrace();
		}
	}
}
