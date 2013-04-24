package com.orangelabs.rcs.service.api.client.sip;

import com.orangelabs.rcs.service.api.client.sip.ISipSession;

/**
 * SIP API
 */
interface ISipApi {

	// Initiate a SIP session
	ISipSession initiateSession(in String contact, in String featureTag, in String sdp);

	// Get current SIP session from its session ID
	ISipSession getSession(in String id);

	// Get list of current SIP sessions with a contact
	List<IBinder> getSessionsWith(in String contact);

	// Get list of current SIP sessions
	List<IBinder> getSessions();

	// Send an instant message (SIP MESSAGE)
	boolean sendSipInstantMessage(in String contact, in String featureTag, in String content, in String contentType);
}


