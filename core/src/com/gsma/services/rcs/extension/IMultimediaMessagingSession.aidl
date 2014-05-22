package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;

/**
 * Multimedia messaging session interface
 */
interface IMultimediaMessagingSession {
	String getSessionId();
	
	String getRemoteContact();
	
	String getServiceId();
	
	int getState();
	
	int getDirection();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	void abortSession();
	
	void addEventListener(in IMultimediaMessagingSessionListener listener);
	
	void removeEventListener(in IMultimediaMessagingSessionListener listener);

	boolean sendMessage(in byte[] content);
}

