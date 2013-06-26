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
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
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
    public static String buildSdpWithoutOrientation(VideoCodec[] supportedCodecs, int localRtpPort) {
        StringBuilder result = new StringBuilder();

        // Create video codec list
        Vector<VideoCodec> codecs = new Vector<VideoCodec>();
        for (int i = 0; i < supportedCodecs.length; i++) {
            codecs.add(supportedCodecs[i]);
        }

        result.append("m=video " + localRtpPort + " RTP/AVP");
        for (VideoCodec codec : codecs) {
            result.append(" ").append(codec.getPayload());
        }
        result.append(SipUtils.CRLF);
        for (VideoCodec codec : codecs) {
            result.append("a=rtpmap:" + codec.getPayload() + " " + codec.getEncoding() + "/" + codec.getClockRate() + SipUtils.CRLF);
            if (codec.getVideoWidth() != 0 && codec.getVideoHeight() != 0) {
                result.append("a=framesize:" + codec.getPayload() + " " + codec.getVideoWidth() + "-" + codec.getVideoHeight() + SipUtils.CRLF);
            }
            if (codec.getFrameRate() != 0) {
                result.append("a=framerate:" + codec.getPayload() + " " + codec.getFrameRate() + SipUtils.CRLF);
            }
            result.append("a=fmtp:" + codec.getPayload() + " " + codec.getParameters() + SipUtils.CRLF);
        }

        return result.toString();
    }

    /**
     * Create the SDP part for a given codec
     *
     * @param codec Media codec
     * @param localRtpPort Local RTP port
     * @return SDP
     */
    private static String buildSdpWithoutOrientation(VideoCodec codec, int localRtpPort) {
        if (codec == null) {
            logger.info("Invalid codec");
            return "";
        }

        StringBuilder sdp = new StringBuilder()
                .append("m=video ").append(localRtpPort).append(" RTP/AVP ")
                .append(codec.getPayload()).append(SipUtils.CRLF)
                .append("a=rtpmap:").append(codec.getPayload()).append(" ")
                .append(codec.getEncoding()).append("/")
                .append(codec.getClockRate()).append(SipUtils.CRLF);
        if (codec.getVideoWidth() != 0 && codec.getVideoHeight() != 0) {
            sdp.append("a=framesize:").append(codec.getPayload()).append(" ")
                    .append(codec.getVideoWidth()).append("-").append(codec.getVideoHeight())
                    .append(SipUtils.CRLF);
        }
        if (codec.getFrameRate() != 0) {
            sdp.append("a=framerate:").append(codec.getFrameRate()).append(SipUtils.CRLF);
        }
        sdp.append("a=fmtp:").append(codec.getPayload()).append(" ")
                .append(codec.getParameters()).append(SipUtils.CRLF);
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
    public static String buildResponseSdp(VideoCodec codec, int localRtpPort, MediaDescription inviteVideoMedia) {
        return buildSdpWithoutOrientation(codec, localRtpPort);
    }
}
