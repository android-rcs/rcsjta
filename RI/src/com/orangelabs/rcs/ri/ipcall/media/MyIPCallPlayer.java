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

import org.gsma.joyn.ipcall.AudioCodec;
import org.gsma.joyn.ipcall.IPCallPlayer;
import org.gsma.joyn.ipcall.VideoCodec;

/**
 * IP call player 
 */
public class MyIPCallPlayer extends IPCallPlayer {
	private AudioCodec audiocodec;
	
	private VideoCodec videocodec;

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

	public AudioCodec getAudioCodec() {
		return audiocodec;
	}

	public int getLocalAudioRtpPort() {
		// TODO
		return 5000;
	}

	public int getLocalVideoRtpPort() {
		// TODO
		return 5002;
	}

	public AudioCodec[] getSupportedAudioCodecs() {
		// TODO
		AudioCodec[] codecs = {
			new AudioCodec("AMR", 96, 8000, "")
		};
		return codecs;
	}

	public VideoCodec[] getSupportedVideoCodecs() {
		// TODO
		VideoCodec[] codecs = {
			new VideoCodec("H264", 97, 90000, 10, 96000, 176, 144, "")
		};
		return codecs;
	}

	public VideoCodec getVideoCodec() {
		return videocodec;
	}
}
