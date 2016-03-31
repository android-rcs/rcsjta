
package com.gsma.rcs.cpim;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnUtils;

import android.test.AndroidTestCase;

/*******************************************************************************
 * Software Name : RCS IMS Stack Copyright (C) 2010-2016 Orange. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/
public class CpimParserTest extends AndroidTestCase {
    // @formatter:off
    private static final String sCpimToTest =
            "From: <sip:anonymous@anonymous.invalid>" + SipUtils.CRLF +
                    "To: <sip:anonymous@anonymous.invalid>" + SipUtils.CRLF +
                    "NS: imdn <urn:ietf:params:imdn>" + SipUtils.CRLF +
                    "imdn.Message-ID: ae6926cfcffa40a89e44252ce9e970a2" + SipUtils.CRLF +
                    "DateTime: 2016-03-24T08:51:42+01:00"+SipUtils.CRLF+
                    "imdn.Disposition-Notification: positive-delivery"+SipUtils.CRLF +
                    SipUtils.CRLF+
                    "Content-type: text/plain;charset=utf-8"+SipUtils.CRLF+
                    "Content-length: 7"+SipUtils.CRLF+
                    SipUtils.CRLF+
                    "Bonjour";

    private static final String sCpimToTestWithBlankContent =
            "From: <sip:anonymous@anonymous.invalid>" + SipUtils.CRLF +
                    "To: <sip:anonymous@anonymous.invalid>" + SipUtils.CRLF +
                    "NS: imdn <urn:ietf:params:imdn>" + SipUtils.CRLF +
                    "imdn.Message-ID: ae6926cfcffa40a89e44252ce9e970a2" + SipUtils.CRLF +
                    "DateTime: 2016-03-24T08:51:42+01:00"+SipUtils.CRLF+
                    "imdn.Disposition-Notification: positive-delivery"+SipUtils.CRLF +
                    SipUtils.CRLF+
                    "Content-type: text/plain;charset=utf-8"+SipUtils.CRLF+
                    "Content-length: 2"+SipUtils.CRLF+
                    SipUtils.CRLF+
                    "  ";

    private static final String sMultipartsBoundary = "----=_Part_3144_497651830.1459426876307";
    private static final String sMultiparts =
            "------=_Part_3144_497651830.1459426876307" + SipUtils.CRLF +
                    "Content-Type: application/sdp" + SipUtils.CRLF +
                    "Content-Length: 325" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    "v=0" + SipUtils.CRLF +
                    "o=- 0 0 IN IP4 sip.imsnsn.fr" + SipUtils.CRLF +
                    "s= " + SipUtils.CRLF +
                    "c=IN IP4 80.12.197.204" + SipUtils.CRLF +
                    "t=0 0" + SipUtils.CRLF +
                    "m=message 53358 TCP/MSRP *" + SipUtils.CRLF +
                    "a=accept-types:message/cpim application/im-iscomposing+xml" + SipUtils.CRLF +
                    "a=accept-wrapped-types:text/plain message/imdn+xml application/vnd.gsma.rcs-ft-http+xml" + SipUtils.CRLF +
                    "a=setup:actpass" + SipUtils.CRLF +
                    "a=path:msrp://80.12.197.204:53358/b29w5mku;tcp" + SipUtils.CRLF +
                    "a=sendrecv" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    "------=_Part_3144_497651830.1459426876307" + SipUtils.CRLF +
                    "Content-Type: message/cpim" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    "From: <sip:anonymous@anonymous.invalid>" + SipUtils.CRLF +
                    "To: <sip:anonymous@anonymous.invalid>" + SipUtils.CRLF +
                    "NS: imdn <urn:ietf:params:imdn>" + SipUtils.CRLF +
                    "imdn.Message-ID: dd4167b82b864c6ea9def42deada1b54" + SipUtils.CRLF +
                    "DateTime: 2016-03-31T14:21:16+02:00" + SipUtils.CRLF +
                    "imdn.Disposition-Notification: positive-delivery, display" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    "Content-type: text/plain;charset=utf-8" + SipUtils.CRLF +
                    "Content-length: 3" + SipUtils.CRLF +
                    SipUtils.CRLF +
                    "   "+SipUtils.CRLF +
                    "------=_Part_3144_497651830.1459426876307--" + SipUtils.CRLF;
    // @formatter:on

    public final void testCpimParserString() {
        CpimMessage msg = (new CpimParser(sCpimToTest)).getCpimMessage();
        assertEquals("<sip:anonymous@anonymous.invalid>", msg.getHeader("From"));
        assertEquals("<sip:anonymous@anonymous.invalid>", msg.getHeader("To"));
        assertEquals("imdn <urn:ietf:params:imdn>", msg.getHeader(CpimMessage.HEADER_NS));
        assertEquals("2016-03-24T08:51:42+01:00", msg.getHeader("DateTime"));
        assertEquals("text/plain;charset=utf-8", msg.getContentType());
        assertEquals("positive-delivery", msg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF));
        assertEquals("ae6926cfcffa40a89e44252ce9e970a2",
                msg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID));
        assertEquals("Bonjour", msg.getMessageContent());
        assertEquals("7", msg.getContentHeader(CpimMessage.HEADER_CONTENT_LENGTH));
        assertEquals("text/plain;charset=utf-8",
                msg.getContentHeader(CpimMessage.HEADER_CONTENT_TYPE));
    }

    public final void testCpimParserStringWithBlankContent() {
        CpimMessage msg = (new CpimParser(sCpimToTestWithBlankContent)).getCpimMessage();
        assertEquals("<sip:anonymous@anonymous.invalid>", msg.getHeader("From"));
        assertEquals("<sip:anonymous@anonymous.invalid>", msg.getHeader("To"));
        assertEquals("imdn <urn:ietf:params:imdn>", msg.getHeader(CpimMessage.HEADER_NS));
        assertEquals("2016-03-24T08:51:42+01:00", msg.getHeader("DateTime"));
        assertEquals("text/plain;charset=utf-8", msg.getContentType());
        assertEquals("positive-delivery", msg.getHeader(ImdnUtils.HEADER_IMDN_DISPO_NOTIF));
        assertEquals("ae6926cfcffa40a89e44252ce9e970a2",
                msg.getHeader(ImdnUtils.HEADER_IMDN_MSG_ID));
        assertEquals("  ", msg.getMessageContent());
        assertEquals("2", msg.getContentHeader(CpimMessage.HEADER_CONTENT_LENGTH));
        assertEquals("text/plain;charset=utf-8",
                msg.getContentHeader(CpimMessage.HEADER_CONTENT_TYPE));
    }

    public final void testMultipart() {
        Multipart multipart = new Multipart(sMultiparts, sMultipartsBoundary);
        assertTrue(multipart.isMultipart());
        String cpimPart = multipart.getPart(CpimMessage.MIME_TYPE);
        CpimParser parser = new CpimParser(cpimPart.getBytes(UTF8));
        CpimMessage cpim = parser.getCpimMessage();
        assertEquals("   ", cpim.getMessageContent());
        assertEquals("text/plain;charset=utf-8", cpim.getContentType());
        assertEquals("3", cpim.getContentHeader("Content-length"));
        assertEquals("positive-delivery, display", cpim.getHeader("imdn.Disposition-Notification"));
    }
}
