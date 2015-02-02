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
package com.orangelabs.rcs.richcall;

import java.util.Vector;

//import android.media.MediaCodec;
import android.test.AndroidTestCase;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1_2;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoCodecManager;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoSdpBuilder;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.vsh.VideoCodec;

public class VideoSdpTest extends AndroidTestCase {
	private static int RTP_PORT = 12345;

	// @formatter:off
    private static String videoSdp = 
            "v=tester" + SipUtils.CRLF +
            "m=video 12345 RTP/AVP 99 98 97 96" + SipUtils.CRLF +
            "a=framerate:15" + SipUtils.CRLF +
            "a=rtpmap:99 H264/90000" + SipUtils.CRLF +
            "a=framesize:99 352-288" + SipUtils.CRLF +
            "a=fmtp:99 profile-level-id=42800c;packetization-mode=1" + SipUtils.CRLF +
            "a=rtpmap:98 H264/90000" + SipUtils.CRLF +
            "a=framesize:98 352-288" + SipUtils.CRLF +
            "a=fmtp:98 profile-level-id=42800c;packetization-mode=1" + SipUtils.CRLF +
            "a=rtpmap:97 H264/90000" + SipUtils.CRLF +
            "a=framesize:97 320-240" + SipUtils.CRLF +
            "a=fmtp:97 profile-level-id=42800c;packetization-mode=1" + SipUtils.CRLF +
            "a=rtpmap:96 H264/90000" + SipUtils.CRLF +
            "a=framesize:96 176-144" + SipUtils.CRLF +
            "a=fmtp:96 profile-level-id=42900b;packetization-mode=1" + SipUtils.CRLF;
    private static String videoSdp2 = 
            "v=tester" + SipUtils.CRLF +
            "m=video 12345 RTP/AVP 99 98 97 96" + SipUtils.CRLF +
            "b=AS:128" + SipUtils.CRLF +
            "b=RS:256" + SipUtils.CRLF +
            "b=RR:1024" + SipUtils.CRLF +
            "a=rtpmap:99 H264/90000" + SipUtils.CRLF +
            "a=framesize:99 352-288" + SipUtils.CRLF +
            "a=framerate:99 15" + SipUtils.CRLF +
            "a=fmtp:99 profile-level-id=42800c;packetization-mode=1" + SipUtils.CRLF +
            "a=rtpmap:98 H264/90000" + SipUtils.CRLF +
            "a=framesize:98 352-288" + SipUtils.CRLF +
            "a=framerate:98 12" + SipUtils.CRLF +
            "a=fmtp:98 profile-level-id=42800c;packetization-mode=1" + SipUtils.CRLF +
            "a=rtpmap:97 H264/90000" + SipUtils.CRLF +
            "a=framesize:97 320-240" + SipUtils.CRLF +
            "a=framerate:97 12" + SipUtils.CRLF +
            "a=fmtp:97 profile-level-id=42800c;packetization-mode=1" + SipUtils.CRLF +
            "a=rtpmap:96 H264/90000" + SipUtils.CRLF +
            "a=framesize:96 176-144" + SipUtils.CRLF +
            "a=framerate:96 10" + SipUtils.CRLF +
            "a=fmtp:96 profile-level-id=42900b;packetization-mode=1" + SipUtils.CRLF;
    // @formatter:on
	private VideoCodec[] codecs;

	protected void setUp() throws Exception {
		super.setUp();

		RcsSettings.createInstance(getContext());

		// @formatter:off
        // Create list of codecs
        codecs = new VideoCodec[4];
        int payload_count = 95;
        codecs[3] = new VideoCodec(H264Config.CODEC_NAME,
                ++payload_count,
                H264Config.CLOCK_RATE,
                10,
                96000,
                H264Config.QCIF_WIDTH, 
                H264Config.QCIF_HEIGHT,
        H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=1");
        codecs[2] = new VideoCodec(H264Config.CODEC_NAME,
                ++payload_count,
                H264Config.CLOCK_RATE,
                12,
                256,
                H264Config.QVGA_WIDTH, 
                H264Config.QVGA_HEIGHT,
//                H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1_2.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=1").getMediaCodec();
                H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1_2.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=1");
        codecs[1] = new VideoCodec(H264Config.CODEC_NAME,
                ++payload_count,
                H264Config.CLOCK_RATE,
                12,
                256,
                H264Config.CIF_WIDTH, 
                H264Config.CIF_HEIGHT,
               H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1_2.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=1");
        codecs[0] = new VideoCodec(H264Config.CODEC_NAME,
                ++payload_count,
                H264Config.CLOCK_RATE,
                15,
                396,
                H264Config.CIF_WIDTH, 
                H264Config.CIF_HEIGHT,
               H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1_2.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=1");
     // @formatter:on
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCreateSdp() {
		// Create SDP
		String createdSdp = "v=tester" + SipUtils.CRLF + VideoSdpBuilder.buildSdpOfferWithoutOrientation(codecs, RTP_PORT);
		// TEST SDP
		assertEquals(videoSdp, createdSdp);
	}

	public void testParseSdp() {
		// Parse the remote SDP part
		SdpParser parser = new SdpParser(videoSdp2.getBytes());

		// Test port
		MediaDescription mediaVideo = parser.getMediaDescription("video");
		int port = mediaVideo.port;
		assertEquals(port, RTP_PORT);

		// Test codecs
		Vector<MediaDescription> medias = parser.getMediaDescriptions("video");
		Vector<VideoCodec> proposedCodecs = VideoCodecManager.extractVideoCodecsFromSdp(medias);
		assertEquals(proposedCodecs.size(), codecs.length);
		for (int i = 0; i < proposedCodecs.size(); i++) {
			VideoCodec codec = codecs[i];
			assertEquals(proposedCodecs.elementAt(i).getEncoding(), codec.getEncoding());
			assertEquals(proposedCodecs.elementAt(i).getPayloadType(), codec.getPayloadType());
			assertEquals(proposedCodecs.elementAt(i).getParameters(), codec.getParameters());
			assertEquals(proposedCodecs.elementAt(i).getFrameRate(), codec.getFrameRate());
			assertEquals(proposedCodecs.elementAt(i).getWidth(), codec.getWidth());
			assertEquals(proposedCodecs.elementAt(i).getHeight(), codec.getHeight());
			// Bitrate and order pref not tested because not in SDP
		}
	}
}
