package com.gsma.services.rcs.ish;

/**
 * Callback methods for image sharing events
 */
interface IImageSharingListener {
	void onSharingStarted();
	
	void onSharingAborted();

	void onSharingError(in int error);
	
	void onSharingProgress(in long currentSize, in long totalSize);

	void onImageShared(in String filename);
}