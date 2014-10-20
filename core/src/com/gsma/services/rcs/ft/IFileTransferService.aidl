package com.gsma.services.rcs.ft;

import android.net.Uri;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
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

	void addServiceRegistrationListener(IRcsServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IRcsServiceRegistrationListener listener);

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