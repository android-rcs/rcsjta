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

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;

import android.test.AndroidTestCase;

import java.util.Vector;

public class ChatSdpUtilsTest extends AndroidTestCase {

    public void testbuildChatSDP() {

        /***
         * v=0 o=- 3667904944 3667904944 IN IP4 192.168.1.50 s=- c=IN IP4 192.168.1.50 t=0 0
         * m=message 20000 TCP/MSRP * a=accept-types:message/cpim application/im-iscomposing+xml
         * a=accept-wrapped-types:text/plain message/imdn+xml
         * application/vnd.gsma.rcspushlocation+xml application/vnd.gsma.rcs-ft-http+xml
         * a=setup:actpass a=path:msrp://192.168.1.50:20000/1458916144436;tcp a=sendrecv
         */
        // Parse the remote SDP part
        String ntpTime = "3667904944 3667904944";
        String ipAddress = "192.168.1.50";
        int localMsrpPort = 20000;
        String localSocketProtocol = "TCP/MSRP";
        String localMsrpPath = "msrp://10.29.67.37:20000/1391503972255;tcp";
        String localSetup = "actpass";
        String acceptedTypes = "message/cpim application/im-iscomposing+xml";
        String wrappedTypes = "text/plain message/imdn+xml application/vnd.gsma.rcspushlocation+xml application/vnd.gsma.rcs-ft-http+xml";

        //@formatter:off
        String sdp = "v=0" + SipUtils.CRLF
                + "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF
                + "s=-" + SipUtils.CRLF
                + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF
                + "t=0 0" + SipUtils.CRLF
                + "m=message " + localMsrpPort + " " + localSocketProtocol + " *" + SipUtils.CRLF
                + "a=path:" + localMsrpPath + SipUtils.CRLF
                + "a=setup:" + localSetup + SipUtils.CRLF
                + "a=accept-types:" + acceptedTypes + SipUtils.CRLF
                + "a=accept-wrapped-types:" + wrappedTypes + SipUtils.CRLF
                + "a=sendrecv" + SipUtils.CRLF;

        //@formatter:on
        SdpParser parser = new SdpParser(sdp.getBytes());
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        assertEquals(mediaDesc.getMediaAttribute("setup").getValue(), localSetup);
        assertEquals(mediaDesc.getMediaAttribute("accept-types").getValue(), acceptedTypes);
        assertEquals(mediaDesc.getMediaAttribute("accept-wrapped-types").getValue(),
                wrappedTypes);
        assertEquals(mediaDesc.getMediaAttribute("path").getValue(), localMsrpPath);
        assertEquals(mediaDesc.mPort, localMsrpPort);
        assertEquals(mediaDesc.mProtocol, localSocketProtocol);
    }
}
