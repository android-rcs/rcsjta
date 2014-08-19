package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for group file transfer events
 */
interface IGroupFileTransferListener {

	void onTransferStateChanged(in String chatId, in String transferId, in int state);

	void onGroupDeliveryInfoChanged(in String chatId, in ContactId contact, in String transferId, in int state);

	void onTransferProgress(in String chatId, in String transferId, in long currentSize, in long totalSize);
}