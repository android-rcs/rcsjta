package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for one-to-one file transfer events
 */
interface IOneToOneFileTransferListener {

	void onStateChanged(in ContactId contact, in String transferId, in int state, in int reasonCode);

	void onProgressUpdate(in ContactId contact, in String transferId, in long currentSize, in long totalSize);
}