package com.gsma.services.rcs.extension;

/**
 * Callback methods for multimedia streaming session events
 */
interface IMultimediaStreamingSessionListener {
	void onSessionStarted();
	
	void onSessionRinging();

	void onSessionAborted();

	void onSessionError(in int error);

	void onNewPayload(in byte[] content);
}
