/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
package com.orangelabs.rcs.chat;

import java.util.Vector;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.utils.logger.Logger;

public class ChatSdpUtilsTest extends AndroidTestCase {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String sdp = null;
	private String ipAddress = null;
	private int localMsrpPort = -1;
	private String localSocketProtocol = null;
	private String acceptedTypes = null;
	private String wrappedTypes = null;
	private String localSetup = null;
	private String localMsrpPath = null;

	protected void setUp() throws Exception {
		super.setUp();
		// @formatter:off
		/*
				v=0
				o=- 3600492772 3600492772 IN IP4 10.29.67.37
				s=-
				c=IN IP4 10.29.67.37
				t=0 0
				m=message 20000 TCP/MSRP *
				a=path:msrp://10.29.67.37:20000/1391503972255;tcp
				a=setup:actpass
				a=accept-types:message/cpim application/im-iscomposing+xml
				a=accept-wrapped-types:text/plain message/imdn+xml application/vnd.gsma.rcspushlocation+xml application/vnd.gsma.rcs-ft-http+xml
				a=sendrecv
		*/
		// @formatter:on
		String ntpTime = "3600492772";
		ipAddress = "10.29.67.37";
		localMsrpPort = 2000;
		localSocketProtocol = "TCP/MSRP";
		localMsrpPath = "msrp://10.29.67.37:20000/1391503972255;tcp";
		localSetup = "actpass";
		acceptedTypes = "message/cpim application/im-iscomposing+xml";
		wrappedTypes = "text/plain message/imdn+xml application/vnd.gsma.rcspushlocation+xml application/vnd.gsma.rcs-ft-http+xml";
		sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress)
				+ SipUtils.CRLF + "s=-" + SipUtils.CRLF + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
				+ SipUtils.CRLF + "m=message " + localMsrpPort + " " + localSocketProtocol + " *" + SipUtils.CRLF + "a=path:"
				+ localMsrpPath + SipUtils.CRLF + "a=setup:" + localSetup + SipUtils.CRLF + "a=accept-types:" + acceptedTypes
				+ SipUtils.CRLF + "a=accept-wrapped-types:" + wrappedTypes + SipUtils.CRLF + "a=sendrecv" + SipUtils.CRLF;

	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testbuildChatSDP() {
		String buildSdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, localSocketProtocol, acceptedTypes, wrappedTypes,
				localSetup, localMsrpPath, SdpUtils.DIRECTION_SENDRECV);
		logger.info("built SDP " + buildSdp);
		logger.info("SDP " + sdp);
		// Parse the remote SDP part
		SdpParser parser = new SdpParser(buildSdp.getBytes());

		Vector<MediaDescription> media = parser.getMediaDescriptions();
		MediaDescription mediaDesc = media.elementAt(0);
		// for (MediaDescription mediaDescription : media) {
		// logger.info(media.toString());
		// for (MediaAttribute attribute : mediaDescription.mediaAttributes) {
		// logger.info("attribute: (name=" + attribute.getName() + ") (value=" + attribute.getValue() + ")");
		// }
		//
		// }
		assertEquals(mediaDesc.getMediaAttribute("setup").getValue(), localSetup);
		assertEquals(mediaDesc.getMediaAttribute("accept-types").getValue(), acceptedTypes);
		assertEquals(mediaDesc.getMediaAttribute("accept-wrapped-types").getValue(), wrappedTypes);
		assertEquals(mediaDesc.getMediaAttribute("path").getValue(), localMsrpPath);
		assertEquals(mediaDesc.port, localMsrpPort);
		assertEquals(mediaDesc.protocol, localSocketProtocol);
	}
}
