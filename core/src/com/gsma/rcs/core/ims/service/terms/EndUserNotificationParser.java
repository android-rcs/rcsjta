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
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * End user notification request parser. If the message contains the text in different languages
 * it's returns the text in the requested language if present or in the default language (English).
 * 
 * @author Deutsche Telekom AG
 */
public class EndUserNotificationParser extends DefaultHandler {
    /*
     * SAMPLE: <?xml version="1.0" standalone="yes"?> <EndUserNotification id="xxxxxxxxx"> <Subject
     * xml:lang="en">xxxxxxxxxx</Subject> <Subject xml:lang="de">xxxxxxxxxx</Subject> <Subject
     * xml:lang="es">xxxxxxxxxx</Subject> <Text xml:lang="en">xxxxxxxxxx</Text> <Text
     * xml:lang="de">xxxxxxxxxx</Text> <Text xml:lang="es">xxxxxxxxxx</Text> <ButtonOK
     * xml:lang="en">xxxxxxxxxx</ButtonOK> <ButtonOK xml:lang="de">xxxxxxxxxx</ButtonOK> <ButtonOK
     * xml:lang="es">xxxxxxxxxx</ButtonOK> </EndUserNotification>
     */

    /**
     * Default language is English
     */
    private final static String DEFAULT_LANGUAGE = "en";

    /**
     * Char buffer for parsing text from one element
     */
    private StringBuffer mAccumulator;

    /**
     * Value off attribute 'id' off element 'EndUserNotification'
     */
    private String mId;

    /**
     * Requested language (given in constructor)
     */
    private String mRequestedLanguage;

    /**
     * Language from the first 'Subject' element
     */
    private String mFirstLanguage;

    /**
     * Flag if variable 'firstLanguage' is set
     */
    private boolean isFirstSubjectParsed;

    /**
     * Value of language attribute of current xml element during parsing
     */
    private String mCurrentLangAttribute;

    /**
     * HashMap<('ElementName' + 'Language'), text>
     */
    private final HashMap<String, String> mElementMap;

    private final InputSource mInputSource;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(EndUserNotificationParser.class
            .getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @param requestedLanguage requested language
     */
    public EndUserNotificationParser(InputSource inputSource, String requestedLanguage) {
        mRequestedLanguage = requestedLanguage;
        mElementMap = new HashMap<String, String>();
        mInputSource = inputSource;
    }

    /**
     * Parse the end user notification information
     * 
     * @return EndUserNotificationParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public EndUserNotificationParser parse() throws ParserConfigurationException, SAXException,
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

    public String getSubject() {
        return giveTextInBestLanguage("Subject");
    }

    public String getText() {
        return giveTextInBestLanguage("Text");
    }

    public String getButtonOk() {
        return giveTextInBestLanguage("ButtonOK");
    }

    public void startDocument() {
        if (sLogger.isActivated()) {
            sLogger.debug("Start document 'EndUserNotification'.");
        }
        mAccumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);

        if (qname.equals("EndUserNotification")) {
            mId = attr.getValue("id").trim();
        } else {
            mCurrentLangAttribute = attr.getValue("xml:lang");
            if (mCurrentLangAttribute == null) {
                // for xml failure tolerance
                mCurrentLangAttribute = attr.getValue("lang");
            }

            if (mCurrentLangAttribute == null) {
                // to avoid null pointer exception
                mCurrentLangAttribute = "";
            }
            // put to lower case for xml failure tolerance
            mCurrentLangAttribute = mCurrentLangAttribute.trim().toLowerCase();
            if (!isFirstSubjectParsed) {
                isFirstSubjectParsed = true;
                mFirstLanguage = mCurrentLangAttribute;
            }
        }

    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (qname.equals("EndUserNotification")) {
            if (sLogger.isActivated()) {
                sLogger.debug("EndUserNotification document is complete");
            }
        } else if (mCurrentLangAttribute.equals(mRequestedLanguage)
                || mCurrentLangAttribute.equals(DEFAULT_LANGUAGE)
                || mCurrentLangAttribute.equals(mFirstLanguage) || mCurrentLangAttribute.equals("")) {
            mElementMap.put(qname + mCurrentLangAttribute, mAccumulator.toString().trim());
        }
    }

    // Returns text part off xml element,
    // if found for requested language ('xml:lang' attribute == requestedLanguage)
    // or with 'xml:lang' is equal "en" (english)
    // or with 'xml:lang' attribute equals to this from the first 'Subject' element
    // or the text from the element with out any 'xml:lang' attribute
    // or null if element not found
    private String giveTextInBestLanguage(String elementName) {
        if (mElementMap.containsKey(elementName + mRequestedLanguage)) {
            return mElementMap.get(elementName + mRequestedLanguage);

        } else if (mElementMap.containsKey(elementName + DEFAULT_LANGUAGE)) {
            return mElementMap.get(elementName + DEFAULT_LANGUAGE);

        } else if (mElementMap.containsKey(elementName + mFirstLanguage)) {
            return mElementMap.get(elementName + mFirstLanguage);

        } else {
            return mElementMap.get(elementName);
        }
    }

    public void endDocument() {
        if (sLogger.isActivated()) {
            sLogger.debug("End document: 'EndUserNotification'");
        }
    }
}
