package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.IIPCallRendererListener;
import org.gsma.joyn.ipcall.AudioCodec;
import org.gsma.joyn.ipcall.VideoCodec;

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