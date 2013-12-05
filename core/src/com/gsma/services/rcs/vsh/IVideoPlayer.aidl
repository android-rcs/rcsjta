package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.IVideoPlayerListener;

/**
 * Video player interface
 */
interface IVideoPlayer {
	void open(in VideoCodec codec, in String remoteHost, in int remotePort);
	
	void close();

	void start();

	void stop();

	int getLocalRtpPort();

	VideoCodec getCodec();

	VideoCodec[] getSupportedCodecs();

	void addEventListener(in IVideoPlayerListener listener);

	void removeEventListener(in IVideoPlayerListener listener);	
}
