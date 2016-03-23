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
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

public class ImdnParserTest extends AndroidTestCase {

    private static final Logger sLogger = Logger.getLogger(ImdnParserTest.class.getName());

    // @formatter:off

    private static final String sXmlImdnDeliveredContentToParse =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    "    <imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">\n" +
    "    <message-id>475fd571863a4791baac88e7d3473951</message-id>\n" +
    "    <datetime>2016-03-22T08:10:58.000Z</datetime>\n" +
    "    <delivery-notification>\n" +
    "        <status>\n" +
    "            <delivered/>\n" +
    "        </status>\n" +
    "    </delivery-notification>\n" +
    "</imdn>";

    // @formatter:on

    public void testParseImdnDelivered() throws SAXException, ParserConfigurationException,
            IOException, ParseFailureException {
        ImdnParser parser = new ImdnParser(new InputSource(new ByteArrayInputStream(
                sXmlImdnDeliveredContentToParse.getBytes())));
        parser.parse();
        ImdnDocument imdnDoc = parser.getImdnDocument();
        if (sLogger.isActivated()) {
            sLogger.info("MsgId=" + imdnDoc.getMsgId() + "  status=" + imdnDoc.getStatus());
        }
        assertEquals("475fd571863a4791baac88e7d3473951", imdnDoc.getMsgId());
        assertEquals("delivered", imdnDoc.getStatus());
        assertEquals("delivery-notification", imdnDoc.getNotificationType());
        assertEquals(DateUtils.decodeDate("2016-03-22T08:10:58.000Z"), imdnDoc.getDateTime());
    }
}
