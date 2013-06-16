package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.VideoCodec;

/**
 * Video player interface
 */
interface IVideoPlayer {
	void open(in VideoCodec codec, in String remoteHost, in int remotePort);
	
	void close();

	void start();

	void stop();

	int getLocalRtpPort();

	VideoCodec[] getSupportedCodecs();
}
