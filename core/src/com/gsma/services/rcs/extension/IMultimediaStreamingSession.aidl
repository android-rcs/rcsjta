package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Multimedia streaming session interface
 */
interface IMultimediaStreamingSession {

	String getSessionId();
	
	ContactId getRemoteContact();
	
	String getServiceId();
	
	int getState();
	
	int getReasonCode();
	
	int getDirection();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	void abortSession();

	void sendPayload(in byte[] content);
}

