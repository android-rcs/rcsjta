package com.gsma.services.rcs.session;

import com.gsma.services.rcs.session.IMultimediaSessionListener;

/**
 * Multimedia session interface
 */
interface IMultimediaSession {
	String getSessionId();
	
	String getRemoteContact();
	
	String getServiceId();
	
	int getState();
	
	int getDirection();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	void abortSession();
	
	void addEventListener(in IMultimediaSessionListener listener);
	
	void removeEventListener(in IMultimediaSessionListener listener);

	boolean sendMessage(in byte[] content);
}

