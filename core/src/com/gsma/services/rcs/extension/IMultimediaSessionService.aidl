package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.IMultimediaStreamingSession;
import com.gsma.services.rcs.extension.IMultimediaStreamingSessionListener;
import com.gsma.services.rcs.extension.MultimediaSessionServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Multimedia session service API for extended services
 */
interface IMultimediaSessionService {

	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	MultimediaSessionServiceConfiguration getConfiguration();

	List<IBinder> getMessagingSessions(in String serviceId);
	
	IMultimediaMessagingSession getMessagingSession(in String sessionId);
	
	IMultimediaMessagingSession initiateMessagingSession(in String serviceId, in ContactId contact);
	
	List<IBinder> getStreamingSessions(in String serviceId);
	
	IMultimediaStreamingSession getStreamingSession(in String sessionId);
	
	IMultimediaStreamingSession initiateStreamingSession(in String serviceId, in ContactId contact);

	int getServiceVersion();

	void addMessagingEventListener(in IMultimediaMessagingSessionListener listener);

	void removeMessagingEventListener(in IMultimediaMessagingSessionListener listener);

	void addStreamingEventListener(in IMultimediaStreamingSessionListener listener);

	void removeStreamingEventListener(in IMultimediaStreamingSessionListener listener);
}


