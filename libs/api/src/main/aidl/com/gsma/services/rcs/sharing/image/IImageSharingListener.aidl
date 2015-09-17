package com.gsma.services.rcs.sharing.image;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for image sharing events
 */
interface IImageSharingListener {

	void onStateChanged(in ContactId contact, in String sharingId, in int state, in int reasonCode);

	void onProgressUpdate(in ContactId contact, in String sharingId, in long currentSize, in long totalSize);

	void onDeleted(in ContactId contact, in List<String> sharingIds);
}