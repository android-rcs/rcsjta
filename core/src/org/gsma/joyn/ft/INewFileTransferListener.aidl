package org.gsma.joyn.ft;

/**
 * Callback method for new file transfer invitations
 */
interface INewFileTransferListener {
	void onNewFileTransfer(in String transferId);
}