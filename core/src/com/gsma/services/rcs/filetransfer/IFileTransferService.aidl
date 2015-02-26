package com.gsma.services.rcs.filetransfer;

import android.net.Uri;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.filetransfer.IFileTransfer;
import com.gsma.services.rcs.filetransfer.IOneToOneFileTransferListener;
import com.gsma.services.rcs.filetransfer.IGroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.IFileTransferServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * File transfer service API
 */
interface IFileTransferService {

	boolean isServiceRegistered();
	
	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IFileTransferServiceConfiguration getConfiguration();

	List<IBinder> getFileTransfers();
	
	IFileTransfer getFileTransfer(in String transferId);

	IFileTransfer transferFile(in ContactId contact, in Uri file, in boolean attachFileicon);

	IFileTransfer transferFileToGroupChat(in String chatId, in Uri file, in boolean attachFileicon);

	void markFileTransferAsRead(in String transferId);
	
	void addEventListener2(in IOneToOneFileTransferListener listener);

	void removeEventListener2(in IOneToOneFileTransferListener listener);

	void addEventListener3(in IGroupFileTransferListener listener);

	void removeEventListener3(in IGroupFileTransferListener listener);
	
	int getServiceVersion();
	
	void setAutoAccept(in boolean enable);
	
	void setAutoAcceptInRoaming(in boolean enable);
	
	void setImageResizeOption(in int option);

	boolean isAllowedToTransferFile(in ContactId contact);

	boolean isAllowedToTransferFileToGroupChat(in String chatId);

	void deleteOneToOneFileTransfers();

	void deleteGroupFileTransfers();

	void deleteOneToOneFileTransfers2(in ContactId contact);

	void deleteGroupFileTransfers2(in String chatId);

	void deleteFileTransfer(in String transferId);

	void markUndeliveredFileTransfersAsProcessed(in List<String> transferIds);
}