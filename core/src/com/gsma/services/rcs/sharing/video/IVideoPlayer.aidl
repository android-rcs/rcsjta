package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.sharing.video.VideoCodec;

/**
 * Video player interface
 */
interface IVideoPlayer {
	void setRemoteInfo(in VideoCodec codec, in String remoteHost, in int remotePort, in int orientation);
	
	int getLocalRtpPort();

	VideoCodec[] getSupportedCodecs();

	VideoCodec getCodec();
}
