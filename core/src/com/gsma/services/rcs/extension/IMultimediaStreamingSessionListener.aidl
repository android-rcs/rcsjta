package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for multimedia streaming session events
 */
interface IMultimediaStreamingSessionListener {

	void onMultimediaStreamingStateChanged(in ContactId contact, in String sessionId, in int state);

	void onNewPayload(in ContactId contact, in String sessionId, in byte[] content);
}
