package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Geoloc sharing interface
 */
interface IGeolocSharing {

	String getSharingId();

	ContactId getRemoteContact();

	Geoloc getGeoloc();

	int getState();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();
}
