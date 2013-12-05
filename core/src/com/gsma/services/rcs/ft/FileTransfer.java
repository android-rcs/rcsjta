/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.gsma.services.rcs.ft;

import com.gsma.services.rcs.JoynServiceException;

/**
 * File transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransfer {

    /**
     * File transfer state
     */
    public static class State {
    	/**
    	 * Unknown state
    	 */
    	public final static int UNKNOWN = 0;

    	/**
    	 * File transfer invitation received
    	 */
    	public final static int INVITED = 1;
    	
    	/**
    	 * File transfer invitation sent
    	 */
    	public final static int INITIATED = 2;
    	
    	/**
    	 * File transfer is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * File transfer has been transferred with success 
    	 */
    	public final static int TRANSFERRED = 4;
    	
    	/**
    	 * File transfer has been aborted 
    	 */
    	public final static int ABORTED = 5;
    	
    	/**
    	 * File transfer has failed 
    	 */
    	public final static int FAILED = 6;
    	
        private State() {
        }    	
    }
    
    /**
     * Direction of the transfer
     */
    public static class Direction {
        /**
         * Incoming transfer
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing transfer
         */
        public static final int OUTGOING = 1;
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
    	 * Transfer invitation has been declined by remote
    	 */
    	public final static int INVITATION_DECLINED = 1;

    	/**
    	 * File saving has failed 
       	 */
    	public final static int SAVING_FAILED = 2;
    	
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
     * @hide
     */
    public FileTransfer(IFileTransfer transferIntf) {
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
     * Returns the complete filename including the path of the file to be transferred
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
     * Returns the complete filename including the path of the file icon
     *
     * @return Filename
	 * @throws JoynServiceException
     */
	public String getFileIconName() throws JoynServiceException {
		try {
			return transferInf.getFileIconName();
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
	 * Returns the direction of the transfer (incoming or outgoing)
	 * 
	 * @return Direction
	 * @see FileTransfer.Direction
	 * @throws JoynServiceException
	 */
	public int getDirection() throws JoynServiceException {
		try {
			return transferInf.getDirection();
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
	 * Pauses the file transfer
	 * 
	 * @throws JoynServiceException
	 */
	public void pauseTransfer() throws JoynServiceException {
		try {
			transferInf.pauseTransfer();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Resumes the file transfer
	 * 
	 * @throws JoynServiceException
	 */
	public void resumeTransfer() throws JoynServiceException {
		try {
			transferInf.resumeTransfer();
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
	public void addEventListener(FileTransferListener listener) throws JoynServiceException {
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
	public void removeEventListener(FileTransferListener listener) throws JoynServiceException {
		try {
			transferInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
