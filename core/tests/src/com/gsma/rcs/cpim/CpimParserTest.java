
package com.gsma.rcs.cpim;

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
    }

}
