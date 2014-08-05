package com.gsma.services.rcs.fsh;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * File sharing interface
 */
interface IFileSharing {

	String getSharingId();

	ContactId getRemoteContact();

	Uri getFile();

	int getState();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();
}
