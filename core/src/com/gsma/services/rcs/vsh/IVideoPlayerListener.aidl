package com.gsma.services.rcs.vsh;

/**
 * Video player event listener interface
 */
interface IVideoPlayerListener {
	void onPlayerOpened();

	void onPlayerStarted();

	void onPlayerStopped();

	void onPlayerClosed();

	void onPlayerError(in int error);
}
