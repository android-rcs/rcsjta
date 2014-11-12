package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
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

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	MultimediaSessionServiceConfiguration getConfiguration();

	List<IBinder> getMessagingSessions(in String serviceId);
	
	IMultimediaMessagingSession getMessagingSession(in String sessionId);
	
	IMultimediaMessagingSession initiateMessagingSession(in String serviceId, in ContactId contact);
	
	List<IBinder> getStreamingSessions(in String serviceId);
	
	IMultimediaStreamingSession getStreamingSession(in String sessionId);
	
	IMultimediaStreamingSession initiateStreamingSession(in String serviceId, in ContactId contact);

	int getServiceVersion();

	void addEventListener2(in IMultimediaMessagingSessionListener listener);

	void removeEventListener2(in IMultimediaMessagingSessionListener listener);

	void addEventListener3(in IMultimediaStreamingSessionListener listener);

	void removeEventListener3(in IMultimediaStreamingSessionListener listener);
}


