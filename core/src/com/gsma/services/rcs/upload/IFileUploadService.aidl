package com.gsma.services.rcs.upload;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.upload.IFileUpload;
import com.gsma.services.rcs.upload.IFileUploadListener;

/**
 * File uploadservice API
 */
interface IFileUploadService {

	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 
    
	List<IBinder> getFileUploads();
	
	IFileUpload getFileUpload(in String uploadId);

	IFileUpload uploadFile(in Uri file, in boolean fileicon);



	void addEventListener(in IFileUploadListener listener);



	void removeEventListener(in IFileUploadListener listener);


	int getServiceVersion();
}