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
package com.orangelabs.rcs.ri.ipcall.media;

import android.util.Log;

import com.gsma.services.rcs.ipcall.AudioCodec;
import com.gsma.services.rcs.ipcall.IPCallRenderer;
import com.gsma.services.rcs.ipcall.VideoCodec;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.ri.sharing.video.media.NetworkRessourceManager;
import com.orangelabs.rcs.ri.utils.LogUtils;

/**
 * IP call renderer 
 */
public class MyIPCallRenderer extends IPCallRenderer {
	/**
	 * Audio codec
	 */
	private AudioCodec audiocodec;
	
	/**
	 * Local RTP port for audio
	 */
	private int localAudioRtpPort;

	/**
	 * Video codec
	 */
	private VideoCodec videocodec;

	/**
	 * Local RTP port for video
	 */
	private int localVideoRtpPort;

	/**
	 * Is video activated
	 */
	private boolean video = false;
	
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(MyIPCallRenderer.class.getSimpleName());

    /**
     * Constructor
     */
    public MyIPCallRenderer() {
    	// Set the local RTP port for audio
        localAudioRtpPort = NetworkRessourceManager.generateLocalRtpPort();

        // Set the default audio codec
    	audiocodec = new AudioCodec("AMR", 97, 8000, "");
    	
    	// Set the local RTP port for video
        localVideoRtpPort = NetworkRessourceManager.generateLocalRtpPort();

        // Set the default video codec
    	videocodec = new VideoCodec(H264Config.CODEC_NAME,
			H264VideoFormat.PAYLOAD,
            H264Config.CLOCK_RATE,
            15,
            96000,
            H264Config.QCIF_WIDTH, 
            H264Config.QCIF_HEIGHT,
			H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=" + JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE);
    	if (LogUtils.isActive) {
			Log.d(LOGTAG, "MyIPCallRenderer localAudioRtpPort=" + localAudioRtpPort + " localVideoRtpPort=" + localVideoRtpPort);
		}
    }

    public void open(AudioCodec audiocodec, VideoCodec videocodec, String remoteHost, int remoteAudioPort, int remoteVideoPort) {
		// TODO
		this.audiocodec = audiocodec;
		this.videocodec = videocodec;
	}

	public void start() {
		// TODO
	}

	public void stop() {
		// TODO
	}

	public void close() {
		// TODO
	}

	public int getLocalAudioRtpPort() {
		// TODO
		return 5000;
	}

	public AudioCodec getAudioCodec() {
		// TODO
		return audiocodec;
	}

	public int getLocalVideoRtpPort() {
		// TODO
		return 5002;
	}

	public AudioCodec[] getSupportedAudioCodecs() {
		// TODO
		AudioCodec[] codecs = {
			new AudioCodec("AMR", 97, 8000, "")
		};
		return codecs;
	}

	public VideoCodec[] getSupportedVideoCodecs() {
		VideoCodec[] codecs = {
			new VideoCodec(H264Config.CODEC_NAME,
    			H264VideoFormat.PAYLOAD,
                H264Config.CLOCK_RATE,
                15,
                96000,
                H264Config.QCIF_WIDTH, 
                H264Config.QCIF_HEIGHT,
    			H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=" + JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE)
		};
		return codecs;
	}

	public VideoCodec getVideoCodec() {
		if (video) {
			return videocodec;
		} else {
			return null;			
		}
	}

	/**
	 * Is video activated
	 * 
	 * @return Boolean
	 */
	public boolean isVideoActivated() {
		return video;
	}
	
	/**
	 * Set video activated
	 * 
	 * @param video Video flag
	 */
	public void setVideoActivation(boolean video) {
		this.video = video;
	}
}
