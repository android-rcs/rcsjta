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
package com.orangelabs.rcs.popup;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Popup parser
 * 
 * @author Jean-Marc AUFFRET
 */
public class PopupParser extends DefaultHandler {
	/**
	 * Buffer
	 */
	private StringBuffer accumulator;
	
	/**
	 * Message 
	 */
	private String message = null;
	
	/**
	 * Animation
	 */
	private String animation = null;

	/**
	 * TTS option
	 */
	private boolean tts = false;
	
    /**
     * Returns the message
     * 
     * @return Message
     */
    public String getMessage() {
    	return message;
    }
    
    /**
     * Returns the animation
     * 
     * @return Animation
     */
    public String getAnimation() {
    	return animation;
    }

    /**
     * Is TTS
     * 
     * @return Boolean
     */
    public boolean isTTS() {
    	return tts;
    }
    
    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @throws Exception
     */
    public PopupParser(InputSource inputSource) throws Exception {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
	}

	public void startDocument() {
		accumulator = new StringBuffer();
	}

	public void characters(char buffer[], int start, int length) {
		accumulator.append(buffer, start, length);
	}

	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		accumulator.setLength(0);
	}

	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equals("message")) {
			message = accumulator.toString().trim();
		} else
		if (localName.equals("tts")) {
			String value = accumulator.toString().trim();
			tts = (value.equalsIgnoreCase("true"));
		} else
		if (localName.equals("animation")) {
			animation = accumulator.toString().trim();
		}
	}

	public void endDocument() {
	}
}
