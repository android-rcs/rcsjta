package com.gsma.services.rcs.ish;

import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Image sharing interface
 */
interface IImageSharing {

	String getSharingId();

	ContactId getRemoteContact();

	Uri getFile();

	String getFileName();

	long getFileSize();

	String getMimeType();

	int getState();

	int getReasonCode();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();
}
