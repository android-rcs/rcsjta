package com.orangelabs.rcs.service.api.client.messaging;

/**
 * File transfer event listener
 */
interface IFileTransferEventListener {
	// Session is started
	void handleSessionStarted();

	// Session has been aborted
	void handleSessionAborted(in int reason);
    
	// Session has been terminated by remote
	void handleSessionTerminatedByRemote();

	// Data transfer progress
	void handleTransferProgress(in long currentSize, in long totalSize);

	// Transfer error
	void handleTransferError(in int error);

	// File has been transfered
	void handleFileTransfered(in String filename);
}
