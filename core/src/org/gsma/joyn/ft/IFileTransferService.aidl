package org.gsma.joyn.ft;

import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;
import org.gsma.joyn.ft.INewFileTransferListener;

/**
 * File transfer service API
 */
interface IFileTransferService {
	List<IBinder> getFileTransfers();
	
	IFileTransfer getFileTransfer(in String transferId);

	IFileTransfer transferFile(in String contact, in String filename, in IFileTransferListener listener);
	
	void addNewFileTransferListener(in INewFileTransferListener listener);

	void removeNewFileTransferListener(in INewFileTransferListener listener);
}