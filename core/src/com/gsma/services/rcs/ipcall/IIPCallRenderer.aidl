package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.ipcall.IIPCallRendererListener;
import com.gsma.services.rcs.ipcall.AudioCodec;
import com.gsma.services.rcs.ipcall.VideoCodec;

/**
 * IP call renderer interface
 */
interface IIPCallRenderer {
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

	void addEventListener(in IIPCallRendererListener listener);

	void removeEventListener(in IIPCallRendererListener listener);
}