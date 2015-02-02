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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gsma.services.rcs.vsh.VideoCodec;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.rtp.RtpUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;

/**
 * Builds the Video SDP
 * 
 * @author Deutsche Telekom
 */
public class VideoSdpBuilder {

    /**
     * Extension attribute name, RFC5285
     */
    public static final String ATTRIBUTE_EXTENSION = "extmap";

    /**
     * Build SDP offer without the orientation extension ordered by the
     * preferred codec
     * 
     * @param supportedCodecs Codecs to create SDP
     * @param localRtpPort Local RTP port
     * @return SDP offer
     */
	public static String buildSdpOfferWithoutOrientation(VideoCodec[] supportedCodecs,
			int localRtpPort) {
		// Create video codec list
		List<VideoCodec> codecs = new ArrayList<VideoCodec>(Arrays.asList(supportedCodecs));

		StringBuilder result = new StringBuilder("m=video ").append(localRtpPort)
				.append(" RTP/AVP");
		for (VideoCodec codec : codecs) {
			result.append(" ").append(codec.getPayloadType());
		}
		result.append(SipUtils.CRLF);
		int framerate = 0;
		for (VideoCodec codec : codecs) {
			int codeFrameRate = codec.getFrameRate();
			if (codeFrameRate > framerate) {
				framerate = codeFrameRate;
			}
		}
		if (framerate > 0) {
			result.append("a=framerate:").append(framerate).append(SipUtils.CRLF);
		}
		for (VideoCodec codec : codecs) {
			int payloadType = codec.getPayloadType();
			result.append("a=rtpmap:").append(payloadType).append(" ").append(codec.getEncoding())
					.append("/").append(codec.getClockRate()).append(SipUtils.CRLF);
			int width = codec.getWidth();
			int height = codec.getHeight();
			if (width != 0 && height != 0) {
				result.append("a=framesize:").append(payloadType).append(" ").append(width)
						.append("-").append(height).append(SipUtils.CRLF);
			}
			result.append("a=fmtp:").append(payloadType).append(" ").append(codec.getParameters())
					.append(SipUtils.CRLF);
		}

		return result.toString();
	}

    /**
     * Build SDP offer without the orientation extension ordered by the
     * preferred codec
     * 
     * @param supportedCodecs Codecs to create SDP
     * @param localRtpPort Local RTP port
     * @return SDP offer
     */
    public static String buildSdpOfferWithOrientation(VideoCodec[] supportedCodecs, int localRtpPort) {
        StringBuilder sdp = new StringBuilder(buildSdpOfferWithoutOrientation(supportedCodecs, localRtpPort))
                .append("a=").append(ATTRIBUTE_EXTENSION).append(':').append(RtpUtils.RTP_DEFAULT_EXTENSION_ID)
                .append(" ").append(SdpOrientationExtension.VIDEO_ORIENTATION_URI).append(SipUtils.CRLF);
        return sdp.toString();
    }

    /**
     * Create the SDP part for a given codec
     *
     * @param codec Media codec
     * @param localRtpPort Local RTP port
     * @return SDP
     */
    private static String buildSdpWithoutOrientation(VideoCodec videoCodec, int localRtpPort) {
		int payloadType = videoCodec.getPayloadType();
		StringBuilder sdp = new StringBuilder("m=video ").append(localRtpPort).append(" RTP/AVP ")
				.append(payloadType).append(SipUtils.CRLF).append("a=rtpmap:").append(payloadType)
				.append(" ").append(videoCodec.getEncoding()).append("/")
				.append(videoCodec.getClockRate()).append(SipUtils.CRLF);
		int width = videoCodec.getWidth();
		int height = videoCodec.getHeight();
		if (width != 0 && height != 0) {
			sdp.append("a=framesize:").append(payloadType).append(" ").append(width).append("-")
					.append(height).append(SipUtils.CRLF);
		}
		int frameRate = videoCodec.getFrameRate();
		if (frameRate != 0) {
			sdp.append("a=framerate:").append(frameRate).append(SipUtils.CRLF);
		}
		sdp.append("a=fmtp:").append(frameRate).append(" ").append(videoCodec.getParameters())
				.append(SipUtils.CRLF);
		return sdp.toString();
    }

    /**
     * Create the SDP part with orientation extension for a given codec
     *
     * @param codec Media Codec
     * @param localRtpPort Local RTP Port
     * @param extensionId
     * @return SDP
     */
    private static String buildSdpWithOrientationExtension(VideoCodec codec, int localRtpPort, int extensionId) {
        StringBuilder sdp = new StringBuilder(buildSdpWithoutOrientation(codec, localRtpPort))
                .append("a=").append(ATTRIBUTE_EXTENSION).append(':').append(extensionId)
                .append(" ").append(SdpOrientationExtension.VIDEO_ORIENTATION_URI).append(SipUtils.CRLF);
        return sdp.toString();
    }


    /**
     * Builds the SDP for a SIP INVITE response. If the SIP INVITE SDP
     * doesn't have the orientation extension then the response SDP
     * also shouldn't have.
     * 
     * @param codec Media Codec
     * @param localRtpPort Local RTP Port
     * @param inviteVideoMedia 
     * @return SDP answer
     */
    public static String buildSdpAnswer(VideoCodec codec, int localRtpPort, MediaDescription inviteVideoMedia) {
        if (inviteVideoMedia != null) {
            SdpOrientationExtension extension = SdpOrientationExtension.create(inviteVideoMedia);
            if (extension != null) {
                return buildSdpWithOrientationExtension(codec, localRtpPort,
                        extension.getExtensionId());
            }
        }

        return buildSdpWithoutOrientation(codec, localRtpPort);
    }
}
