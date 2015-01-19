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

package com.orangelabs.rcs.core.ims.service.presence.xdm;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * XCAP photo-icon response parser
 *
 * @author jexa7410
 */
public class XcapPhotoIconResponseParser extends DefaultHandler {

	private StringBuffer accumulator;

	private byte[] data = null;
	private String mime = null;
	private String encoding = null;
	private String desc = null;

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
    public XcapPhotoIconResponseParser(InputSource inputSource) throws Exception {
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

	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		accumulator.setLength(0);
	}

	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equals("data")) {
			data = accumulator.toString().getBytes(UTF8);
		} else
		if (localName.equals("mime-type")) {
			mime = accumulator.toString();
		} else
		if (localName.equals("encoding")) {
			encoding = accumulator.toString();
		} else
		if (localName.equals("description")) {
			desc = accumulator.toString();
		}
	}

	public void endDocument() {
		if (logger.isActivated()) {
			logger.debug("End document");
		}
	}

	public byte[] getData() {
		return data;
	}

	public String getMime() {
		return mime;
	}

	public String getEncoding() {
		return encoding;
	}

	public String getDesc() {
		return desc;
	}
}
