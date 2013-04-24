package com.orangelabs.rcs.service.api.client.sip;

import com.orangelabs.rcs.service.api.client.sip.ISipSessionEventListener;

/**
 * SIP session interface
 */
interface ISipSession {
	// Get session ID
	String getSessionID();

	// Get remote contact
	String getRemoteContact();
	
	// Get session state
	int getSessionState();

	// Get feature tag of the service
	String getFeatureTag();

	// Get local SDP
      	String getLocalSdp();
      
	// Get remote SDP
      	String getRemoteSdp();

	// Accept the session invitation
	void acceptSession(in String sdp);

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Add session listener
	void addSessionListener(in ISipSessionEventListener listener);

	// Remove session listener
	void removeSessionListener(in ISipSessionEventListener listener);
}

