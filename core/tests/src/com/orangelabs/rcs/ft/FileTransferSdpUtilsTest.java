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
package com.orangelabs.rcs.ft;

import java.util.Vector;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import junit.framework.TestCase;

public class FileTransferSdpUtilsTest extends TestCase {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String sdp = null;
	private String ipAddress = null;
	private int localMsrpPort = -1;
	private String localSocketProtocol = null;
	private String acceptedTypes = null;
	private String localSetup = null;
	private String localMsrpPath = null;
	private String fileTransferID = null;
	private int maxSize = -1;
	private String fileSelector = null;

	protected void setUp() throws Exception {

		super.setUp();
		// @formatter:off
		/*
				v=0
				o=- 3600507138 3600507138 IN IP4 10.29.67.37
				s=-
				c=IN IP4 10.29.67.37
				t=0 0
				m=message 20000 TCP/MSRP *
				a=accept-types:image/jpeg
				a=file-transfer-id:1391518338244
				a=file-disposition:attachment
				a=file-selector:name:"phototmp_3_1_1_1.jpg" type:image/jpeg size:195490
				a=setup:actpass
				a=path:msrp://10.29.67.37:20000/1391518338240;tcp
				a=sendonly
				a=max-size:15728640
		*/
		// @formatter:on

		String ntpTime = "3600492772";
		ipAddress = "10.29.67.37";
		localMsrpPort = 2000;
		localSocketProtocol = "TCP/MSRP";
		localMsrpPath = "msrp://10.29.67.37:20000/1391503972255;tcp";
		localSetup = "actpass";
		acceptedTypes = "image/jpeg";
		fileTransferID = "1391518338244";
		maxSize = 15728640;
		fileSelector = "name:\"phototmp_3_1_1_1.jpg\" type:image/jpeg size:195490";
		sdp = "v=0" + SipUtils.CRLF + "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress)
				+ SipUtils.CRLF + "s=-" + SipUtils.CRLF + "c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + "t=0 0"
				+ SipUtils.CRLF + "m=message " + localMsrpPort + " " + localSocketProtocol + " *" + SipUtils.CRLF + "a=path:"
				+ localMsrpPath + SipUtils.CRLF + "a=setup:" + localSetup + SipUtils.CRLF + "a=accept-types: " + acceptedTypes
				+ SipUtils.CRLF + "a=file-transfer-id:" + fileTransferID + SipUtils.CRLF + "a=file-disposition:attachment"
				+ SipUtils.CRLF + "a=sendonly" + SipUtils.CRLF + "a=max-size:" + maxSize + SipUtils.CRLF + "a=file-selector:"
				+ fileSelector + SipUtils.CRLF;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testbuildFtSDP() {
		String buildSdp = SdpUtils.buildFileSDP(ipAddress, localMsrpPort, localSocketProtocol, acceptedTypes, fileTransferID,
				fileSelector, "attachment", localSetup, localMsrpPath, SdpUtils.DIRECTION_SENDONLY, maxSize);
		logger.info("built SDP " + buildSdp);
		logger.info("SDP " + sdp);
		// Parse the remote SDP part
		SdpParser parser = new SdpParser(buildSdp.getBytes());

		Vector<MediaDescription> media = parser.getMediaDescriptions();
		MediaDescription mediaDesc = media.elementAt(0);
		for (MediaDescription mediaDescription : media) {
			logger.info(media.toString());
			for (MediaAttribute attribute : mediaDescription.mediaAttributes) {
				logger.info("attribute: (name=" + attribute.getName() + ") (value=" + attribute.getValue() + ")");
			}

		}
		assertEquals(mediaDesc.getMediaAttribute("setup").getValue(), localSetup);
		assertEquals(mediaDesc.getMediaAttribute("file-transfer-id").getValue(), fileTransferID);
		assertEquals(mediaDesc.getMediaAttribute("file-disposition").getValue(), "attachment");
		assertEquals(mediaDesc.getMediaAttribute("max-size").getValue(), "" + maxSize);
		assertEquals(mediaDesc.getMediaAttribute("accept-types").getValue(), acceptedTypes);
		assertEquals(mediaDesc.getMediaAttribute("path").getValue(), localMsrpPath);
		assertEquals(mediaDesc.getMediaAttribute("file-selector").getValue(), fileSelector);
		assertEquals(mediaDesc.port, localMsrpPort);
		assertEquals(mediaDesc.protocol, localSocketProtocol);
	}

}
