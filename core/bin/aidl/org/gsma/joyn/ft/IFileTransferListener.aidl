package org.gsma.joyn.ft;

/**
 * Callback methods for file transfer events
 */
interface IFileTransferListener {
	void onTransferStarted();
	
	void onTransferAborted();

	void onTransferError(in int error);
	
	void onTransferProgress(in long currentSize, in long totalSize);

	void onFileTransferred(in String filename);
}