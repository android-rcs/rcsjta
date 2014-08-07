package com.gsma.services.rcs.ext.upload;

/**
 * Callback methods for file upload events
 */
interface IFileUploadListener {
	void onFileUploadStateChanged(in String uploadId, in int state);

	void onFileUploadProgress(in String uploadId, in long currentSize, in long totalSize);

}