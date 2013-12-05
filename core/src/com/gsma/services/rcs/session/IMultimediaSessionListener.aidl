package com.gsma.services.rcs.session;

/**
 * Callback methods for multimedia session events
 */
interface IMultimediaSessionListener {
	void onSessionStarted();
	
	void onSessionRinging();

	void onSessionAborted();

	void onSessionError(in int error);
}
