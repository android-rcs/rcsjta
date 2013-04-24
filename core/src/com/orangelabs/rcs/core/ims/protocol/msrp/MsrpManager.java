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

package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.io.IOException;
import java.io.InputStream;

import com.orangelabs.rcs.utils.IpAddressUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * MSRP manager
 * 
 * @author jexa7410
 */
public class MsrpManager {
	/**
     * Local MSRP address
     */
    private String localMsrpAddress;

    /**
     * Local MSRP port
     */
    private int localMsrpPort;

    /**
     * MSRP session
     */
    private MsrpSession msrpSession = null;
    
    /**
     * Session Id
     */
    private long sessionId;
   
    /**
     * Secured connection
     */
    private boolean secured = false;
    
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param localIpAddress Local MSRP address
     * @param localMsrpPort Local MSRP port
     */    
    public MsrpManager(String localMsrpAddress, int localMsrpPort) {
    	this.localMsrpAddress = localMsrpAddress;
    	this.localMsrpPort = localMsrpPort;
    	this.sessionId = System.currentTimeMillis();
    }

	/**
	 * Returns the local MSRP port
	 * 
	 * @return Port number
	 */
	public int getLocalMsrpPort() {
		return localMsrpPort;
	}
    
    /**
     * Get the local socket protocol path
     * 
     * @return Protocol
     */
    public String getLocalSocketProtocol() {
    	if (secured) {
    		return "TCP/TLS/MSRP";
    	} else {
    		return "TCP/MSRP";
    	}
    }

	/**
     * Get the local MSRP path
     * 
     * @return MSRP path
     */
    public String getLocalMsrpPath() {
        if (IpAddressUtils.isIPv6(localMsrpAddress)) {
            return getMsrpProtocol() + "://[" + localMsrpAddress + "]:" + localMsrpPort + "/" + sessionId + ";tcp";
        } else {
            return getMsrpProtocol() + "://" + localMsrpAddress + ":" + localMsrpPort + "/" + sessionId + ";tcp";
        }
    }
    
    /**
     * Get the MSRP protocol
     * 
     * @return MSRP protocol
     */
    public String getMsrpProtocol() {
    	if (secured) {
    		return MsrpConstants.MSRP_SECURED_PROTOCOL;
    	} else {
    		return MsrpConstants.MSRP_PROTOCOL;
    	}
    }
    
    /**
	 * Return the MSRP session
	 * 
	 * @return MSRP session
	 */
	public MsrpSession getMsrpSession() {
		return msrpSession;
	}

	/**
	 * Is secured
	 * 
	 * @return Boolean
	 */
	public boolean isSecured() {
		return secured;
	}
	
	/**
	 * Set secured
	 * 
	 * @param flag Boolean flag
	 */
	public void setSecured(boolean flag) {
		this.secured = flag;
	}

	/**
	 * Open the MSRP session
	 * 
	 * @throws IOException
	 */
	public void openMsrpSession() throws IOException {
		if ((msrpSession == null) || (msrpSession.getConnection() == null)) {
			throw new IOException("Session not yet created");
		}
		
		msrpSession.getConnection().open();
	}
	
	/**
	 * Open the connection with SO_TIMEOUT on the socket
	 * 
	 * @param timeout Timeout value (in seconds)
	 * @throws IOException
	 */
	public void openMsrpSession(int timeout) throws IOException {
		if ((msrpSession == null) || (msrpSession.getConnection() == null)) {
			throw new IOException("Session not yet created");
		}

		msrpSession.getConnection().open(timeout);
	}
	
	/**
	 * Create a MSRP client session
	 * 
	 * @param remoteHost Remote host
	 * @param remotePort Remote port
     * @param remoteMsrpPath Remote MSRP path
     * @param listener Event listener
     * @return Created session
	 * @throws MsrpException
	 */
	public MsrpSession createMsrpClientSession(String remoteHost, int remotePort, String remoteMsrpPath, MsrpEventListener listener) throws MsrpException {
        try {
	        if (logger.isActivated()) {
				logger.info("Create MSRP client end point at " + remoteHost + ":" + remotePort);
			}
	
			// Create a new MSRP session
			msrpSession = new MsrpSession();
			msrpSession.setFrom(getLocalMsrpPath());
			msrpSession.setTo(remoteMsrpPath);

			// Create a MSRP client connection
			MsrpConnection connection = new MsrpClientConnection(msrpSession, remoteHost, remotePort, secured);

			// Associate the connection to the session
			msrpSession.setConnection(connection);
			
			// Add event listener
			msrpSession.addMsrpEventListener(listener);
			
        	// Return the created session
        	return msrpSession;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't create the MSRP client session", e);
			}
			throw new MsrpException("Create MSRP client session has failed");
		}
	}
	
	/**
	 * Create a MSRP server session
	 *
     * @param remoteMsrpPath Remote MSRP path
     * @param listener Event listener
     * @return Created session
	 * @throws MsrpException
	 */
	public MsrpSession createMsrpServerSession(String remoteMsrpPath, MsrpEventListener listener) throws MsrpException {
		if (logger.isActivated()) {
			logger.info("Create MSRP server end point at " + localMsrpPort);
		}

		// Create a MSRP session
		msrpSession = new MsrpSession();
		msrpSession.setFrom(getLocalMsrpPath());
		msrpSession.setTo(remoteMsrpPath);

		// Create a MSRP server connection
		MsrpConnection connection = new MsrpServerConnection(msrpSession, localMsrpPort);

		// Associate the connection to the session
		msrpSession.setConnection(connection);
		
		// Add event listener
		msrpSession.addMsrpEventListener(listener);

    	// Return the created session
    	return msrpSession;
	}

	/**
     * Send data chunks
     * 
     * @param inputStream Input stream
     * @param msgId Message ID
     * @param contentType Content type
     * @param contentSize Content size
     * @throws MsrpException
     */
    public void sendChunks(InputStream inputStream, String msgId, String contentType, long contentSize) throws MsrpException {
        if (msrpSession == null) {
        	throw new MsrpException("MSRP session is null");
        }

        msrpSession.sendChunks(inputStream, msgId, contentType, contentSize);
    }
    
    /**
     * Send an empty chunk
     * 
     * @throws MsrpException
     */
    public void sendEmptyChunk() throws MsrpException {
        if (msrpSession == null) {
        	throw new MsrpException("MSRP session is null");
        }

		msrpSession.sendEmptyChunk();
    }
    
    /**
     * Close the MSRP session
     */
    public synchronized void closeSession() {
        if (msrpSession != null) {
        	if (logger.isActivated()) {
        		logger.info("Close the MSRP session");
        	}
        	try {
	        	msrpSession.close();
        	} catch(Exception e) {
                // Intentionally blank
        	}
            msrpSession = null;
    	}
    }
}
