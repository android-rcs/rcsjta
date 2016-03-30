/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      @Author : Created by yplo6403 on 24/02/2016.
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.im.filetransfer;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferXmlParser;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class FileTransferXmlParserTest extends AndroidTestCase {

    private static final Logger sLogger = Logger.getLogger(FileTransferXmlParserTest.class
            .getName());

    private static Uri sUri = Uri.parse("https://host/path/download?id=12345");
    private static Uri sUriThumbnail = Uri.parse("https://host/path/download?id=6789");

    private static final String sXmlContentToParse1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<file>" + "<file-info type=\"file\" file-disposition=\"render\">"
            + "  <file-size>1234567890</file-size>" + "  <file-name>audio.mp4</file-name>"
            + "  <content-type>audio/mp4</content-type>"
            + "  <am:playing-length>1000</am:playing-length>" + "  <data url = \"" + sUri
            + "\" until = \"1234\"/>" + "</file-info>" + "</file>";

    private static final String sXmlContentToParse2 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<file>" + "<file-info type=\"file\" file-disposition=\"attach\">"
            + "  <file-size>1234567890</file-size>" + "  <file-name>image.jpg</file-name>"
            + "  <content-type>image/jpeg</content-type>" + "  <data url = \"" + sUri
            + "\" until = \"1234\"/>" + "</file-info>" + "</file>";

    private static final String sXmlContentToParse3 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<file>" + "<file-info type=\"thumbnail\">" + "  <file-size>12345</file-size>"
            + "  <content-type>image/png</content-type>" + "  <data url = \"" + sUriThumbnail
            + "\" until = \"56789\"/>" + "</file-info>" + "</file>" + "<file>"
            + "<file-info type=\"file\" file-disposition=\"attach\">"
            + "  <file-size>1234567890</file-size>" + "  <file-name>image.jpg</file-name>"
            + "  <content-type>image/jpeg</content-type>" + "  <data url = \"" + sUri
            + "\" until = \"1234\"/>" + "</file-info>" + "</file>";

    private RcsSettings mRrcsSettings;

    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        mRrcsSettings = RcsSettings.getInstance(localContentResolver);
    }

    public void testFileTransferXmlParserAudioMessage() throws ParseFailureException, SAXException,
            ParserConfigurationException {
        FileTransferXmlParser parser = new FileTransferXmlParser(
                sXmlContentToParse1.getBytes(UTF8), mRrcsSettings);
        sLogger.debug(sXmlContentToParse1);
        parser.parse();
        FileTransferHttpInfoDocument info = parser.getFileTransferInfo();
        assertEquals(1234567890, info.getSize());
        assertEquals("audio.mp4", info.getFilename());
        assertEquals("audio/mp4", info.getMimeType());
        assertEquals("render", info.getFileDisposition());
        assertEquals(1234, info.getExpiration());
        assertNull(info.getFileThumbnail());
        assertEquals(1000, info.getPlayingLength());
        assertEquals(sUri, info.getUri());
    }

    public void testFileTransferXmlParserFileImageWithoutIcon() throws ParseFailureException,
            SAXException, ParserConfigurationException {
        FileTransferXmlParser parser = new FileTransferXmlParser(
                sXmlContentToParse2.getBytes(UTF8), mRrcsSettings);
        sLogger.debug(sXmlContentToParse1);
        parser.parse();
        FileTransferHttpInfoDocument info = parser.getFileTransferInfo();
        assertEquals(1234567890, info.getSize());
        assertEquals("image.jpg", info.getFilename());
        assertEquals("image/jpeg", info.getMimeType());
        assertEquals("attach", info.getFileDisposition());
        assertEquals(1234, info.getExpiration());
        assertNull(info.getFileThumbnail());
        assertEquals(-1, info.getPlayingLength());
        assertEquals(sUri, info.getUri());
    }

    public void testFileTransferXmlParserFileImageWithIcon() throws ParseFailureException,
            SAXException, ParserConfigurationException {
        FileTransferXmlParser parser = new FileTransferXmlParser(
                sXmlContentToParse3.getBytes(UTF8), mRrcsSettings);
        sLogger.debug(sXmlContentToParse1);
        parser.parse();
        FileTransferHttpInfoDocument info = parser.getFileTransferInfo();
        assertEquals(1234567890, info.getSize());
        assertEquals("image.jpg", info.getFilename());
        assertEquals("image/jpeg", info.getMimeType());
        assertEquals("attach", info.getFileDisposition());
        assertEquals(1234, info.getExpiration());
        FileTransferHttpThumbnail icon = info.getFileThumbnail();
        assertNotNull(icon);
        assertEquals(56789, icon.getExpiration());
        assertEquals("image/png", icon.getMimeType());
        assertEquals(sUriThumbnail, icon.getUri());
        assertEquals(12345, icon.getSize());
        assertEquals(-1, info.getPlayingLength());
        assertEquals(sUri, info.getUri());
    }
}
