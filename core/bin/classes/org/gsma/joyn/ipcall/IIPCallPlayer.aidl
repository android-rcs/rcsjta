package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.IIPCallPlayerListener;
import org.gsma.joyn.ipcall.AudioCodec;
import org.gsma.joyn.ipcall.VideoCodec;

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
