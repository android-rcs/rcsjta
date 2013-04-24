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

package com.orangelabs.rcs.core.ims.service.terms;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * End user notification request parser. If the message contains the text in
 * different languages it's returns the text in the requested language if
 * present or in the default language (English).
 *
 * @author Deutsche Telekom AG
 */
public class EndUserNotificationParser extends DefaultHandler {
    /*
     * SAMPLE: <?xml version="1.0" standalone="yes"?> 
     * <EndUserNotification id="xxxxxxxxx"> 
     *   <Subject xml:lang="en">xxxxxxxxxx</Subject> 
     *   <Subject xml:lang="de">xxxxxxxxxx</Subject> 
     *   <Subject xml:lang="es">xxxxxxxxxx</Subject> 
     *   <Text xml:lang="en">xxxxxxxxxx</Text> 
     *   <Text xml:lang="de">xxxxxxxxxx</Text> 
     *   <Text xml:lang="es">xxxxxxxxxx</Text> 
     *   <ButtonOK xml:lang="en">xxxxxxxxxx</ButtonOK>
     *   <ButtonOK xml:lang="de">xxxxxxxxxx</ButtonOK>
     *   <ButtonOK xml:lang="es">xxxxxxxxxx</ButtonOK>
     * </EndUserNotification>
     */

    /**
     * Default language is English
     */
    private final static String DEFAULT_LANGUAGE = "en";

    /**
     * Char buffer for parsing text from one element
     */
    private StringBuffer accumulator;

    /**
     * Value off attribute 'id' off element 'EndUserNotification'
     */
    private String id = null;

    /**
     * Requested language (given in constructor)
     */
    private String requestedLanguage = null;

    /**
     * Language from the first 'Subject' element
     */
    private String firstLanguage = null;

    /**
     * Flag if variable 'firstLanguage' is set
     */
    private boolean isFirstSubjectParsed = false;

    /**
     * Value of language attribute of current xml element during parsing
     */
    private String currentLangAttribute = null;

    /**
     * HashMap<('ElementName' + 'Language'), text>
     */
    private HashMap<String, String> elementMap = new HashMap<String, String>();

    /** 
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param inputSource Input source
     * @param requestedLanguage requested language
     * @throws Exception
     */
    public EndUserNotificationParser(InputSource inputSource, String requestedLanguage) throws Exception {
        this.requestedLanguage = requestedLanguage;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
    }

    public String getId() {
        return id;
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
        if (logger.isActivated()) {
            logger.debug("Start document 'EndUserNotification'.");
        }
        accumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);

        if (qname.equals("EndUserNotification")) {
            id = attr.getValue("id").trim();
        } else {
            currentLangAttribute = attr.getValue("xml:lang");
            if (currentLangAttribute == null) {
                // for xml failure tolerance
                currentLangAttribute = attr.getValue("lang");
            }

            if (currentLangAttribute == null) {
                // to avoid null pointer exception
                currentLangAttribute = "";
            }
            // put to lower case for xml failure tolerance
            currentLangAttribute = currentLangAttribute.trim().toLowerCase();
            if (!isFirstSubjectParsed) {
                isFirstSubjectParsed = true;
                firstLanguage = currentLangAttribute;
            }
        }

    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (qname.equals("EndUserNotification")) {
            if (logger.isActivated()) {
                logger.debug("EndUserNotification document is complete");
            }
        } else if (currentLangAttribute.equals(requestedLanguage)
                || currentLangAttribute.equals(DEFAULT_LANGUAGE)
                || currentLangAttribute.equals(firstLanguage)
                || currentLangAttribute.equals("")) {
            elementMap.put(qname + currentLangAttribute, accumulator.toString().trim());
        }
    }

    // Returns text part off xml element,
    // if found for requested language ('xml:lang' attribute == requestedLanguage)
    // or with 'xml:lang' is equal "en" (english)
    // or with 'xml:lang' attribute equals to this from the first 'Subject' element
    // or the text from the element with out any 'xml:lang' attribute
    // or null if element not found
    private String giveTextInBestLanguage(String elementName) {
        if (elementMap.containsKey(elementName + requestedLanguage)) {
            return elementMap.get(elementName + requestedLanguage);
        } else if (elementMap.containsKey(elementName + DEFAULT_LANGUAGE)) {
            return elementMap.get(elementName + DEFAULT_LANGUAGE);
        } else if (elementMap.containsKey(elementName + firstLanguage)) {
            return elementMap.get(elementName + firstLanguage);
        } else {
            return elementMap.get(elementName);
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document: 'EndUserNotification'");
        }
    }
}
