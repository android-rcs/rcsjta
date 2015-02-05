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

package com.orangelabs.rcs.core.ims.service.im.chat.resourcelist;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Resource list parser
 * 
 * @author jexa7410
 */
public class ResourceListParser extends DefaultHandler {

    /*
     * Resource-List SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <resource-lists
     * xmlns="urn:ietf:params:xml:ns:resource-lists" xmlns:cp="urn:ietf:params:xml:ns:copycontrol">
     * <list> <entry uri="sip:bill@example.com" cp:copyControl="to" /> <entry
     * uri="sip:joe@example.org" cp:copyControl="cc" /> <entry uri="sip:ted@example.net"
     * cp:copyControl="bcc" /> </list> </resource-lists>
     */

    private StringBuffer accumulator;
    private ResourceListDocument list = null;

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
    public ResourceListParser(InputSource inputSource) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
    }

    public ResourceListDocument getResourceList() {
        return list;
    }

    public void startDocument() {
        if (logger.isActivated()) {
            logger.debug("Start document");
        }
        accumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);

        if (localName.equals("resource-lists")) {
            list = new ResourceListDocument();
        } else if (localName.equals("entry")) {
            String uri = attr.getValue("uri").trim();
            list.addEntry(uri);
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("resource-lists")) {
            if (logger.isActivated()) {
                logger.debug("Resource-list document complete");
            }
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}
