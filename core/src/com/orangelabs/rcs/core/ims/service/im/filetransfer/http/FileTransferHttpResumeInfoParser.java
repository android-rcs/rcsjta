/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;

import com.orangelabs.rcs.utils.logger.Logger;

public class FileTransferHttpResumeInfoParser extends DefaultHandler {
/*  File-Transfer HTTP SAMPLE:
	<?xml version="1.0" encoding=”UTF-8”?>
	<file-resume-info>
	<file-range start="[start-offset in bytes]" end="[end-offset in bytes]" / >
	<data url="[HTTP upload URL for the file]"/>
	</file-resume-info>
*/
	
    /**
     * File transfer over HTTP info document
     */
	private FileTransferHttpResumeInfo ftResumeInfo = null;
	
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public FileTransferHttpResumeInfoParser(InputSource ftHttpInput) throws ParserConfigurationException, SAXException, IOException {
	    	SAXParserFactory factory = SAXParserFactory.newInstance();
	        SAXParser parser = factory.newSAXParser();
	        parser.parse(ftHttpInput, this);
	}

	public FileTransferHttpResumeInfo getResumeInfo() {
		return ftResumeInfo;
	}
	
	/**
	 * Receive notification of the beginning of the document.
	 */
	public void startDocument() {
		if (logger.isActivated()) {
			logger.debug("Start document");
		}
	}
	
	/**
	 * Receive notification of the start of an element.
	 */
	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		if (localName.equals("file-resume-info")) {		
			ftResumeInfo = new FileTransferHttpResumeInfo();
		} else
		if (localName.equals("file-range")) {
			if (ftResumeInfo != null) {
				String start = attr.getValue("start").trim();	
				ftResumeInfo.setStart(Integer.parseInt(start));
				String end = attr.getValue("end").trim();	
				ftResumeInfo.setEnd(Integer.parseInt(end));
				
			}
		} else		
		if (localName.equals("data")) {	
			if (ftResumeInfo != null) {
				String url = attr.getValue("url").trim();	
				ftResumeInfo.setUri(Uri.parse(url));
			}
		}
	}
	
	/**
	 * Receive notification of the end of the document.
	 */
	public void endDocument() {
		if (logger.isActivated()) {
			logger.debug("End document");
		}
	}
}
