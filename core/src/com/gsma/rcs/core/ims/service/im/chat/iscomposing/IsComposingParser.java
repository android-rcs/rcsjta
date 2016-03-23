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

package com.gsma.rcs.core.ims.service.im.chat.iscomposing;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.logger.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Is composing event parser (RFC3994)
 */
public class IsComposingParser extends DefaultHandler {
    // @formatter:off
    /* Example of CPIM message having application/im-iscomposing+xml for content type:
    <?xml version="1.0" encoding="utf-8"?>
        <isComposing xmlns="urn:ietf:params:xml:ns:im-iscomposing"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="urn:ietf:params:xml:ns:im-composing iscomposing.xsd">
            <state>active</state>
            <contenttype>text/plain</contenttype>
            <lastactive>2012-02-22T17:53:49.000Z</lastactive>
            <refresh>60</refresh>
    </isComposing>
*/    // @formatter:on

    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    private StringBuilder mAccumulator;

    private IsComposingInfo mComposingInfo;

    private static final Logger sLogger = Logger.getLogger(IsComposingParser.class.getSimpleName());

    private final InputSource mInputSource;

    /**
     * Constructor
     * 
     * @param inputSource Input source
     */
    public IsComposingParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the is composing input
     * 
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public IsComposingParser parse() throws ParserConfigurationException, SAXException,
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

    public void startDocument() {
        mAccumulator = new StringBuilder();
    }

    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);
        if ("isComposing".equals(localName)) {
            mComposingInfo = new IsComposingInfo();
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        switch (localName) {
            case "state":
                if (mComposingInfo != null) {
                    mComposingInfo.setState(mAccumulator.toString());
                }
                break;
            case "lastactive":
                if (mComposingInfo != null) {
                    mComposingInfo.setLastActiveDate(mAccumulator.toString());
                }
                break;
            case "contenttype":
                if (mComposingInfo != null) {
                    mComposingInfo.setContentType(mAccumulator.toString());
                }
                break;
            case "refresh":
                if (mComposingInfo != null) {
                    long time = Long.parseLong(mAccumulator.toString())
                            * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
                    mComposingInfo.setRefreshTime(time);
                }
                break;
            case "isComposing":
                if (sLogger.isActivated()) {
                    sLogger.debug("Watcher document is complete");
                }
                break;
        }
    }

    public void endDocument() {
    }

    public void warning(SAXParseException exception) {
        if (sLogger.isActivated()) {
            sLogger.error("Warning: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }

    public void error(SAXParseException exception) {
        if (sLogger.isActivated()) {
            sLogger.error("Error: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        if (sLogger.isActivated()) {
            sLogger.error("Fatal: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
        throw exception;
    }

    public IsComposingInfo getIsComposingInfo() {
        return mComposingInfo;
    }
}
