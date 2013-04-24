package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;

/**
 * Geoloc sharing event listener
 */
interface IGeolocSharingEventListener {
	// Session is started
	void handleSessionStarted();

	// Session has been aborted
	void handleSessionAborted(in int reason);
       
	// Session has been terminated by remote
	void handleSessionTerminatedByRemote();

	// Content sharing error
	void handleSharingError(in int error);

	// Geoloc has been transfered
	void handleGeolocTransfered(in GeolocPush geoloc);
}
