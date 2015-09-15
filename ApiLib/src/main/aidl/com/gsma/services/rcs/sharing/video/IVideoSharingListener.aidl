package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for video sharing events
 */
interface IVideoSharingListener {

	void onStateChanged(in ContactId contact, in String sharingId, in int state, in int reasonCode);

	void onDeleted(in ContactId contact, in List<String> sharingIds);
}