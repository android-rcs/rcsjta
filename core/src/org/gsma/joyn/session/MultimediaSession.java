package org.gsma.joyn.session;

import org.gsma.joyn.JoynServiceException;

/**
 * This class maintains the information related to a multimedia
 * session and offers methods to manage it. 
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSession {
    /**
     * Multimedia session state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * Session invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * Session invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * Session is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * Session has been aborted 
    	 */
    	public final static int ABORTED = 5;
    	
        /**
         * Session has been terminated
         */
        public static final int TERMINATED = 6;

        /**
    	 * Session has failed 
    	 */
    	public final static int FAILED = 7;
    	
        private State() {
        }    	
    }
    
    /**
     * Direction of the session
     */
    public static class Direction {
        /**
         * Incoming session
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing session
         */
        public static final int OUTGOING = 1;
    }     
    
    /**
     * Image sharing error
     */
    public static class Error {
    	/**
    	 * Session invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * Session has been cancelled
    	 */
    	public final static int SESSION_CANCELLED = 2;
    	
    	/**
    	 * Session has failed
    	 */
    	public final static int SESSION_FAILED = 3;

    	private Error() {
        }    	
    }
    
    /**
     * Multimedia session interface
     */
    private IMultimediaSession sessionInf;

    /**
     * Constructor
     * 
     * @param sessionInf Multimedia session interface
     */
    MultimediaSession(IMultimediaSession sessionInf) {
    	this.sessionInf = sessionInf;
    }

    /**
	 * Returns the session ID of the multimedia session
	 * 
	 * @return Session ID
	 * @throws JoynServiceException
	 */
	public String getSessionId() throws JoynServiceException {
		try {
			return sessionInf.getSessionId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the remote contact
	 * 
	 * @return Contact
	 * @throws JoynServiceException
	 */
	public String getRemoteContact() throws JoynServiceException {
		try {
			return sessionInf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 * @throws JoynServiceException
	 */
	public String getServiceId() throws JoynServiceException {
		try {
			return sessionInf.getServiceId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the state of the session
	 * 
	 * @return State
	 * @see MultimediaSession.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return sessionInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the direction of the session (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see MultimediaSession.Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return sessionInf.getDirection();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	
	
	/**
	 * Returns the local SDP
	 * 
	 * @return SDP
	 * @throws JoynServiceException
	 */
	public String getLocalSdp() throws JoynServiceException {
		try {
			return sessionInf.getLocalSdp();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the remote SDP
	 * 
	 * @return SDP
	 * @throws JoynServiceException
	 */
	public String getRemoteSdp() throws JoynServiceException {
		try {
			return sessionInf.getRemoteSdp();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Accepts session invitation. The SDP (Session Description Protocol)
	 * parameter is used to describe the supported media.
	 * 
	 * @param sdp SDP
	 * @throws JoynServiceException
	 */
	public void acceptInvitation(String sdp) throws JoynServiceException {
		try {
			sessionInf.acceptInvitation(sdp);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects session invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			sessionInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Aborts the session
	 * 
	 * @throws JoynServiceException
	 */
	public void abortSession() throws JoynServiceException {
		try {
			sessionInf.abortSession();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Adds a listener on session events
	 * 
	 * @param listener Session event listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(MultimediaSessionListener listener) throws JoynServiceException {
		try {
			sessionInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener on session events
	 * 
	 * @param listener Session event listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(MultimediaSessionListener listener) throws JoynServiceException {
		try {
			sessionInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
