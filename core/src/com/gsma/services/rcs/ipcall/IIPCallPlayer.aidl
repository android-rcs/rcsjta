package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.ipcall.IIPCallPlayerListener;
import com.gsma.services.rcs.ipcall.AudioCodec;
import com.gsma.services.rcs.ipcall.VideoCodec;

/**
 * IP call player interface
 */
interface IIPCallPlayer {
	void open(in AudioCodec audiocodec, in VideoCodec videocodec, in String remoteHost, in int remoteAudioPort, in int remoteVideoPort);
	
	void close();

	void start();

	void stop();

	int getLocalAudioRtpPort();

	AudioCodec getAudioCodec();

	AudioCodec[] getSupportedAudioCodecs();

	int getLocalVideoRtpPort();

	VideoCodec getVideoCodec();
	
	VideoCodec[] getSupportedVideoCodecs();

	void addEventListener(in IIPCallPlayerListener listener);

	void removeEventListener(in IIPCallPlayerListener listener);
}
