package com.gsma.services.rcs.sharing.image;

import android.net.Uri;

import com.gsma.services.rcs.contact.ContactId;

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

	long getTimestamp();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();
}
