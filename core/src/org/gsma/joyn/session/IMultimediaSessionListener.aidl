package org.gsma.joyn.session;

/**
 * Callback methods for multimedia session events
 */
interface IMultimediaSessionListener {
	void onSessionRinging();
	
	void onSessionStarted();
	
	void onSessionAborted();
	
	void onSessionError(in int error);
}
