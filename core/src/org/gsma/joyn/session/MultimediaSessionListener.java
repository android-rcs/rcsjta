package org.gsma.joyn.session;

/**
 * This class offers callback methods on multimedia session events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class MultimediaSessionListener extends IMultimediaSessionListener.Stub {
	/**
	 * Callback called when the session is pending.
	 */
	public abstract void onSessionRinging();
	
	/**
	 * Callback called when the session is started
	 */
	public abstract void onSessionStarted();
	
	/**
	 * Callback called when the session has been aborted
	 */
	public abstract void onSessionAborted();
	
	/**
	 * Callback called when the session has failed
	 * 
	 * @param error Error
	 * @see MultimediaSession.Error
	 */
	public abstract void onSessionError(int error);
}
