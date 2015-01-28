package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.vsh.VideoCodec;

/**
 * Video player interface
 */
interface IVideoPlayer {
	void setRemoteInfo(in VideoCodec codec, in String remoteHost, in int remotePort, in int orientationHeaderId);
	
	int getLocalRtpPort();

	VideoCodec[] getSupportedCodecs();

	VideoCodec getCodec();
}
