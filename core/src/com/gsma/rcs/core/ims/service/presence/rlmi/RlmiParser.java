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

package com.gsma.rcs.core.ims.service.presence.rlmi;

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
 * PDIF parser
 * 
 * @author jexa7410
 */
public class RlmiParser extends DefaultHandler {
    /*
     * RLMI SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <list xmlns="urn:ietf:params:xml:ns:rlmi"
     * uri="sip:+33960810101@domain.com;pres-list=rcs" version="1" fullState="true"><name>rcs</name>
     * <resource uri="sip:+33960810100@domain.com"> <instance id="001" state="pending"
     * reason="subscribe"/> </resource> </list>
     */

    private StringBuffer accumulator;
    private ResourceInstance resourceInstance = null;
    private RlmiDocument resourceInfo = null;

    private final InputSource mInputSource;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     */
    public RlmiParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return RlmiParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public RlmiParser parse() throws ParserConfigurationException, SAXException,
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

    public RlmiDocument getResourceInfo() {
        return resourceInfo;
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

        if (localName.equals("list")) {
            String uri = attr.getValue("uri").trim();
            resourceInfo = new RlmiDocument(uri);
        } else if (localName.equals("resource")) {
            String uri = attr.getValue("uri").trim();
            resourceInstance = new ResourceInstance(uri);
        } else if (localName.equals("instance")) {
            String state = attr.getValue("state");
            if ((resourceInstance != null) && (state != null)) {
                resourceInstance.setState(state.trim());
            }
            String reason = attr.getValue("reason");
            if ((resourceInstance != null) && (reason != null)) {
                resourceInstance.setReason(reason.trim());
            }
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("resource")) {
            if (resourceInfo != null) {
                resourceInfo.addResource(resourceInstance);
            }
            resourceInstance = null;
        } else if (localName.equals("list")) {
            if (logger.isActivated()) {
                logger.debug("RLMI document is complete");
            }
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}
