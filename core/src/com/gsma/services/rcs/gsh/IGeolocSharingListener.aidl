package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for geoloc sharing events
 */
interface IGeolocSharingListener {

	void onGeolocSharingStateChanged(in ContactId contact, in String sharingId, in int state);

	void onGeolocSharingProgress(in ContactId contact, in String sharingId, in long currentSize, in long totalSize);
}