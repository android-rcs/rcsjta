package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.IVideoRendererListener;

/**
 * Video renderer interface
 */
interface IVideoRenderer {
	void open(in VideoCodec codec, in String remoteHost, in int remotePort);

	void close();

	void start();

	void stop();

	int getLocalRtpPort();

	VideoCodec getCodec();

	VideoCodec[] getSupportedCodecs();
	
	void addEventListener(in IVideoRendererListener listener);

	void removeEventListener(in IVideoRendererListener listener);
}