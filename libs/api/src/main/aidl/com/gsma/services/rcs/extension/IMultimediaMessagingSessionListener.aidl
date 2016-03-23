package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for multimedia messaging session events
 */
interface IMultimediaMessagingSessionListener {

	void onStateChanged(in ContactId contact, in String sessionId, in int state, in int reasonCode);

	void onMessageReceived(in ContactId contact, in String sessionId, in byte[] content);

	void onMessageReceived2(in ContactId contact, in String sessionId, in byte[] content, in String contentType);

	void onMessagesFlushed(in ContactId contact, in String sessionId);
}
