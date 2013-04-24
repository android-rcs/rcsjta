package com.orangelabs.rcs.service.api.client.richcall;

/**
 * Image sharing event listener
 */
interface IImageSharingEventListener {
	// Session is started
	void handleSessionStarted();

	// Session has been aborted
	void handleSessionAborted(in int reason);
       
	// Session has been terminated by remote
	void handleSessionTerminatedByRemote();

	// Content sharing progress
	void handleSharingProgress(in long currentSize, in long totalSize);

	// Content sharing error
	void handleSharingError(in int error);

	// Image has been transfered
	void handleImageTransfered(in String filename);
}
