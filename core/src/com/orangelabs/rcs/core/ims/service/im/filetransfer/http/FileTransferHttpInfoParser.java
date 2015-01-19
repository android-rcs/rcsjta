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

import static com.orangelabs.rcs.utils.StringUtils.UTF8_STR;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;
import android.util.TimeFormatException;

import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * File transfer over HTTP info parser
 *
 * @author vfml3370
 */
public class FileTransferHttpInfoParser extends DefaultHandler {
/*  File-Transfer HTTP SAMPLE:
    <?xml version="1.0" encoding=”UTF-8”?>
    <file>
        <file-info type="thumbnail">
            <file-size>[thumbnail size in bytes]</file-size>
            <content-type>[MIME-type for thumbnail]</content-type>
            <data url = "[HTTP URL for the thumbnail]" until = "[validity of the thumbnail]"/>
        </file-info>
        <file-info type="file">
            <file-size>[file size in bytes]</file-size>
            <file-name>[original file name]</file-name>
            <content-type>[MIME-type for file]</content-type>
            <data url = "[HTTP URL for the file]" until = "[validity of the file]"/>
        </file-info>
    <file>
*/

    /**
     * Accumulator buffer
     */
	private StringBuffer accumulator;

    /**
     * File transfer over HTTP info document
     */
	private FileTransferHttpInfoDocument ftInfo = null;

    /**
     * File transfer over HTTP thumbnail info
     */
	private FileTransferHttpThumbnail thumbnailInfo = null;

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
    public FileTransferHttpInfoParser(InputSource inputSource) throws Exception {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
	}

    /**
     * Get file transfer info
     *
     * @return Info document
     */
	public FileTransferHttpInfoDocument getFtInfo() {
		return ftInfo;
	}

	/**
	 * Receive notification of the beginning of the document.
	 */
	public void startDocument() {
		if (logger.isActivated()) {
			logger.debug("Start document");
		}
		accumulator = new StringBuffer();
	}

	/**
	 * Receive notification of character data inside an element.
	 */
	public void characters(char buffer[], int start, int length) {
		accumulator.append(buffer, start, length);
	}

	/**
	 * Receive notification of the start of an element.
	 */
	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		accumulator.setLength(0);

		if (localName.equalsIgnoreCase("file")) {
			ftInfo = new FileTransferHttpInfoDocument();
		} else
		if (localName.equalsIgnoreCase("file-info")) {
			if (ftInfo != null) {
				String type = attr.getValue("type").trim();
				if (type.equalsIgnoreCase("thumbnail")) {
					thumbnailInfo = new FileTransferHttpThumbnail();
				}
			}
		} else
		if (localName.equalsIgnoreCase("data")) {
			if (ftInfo != null) {
				String url = attr.getValue("url").trim();
				String validity = attr.getValue("until").trim();

				if (ftInfo.getFileThumbnail() != null || thumbnailInfo == null){
					ftInfo.setFileUri(Uri.parse(url));
					ftInfo.setTransferValidity(parseValidityDate(validity));

				} else
				if (thumbnailInfo != null) {
					thumbnailInfo.setThumbnailUri(Uri.parse(url));
					thumbnailInfo.setThumbnailValidity(parseValidityDate(validity));
					ftInfo.setFileThumbnail(thumbnailInfo);
				}
			}
		}
	}

	private long parseValidityDate(String validity) {
		try
		{
			return DateUtils.decodeDate(validity);
		}
		catch(TimeFormatException tfe) // validity is not in the expected date format
		{
			try
			{
				return Long.decode(validity); // validity may be already the long value
			}
			catch(NumberFormatException nfe)
			{
				if (logger.isActivated()) {
					logger.error("Could not parse transfer validity:"+validity);
				}
				return java.lang.System.currentTimeMillis()+300000;	// TODO default validity ?
			}
		}
	}

	/**
	 * Receive notification of the end of an element.
	 */
	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equalsIgnoreCase("file-size")) {
			if ((ftInfo != null && ftInfo.getFileThumbnail() != null) || (ftInfo != null && thumbnailInfo == null)) {
				ftInfo.setFileSize(Integer.parseInt(accumulator.toString().trim()));
			} else if (thumbnailInfo != null) {
				thumbnailInfo.setThumbnailSize(Integer.parseInt(accumulator.toString().trim()));
			}
		} else if (localName.equalsIgnoreCase("file-name")) {
			if (ftInfo != null) {
				try {
					ftInfo.setFilename(URLDecoder.decode(accumulator.toString().trim(), UTF8_STR));
				} catch (UnsupportedEncodingException e) {
					if (logger.isActivated()) {
						logger.debug(new StringBuilder("Could not decode filename '").append(accumulator).append("'").toString());
					}
				}
			}
		} else if (localName.equalsIgnoreCase("content-type")) {
			if (ftInfo != null && (ftInfo.getFileThumbnail() != null  || thumbnailInfo == null)) {
				ftInfo.setFileType(accumulator.toString().trim());
			} else if (thumbnailInfo != null) {
				thumbnailInfo.setThumbnailType(accumulator.toString().trim());
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
