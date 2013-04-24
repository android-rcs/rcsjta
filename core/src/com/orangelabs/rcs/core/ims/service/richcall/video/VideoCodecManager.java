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

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;

/**
 * Video codec management
 *
 * @author hlxn7157
 */
public class VideoCodecManager {

    /**
     * Video codec negotiation
     *
     * @param supportedCodecs List of supported media codecs
     * @param proposedCodecs List of proposed video codecs
     * @return Selected codec or null if no codec supported
     */
    public static VideoCodec negociateVideoCodec(MediaCodec[] supportedCodecs, Vector<VideoCodec> proposedCodecs) {
        VideoCodec selectedCodec = null;
        int pref = -1;
        for (int i = 0; i < proposedCodecs.size(); i++) {
            VideoCodec proposedCodec = proposedCodecs.get(i);
            for (int j = 0; j < supportedCodecs.length; j++) {
                VideoCodec videoCodec = new VideoCodec(supportedCodecs[j]);
                int videoCodecPref = supportedCodecs.length - 1 - j;
                // Compare Codec
                if (proposedCodec.compare(videoCodec)) {
                    if (videoCodecPref > pref) {
                        pref = videoCodecPref;
                        selectedCodec = new VideoCodec(proposedCodec.getCodecName(),
                                (proposedCodec.getPayload() == 0) ? videoCodec.getPayload() : proposedCodec.getPayload(),
                                (proposedCodec.getClockRate() == 0) ? videoCodec.getClockRate() : proposedCodec.getClockRate(),
                                (proposedCodec.getCodecParams().length() == 0) ? videoCodec.getCodecParams() : proposedCodec.getCodecParams(),
                                (proposedCodec.getFramerate() == 0) ? videoCodec.getFramerate() : proposedCodec.getFramerate(),
                                (proposedCodec.getBitrate() == 0) ? videoCodec.getBitrate() : proposedCodec.getBitrate(),
                                (proposedCodec.getWidth() == 0) ? videoCodec.getWidth() : proposedCodec.getWidth(),
                                (proposedCodec.getHeight() == 0) ? videoCodec.getHeight() : proposedCodec.getHeight());
                    }
                }
            }
        }
        return selectedCodec;
    }

    

    /**
     * Create a video codec from its SDP description
     *
     * @param media Media SDP description
     * @return Video codec
     */
    public static VideoCodec createVideoCodecFromSdp(MediaDescription media) {
    	try {
	        String rtpmap = media.getMediaAttribute("rtpmap").getValue();
	
	        // Extract encoding name
	        String encoding = rtpmap.substring(rtpmap.indexOf(media.payload)
	        		+ media.payload.length() + 1).trim();
	        String codecName = encoding;
	
	        // Extract clock rate
	        int clockRate = 0;
	        int index = encoding.indexOf("/");
	        if (index != -1) {
	            codecName = encoding.substring(0, index);
	            clockRate = Integer.parseInt(encoding.substring(index + 1));
	        }
	
	        // Extract video size
	        MediaAttribute frameSize = media.getMediaAttribute("framesize");
            int videoWidth = 0;
            int videoHeight = 0;
	        if (frameSize != null) {
	        	try {
		            String value = frameSize.getValue();
		            index = value.indexOf(media.payload);
		            int separator = value.indexOf('-');
		            if ((index != -1) && (separator != -1)) {
			            videoWidth = Integer.parseInt(
			            		value.substring(index + media.payload.length() + 1,
			                    separator));
			            videoHeight = Integer.parseInt(value.substring(separator + 1));
		            }
	        	} catch(NumberFormatException e) {
	        		// Use default value
	        	}
	        }

	        // Extract frame rate
	        MediaAttribute attr = media.getMediaAttribute("framerate");
	        int frameRate = H264Config.FRAME_RATE; // default value
	        if (attr != null) {
	            try {
	                String value = attr.getValue();
                    index = value.indexOf(media.payload);
                    if ((index != -1) && (value.length() > media.payload.length())) {
                        frameRate = Integer.parseInt(value.substring(index + media.payload.length() + 1));
                    } else {
                        frameRate = Integer.parseInt(value);
                    }
                } catch(NumberFormatException e) {
                    // Use default value
                }
            }

	        // Extract the video codec parameters.
	        MediaAttribute fmtp = media.getMediaAttribute("fmtp");
	        String codecParameters = "";
	        if (fmtp != null) {
                String value = fmtp.getValue();
                index = 0; // value.indexOf(media.payload);
                if ((index != -1) && (value.length() > media.payload.length())) {
                    codecParameters = value.substring(index + media.payload.length() + 1);
                }
            }

	        // Create a video codec
	        VideoCodec videoCodec = new VideoCodec(codecName,
	        		Integer.parseInt(media.payload), clockRate,
	        		codecParameters, frameRate, 0,
	                videoWidth, videoHeight);

            return videoCodec;
    	} catch(NullPointerException e) {
        	return null;
		} catch(IndexOutOfBoundsException e) {
        	return null;
		}
    }

    /**
     * Extract list of video codecs from SDP part
     *
     * @param sdp SDP part
     * @return List of video codecs
     */
    public static Vector<VideoCodec> extractVideoCodecsFromSdp(Vector<MediaDescription> medias) {
    	Vector<VideoCodec> list = new Vector<VideoCodec>();
    	for(int i=0; i < medias.size(); i++) {
    		VideoCodec codec = createVideoCodecFromSdp(medias.get(i));
    		if (codec != null) {
    			list.add(codec);
    		}
    	}
    	return list;
    }
}
