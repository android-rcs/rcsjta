package com.gsma.services.rcs.ish;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for image sharing events
 */
interface IImageSharingListener {

	void onImageSharingStateChanged(in ContactId contact, in String sharingId, in int state, in int reasonCode);

	void onImageSharingProgress(in ContactId contact, in String sharingId, in long currentSize, in long totalSize);
}