package org.gsma.joyn.vsh;

/**
 * Callback methods for video sharing events
 */
interface IVideoSharingListener {
	void onSharingStarted();
	
	void onSharingAborted();

	void onSharingError(in int error);
}