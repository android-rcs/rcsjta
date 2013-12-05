package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.chat.Geoloc;

/**
 * Callback methods for geoloc sharing events
 */
interface IGeolocSharingListener {
	void onSharingStarted();
	
	void onSharingAborted();

	void onSharingError(in int error);
	
	void onSharingProgress(in long currentSize, in long totalSize);

	void onGeolocShared(in Geoloc geoloc);
}