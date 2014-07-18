package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.ft.IGroupFileTransferListener;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

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

	IFileTransfer transferFile(in ContactId contact, in Uri file, in boolean fileicon);

	IFileTransfer transferFileToGroupChat(in String chatId, in Uri file, in boolean fileicon);

	void markFileTransferAsRead(in String transferId);
	
	void addOneToOneFileTransferListener(in IFileTransferListener listener);

	void removeOneToOneFileTransferListener(in IFileTransferListener listener);

	void addGroupFileTransferListener(in IGroupFileTransferListener listener);

	void removeGroupFileTransferListener(in IGroupFileTransferListener listener);
	
	int getServiceVersion();
	
	void setAutoAccept(in boolean enable);
	
	void setAutoAcceptInRoaming(in boolean enable);
	
	void setImageResizeOption(in int option);
}