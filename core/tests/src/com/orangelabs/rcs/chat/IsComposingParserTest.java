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

import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingParser;
import com.orangelabs.rcs.utils.logger.Logger;

public class IsComposingParserTest extends AndroidTestCase {

	// @formatter:off
	/*
	 * IsComposing SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <isComposing
	 * xmlns="urn:ietf:params:xml:ns:im-iscomposing"
	 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 * xsi:schemaLocation="urn:ietf:params:xml:ns:im-composing iscomposing.xsd">
	 * <state>idle</state> <lastactive>2003-01-27T10:43:00Z</lastactive>
	 * <contenttype>audio</contenttype> </isComposing>
	 */
	// @formatter:on
	private Logger logger = Logger.getLogger(this.getClass().getName());

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsComposingParser() {
		StringBuffer sb = new StringBuffer("<?xml version=\"1.08\" encoding=\"UTF-8\"?>");
		sb.append("<isComposing xmlns=\"urn:ietf:params:xml:ns:im-isComposing\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
		sb.append("xsi:schemaLocation=\"urn:ietf:params:xml:ns:im-composing iscomposing.xsd\">");
		sb.append("<state>idle</state>");
		sb.append("<lastactive>2008-12-13T13:40:00Z</lastactive>");
		sb.append("<contenttype>audio</contenttype> </isComposing>");
		String xml = sb.toString();
		try {
			InputSource inputso = new InputSource(new ByteArrayInputStream(xml.getBytes()));
			IsComposingParser parser = new IsComposingParser(inputso);
			IsComposingInfo isInfo = parser.getIsComposingInfo();
			assertEquals(isInfo.getContentType(), "audio");
			assertEquals(isInfo.isStateActive(), false);
			if (logger.isActivated()) {
				logger.info("isComposing lastActiveDate = " + isInfo.getLastActiveDate());
			}
		} catch (Exception e) {
			fail("IsComposing source parsed");
			e.printStackTrace();
		}
	}
}
