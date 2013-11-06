package org.gsma.joyn.gsh;

import org.gsma.joyn.gsh.IGeolocSharingListener;
import org.gsma.joyn.chat.Geoloc;

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
