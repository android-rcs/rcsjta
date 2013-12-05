package com.gsma.services.rcs.vsh;

/**
 * Video renderer event listener interface
 */
interface IVideoRendererListener {
	void onRendererOpened();

	void onRendererStarted();

	void onRendererStopped();

	void onRendererClosed();

	void onRendererError(in int error);
	
	void onRendererResized(in int width, in int height);
}
