/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.chat;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

public class ImdnParserTest extends AndroidTestCase {
    private static Logger sLogger = Logger.getLogger(ImdnParserTest.class.getName());

    private static final String sXmlContentToParse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">\n"
            + "\t<message-id>34jk324j</message-id>\n"
            + "\t<datetime>2008-04-04T12:16:49-05:00</datetime>\n" + "\t<display-notification>\n"
            + "\t\t<status>\n" + "\t\t\t<displayed/>\n" + "\t\t</status>\n"
            + "\t</display-notification>\n" + "</imdn>";

    public void testGetImdnDocument() throws SAXException, ParserConfigurationException,
            IOException, ParseFailureException {
        ImdnParser parser = new ImdnParser(new InputSource(new ByteArrayInputStream(
                sXmlContentToParse.getBytes())));
        parser.parse();
        ImdnDocument imdnDoc = parser.getImdnDocument();
        if (sLogger.isActivated()) {
            sLogger.info("MsgId=" + imdnDoc.getMsgId() + "  status=" + imdnDoc.getStatus());
        }
        assertEquals("34jk324j", imdnDoc.getMsgId());
        assertEquals("displayed", imdnDoc.getStatus().toString());
        assertEquals("display-notification", imdnDoc.getNotificationType());
        assertEquals(DateUtils.decodeDate("2008-04-04T12:16:49-05:00"), imdnDoc.getDateTime());
    }
}
