package com.gsma.rcs.service.ipcalldraft;

import com.gsma.rcs.service.ipcalldraft.IIPCallPlayerListener;
import com.gsma.rcs.service.ipcalldraft.AudioCodec;
import com.gsma.rcs.service.ipcalldraft.VideoCodec;

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
