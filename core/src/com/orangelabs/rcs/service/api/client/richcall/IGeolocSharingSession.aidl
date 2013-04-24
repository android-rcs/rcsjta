package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingEventListener;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;

/**
 * Geoloc sharing session interface
 */
interface IGeolocSharingSession {
	// Get session ID
	String getSessionID();

	// Get remote contact
	String getRemoteContact();
	
	// Get session state
	int getSessionState();

	// Accept the session invitation
	void acceptSession();

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Add session listener
	void addSessionListener(in IGeolocSharingEventListener listener);

	// Remove session listener
	void removeSessionListener(in IGeolocSharingEventListener listener);
}