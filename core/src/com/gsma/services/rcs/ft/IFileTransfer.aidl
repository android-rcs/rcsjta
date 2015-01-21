package com.gsma.services.rcs.ft;

import android.net.Uri;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * File transfer interface
 */
interface IFileTransfer {

	String getChatId();

	String getTransferId();

	ContactId getRemoteContact();

	String getFileName();

	long getFileSize();

	String getMimeType();

	Uri getFileIcon();

	Uri getFile();

	int getState();

	int getReasonCode();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortTransfer();
	
	void pauseTransfer();
	
	void resumeTransfer();

	boolean canResendTransfer();

	void resendTransfer();
}
