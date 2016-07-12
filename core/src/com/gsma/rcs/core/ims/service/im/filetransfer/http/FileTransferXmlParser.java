/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2015 Orange.
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

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.net.Uri;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A class to parse the XML descriptor containing the HTTP file transfer information.
 *
 * @author Philippe LEMORDANT
 */
public class FileTransferXmlParser {
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

    private final RcsSettings mRcsSettings;
    private final String mXmlSource;
    private FileTransferHttpInfoDocument mFtInfo;
    private FileTransferHttpThumbnail mThumbnailInfo;
    private boolean mThumbnailProcessed;

    /**
     * Constructor
     *
     * @param xml the XML to be parsed
     * @param rcsSettings the RCS settings accessor
     */
    public FileTransferXmlParser(byte[] xml, RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
        mXmlSource = new String(xml, Charset.forName("UTF8"));
    }

    /**
     * Parses the XML file transfer document
     * 
     * @return FileTransferXmlParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */

    public FileTransferXmlParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(mXmlSource));
            int eventType = xpp.getEventType();
            String text = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("file".equalsIgnoreCase(tagName)) {
                            if (mFtInfo == null) {
                                mFtInfo = new FileTransferHttpInfoDocument(mRcsSettings);
                            }
                        } else if ("file-info".equalsIgnoreCase(tagName)) {
                            if (mFtInfo == null) {
                                break;
                            }
                            String type = xpp.getAttributeValue(null, "type");
                            if ("thumbnail".equalsIgnoreCase(type)) {
                                mThumbnailInfo = new FileTransferHttpThumbnail(mRcsSettings);

                            } else if ("file".equalsIgnoreCase(type)) {
                                mThumbnailProcessed = true;
                                String typeDispo = xpp.getAttributeValue(null, "file-disposition");
                                if (typeDispo != null) {
                                    switch (typeDispo) {
                                        case FileSharingSession.FILE_DISPOSITION_ATTACH:
                                            mFtInfo.setFileDisposition(FileTransfer.Disposition.ATTACH);
                                            break;
                                        case FileSharingSession.FILE_DISPOSITION_RENDER:
                                            mFtInfo.setFileDisposition(FileTransfer.Disposition.RENDER);
                                            break;
                                    }
                                }
                            }
                        } else if ("data".equalsIgnoreCase(tagName)) {
                            if (mFtInfo == null) {
                                break;
                            }
                            String url = xpp.getAttributeValue(null, "url");
                            String expiration = xpp.getAttributeValue(null, "until");
                            if (mThumbnailProcessed) {
                                mFtInfo.setUri(Uri.parse(url));
                                mFtInfo.setExpiration(DateUtils.decodeDate(expiration));

                            } else if (mThumbnailInfo != null) {
                                mThumbnailProcessed = true;
                                mThumbnailInfo.setUri(Uri.parse(url));
                                mThumbnailInfo.setExpiration(DateUtils.decodeDate(expiration));
                                mFtInfo.setFileThumbnail(mThumbnailInfo);
                            }
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = xpp.getText().trim();
                        break;

                    case XmlPullParser.END_TAG:
                        if (mFtInfo == null) {
                            break;
                        }
                        if (text == null) {
                            throw new ParseFailureException("Bad HTTP file transfer information "
                                    + mXmlSource);
                        }
                        if ("file-name".equalsIgnoreCase(tagName)) {
                            if (mThumbnailProcessed) {
                                mFtInfo.setFilename(text);
                            }
                        } else if ("file-size".equalsIgnoreCase(tagName)) {
                            if (mThumbnailProcessed) {
                                mFtInfo.setSize(Integer.parseInt(text));
                            } else if (mThumbnailInfo != null) {
                                mThumbnailInfo.setSize(Integer.parseInt(text));
                            }
                        } else if ("content-type".equalsIgnoreCase(tagName)) {
                            if (mThumbnailProcessed) {
                                mFtInfo.setMimeType(text);
                            } else if (mThumbnailInfo != null) {
                                mThumbnailInfo.setMimeType(text);
                            }
                        } else if ("am:playing-length".equalsIgnoreCase(tagName)) {
                            mFtInfo.setPlayingLength(Integer.parseInt(text));
                        }
                        break;

                    default:
                        break;
                }
                eventType = xpp.next();
            }
            return this;

        } catch (XmlPullParserException | IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    /**
     * Get the file transfer information
     * 
     * @return FileTransferHttpInfoDocument
     */
    public FileTransferHttpInfoDocument getFileTransferInfo() {
        return mFtInfo;
    }
}
