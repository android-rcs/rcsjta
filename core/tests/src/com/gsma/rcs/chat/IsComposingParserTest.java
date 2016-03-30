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
 **
 /**
 * Created by yplo6403 on 24/02/2016.
 */
package com.gsma.rcs.chat;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.gsma.rcs.core.ims.service.im.chat.iscomposing.IsComposingParser;
import com.gsma.rcs.utils.DateUtils;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.ParserConfigurationException;

public class IsComposingParserTest extends AndroidTestCase {

    private static final String sXmlContentToParse1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "        <isComposing xmlns=\"urn:ietf:params:xml:ns:im-iscomposing\""
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + "        xsi:schemaLocation=\"urn:ietf:params:xml:ns:im-composing iscomposing.xsd\">"
            + "            <state>active</state>"
            + "            <contenttype>text/plain</contenttype>"
            + "            <lastactive>2012-02-22T17:53:49.000Z</lastactive>"
            + "            <refresh>60</refresh>" + "    </isComposing>";

    private static final String sXmlContentToParse2 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "        <isComposing xmlns=\"urn:ietf:params:xml:ns:im-iscomposing\""
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + "        xsi:schemaLocation=\"urn:ietf:params:xml:ns:im-composing iscomposing.xsd\">"
            + "            <state>idle</state>"
            + "            <contenttype>text/plain</contenttype>"
            + "            <lastactive>2012-02-22T17:53:49.000Z</lastactive>"
            + "            <refresh>60</refresh>" + "    </isComposing>";

    public void testIsComposingParserActive() throws ParseFailureException, SAXException,
            ParserConfigurationException {
        IsComposingParser parser = new IsComposingParser(new InputSource(new ByteArrayInputStream(
                sXmlContentToParse1.getBytes())));
        parser.parse();
        IsComposingInfo info = parser.getIsComposingInfo();
        assertEquals("text/plain", info.getContentType());
        assertEquals(60000, info.getRefreshTime());
        assertEquals(true, info.isStateActive());
        assertEquals(DateUtils.decodeDate("2012-02-22T17:53:49.000Z"), info.getLastActiveDate());
    }

    public void testIsComposingParserIdle() throws ParseFailureException, SAXException,
            ParserConfigurationException {
        IsComposingParser parser = new IsComposingParser(new InputSource(new ByteArrayInputStream(
                sXmlContentToParse2.getBytes())));
        parser.parse();
        IsComposingInfo info = parser.getIsComposingInfo();
        assertEquals("text/plain", info.getContentType());
        assertEquals(60000, info.getRefreshTime());
        assertEquals(false, info.isStateActive());
        assertEquals(DateUtils.decodeDate("2012-02-22T17:53:49.000Z"), info.getLastActiveDate());
    }

}
