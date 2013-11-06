package org.gsma.joyn.session;

/**
 * Callback methods for multimedia session events
 */
interface IMultimediaSessionListener {
	void onSessionStarted();
	
	void onSessionRinging();

	void onSessionAborted();

	void onSessionError(in int error);
}
