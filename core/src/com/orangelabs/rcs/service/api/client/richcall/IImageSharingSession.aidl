package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IImageSharingEventListener;

/**
 * Image sharing session interface
 */
interface IImageSharingSession {
	// Get session ID
	String getSessionID();

	// Get remote contact
	String getRemoteContact();
	
	// Get session state
	int getSessionState();

	// Get filename
	String getFilename();
	
	// Get file size
	long getFilesize();

    // Get file thumbnail
    byte[] getFileThumbnail();

	// Accept the session invitation
	void acceptSession();

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Add session listener
	void addSessionListener(in IImageSharingEventListener listener);

	// Remove session listener
	void removeSessionListener(in IImageSharingEventListener listener);
}

