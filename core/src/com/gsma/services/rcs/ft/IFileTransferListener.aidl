package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for file transfer events
 */
interface IFileTransferListener {

	void onTransferStateChanged(in ContactId contact, in String transferId, in int state, in int reasonCode);

	void onTransferProgress(in ContactId contact, in String transferId, in long currentSize, in long totalSize);
}