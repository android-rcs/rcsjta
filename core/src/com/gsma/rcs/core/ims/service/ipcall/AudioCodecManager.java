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

package com.gsma.rcs.core.ims.service.ipcall;

import java.util.Vector;

import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.service.ipcalldraft.AudioCodec;

/**
 * Audio codec management
 * 
 * @author opob7414
 */
public class AudioCodecManager {

    /**
     * Audio codec negotiation
     * 
     * @param supportedCodecs List of supported media codecs
     * @param proposedCodecs List of proposed audio codecs
     * @return Selected codec or null if no codec supported
     */
    public static AudioCodec negociateAudioCodec(AudioCodec[] supportedCodecs,
            Vector<AudioCodec> proposedCodecs) {
        AudioCodec selectedCodec = null;
        int pref = -1;
        for (int i = 0; i < proposedCodecs.size(); i++) {
            AudioCodec proposedCodec = proposedCodecs.get(i);
            for (int j = 0; j < supportedCodecs.length; j++) {
                AudioCodec audioCodec = supportedCodecs[j];
                int audioCodecPref = supportedCodecs.length - 1 - j;
                // Compare Codec
                if (proposedCodec.compare(audioCodec)) {
                    if (audioCodecPref > pref) {
                        pref = audioCodecPref;
                        selectedCodec = new AudioCodec(proposedCodec.getEncoding(),
                                (proposedCodec.getPayloadType() == 0) ? audioCodec.getPayloadType()
                                        : proposedCodec.getPayloadType(),
                                (proposedCodec.getSampleRate() == 0) ? audioCodec.getSampleRate()
                                        : proposedCodec.getSampleRate(),
                                (proposedCodec.getParameters().length() == 0) ? audioCodec
                                        .getParameters() : proposedCodec.getParameters());
                    }
                }
            }
        }
        return selectedCodec;
    }

    /**
     * Create an audio codec from its SDP description
     * 
     * @param media Media SDP description
     * @return Audio codec
     */
    public static AudioCodec createAudioCodecFromSdp(MediaDescription media) {
        try {
            String rtpmap = media.getMediaAttribute("rtpmap").getValue();

            // Extract encoding name
            String encoding = rtpmap.substring(
                    rtpmap.indexOf(media.mPayload) + media.mPayload.length() + 1).trim();
            String codecName = encoding;

            int index = encoding.indexOf("/");
            if (index != -1) {
                codecName = encoding.substring(0, index);
            }

            // Extract sample rate
            MediaAttribute attr = media.getMediaAttribute("samplerate");
            int sampleRate = 16000; // default value (AMR_WB)
            if (attr != null) {
                try {
                    String value = attr.getValue();
                    index = value.indexOf(media.mPayload);
                    if ((index != -1) && (value.length() > media.mPayload.length())) {
                        sampleRate = Integer.parseInt(value.substring(index
                                + media.mPayload.length() + 1));
                    } else {
                        sampleRate = Integer.parseInt(value);
                    }
                } catch (NumberFormatException e) {
                    // Use default value
                }
            }

            // Extract the audio codec parameters.
            MediaAttribute fmtp = media.getMediaAttribute("fmtp");
            String codecParameters = "";
            if (fmtp != null) {
                String value = fmtp.getValue();
                index = 0; // value.indexOf(media.payload);
                if ((index != -1) && (value.length() > media.mPayload.length())) {
                    codecParameters = value.substring(index + media.mPayload.length() + 1);
                }
            }

            // Create an audio codec
            AudioCodec audioCodec = new AudioCodec(codecName, Integer.parseInt(media.mPayload),
                    sampleRate, codecParameters);
            return audioCodec;
        } catch (NullPointerException e) {
            return null;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Extract list of audio codecs from SDP part
     * 
     * @param sdp SDP part
     * @return List of audio codecs
     */
    public static Vector<AudioCodec> extractAudioCodecsFromSdp(Vector<MediaDescription> medias) {
        Vector<AudioCodec> list = new Vector<AudioCodec>();
        for (int i = 0; i < medias.size(); i++) {
            AudioCodec codec = createAudioCodecFromSdp(medias.get(i));
            if (codec != null) {
                list.add(codec);
            }
        }
        return list;
    }
}
