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

	int getState();
	
	void acceptInvitation();

	void rejectInvitation();

	void abortTransfer();
	
	void addEventListener(in IFileTransferListener listener);

	void removeEventListener(in IFileTransferListener listener);
}
