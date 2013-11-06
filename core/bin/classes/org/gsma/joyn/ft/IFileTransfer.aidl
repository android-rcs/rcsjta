package org.gsma.joyn.ft;

import org.gsma.joyn.ft.IFileTransferListener;

/**
 * File transfer interface
 */
interface IFileTransfer {

	String getTransferId();

	String getRemoteContact();

	String getFileName();

	long getFileSize();

	String getFileType();

	String getFileIconName();

	int getState();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortTransfer();
	
	void addEventListener(in IFileTransferListener listener);

	void removeEventListener(in IFileTransferListener listener);
	
	int getServiceVersion();
}
