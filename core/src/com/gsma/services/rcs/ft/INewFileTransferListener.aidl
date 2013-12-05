package com.gsma.services.rcs.ft;

/**
 * Callback method for new file transfer invitations and delivery reports
 */
interface INewFileTransferListener {
	void onNewFileTransfer(in String transferId);
	
	void onReportFileDelivered(String transferId);
	
	void onReportFileDisplayed(String transferId);
}