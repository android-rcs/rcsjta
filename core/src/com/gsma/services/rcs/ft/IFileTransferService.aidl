package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.ft.INewFileTransferListener;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;

/**
 * File transfer service API
 */
interface IFileTransferService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	FileTransferServiceConfiguration getConfiguration();

	List<IBinder> getFileTransfers();
	
	IFileTransfer getFileTransfer(in String transferId);

	IFileTransfer transferFile(in String contact, in String filename, in boolean tryAttachThumbnail, in IFileTransferListener listener);

	void markFileTransferAsRead(in String transferId);
	
	void addNewFileTransferListener(in INewFileTransferListener listener);

	void removeNewFileTransferListener(in INewFileTransferListener listener);
	
	int getServiceVersion();
}