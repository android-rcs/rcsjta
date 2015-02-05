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

package com.orangelabs.rcs.core.ims.service.im.chat.iscomposing;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Is composing event parser (RFC3994)
 */
public class IsComposingParser extends DefaultHandler {
    /*
     * IsComposing SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <isComposing
     * xmlns="urn:ietf:params:xml:ns:im-iscomposing"
     * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     * xsi:schemaLocation="urn:ietf:params:xml:ns:im-composing iscomposing.xsd"> <state>idle</state>
     * <lastactive>2003-01-27T10:43:00Z</lastactive> <contenttype>audio</contenttype> </isComposing>
     */
    private StringBuffer accumulator = null;

    private IsComposingInfo isComposingInfo = null;

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
    public IsComposingParser(InputSource inputSource) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
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

        if (localName.equals("isComposing")) {
            isComposingInfo = new IsComposingInfo();
        }

    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("state")) {
            if (isComposingInfo != null) {
                isComposingInfo.setState(accumulator.toString());
            }
        } else if (localName.equals("lastactive")) {
            if (isComposingInfo != null) {
                isComposingInfo.setLastActiveDate(accumulator.toString());
            }
        } else if (localName.equals("contenttype")) {
            if (isComposingInfo != null) {
                isComposingInfo.setContentType(accumulator.toString());
            }
        } else if (localName.equals("refresh")) {
            if (isComposingInfo != null) {
                isComposingInfo.setRefreshTime(accumulator.toString());
            }
        } else if (localName.equals("isComposing")) {
            if (logger.isActivated()) {
                logger.debug("Watcher document is complete");
            }
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }

    public void warning(SAXParseException exception) {
        if (logger.isActivated()) {
            logger.error("Warning: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }

    public void error(SAXParseException exception) {
        if (logger.isActivated()) {
            logger.error("Error: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        if (logger.isActivated()) {
            logger.error("Fatal: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
        throw exception;
    }

    public IsComposingInfo getIsComposingInfo() {
        return isComposingInfo;
    }
}
