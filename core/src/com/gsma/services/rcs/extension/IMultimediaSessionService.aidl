package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;

/**
 * Multimedia session service API for extended services
 */
interface IMultimediaSessionService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	List<IBinder> getMessagingSessions(in String serviceId);
	
	IMultimediaMessagingSession getMessagingSession(in String sessionId);
	
	IMultimediaMessagingSession initiateMessagingSession(in String serviceId, in String contact, in IMultimediaMessagingSessionListener listener);
	
	int getServiceVersion();
}


