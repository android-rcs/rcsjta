package com.orangelabs.rcs.service.api.client.sip;

/**
 * SIP session event listener
 */
interface ISipSessionEventListener {
	// Session is started
	void handleSessionStarted();

	// Session has been aborted
	void handleSessionAborted(in int reason);
       
	// Session has been terminated by remote
	void handleSessionTerminatedByRemote();

	// Session error
	void handleSessionError(in int error);
}
