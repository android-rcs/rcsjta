package org.gsma.joyn.session;

import org.gsma.joyn.session.IMultimediaSession;
import org.gsma.joyn.session.IMultimediaSessionListener;

/**
 * Multimedia session service API
 */
interface IMultimediaSessionService {
	List<IBinder> getSessions(in String serviceId);
	
	IMultimediaSession getSession(in String sessionId);
	
	IMultimediaSession initiateSession(in String serviceId, in String contact, in String sdp, in IMultimediaSessionListener listener);
}


