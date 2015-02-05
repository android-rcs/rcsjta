package com.gsma.services.rcs.upload;

/**
 * Callback methods for file upload events
 */
interface IFileUploadListener {

	void onStateChanged(in String uploadId, in int state);

	void onProgressUpdate(in String uploadId, in long currentSize, in long totalSize);

}