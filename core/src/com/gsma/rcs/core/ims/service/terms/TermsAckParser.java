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

package com.gsma.rcs.core.ims.service.terms;

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
 * Terms & condition request parser
 * 
 * @author jexa7410
 */
public class TermsAckParser extends DefaultHandler {
    /*
     * SAMPLE: <?xml version="1.0" standalone="yes"?> <EndUserConfirmationAck id="xxxxxxxxx"
     * status="xxxxxxxxxxx"> <Subject>xxxxxxxxxx</Subject> <Text>xxxxxxxxxx</Text>
     * </EndUserConfirmationAck>
     */

    private StringBuffer mAccumulator;

    private String mId;

    private String mStatus;

    private String mSubject;

    private String mText;

    private final InputSource mInputSource;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(TermsAckParser.class.getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @param requestedLanguage
     * @param rcsSettings
     */
    public TermsAckParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the terms ack information
     * 
     * @return TermsAckParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public TermsAckParser parse() throws ParserConfigurationException, SAXException,
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

    public String getId() {
        return mId;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getSubject() {
        return mSubject;
    }

    public String getText() {
        return mText;
    }

    public void startDocument() {
        if (sLogger.isActivated()) {
            sLogger.debug("Start document");
        }
        mAccumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);

        if (localName.equals("EndUserConfirmationAck")) {
            mId = attr.getValue("id").trim();
            mStatus = attr.getValue("status").trim();
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("EndUserConfirmationAck")) {
            if (sLogger.isActivated()) {
                sLogger.debug("Terms request document is complete");
            }
        } else if (localName.equals("Subject")) {
            mSubject = mAccumulator.toString().trim();
        } else if (localName.equals("Text")) {
            mText = mAccumulator.toString().trim();
        }
    }

    public void endDocument() {
        if (sLogger.isActivated()) {
            sLogger.debug("End document");
        }
    }
}
