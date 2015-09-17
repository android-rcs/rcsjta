package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for multimedia streaming session events
 */
interface IMultimediaStreamingSessionListener {

	void onStateChanged(in ContactId contact, in String sessionId, in int state, in int reasonCode);

	void onPayloadReceived(in ContactId contact, in String sessionId, in byte[] content);
}
