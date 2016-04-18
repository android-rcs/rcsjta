/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.im.chat.imdn;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.DateUtils;
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
 * IMDN parser (RFC5438)
 */
public class ImdnParser extends DefaultHandler {
    /*
     * IMDN SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <imdn
     * xmlns="urn:ietf:params:xml:ns:imdn"> <message-id>34jk324j</message-id>
     * <datetime>2008-04-04T12:16:49-05:00</datetime> <display-notification> <status> <displayed/>
     * </status> </display-notification> </imdn>
     */
    private StringBuffer accumulator;
    private String mNotificationType;
    private String mStatus;
    private String mMsgId;
    private long mDateTime;
    private final InputSource mInputSource;

    private static final Logger sLogger = Logger.getLogger(ImdnParser.class.getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     */
    public ImdnParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the imdn parser
     * 
     * @return ImdnParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public ImdnParser parse() throws ParserConfigurationException, SAXException,
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
        accumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(localName)) {
            mNotificationType = ImdnDocument.DELIVERY_NOTIFICATION;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(localName)) {
            mNotificationType = ImdnDocument.DISPLAY_NOTIFICATION;
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (ImdnDocument.MESSAGE_ID_TAG.equals(localName)) {
            mMsgId = accumulator.toString();
        } else if (ImdnDocument.IMDN_DATETIME.equals(localName)) {
            mDateTime = DateUtils.decodeDate(accumulator.toString());
        } else if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(localName)) {
            mStatus = ImdnDocument.DELIVERY_STATUS_DELIVERED;
        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(localName)) {
            mStatus = ImdnDocument.DELIVERY_STATUS_DISPLAYED;
        } else if (ImdnDocument.DELIVERY_STATUS_FAILED.equals(localName)) {
            mStatus = ImdnDocument.DELIVERY_STATUS_FAILED;
        } else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(localName)) {
            mStatus = ImdnDocument.DELIVERY_STATUS_ERROR;
        } else if (ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(localName)) {
            mStatus = ImdnDocument.DELIVERY_STATUS_FORBIDDEN;
        }
    }

    public void warning(SAXParseException exception) {
        if (sLogger.isActivated()) {
            sLogger.error("Warning: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }

    public void error(SAXParseException exception) {
        if (sLogger.isActivated()) {
            sLogger.error("Error: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        if (sLogger.isActivated()) {
            sLogger.error("Fatal: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
        throw exception;
    }

    public ImdnDocument getImdnDocument() {
        if (mMsgId == null || mNotificationType == null || mStatus == null) {
            return null;
        }
        return new ImdnDocument(mMsgId, mNotificationType, mStatus, mDateTime);
    }
}
