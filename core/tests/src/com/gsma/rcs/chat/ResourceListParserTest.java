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
import com.gsma.rcs.core.ims.service.im.chat.resourcelist.ResourceListDocument;
import com.gsma.rcs.core.ims.service.im.chat.resourcelist.ResourceListParser;
import com.gsma.rcs.utils.logger.Logger;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

public class ResourceListParserTest extends AndroidTestCase {
    private static final String sXmlContentToParse1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" xmlns:cp=\"urn:ietf:params:xml:ns:copycontrol\">\n"
            + "\t<list>\n"
            + "\t\t<entry cp:copyControl=\"to\" uri=\"sip:bill@example.com\"/>\n"
            + "\t\t<entry cp:copyControl=\"cc\" uri=\"sip:joe@example.org\"/>\n"
            + "\t\t<entry cp:copyControl=\"bcc\" uri=\"sip:ted@example.net\"/>\n"
            + "\t</list>\n"
            + "</resource-lists>";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public void testGetResourceListDocument() throws ParserConfigurationException, SAXException,
            IOException, ParseFailureException {

        ResourceListParser parser = new ResourceListParser(new InputSource(
                new ByteArrayInputStream(sXmlContentToParse1.getBytes())));
        parser.parse();
        ResourceListDocument rlistDoc = parser.getResourceList();
        if (logger.isActivated()) {
            if (rlistDoc.getEntries() != null) {
                logger.info("resources number = " + rlistDoc.getEntries().size());
            } else {
                logger.info("resources list is null");
            }
        }
        assertTrue(rlistDoc.getEntries().contains("sip:bill@example.com"));
        assertTrue(rlistDoc.getEntries().contains("sip:joe@example.org"));
        assertTrue(rlistDoc.getEntries().contains("sip:ted@example.net"));

    }
}
