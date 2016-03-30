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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.im.filetransfer;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.utils.logger.Logger;

import junit.framework.TestCase;

import java.util.Vector;

public class FileTransferSdpUtilsTest extends TestCase {

    private static Logger sLogger = Logger.getLogger(FileTransferSdpUtilsTest.class.getName());

    public void testbuildFtSDP() {

        /*
         * v=0 o=- 3600507138 3600507138 IN IP4 10.29.67.37 s=- c=IN IP4 10.29.67.37 t=0 0 m=message
         * 20000 TCP/MSRP * a=accept-types:image/jpeg a=file-transfer-id:1391518338244
         * a=file-disposition:attachment a=file-selector:name:"phototmp_3_1_1_1.jpg" type:image/jpeg
         * size:195490 a=setup:actpass a=path:msrp://10.29.67.37:20000/1391518338240;tcp a=sendonly
         * a=max-size:15728640
         */
        // @formatter:on

        String ntpTime = "3600507138 3600507138";
        String ipAddress = "10.29.67.37";
        int localMsrpPort = 20000;
        String localSocketProtocol = "TCP/MSRP";
        String localMsrpPath = "msrp://10.29.67.37:20000/1391503972255;tcp";
        String localSetup = "actpass";
        String acceptedTypes = "image/jpeg";
        String fileTransferID = "1391518338244";
        int maxSize = 15728640;
        String fileSelector = "name:\"phototmp_3_1_1_1.jpg\" type:image/jpeg size:195490";
        // / @formatter:off
        String sdp = "v=0" + SipUtils.CRLF
                + "o=- " + ntpTime + " " + ntpTime + " "
                + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF
                + "s=-" + SipUtils.CRLF
                + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF
                + "t=0 0" + SipUtils.CRLF
                + "m=message " + localMsrpPort + " " + localSocketProtocol + " *" + SipUtils.CRLF
                + "a=path:" + localMsrpPath + SipUtils.CRLF
                + "a=setup:" + localSetup + SipUtils.CRLF
                + "a=accept-types: " + acceptedTypes + SipUtils.CRLF
                + "a=file-transfer-id:" + fileTransferID + SipUtils.CRLF
                + "a=file-disposition:attachment" + SipUtils.CRLF
                + "a=sendonly" + SipUtils.CRLF
                + "a=max-size:" + maxSize + SipUtils.CRLF
                + "a=file-selector:" + fileSelector + SipUtils.CRLF;
        // @formatter:on
        sLogger.info("SDP " + sdp);
        // Parse the remote SDP part
        SdpParser parser = new SdpParser(sdp.getBytes());
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription mediaDesc = media.elementAt(0);
        for (MediaDescription mediaDescription : media) {
            sLogger.info(media.toString());
            for (MediaAttribute attribute : mediaDescription.mMediaAttributes) {
                sLogger.info("attribute: (name=" + attribute.getName() + ") (value="
                        + attribute.getValue() + ")");
            }
        }
        assertEquals(mediaDesc.getMediaAttribute("setup").getValue().trim(), localSetup);
        assertEquals(mediaDesc.getMediaAttribute("file-transfer-id").getValue().trim(),
                fileTransferID);
        assertEquals(mediaDesc.getMediaAttribute("file-disposition").getValue().trim(),
                "attachment");

        assertEquals(mediaDesc.getMediaAttribute("max-size").getValue().trim(), "" + maxSize);
        assertEquals(mediaDesc.getMediaAttribute("accept-types").getValue().trim(), acceptedTypes);
        assertEquals(mediaDesc.getMediaAttribute("path").getValue().trim(), localMsrpPath);
        assertEquals(mediaDesc.getMediaAttribute("file-selector").getValue().trim(), fileSelector);

        assertEquals(mediaDesc.mPort, localMsrpPort);
        assertEquals(mediaDesc.mProtocol, localSocketProtocol);
    }

}
