/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.im.chat.resourcelist;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.logger.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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

    private StringBuffer mAccumulator;
    private ResourceListDocument mList;

    private final InputSource mInputSource;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(ResourceListParser.class.getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     */
    public ResourceListParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the resource list
     * 
     * @return ResourceListParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public ResourceListParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(mInputSource, this);
            return this;

        } catch (IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    public ResourceListDocument getResourceList() {
        return mList;
    }

    @Override
    public void startDocument() {
        mAccumulator = new StringBuffer();
    }

    @Override
    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    @Override
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);
        if ("resource-lists".equals(localName)) {
            mList = new ResourceListDocument();

        } else if ("entry".equals(localName)) {
            String uri = attr.getValue("uri").trim();
            mList.addEntry(uri);
        }
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
        if ("resource-lists".equals(localName)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Resource-list document complete");
            }
        }
    }

}
