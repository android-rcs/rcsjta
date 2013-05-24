package org.gsma.joyn.ft;

import org.gsma.joyn.JoynServiceException;

/**
 * File transfer
 * 
 * @author jexa7410
 */
public class FileTransfer {

    /**
     * File transfer state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = -1;

    	/**
    	 * File transfer invitation received
    	 */
    	public final static int INVITED = 0;
    	
    	/**
    	 * File transfer invitation sent
    	 */
    	public final static int INITIATED = 1;
    	
    	/**
    	 * File transfer is started
    	 */
    	public final static int STARTED = 2;
    	
    	/**
    	 * File transfer has been transfered with success 
    	 */
    	public final static int TRANSFERED = 3;
    	
    	/**
    	 * File transfer has been aborted 
    	 */
    	public final static int ABORTED = 4;
    	
    	/**
    	 * File transfer has failed 
    	 */
    	public final static int FAILED = 5;
    	
        private State() {
        }    	
    }
    
    /**
     * File transfer error
     */
    public static class Error {
    	/**
    	 * Transfer has failed
    	 */
    	public final static int TRANSFER_FAILED = 0;
    	
    	/**
    	 * Transfer has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * Initiation has been cancelled
    	 */
    	public final static int TRANSFER_CANCELLED = 2;
    	
    	/**
    	 * Unsupported file type
    	 */
    	public final static int UNSUPPORTED_TYPE = 3;

    	/**
    	 * File saving has failed 
       	 */
    	public final static int SAVING_FAILED = 4;

        /**
         * File is too big
         */
        public final static int SIZE_TOO_BIG = 5;
    	
        private Error() {
        }    	
    }

    /**
     * File transfer interface
     */
    private IFileTransfer transferInf;
    
    /**
     * Constructor
     * 
     * @param transferIntf File transfer interface
     */
    FileTransfer(IFileTransfer transferIntf) {
    	this.transferInf = transferIntf;
    }
    	
    /**
	 * Returns the file transfer ID of the file transfer
	 * 
	 * @return Transfer ID
	 * @throws JoynServiceException
	 */
	public String getTransferId() throws JoynServiceException {
		try {
			return transferInf.getTransferId();
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
			return transferInf.getRemoteContact();
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
			return transferInf.getFileName();
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
			return transferInf.getFileSize();
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
			return transferInf.getFileType();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
    }

	/**
	 * Returns the state of the file transfer
	 * 
	 * @return State
	 * @see FileTransfer.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return transferInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}		
		
	/**
	 * Accepts file transfer invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void acceptInvitation() throws JoynServiceException {
		try {
			transferInf.acceptInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Rejects file transfer invitation
	 * 
	 * @throws JoynServiceException
	 */
	public void rejectInvitation() throws JoynServiceException {
		try {
			transferInf.rejectInvitation();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Aborts the file transfer
	 * 
	 * @throws JoynServiceException
	 */
	public void abortTransfer() throws JoynServiceException {
		try {
			transferInf.abortTransfer();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Adds a listener on file transfer events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public synchronized void addEventListener(FileTransferListener listener) throws JoynServiceException {
		try {
			transferInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener from file transfer
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public synchronized void removeEventListener(FileTransferListener listener) throws JoynServiceException {
		try {
			transferInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
