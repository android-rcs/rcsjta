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

package com.orangelabs.rcs.core.ims.service.ipcall;

import java.util.Vector;

import com.gsma.services.rcs.ipcall.AudioCodec;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;

/**
 * Builds the audio SDP
 * 
 * @author Olivier Briand
 */
public class AudioSdpBuilder {

    /**
     * Build SDP offer for audio
     * 
     * @param localRtpPort Local RTP port
     * @return SDP offer
     */
    public static String buildSdpOffer(AudioCodec[] supportedCodecs, int localRtpPort) {
        StringBuilder result = new StringBuilder();

        // Create video codec list
        Vector<AudioCodec> codecs = new Vector<AudioCodec>();
        for (int i = 0; i < supportedCodecs.length; i++) {
            codecs.add(supportedCodecs[i]);
        }

        // First Sdp line
        result.append("m=audio " + localRtpPort + " RTP/AVP");
        for (AudioCodec codec : codecs) {
            result.append(" ").append(codec.getPayloadType());
        }
        result.append(SipUtils.CRLF);

        // For each codecs
        for (AudioCodec codec : codecs) {
            result.append("a=rtpmap:" + codec.getPayloadType() + " " + codec.getEncoding() + "/"
                    + codec.getSampleRate() + SipUtils.CRLF);
            if (!codec.getParameters().equals(""))
                result.append("a=fmtp:" + codec.getPayloadType() + " " + codec.getParameters()
                        + SipUtils.CRLF);
        }

        return result.toString();
    }

    /**
     * Build SDP answer for audio
     * 
     * @param selectedMediaCodec Selected audio codec after negociation
     * @param localRtpPort Local RTP Port
     * @return SDP answer
     */
    public static String buildSdpAnswer(AudioCodec selectedMediaCodec, int localRtpPort) {
        StringBuilder result = new StringBuilder();
        result.append("m=audio " + localRtpPort + " RTP/AVP");
        AudioCodec codec = selectedMediaCodec;
        result.append(" ").append(codec.getPayloadType());
        result.append(SipUtils.CRLF);
        result.append("a=rtpmap:" + codec.getPayloadType() + " " + codec.getEncoding() + "/"
                + codec.getSampleRate() + SipUtils.CRLF);
        return result.toString();
    }
}
