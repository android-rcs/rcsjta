package org.gsma.joyn.ish;

import org.gsma.joyn.JoynServiceException;

/**
 * Image sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharing {

    /**
     * Image sharing state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = -1;

    	/**
    	 * Sharing invitation received
    	 */
    	public final static int INVITED = 0;
    	
    	/**
    	 * Sharing invitation sent
    	 */
    	public final static int INITIATED = 1;
    	
    	/**
    	 * Sharing is started
    	 */
    	public final static int STARTED = 2;
    	
    	/**
    	 * Image has been transfered with success 
    	 */
    	public final static int TRANSFERED = 3;
    	
    	/**
    	 * Sharing has been aborted 
    	 */
    	public final static int ABORTED = 4;
    	
    	/**
    	 * Sharing has failed 
    	 */
    	public final static int FAILED = 5;
    	
        private State() {
        }    	
    }
    
    /**
     * Image sharing error
     */
    public static class Error {
    	/**
    	 * Sharing has failed
    	 */
    	public final static int SHARING_FAILED = 0;
    	
    	/**
    	 * Sharing has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * Initiation has been cancelled
    	 */
    	public final static int SHARING_CANCELLED = 2;
    	
    	/**
    	 * Unsupported file type
    	 */
    	public final static int UNSUPPORTED_TYPE = 3;

    	/**
    	 * Image saving has failed 
       	 */
    	public final static int SAVING_FAILED = 4;

        /**
         * Image is too big
         */
        public final static int SIZE_TOO_BIG = 5;
    	
        private Error() {
        }    	
    }

    /**
     * Image sharing interface
     */
    private IImageSharing sharingInf;
    
    /**
     * Constructor
     * 
     * @param sharingInf Image sharing interface
     */
    ImageSharing(IImageSharing sharingInf) {
    	this.sharingInf = sharingInf;
    }
    	
    /**
	 * Returns the sharing ID of the image sharing
	 * 
	 * @return Sharing ID
	 * @throws JoynServiceException
	 */
	public String getSharingId() throws JoynServiceException {
		try {
			return sharingInf.getSharingId();
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
			return sharingInf.getRemoteContact();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
     * Returns the complete filename including the path of the file to be transfered
     *
     * @return Filename
	 * @throws JoynServiceException
     */
	public String getFileName() throws JoynServiceException {
		try {
			return sharingInf.getFileName();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
     * Returns the size of the file to be transferred
     *
     * @return Size in bytes
	 * @throws JoynServiceException
     */
	public long getFileSize() throws JoynServiceException {
		try {
			return sharingInf.getFileSize();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
	 * @throws JoynServiceException
     */
    public String getFileType() throws JoynServiceException {
		try {
			return sharingInf.getFileType();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
    }

	/**
	 * Returns the state of the sharing
	 * 
	 * @return State
	 * @see ImageSharing.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return sharingInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}		
		
	/**
	 * Accepts image sharing invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void acceptInvitation() throws JoynServiceException {
		try {
			sharingInf.acceptInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects image sharing invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			sharingInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the sharing
	 * 
	 * @throws JoynServiceException
	 */
	public void abortSharing() throws JoynServiceException {
		try {
			sharingInf.abortSharing();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Adds a listener on image sharing events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public synchronized void addEventListener(ImageSharingListener listener) throws JoynServiceException {
		try {
			sharingInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener from image sharing
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public synchronized void removeEventListener(ImageSharingListener listener) throws JoynServiceException {
		try {
			sharingInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
