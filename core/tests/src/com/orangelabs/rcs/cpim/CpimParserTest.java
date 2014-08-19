package com.orangelabs.rcs.cpim;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;

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
public class CpimParserTest extends AndroidTestCase {
	/**
	 * CRLF constant
	 */
	private static final String CRLF = "\r\n";

	/**
	 * Double CRLF constant
	 */
	private static final String DOUBLE_CRLF = CRLF + CRLF;

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testCpimParserString() {
		// @formatter:off
		/*
		 * CPIM sample: From: MR SANDERS <im:piglet@100akerwood.com> To:
		 * Depressed Donkey <im:eeyore@100akerwood.com> DateTime:
		 * 2000-12-13T13:40:00-08:00 Subject: the weather will be fine today
		 * 
		 * Content-type: text/plain Content-ID: <1234567890@foo.com>
		 * 
		 * Here is the text of my message.
		 */
		// @formatter:on
		StringBuffer sb = new StringBuffer();
		sb.append("From: MR SANDERS <im:piglet@100akerwood.com>");
		sb.append(CRLF);
		sb.append("To: Depressed Donkey <im:eeyore@100akerwood.com>");
		sb.append(CRLF);
		sb.append("DateTime: 2000-12-13T13:40:00-08:00");
		sb.append(CRLF);
		sb.append("Subject: the weather will be fine today");
		sb.append(DOUBLE_CRLF);
		sb.append("Content-type: text/plain");
		sb.append(CRLF);
		sb.append("Content-ID: <1234567890@foo.com>");
		sb.append(DOUBLE_CRLF);
		sb.append("Here is the text of my message.");
		String text = sb.toString();
		CpimMessage msg = null;
		try {
			msg = (new CpimParser(text)).getCpimMessage();
		} catch (Exception e) {
			fail("no message parsed");
			e.printStackTrace();
		}
		if (msg != null) {
			assertEquals(msg.getHeader("From"), "MR SANDERS <im:piglet@100akerwood.com>");
			assertEquals(msg.getHeader("To"), "Depressed Donkey <im:eeyore@100akerwood.com>");
			assertEquals(msg.getHeader("DateTime"), "2000-12-13T13:40:00-08:00");
			assertEquals(msg.getHeader("Subject"), "the weather will be fine today");
			assertEquals(msg.getContentHeader("Content-ID"), "<1234567890@foo.com>");
			assertEquals(msg.getContentType(), "text/plain");
			assertEquals(msg.getMessageContent(), "Here is the text of my message.");
		}
	}

}
