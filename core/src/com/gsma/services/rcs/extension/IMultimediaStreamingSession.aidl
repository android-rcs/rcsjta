package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.extension.IMultimediaStreamingSessionListener;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Multimedia streaming session interface
 */
interface IMultimediaStreamingSession {
	String getSessionId();
	
	ContactId getRemoteContact();
	
	String getServiceId();
	
	int getState();
	
	int getDirection();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	void abortSession();
	
	void addEventListener(in IMultimediaStreamingSessionListener listener);
	
	void removeEventListener(in IMultimediaStreamingSessionListener listener);

	boolean sendPayload(in byte[] content);
}

