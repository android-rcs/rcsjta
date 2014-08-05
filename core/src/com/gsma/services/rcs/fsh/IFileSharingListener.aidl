package com.gsma.services.rcs.fsh;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for file sharing events
 */
interface IFileSharingListener {

	void onFileSharingStateChanged(in ContactId contact, in String sharingId, in int state);

	void onFileSharingProgress(in ContactId contact, in String sharingId, in long currentSize, in long totalSize);
}