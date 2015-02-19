package com.gsma.services.rcs.sharing.geoloc;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Geoloc sharing interface
 */
interface IGeolocSharing {

	String getSharingId();

	ContactId getRemoteContact();

	Geoloc getGeoloc();

	int getState();

	int getReasonCode();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();

	long getTimestamp();
}
