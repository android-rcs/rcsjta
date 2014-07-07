package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * File transfer interface
 */
interface IFileTransfer {

	String getTransferId();

	ContactId getRemoteContact();

	String getFileName();

	long getFileSize();

	String getFileType();

	String getFileIconName();

	Uri getFile();

	int getState();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortTransfer();
	
	void pauseTransfer();
	
	void resumeTransfer();

	void addEventListener(in IFileTransferListener listener);

	void removeEventListener(in IFileTransferListener listener);
}
