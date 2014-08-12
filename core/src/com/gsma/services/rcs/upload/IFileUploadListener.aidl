package com.gsma.services.rcs.upload;

/**
 * Callback methods for file upload events
 */
interface IFileUploadListener {
	void onUploadStateChanged(in String uploadId, in int state);

	void onUploadProgress(in String uploadId, in long currentSize, in long totalSize);

}