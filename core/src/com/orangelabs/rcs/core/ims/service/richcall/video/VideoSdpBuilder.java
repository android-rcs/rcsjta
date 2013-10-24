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

import java.util.Vector;

import org.gsma.joyn.vsh.VideoCodec;
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
    public static String buildSdpOfferWithoutOrientation(VideoCodec[] supportedCodecs, int localRtpPort) {
        StringBuilder result = new StringBuilder();

        // Create video codec list
        Vector<VideoCodec> codecs = new Vector<VideoCodec>();
        for (int i = 0; i < supportedCodecs.length; i++) {
            codecs.add(supportedCodecs[i]);
        }

        result.append("m=video " + localRtpPort + " RTP/AVP");
        for (VideoCodec codec : codecs) {
            result.append(" ").append(codec.getPayloadType());
        }
        result.append(SipUtils.CRLF);
        int framerate = 0;
        for (VideoCodec codec : codecs) {
            if (codec.getFrameRate() > framerate) {
                framerate = codec.getFrameRate();
            }
        }
        if (framerate > 0) {
            result.append("a=framerate:" + framerate + SipUtils.CRLF);
        }
        for (VideoCodec codec : codecs) {
            result.append("a=rtpmap:" + codec.getPayloadType() + " " + codec.getEncoding() + "/" + codec.getClockRate() + SipUtils.CRLF);
            if (codec.getVideoWidth() != 0 && codec.getVideoHeight() != 0) {
                result.append("a=framesize:" + codec.getPayloadType() + " " + codec.getVideoWidth() + "-" + codec.getVideoHeight() + SipUtils.CRLF);
            }
            result.append("a=fmtp:" + codec.getPayloadType() + " " + codec.getParameters() + SipUtils.CRLF);
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
                .append(" " + SdpOrientationExtension.VIDEO_ORIENTATION_URI).append(SipUtils.CRLF);
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
        StringBuilder sdp = new StringBuilder()
                .append("m=video ").append(localRtpPort).append(" RTP/AVP ")
                .append(videoCodec.getPayloadType()).append(SipUtils.CRLF)
                .append("a=rtpmap:").append(videoCodec.getPayloadType()).append(" ")
                .append(videoCodec.getEncoding()).append("/")
                .append(videoCodec.getClockRate()).append(SipUtils.CRLF);
        if (videoCodec.getVideoWidth() != 0 && videoCodec.getVideoHeight() != 0) {
            sdp.append("a=framesize:").append(videoCodec.getPayloadType()).append(" ")
                    .append(videoCodec.getVideoWidth()).append("-").append(videoCodec.getVideoHeight())
                    .append(SipUtils.CRLF);
        }
        if (videoCodec.getFrameRate() != 0) {
            sdp.append("a=framerate:").append(videoCodec.getFrameRate()).append(SipUtils.CRLF);
        }
        sdp.append("a=fmtp:").append(videoCodec.getPayloadType()).append(" ")
                .append(videoCodec.getParameters()).append(SipUtils.CRLF);
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
                .append(" " + SdpOrientationExtension.VIDEO_ORIENTATION_URI).append(SipUtils.CRLF);
        return sdp.toString();
    }


    /**
     * Builds the {@link MediaCodec} SDP for a SIP INVITE response. If the SIP
     * INVITE SDP doesn't have the orientation extension then the response SDP
     * also shouldn't have.
     * 
     * @param codec Media Codec
     * @param localRtpPort Local RTP Port
     * @param videoMedia Invite video media
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
