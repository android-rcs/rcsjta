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

import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * End User Confirmation Request request parser.
 * Parse message of type end-user-confirmation-request.
 * If the message contains the text in different languages it's returns the text
 * in the requested language if present or in the default language (English).
 *
 * @author jexa7410
 * @author Deutsche Telekom AG
 */
public class TermsRequestParser extends DefaultHandler {
    /* SAMPLE:
     * <?xml version="1.0" standalone="yes"?>
     * <EndUserConfirmationRequest id="xxxxxxx" type="xxxxxxx" pin="xxxxxx" timeout="120">
     *   <Subject xml:lang="en">xxxxxxxxxx</Subject> 
     *   <Subject xml:lang="de">xxxxxxxxxx</Subject> 
     *   <Subject xml:lang="es">xxxxxxxxxx</Subject> 
     *   <Text xml:lang="en">xxxxxxxxxx</Text> 
     *   <Text xml:lang="de">xxxxxxxxxx</Text> 
     *   <Text xml:lang="es">xxxxxxxxxx</Text> 
     *   <ButtonAccept xml:lang="en">xxxxxxxxxx</ButtonAccept>
     *   <ButtonAccept xml:lang="de">xxxxxxxxxx</ButtonAccept>
     *   <ButtonAccept xml:lang="es">xxxxxxxxxx</ButtonAccept>
     *   <ButtonReject xml:lang="en">xxxxxxxxxx</ButtonReject>
     *   <ButtonReject xml:lang="de">xxxxxxxxxx</ButtonReject>
     *   <ButtonReject xml:lang="es">xxxxxxxxxx</ButtonReject>
     * </EndUserConfirmationRequest>
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
     * Value off attribute 'id' off element 'EndUserConfirmationRequest'
     */
    private String id = null;

    /**
     * Value off attribute 'type' off element 'EndUserNotification'
     */
    private String type;

    /**
     * Value off attribute 'timeout' off element 'EndUserNotification'
     */
    private int timeout;

    /**
     * Value off attribute 'pin' off element 'EndUserConfirmationRequest'
     */
    private boolean pin = false;

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
	 * @throws Exception
	 */
	public TermsRequestParser(InputSource inputSource, String requestedLanguage) throws Exception {
        this.requestedLanguage = requestedLanguage;
		SAXParserFactory factory = SAXParserFactory.newInstance();
	    SAXParser parser = factory.newSAXParser();
	    parser.parse(inputSource, this);
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

    public int getTimeout() {
        return timeout;
    }

	public boolean getPin() {
		return pin;
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
		accumulator = new StringBuffer();
	}

    @Override
	public void characters(char buffer[], int start, int length) {
		accumulator.append(buffer, start, length);
	}

    @Override
	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		accumulator.setLength(0);

		if (localName.equals("EndUserConfirmationRequest")) {
			id = attr.getValue("id").trim();
			type = attr.getValue("type").trim();
            if (type.equalsIgnoreCase("Volatile")) {
                try {
                    timeout = 1000 * Integer.parseInt(attr.getValue("timeout").trim());
                } catch (Exception e) {
                    // If the attribute timeout is not present a default value of 64*T1 seconds (with T1 as defined in
                    // [RFC3261]) shall be used
                    RcsSettings rcsSettings = RcsSettings.getInstance();
                    if (rcsSettings != null) {
                        timeout = rcsSettings.getSipTimerT1() * 64;
                    } else {
                        // T1 is an estimate of the round-trip time (RTT), and it defaults to 500 ms.
                        timeout = 500 * 64;
                    }
                }
            } else { // type.equalsIgnoreCase('Persistent')
                timeout = -1; // means infinite ( no timeout)
            }

            // Get optional attribute pin
            pin = false; // Default value according to RCSe spec 1.2.2
            String pinAttr = attr.getValue("pin");
            if (pinAttr != null) {
                pin = Boolean.parseBoolean(attr.getValue("pin").trim());
            }

        } else { //check lang attribute for all sub elements
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

    @Override
	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equals("EndUserConfirmationRequest")) {
			if (logger.isActivated()) {
				logger.debug("Terms request document is complete");
			}
        } else if (currentLangAttribute.equals(requestedLanguage)
                || currentLangAttribute.equals(DEFAULT_LANGUAGE)
                || currentLangAttribute.equals(firstLanguage)
                || currentLangAttribute.equals("")) {
            elementMap.put(qname + currentLangAttribute, accumulator.toString().trim());
        }
	}

    /** 
     * Returns text part off xml element,
     * if found for requested language ('xml:lang' attribute == requestedLanguage)
     * or with 'xml:lang' is equal "en" (english)
     * or with 'xml:lang' attribute equals to this from the first 'Subject' element
     * or the text from the element with out any 'xml:lang' attribute
     * or null if element not found
     * @param elementName
     * @return
     */
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
			logger.debug("End document");
		}
	}
}
