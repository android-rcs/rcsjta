package com.gsma.services.rcs.upload;

import com.gsma.services.rcs.upload.FileUploadInfo;

/**
 * Callback methods for file upload events
 */
interface IFileUploadListener {

	void onStateChanged(in String uploadId, in int state);

	void onProgressUpdate(in String uploadId, in long currentSize, in long totalSize);

	void onUploaded(in String uploadId, in FileUploadInfo info);
}