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

import com.orangelabs.rcs.utils.logger.Logger;

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

    private StringBuffer accumulator;
    private String id = null;
    private String status = null;
    private String subject = null;
    private String text = null;

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
    public TermsAckParser(InputSource inputSource) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
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

        if (localName.equals("EndUserConfirmationAck")) {
            id = attr.getValue("id").trim();
            status = attr.getValue("status").trim();
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("EndUserConfirmationAck")) {
            if (logger.isActivated()) {
                logger.debug("Terms request document is complete");
            }
        } else if (localName.equals("Subject")) {
            subject = accumulator.toString().trim();
        } else if (localName.equals("Text")) {
            text = accumulator.toString().trim();
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}
