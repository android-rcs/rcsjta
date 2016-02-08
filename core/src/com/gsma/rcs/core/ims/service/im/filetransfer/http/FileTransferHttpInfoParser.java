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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;
import android.util.TimeFormatException;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.logger.Logger;

/**
 * File transfer over HTTP info parser
 * 
 * @author vfml3370
 */
public class FileTransferHttpInfoParser extends DefaultHandler {
    // @formatter:off
    
    /*
    <?xml version="1.0" encoding=UTF-8>
    <file>
    <file-info type="thumbnail">
                <file-size>[thumbnail size in bytes]</file-size>
                <content-type>[MIME-type for thumbnail]</content-type>
                <data url = "[HTTPS URL for the thumbnail]" until = "[validity of the thumbnail]"/>
    </file-info>
    <file-info type="file" file-disposition="attach|render">
                <file-size>[file size in bytes]</file-size>
                <file-name>[original file name]</file-name>
                <content-type>[MIME-type for file]</content-type>
                <am:playing-length>[duration of the rram]</am:playing-length>
               <data url = "[HTTPS URL for the file]" until = "[validity of the file]"/>
    </file-info>
    <file>
    */
    /* 
     * Note: in the specification 'validity' refers to a timestamp.
     * Term 'validity' is more suitable for a duration so we call it instead 'expiration' in the code.
     * 
     */
    // @formatter:on

    /**
     * Accumulator buffer
     */
    private StringBuffer mAccumulator;

    /**
     * File transfer over HTTP info document
     */
    private FileTransferHttpInfoDocument mFtInfo;

    /**
     * File transfer over HTTP thumbnail info
     */
    private FileTransferHttpThumbnail mThumbnailInfo;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    private final InputSource mInputSource;

    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @param rcsSettings
     */
    public FileTransferHttpInfoParser(InputSource inputSource, RcsSettings rcsSettings) {
        mInputSource = inputSource;
        mRcsSettings = rcsSettings;
    }

    /**
     * Parse the Http file transfer content
     * 
     * @return FileTransferHttpInfoParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public FileTransferHttpInfoParser parse() throws ParserConfigurationException, SAXException,
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

    /**
     * Get file transfer info
     * 
     * @return Info document
     */
    public FileTransferHttpInfoDocument getFtInfo() {
        return mFtInfo;
    }

    /**
     * Receive notification of the beginning of the document.
     */
    public void startDocument() {
        if (logger.isActivated()) {
            logger.debug("Start document");
        }
        mAccumulator = new StringBuffer();
    }

    /**
     * Receive notification of character data inside an element.
     * 
     * @param buffer
     * @param start
     * @param length
     */
    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    /**
     * Receive notification of the start of an element.
     * 
     * @param namespaceURL
     * @param localName
     * @param qname
     * @param attr
     */
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);

        if (localName.equalsIgnoreCase("file")) {
            mFtInfo = new FileTransferHttpInfoDocument(mRcsSettings);
        } else if (localName.equalsIgnoreCase("file-info")) {
            if (mFtInfo != null) {
                String type = attr.getValue("type").trim();
                if (type.equalsIgnoreCase("thumbnail")) {
                    mThumbnailInfo = new FileTransferHttpThumbnail(mRcsSettings);
                }
                String typeDispo = attr.getValue("file-disposition");
                if (typeDispo != null) {
                    mFtInfo.setFileDisposition(typeDispo.trim());
                }
            }
        } else if (localName.equalsIgnoreCase("data")) {
            if (mFtInfo != null) {
                String url = attr.getValue("url").trim();
                String expiration = attr.getValue("until").trim();

                if (mFtInfo.getFileThumbnail() != null || mThumbnailInfo == null) {
                    mFtInfo.setUri(Uri.parse(url));
                    mFtInfo.setExpiration(parseExpirationDate(expiration));

                } else if (mThumbnailInfo != null) {
                    mThumbnailInfo.setUri(Uri.parse(url));
                    mThumbnailInfo.setExpiration(parseExpirationDate(expiration));
                    mFtInfo.setFileThumbnail(mThumbnailInfo);
                }
            }
        }
    }

    private long parseExpirationDate(String expiration) {
        try {
            return DateUtils.decodeDate(expiration);
        } catch (TimeFormatException tfe) // expiration is not in the expected date format
        {
            try {
                return Long.decode(expiration); // expiration may be already the long value
            } catch (NumberFormatException nfe) {
                if (logger.isActivated()) {
                    logger.error("Could not parse transfer expiration:" + expiration);
                }
                return System.currentTimeMillis() + 300000; // TODO default validity ?
            }
        }
    }

    /**
     * Receive notification of the end of an element.
     * 
     * @param namespaceURL
     * @param localName
     * @param qname
     */
    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equalsIgnoreCase("file-size")) {
            if ((mFtInfo != null && mFtInfo.getFileThumbnail() != null)
                    || (mFtInfo != null && mThumbnailInfo == null)) {
                mFtInfo.setSize(Integer.parseInt(mAccumulator.toString().trim()));
            } else if (mThumbnailInfo != null) {
                mThumbnailInfo.setSize(Integer.parseInt(mAccumulator.toString().trim()));
            }
        } else if (localName.equalsIgnoreCase("file-name")) {
            if (mFtInfo != null) {
                try {
                    mFtInfo.setFilename(URLDecoder.decode(mAccumulator.toString().trim(), UTF8_STR));
                } catch (UnsupportedEncodingException e) {
                    if (logger.isActivated()) {
                        logger.debug(new StringBuilder("Could not decode filename '")
                                .append(mAccumulator).append("'").toString());
                    }
                }
            }
        } else if (localName.equalsIgnoreCase("content-type")) {
            if (mFtInfo != null && (mFtInfo.getFileThumbnail() != null || mThumbnailInfo == null)) {
                mFtInfo.setMimeType(mAccumulator.toString().trim());
            } else if (mThumbnailInfo != null) {
                mThumbnailInfo.setMimeType(mAccumulator.toString().trim());
            }
        } else if (localName.equalsIgnoreCase("playing-length")) {
            if (mFtInfo != null) {
                mFtInfo.setPlayingLength(Integer.parseInt(mAccumulator.toString().trim()));
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
