package com.gsma.services.rcs.upload;

import android.net.Uri;

import com.gsma.services.rcs.upload.IFileUpload;
import com.gsma.services.rcs.upload.IFileUploadListener;
import com.gsma.services.rcs.upload.FileUploadServiceConfiguration;

/**
 * File uploadservice API
 */
interface IFileUploadService {

	FileUploadServiceConfiguration getConfiguration();

	boolean canUploadFile();

	List<IBinder> getFileUploads();
	
	IFileUpload getFileUpload(in String uploadId);

	IFileUpload uploadFile(in Uri file, in boolean fileIcon);




	void addEventListener(in IFileUploadListener listener);



	void removeEventListener(in IFileUploadListener listener);


	int getServiceVersion();
}