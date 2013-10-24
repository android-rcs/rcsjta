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

package com.orangelabs.rcs.core.ims.service;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Abstract IMS service
 * 
 * @author jexa7410
 */
public abstract class ImsService {
    /**
     * Terms & conditions service
     */
	public static final int TERMS_SERVICE = 0;

	/**
     * Capability service
     */
	public static final int CAPABILITY_SERVICE = 1;

    /**
     * Instant Messaging service
     */
	public static final int IM_SERVICE = 2;
	
	/**
     * IP call service
     */
	public static final int IPCALL_SERVICE = 3;

    /**
     * Richcall service
     */
	public static final int RICHCALL_SERVICE = 4;

    /**
     * Presence service
     */
	public static final int PRESENCE_SERVICE = 5;

    /**
     * SIP service
     */
	public static final int SIP_SERVICE = 6;
	
	/**
	 * Activation flag
	 */
	private boolean activated = true;

	/**
	 * Service state
	 */
	private boolean started = false;

	/**
	 * IMS module
	 */
	private ImsModule imsModule;

    /**
     * List of managed sessions
     */
    private Map<String, ImsServiceSession> sessions = Collections.synchronizedMap(new LinkedHashMap<String, ImsServiceSession>());

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param activated Activation flag
     * @throws CoreException
     */
	public ImsService(ImsModule parent, boolean activated) throws CoreException {
		this.imsModule = parent;
		this.activated = activated;
	}

    /**
     * Is service activated
     * 
     * @return Boolean
     */
	public boolean isActivated() {
		return activated;
	}

    /**
     * Change the activation flag of the service
     * 
     * @param activated Activation flag
     */
	public void setActivated(boolean activated) {
		this.activated = activated;
	}

    /**
     * Returns the IMS module
     * 
     * @return IMS module
     */
	public ImsModule getImsModule() {
		return imsModule;
	}

    /**
     * Returns a session
     * 
     * @param id Session ID
     * @return Session
     */
	public ImsServiceSession getSession(String id) {
		return (ImsServiceSession)sessions.get(id);
    }

    /**
     * Returns sessions associated to a contact
     * 
     * @param contact Contact number
     * @return List of sessions
     */
	public Enumeration<ImsServiceSession> getSessions(String contact) {
		Vector<ImsServiceSession> result = new Vector<ImsServiceSession>();
        synchronized(sessions) {
            Enumeration<ImsServiceSession> list = Collections.enumeration(sessions.values());
            while(list.hasMoreElements()) {
                ImsServiceSession session = list.nextElement();
                if (PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
                    result.add(session);
                }
            }
        }
		return result.elements();
    }

    /**
     * Returns the number of sessions in progress associated to a contact
     * 
     * @param contact Contact number
     * @return number of sessions
     */
    public int getNumberOfSessions(String contact) {
        int result = 0;
        synchronized(sessions) {
            Enumeration<ImsServiceSession> list = Collections.enumeration(sessions.values());
            while (list.hasMoreElements()) {
                ImsServiceSession session = list.nextElement();
                if (PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
                    result++;
                }
            }
        }
        return result;
    }

	/**
     * Returns the list of sessions
     * 
     * @return List of sessions
     */
	public Enumeration<ImsServiceSession> getSessions() {
        Vector<ImsServiceSession> result;
        synchronized(sessions) {
            result = new Vector<ImsServiceSession>(sessions.values());
        }
        return result.elements();

    }

    /**
     * Returns the number of sessions in progress
     * 
     * @return Number of sessions
     */
	public int getNumberOfSessions() {
		return sessions.size();
    }

    /**
     * Add a session
     * 
     * @param session Session
     */
	public void addSession(ImsServiceSession session) {
		if (logger.isActivated()) {
			logger.debug("Add new session " + session.getSessionID());
		}
		sessions.put(session.getSessionID(), session);
    }

    /**
     * Remove a session
     * 
     * @param session Session
     */
	public void removeSession(ImsServiceSession session) {
		if (logger.isActivated()) {
			logger.debug("Remove session " + session.getSessionID());
		}
		sessions.remove(session.getSessionID());
    }

    /**
     * Remove a session
     * 
     * @param id Session ID
     */
	public void removeSession(String id) {
		if (logger.isActivated()) {
			logger.debug("Remove session " + id);
		}
		sessions.remove(id);
    }

    /**
     * Is service started
     * 
     * @return Boolean
     */
	public boolean isServiceStarted() {
		return started;
	}

    /**
     * Set service state
     * 
     * @param state State
     */
	public void setServiceStarted(boolean state) {
		started = state;
	}

	/**
     * Start the IMS service
     */
	public abstract void start();

    /**
     * Stop the IMS service
     */
	public abstract void stop();

	/**
     * Check the IMS service
     */
	public abstract void check();
	
    /**
     * Send an error response to an invitation before to create a service session
     *
     * @param invite Invite request
	 * @param error Error code
     */
    public void sendErrorResponse(SipRequest invite, int error) {
        try {
            if (logger.isActivated()) {
                logger.info("Send error " + error);
            }
            SipResponse resp = SipMessageFactory.createResponse(invite, IdGenerator.getIdentifier(), error);

            // Send response
            getImsModule().getSipManager().sendSipResponse(resp);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't send error " + error, e);
            }
        }
    }	
}
