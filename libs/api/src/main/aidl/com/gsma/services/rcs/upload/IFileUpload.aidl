package com.gsma.services.rcs.upload;

import android.net.Uri;

import com.gsma.services.rcs.upload.FileUploadInfo;

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
