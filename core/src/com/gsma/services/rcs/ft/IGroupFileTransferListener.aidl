package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for group file transfer events
 */
interface IGroupFileTransferListener {

	void onStateChanged(in String chatId, in String transferId, in int state, in int reasonCode);

	void onDeliveryInfoChanged(in String chatId, in ContactId contact, in String transferId, in int state, in int reasonCode);

	void onProgressUpdate(in String chatId, in String transferId, in long currentSize, in long totalSize);
}
