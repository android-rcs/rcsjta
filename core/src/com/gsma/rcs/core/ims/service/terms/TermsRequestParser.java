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

package com.gsma.rcs.core.ims.service.terms;

import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * End User Confirmation Request request parser. Parse message of type
 * end-user-confirmation-request. If the message contains the text in different languages it's
 * returns the text in the requested language if present or in the default language (English).
 * 
 * @author jexa7410
 * @author Deutsche Telekom AG
 */
public class TermsRequestParser extends DefaultHandler {
    /*
     * SAMPLE: <?xml version="1.0" standalone="yes"?> <EndUserConfirmationRequest id="xxxxxxx"
     * type="xxxxxxx" pin="xxxxxx" timeout="120"> <Subject xml:lang="en">xxxxxxxxxx</Subject>
     * <Subject xml:lang="de">xxxxxxxxxx</Subject> <Subject xml:lang="es">xxxxxxxxxx</Subject> <Text
     * xml:lang="en">xxxxxxxxxx</Text> <Text xml:lang="de">xxxxxxxxxx</Text> <Text
     * xml:lang="es">xxxxxxxxxx</Text> <ButtonAccept xml:lang="en">xxxxxxxxxx</ButtonAccept>
     * <ButtonAccept xml:lang="de">xxxxxxxxxx</ButtonAccept> <ButtonAccept
     * xml:lang="es">xxxxxxxxxx</ButtonAccept> <ButtonReject xml:lang="en">xxxxxxxxxx</ButtonReject>
     * <ButtonReject xml:lang="de">xxxxxxxxxx</ButtonReject> <ButtonReject
     * xml:lang="es">xxxxxxxxxx</ButtonReject> </EndUserConfirmationRequest>
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
     * Value off attribute 'id' off element 'EndUserConfirmationRequest'
     */
    private String mId;

    /**
     * Value off attribute 'type' off element 'EndUserNotification'
     */
    private String mType;

    /**
     * Value off attribute 'timeout' off element 'EndUserNotification'
     */
    private int mTimeout;

    /**
     * Value off attribute 'pin' off element 'EndUserConfirmationRequest'
     */
    private boolean mPin = false;

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
    private boolean mIsFirstSubjectParsed = false;

    /**
     * Value of language attribute of current xml element during parsing
     */
    private String mCurrentLangAttribute;

    /**
     * HashMap<('ElementName' + 'Language'), text>
     */
    private HashMap<String, String> mElementMap = new HashMap<String, String>();

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @param requestedLanguage
     * @throws Exception
     */
    public TermsRequestParser(InputSource inputSource, String requestedLanguage,
            RcsSettings rcsSettings) throws Exception {
        mRequestedLanguage = requestedLanguage;
        mRcsSettings = rcsSettings;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
    }

    public String getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    public int getTimeout() {
        return mTimeout;
    }

    public boolean getPin() {
        return mPin;
    }

    public String getSubject() {
        return giveTextInBestLanguage("Subject");
    }

    public String getText() {
        return giveTextInBestLanguage("Text");
    }

    public String getButtonAccept() {
        return giveTextInBestLanguage("ButtonAccept");
    }

    public String getButtonReject() {
        return giveTextInBestLanguage("ButtonReject");
    }

    public void startDocument() {
        if (logger.isActivated()) {
            logger.debug("Start document 'EndUserConfirmationRequest'");
        }
        mAccumulator = new StringBuffer();
    }

    @Override
    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    @Override
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);

        if (localName.equals("EndUserConfirmationRequest")) {
            mId = attr.getValue("id").trim();
            mType = attr.getValue("type").trim();
            if (mType.equalsIgnoreCase("Volatile")) {
                try {
                    mTimeout = 1000 * Integer.parseInt(attr.getValue("timeout").trim());
                } catch (Exception e) {
                    // If the attribute timeout is not present a default value of 64*T1 seconds
                    // (with T1 as defined in
                    // [RFC3261]) shall be used
                    if (mRcsSettings != null) {
                        mTimeout = mRcsSettings.getSipTimerT1() * 64;
                    } else {
                        // T1 is an estimate of the round-trip time (RTT), and it defaults to 500
                        // ms.
                        mTimeout = 500 * 64;
                    }
                }
            } else { // type.equalsIgnoreCase('Persistent')
                mTimeout = -1; // means infinite ( no timeout)
            }

            // Get optional attribute pin
            mPin = false; // Default value according to RCSe spec 1.2.2
            String pinAttr = attr.getValue("pin");
            if (pinAttr != null) {
                mPin = Boolean.parseBoolean(attr.getValue("pin").trim());
            }

        } else { // check lang attribute for all sub elements
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
            if (!mIsFirstSubjectParsed) {
                mIsFirstSubjectParsed = true;
                mFirstLanguage = mCurrentLangAttribute;
            }
        }
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("EndUserConfirmationRequest")) {
            if (logger.isActivated()) {
                logger.debug("Terms request document is complete");
            }
        } else if (mCurrentLangAttribute.equals(mRequestedLanguage)
                || mCurrentLangAttribute.equals(DEFAULT_LANGUAGE)
                || mCurrentLangAttribute.equals(mFirstLanguage) || mCurrentLangAttribute.equals("")) {
            mElementMap.put(qname + mCurrentLangAttribute, mAccumulator.toString().trim());
        }
    }

    /**
     * Returns text part off xml element, if found for requested language ('xml:lang' attribute ==
     * requestedLanguage) or with 'xml:lang' is equal "en" (english) or with 'xml:lang' attribute
     * equals to this from the first 'Subject' element or the text from the element with out any
     * 'xml:lang' attribute or null if element not found
     * 
     * @param elementName
     * @return
     */
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
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}
