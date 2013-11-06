package org.gsma.joyn.ft;

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;
import org.gsma.joyn.ft.INewFileTransferListener;
import org.gsma.joyn.ft.FileTransferServiceConfiguration;

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

	IFileTransfer transferFile(in String contact, in String filename, in String fileicon, in IFileTransferListener listener);
	
	void addNewFileTransferListener(in INewFileTransferListener listener);

	void removeNewFileTransferListener(in INewFileTransferListener listener);
	
	int getServiceVersion();
}