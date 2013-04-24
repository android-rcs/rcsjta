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

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Builds the Video SDP
 * 
 * @author Deutsche Telekom
 */
public class VideoSdpBuilder {

    /**
     * RTP Extension ID used by the client. The extension ID is a value between
     * 1 and 15 arbitrarily chosen by the sender, as defined in RFC5285
     */
    public static final int DEFAULT_EXTENSION_ID = 9;

    /**
     * Extension attribute name, RFC5285
     */
    public static final String ATTRIBUTE_EXTENSION = "extmap";

    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(VideoSdpBuilder.class.getName());

    /**
     * Creates the SIP INVITE {@link MediaCodec} SDP without the orientation
     * extension, ordered by the preferred codec
     * 
     * @param supportedCodecs Codecs to create SDP
     * @param localRtpPort Local RTP port
     * @return The SDP for all the codecs
     */
    public static String buildSdpWithoutOrientation(MediaCodec[] supportedCodecs, int localRtpPort) {
        StringBuilder result = new StringBuilder();

        // Create video codec list
        Vector<VideoCodec> codecs = new Vector<VideoCodec>();
        for (int i = 0; i < supportedCodecs.length; i++) {
            codecs.add(new VideoCodec(supportedCodecs[i]));
        }

        result.append("m=video " + localRtpPort + " RTP/AVP");
        for (VideoCodec codec : codecs) {
            result.append(" ").append(codec.getPayload());
        }
        result.append(SipUtils.CRLF);
        for (VideoCodec codec : codecs) {
            result.append("a=rtpmap:" + codec.getPayload() + " " + codec.getCodecName() + "/" + codec.getClockRate() + SipUtils.CRLF);
            if (codec.getWidth() != 0 && codec.getHeight() != 0) {
                result.append("a=framesize:" + codec.getPayload() + " " + codec.getWidth() + "-" + codec.getHeight() + SipUtils.CRLF);
            }
            if (codec.getFramerate() != 0) {
                result.append("a=framerate:" + codec.getPayload() + " " + codec.getFramerate() + SipUtils.CRLF);
            }
            result.append("a=fmtp:" + codec.getPayload() + " " + codec.getCodecParams() + SipUtils.CRLF);
        }

        return result.toString();
    }

    /**
     * Creates the SIP INVITE {@link MediaCodec} SDP with the orientation
     * extension, ordered by the preferred codec
     * 
     * @param supportedCodecs Codecs to create SDP
     * @param localRtpPort Local RTP port
     * @return The SDP for all the codecs
     */
    public static String buildSdpWithOrientationExtension(MediaCodec[] supportedCodecs, int localRtpPort) {
        StringBuilder sdp = new StringBuilder(buildSdpWithoutOrientation(supportedCodecs, localRtpPort))
                .append("a=").append(ATTRIBUTE_EXTENSION).append(':').append(DEFAULT_EXTENSION_ID)
                .append(" urn:gsma:video-orientation").append(SipUtils.CRLF);
        return sdp.toString();
    }

    /**
     * Create the SDP part for a given codec
     *
     * @param codec Media codec
     * @param localRtpPort Local RTP port
     * @return SDP
     */
    private static String buildSdpWithoutOrientation(MediaCodec codec, int localRtpPort) {
        if (codec == null) {
            logger.info("Invalid codec");
            return "";
        }

        VideoCodec videoCodec = new VideoCodec(codec);
        StringBuilder sdp = new StringBuilder()
                .append("m=video ").append(localRtpPort).append(" RTP/AVP ")
                .append(videoCodec.getPayload()).append(SipUtils.CRLF)
                .append("a=rtpmap:").append(videoCodec.getPayload()).append(" ")
                .append(videoCodec.getCodecName()).append("/")
                .append(videoCodec.getClockRate()).append(SipUtils.CRLF);
        if (videoCodec.getWidth() != 0 && videoCodec.getHeight() != 0) {
            sdp.append("a=framesize:").append(videoCodec.getPayload()).append(" ")
                    .append(videoCodec.getWidth()).append("-").append(videoCodec.getHeight())
                    .append(SipUtils.CRLF);
        }
        if (videoCodec.getFramerate() != 0) {
            sdp.append("a=framerate:").append(videoCodec.getFramerate()).append(SipUtils.CRLF);
        }
        sdp.append("a=fmtp:").append(videoCodec.getPayload()).append(" ")
                .append(videoCodec.getCodecParams()).append(SipUtils.CRLF);
        return sdp.toString();
    }

    /**
     * Create the SDP part with orientation extension for a given codec
     *
     * @param codec
     * @param localRtpPort
     * @param extensionId
     * @return
     */
    private static String buildSdpWithOrientationExtension(MediaCodec codec, int localRtpPort,
            int extensionId) {
        StringBuilder sdp = new StringBuilder(buildSdpWithoutOrientation(codec, localRtpPort))
                .append("a=").append(ATTRIBUTE_EXTENSION).append(':').append(extensionId)
                .append(" urn:gsma:video-orientation").append(SipUtils.CRLF);
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
     * @return Response SDP
     */
    public static String buildResponseSdp(MediaCodec codec, int localRtpPort,
            MediaDescription inviteVideoMedia) {
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
