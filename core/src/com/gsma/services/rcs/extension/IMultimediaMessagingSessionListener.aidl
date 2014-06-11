package com.gsma.services.rcs.extension;

/**
 * Callback methods for multimedia messaging session events
 */
interface IMultimediaMessagingSessionListener {
	void onSessionStarted();
	
	void onSessionRinging();

	void onSessionAborted();

	void onSessionError(in int error);

	void onNewMessage(in byte[] content);
}
