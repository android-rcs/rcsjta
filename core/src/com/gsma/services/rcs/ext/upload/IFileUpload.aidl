package com.gsma.services.rcs.ext.upload;

import com.gsma.services.rcs.ext.upload.FileUploadInfo;

/**
 * File upload interface
 */
interface IFileUpload {

	String getUploadId();

	Uri getFile();

	FileUploadInfo getUploadInfo();

	int getState();
	
	void abortUpload();
}
