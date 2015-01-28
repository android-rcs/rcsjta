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

import com.gsma.services.rcs.vsh.VideoCodec;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;

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
    public static VideoCodec negociateVideoCodec(VideoCodec[] supportedCodecs, Vector<VideoCodec> proposedCodecs) {
        VideoCodec selectedCodec = null;
        int pref = -1;
		for (VideoCodec proposedCodec : proposedCodecs) {
			for (int j = 0; j < supportedCodecs.length; j++) {
				VideoCodec videoCodec = supportedCodecs[j];
				int videoCodecPref = supportedCodecs.length - 1 - j;
				// Compare codec
				if (!compareVideoCodec(proposedCodec, videoCodec)) {
					continue;
				}
				if (videoCodecPref <= pref) {
					continue;
				}
				pref = videoCodecPref;
				int proposedCodecWidth = proposedCodec.getWidth();
				int width = (proposedCodecWidth == 0) ? videoCodec.getWidth() : proposedCodecWidth;
				int proposedCodectHeight = proposedCodec.getHeight();
				int height = (proposedCodectHeight == 0) ? videoCodec.getHeight()
						: proposedCodectHeight;
				int proposedCodecPayloadType = proposedCodec.getPayloadType();
				int payloadType = (proposedCodecPayloadType == 0) ? videoCodec.getPayloadType()
						: proposedCodecPayloadType;
				int proposedCodecClockRate = proposedCodec.getClockRate();
				int clockRate = (proposedCodecClockRate == 0) ? videoCodec.getClockRate()
						: proposedCodecClockRate;
				int proposedCodecFrameRate = proposedCodec.getFrameRate();
				int frameRate = (proposedCodecFrameRate == 0) ? videoCodec.getFrameRate()
						: proposedCodecFrameRate;
				int proposedCodecBitRate = proposedCodec.getBitRate();
				int bitRate = (proposedCodecBitRate == 0) ? videoCodec.getBitRate()
						: proposedCodecBitRate;
				String proposedCodecParameters = proposedCodec.getParameters();

				selectedCodec = new VideoCodec(proposedCodec.getEncoding(), payloadType, clockRate,
						frameRate, bitRate, width, height,
						(proposedCodecParameters.length() == 0) ? videoCodec.getParameters()
								: proposedCodecParameters);
			}
		}
        return selectedCodec;
    }

    /**
     * Compare two video codecs
     *
     * @param codec1 Video codec 1
     * @param codec2 Video codec 2
     * @return boolean
     */
    public static boolean compareVideoCodec(VideoCodec codec1, VideoCodec codec2) {
        if (codec1.getEncoding().equalsIgnoreCase(codec2.getEncoding()) 
                && (codec1.getWidth() == codec2.getWidth() || codec1.getWidth() == 0 || codec2.getWidth() == 0)
                && (codec1.getHeight() == codec2.getHeight() || codec1.getHeight() == 0 || codec2.getHeight() == 0)) {
            if (codec1.getEncoding().equalsIgnoreCase(H264Config.CODEC_NAME)) {
                if (H264Config.getCodecProfileLevelId(codec1.getParameters()).compareToIgnoreCase(H264Config.getCodecProfileLevelId(codec2.getParameters())) == 0) {
                    return  true;
                }
            } else {
                if (codec1.getParameters().equalsIgnoreCase(codec2.getParameters())) {
                    return true;
                }
            }
        }
        return false;
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
	        		frameRate, 0,
	        		videoWidth, videoHeight,
	        		codecParameters);

            return videoCodec;
    	} catch(NullPointerException e) {
        	return null;
		} catch(IndexOutOfBoundsException e) {
        	return null;
		}
    }

    /**
     * Extract list of video codecs from SDP part
     * @param medias 
     *
     * @return List of video codecs
     */
    public static Vector<VideoCodec> extractVideoCodecsFromSdp(Vector<MediaDescription> medias) {
    	Vector<VideoCodec> codecs = new Vector<VideoCodec>();
    	for (MediaDescription media : medias) {
    		VideoCodec codec = createVideoCodecFromSdp(media);
    		if (codec != null) {
    			codecs.add(codec);
    		}
		}
    	return codecs;
    }
}
