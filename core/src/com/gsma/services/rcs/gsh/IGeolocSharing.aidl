package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.gsh.IGeolocSharingListener;
import com.gsma.services.rcs.chat.Geoloc;

/**
 * Geoloc sharing interface
 */
interface IGeolocSharing {

	String getSharingId();

	String getRemoteContact();

	Geoloc getGeoloc();

	int getState();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();
	
	void addEventListener(in IGeolocSharingListener listener);

	void removeEventListener(in IGeolocSharingListener listener);
}
