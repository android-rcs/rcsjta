package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Multimedia messaging session interface
 */
interface IMultimediaMessagingSession {

	String getSessionId();
	
	ContactId getRemoteContact();
	
	String getServiceId();
	
	int getState();
	
	int getReasonCode();
	
	int getDirection();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	void abortSession();

	void sendMessage(in byte[] content);

	void sendMessage2(in byte[] content, in String contentType);

	void flushMessages();
}
