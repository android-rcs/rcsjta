package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for multimedia messaging session events
 */
interface IMultimediaMessagingSessionListener {

	void onMultimediaMessagingStateChanged(in ContactId contact, in String sessionId, in int state);

	void onNewMessage(in ContactId contact, in String sessionId, in byte[] content);
}
