package com.gsma.services.rcs.ft;

import android.net.Uri;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IOneToOneFileTransferListener;
import com.gsma.services.rcs.ft.IGroupFileTransferListener;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * File transfer service API
 */
interface IFileTransferService {

	boolean isServiceRegistered();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	FileTransferServiceConfiguration getConfiguration();

	List<IBinder> getFileTransfers();
	
	IFileTransfer getFileTransfer(in String transferId);

	IFileTransfer transferFile(in ContactId contact, in Uri file, in boolean fileicon);

	IFileTransfer transferFileToGroupChat(in String chatId, in Uri file, in boolean fileicon);

	void markFileTransferAsRead(in String transferId);
	
	void addEventListener2(in IOneToOneFileTransferListener listener);

	void removeEventListener2(in IOneToOneFileTransferListener listener);

	void addEventListener3(in IGroupFileTransferListener listener);

	void removeEventListener3(in IGroupFileTransferListener listener);
	
	int getServiceVersion();
	
	void setAutoAccept(in boolean enable);
	
	void setAutoAcceptInRoaming(in boolean enable);
	
	void setImageResizeOption(in int option);
}