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

package com.orangelabs.rcs.core.ims.protocol.sip;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.core.ims.service.SessionTimerManager;
import com.orangelabs.rcs.platform.registry.RegistryFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.IdGenerator;

import java.util.Vector;

import javax2.sip.Dialog;


/**
 * SIP dialog path. A dialog path corresponds to a SIP session, for
 * example from the INVITE to the BYE.
 * 
 * @author JM. Auffret
 */
public class SipDialogPath {
	/**
	 * Last min session expire period key
	 */
	private static final String REGISTRY_MIN_SESSION_EXPIRE_PERIOD = "MinSessionExpirePeriod";

	/**
	 * SIP stack interface
	 */
	private SipInterface stack = null;
	
	/**
	 * Call-Id
	 */
	private String callId = null;

	/**
	 * CSeq number
	 */
	private long cseq = 1;

	/**
	 * Local tag
	 */
	private String localTag = IdGenerator.getIdentifier();

	/**
	 * Remote tag
	 */
	private String remoteTag = null;

	/**
	 * Target
	 */
	private String target = null;

	/**
	 * Local party
	 */
	private String localParty = null;

	/**
	 * Remote party
	 */
	private String remoteParty = null;

	/**
	 * Initial INVITE request
	 */
	private SipRequest invite = null;

	/**
	 * Local content
	 */
	private String localContent = null;

	/**
	 * Remote content
	 */
	private String remoteContent = null;

    /**
     * Remote sip instance
     */
    private String remoteSipInstance = null;

	/**
	 * Route path
	 */
	private Vector<String> route = null;

	/**
	 * Authentication agent
	 */
	private SessionAuthenticationAgent authenticationAgent = null;

	/**
	 * Session expire time 
	 */
	private int sessionExpireTime; 
	
	/**
	 * Flag that indicates if the signalisation is established or not
	 */
	private boolean sigEstablished = false;

	/**
	 * Flag that indicates if the session (sig + media) is established or not
	 */
	private boolean sessionEstablished = false;

	/**
	 * Flag that indicates if the session has been cancelled by the end-user
	 */
	private boolean sessionCancelled = false;

	/**
	 * Flag that indicates if the session has been terminated by the server
	 */
	private boolean sessionTerminated = false;

	/**
	 * Session termination reason code
	 */
	private int sessionTerminationReasonCode = -1;
	
	/**
	 * Session termination reason phrase
	 */
	private String sessionTerminationReasonPhrase = null;
	
	/**
	 * Constructor
	 * 
	 * @param stack SIP stack interface
	 * @param callId Call-Id
	 * @param cseq CSeq
	 * @param target Target
	 * @param localParty Local party
	 * @param remoteParty Remote party
	 * @param route Route path
	 */
	public SipDialogPath(SipInterface stack,
			String callId,
			long cseq,
			String target,
			String localParty,
			String remoteParty,
			Vector<String> route) {
		this.stack = stack;
		this.callId = callId;
		this.cseq = cseq;
		this.target = SipUtils.extractUriFromAddress(target);
		this.localParty = localParty;
		this.remoteParty = remoteParty;
		this.route = route;
		
    	int defaultExpireTime = RcsSettings.getInstance().getSessionRefreshExpirePeriod();
    	int minExpireValue = RegistryFactory.getFactory().readInteger(REGISTRY_MIN_SESSION_EXPIRE_PERIOD, -1);
    	if ((defaultExpireTime > SessionTimerManager.MIN_EXPIRE_PERIOD) && (minExpireValue != -1) && (defaultExpireTime < minExpireValue)) {
        	this.sessionExpireTime = minExpireValue;
    	} else {
    		this.sessionExpireTime = defaultExpireTime;
    	}
	}

	/**
	 * Get the current SIP stack interface
	 * 
	 * @return SIP stack interface
	 */
	public SipInterface getSipStack() {
		return stack;
	}
	
	/**
	 * Get the target of the dialog path
	 * 
	 * @return String
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Set the target of the dialog path
	 * 
	 * @param tg Target address
	 */
	public void setTarget(String tg) {
		target = tg;
	}

	/**
	 * Get the local party of the dialog path
	 * 
	 * @return String
	 */
	public String getLocalParty() {
		return localParty;
	}

	/**
	 * Get the remote party of the dialog path
	 * 
	 * @return String
	 */
	public String getRemoteParty() {
		return remoteParty;
	}

	/**
	 * Get the local tag of the dialog path
	 * 
	 * @return String
	 */
	public String getLocalTag() {
		return localTag;
	}

	/**
	 * Get the remote tag of the dialog path
	 * 
	 * @return String
	 */
	public String getRemoteTag() {
		return remoteTag;
	}

	/**
	 * Set the remote tag of the dialog path
	 * 
	 * @param tag Remote tag
	 */
	public void setRemoteTag(String tag) {
		remoteTag = tag;
	}

	/**
	 * Get the call-id of the dialog path
	 * 
	 * @return String
	 */
	public String getCallId() {
		return callId;
	}

    /**
     * Set the call-id of the dialog path
     *
     * @return String
     */
    public void setCallId(String callId) {
        this.callId = callId;
    }

	/**
	 * Return the Cseq number of the dialog path
	 * 
	 * @return Cseq number
	 */
	public long getCseq() {
		return cseq;
	}

	/**
	 * Increment the Cseq number of the dialog path
	 */
	public void incrementCseq() {
		cseq++;
		
		// Increment internal stack CSeq if terminating side (NIST stack issue?)
		Dialog dlg = getStackDialog();
		if ((dlg != null) && dlg.isServer()) {
			dlg.incrementLocalSequenceNumber();
		}
	}

	/**
	 * Get the initial INVITE request of the dialog path
	 * 
	 * @return String
	 */
	public SipRequest getInvite() {
		return invite;
	}

	/**
	 * Set the initial INVITE request of the dialog path
	 * 
	 * @param invite INVITE request
	 */
	public void setInvite(SipRequest invite) {
		this.invite = invite;
	}
		
	/**
	 * Returns the local content
	 * 
	 * @return String
	 */
	public String getLocalContent() {
		return localContent;
	}

	/**
	 * Returns the remote content
	 * 
	 * @return String
	 */
	public String getRemoteContent() {
		return remoteContent;
	}

	/**
	 * Sets the local content
	 * 
	 * @param local Local content
	 */
	public void setLocalContent(String local) {
		this.localContent = local;
	}

    /**
     * Returns the remote SIP instance ID
     *
     * @return String
     */
    public String getRemoteSipInstance() {
        return remoteSipInstance;
    }

    /**
     * Sets the remote SIP instance ID
     *
     * @param instanceId SIP instance ID
     */
    public void setRemoteSipInstance(String instanceId) {
        this.remoteSipInstance = instanceId;
    }

	/**
	 * Sets the remote content
	 * 
	 * @param remote Remote content
	 */
	public void setRemoteContent(String remote) {
		this.remoteContent = remote;
	}

	/**
	 * Returns the route path
	 * 
	 * @return Vector of string
	 */
	public Vector<String> getRoute() {
		return route;
	}

	/**
	 * Set the route path
	 * 
	 * @param route New route path
	 */
	public void setRoute(Vector<String> route) {
		this.route = route;
	}
	
	/**
	 * Is session cancelled
	 * 
	 * @return Boolean
	 */
	public boolean isSessionCancelled() {
		return sessionCancelled;
	}
	
	/**
	 * The session has been cancelled
	 */
	public synchronized void sessionCancelled() {
		this.sessionCancelled = true;
	}
	
	/**
	 * Is session established
	 * 
	 * @return Boolean
	 */
	public boolean isSessionEstablished() {
		return sessionEstablished;
	}
	
	/**
	 * Session is established
	 */
	public synchronized void sessionEstablished() {
		this.sessionEstablished = true;
	}
	
	/**
	 * Is session terminated
	 * 
	 * @return Boolean
	 */
	public boolean isSessionTerminated() {
		return sessionTerminated;
	}
	
	/**
	 * Session is terminated
	 */
	public synchronized void sessionTerminated() {
		this.sessionTerminated = true;
		this.sessionTerminationReasonCode = -1;
		this.sessionTerminationReasonPhrase = null;
	}
	
	/**
	 * Session is terminated with a specific reason code
	 * 
	 * @param reason Reason code
	 * @param phrase Reason phrase
	 */
	public synchronized void sessionTerminated(int code, String phrase) {
		this.sessionTerminated = true;
		this.sessionTerminationReasonCode = code;
		this.sessionTerminationReasonPhrase = phrase;
	}

	/**
	 * Get session termination reason code
	 * 
	 * @return Reason code
	 */
	public int getSessionTerminationReasonCode() {
		return sessionTerminationReasonCode;
	}

	/**
	 * Get session termination reason phrase
	 * 
	 * @return Reason phrase
	 */
	public String getSessionTerminationReasonPhrase() {
		return sessionTerminationReasonPhrase;
	}

	/**
	 * Is signalisation established with success
	 * 
	 * @return Boolean
	 */
	public boolean isSigEstablished() {
		return sigEstablished;
	}
	
	/**
	 * Signalisation is established with success
	 */
	public synchronized void sigEstablished() {
		this.sigEstablished = true;
	}

	/**
	 * Set the session authentication agent
	 * 
	 * @param agent Authentication agent
	 */
	public void setAuthenticationAgent(SessionAuthenticationAgent agent) {
		this.authenticationAgent = agent;
	}
	
	/**
	 * Returns the session authentication agent
	 * 
	 * @return Authentication agent
	 */
	public SessionAuthenticationAgent getAuthenticationAgent() {
		return authenticationAgent;
	}

	/**
	 * Returns the session expire value
	 * 
	 * @return Session expire time in seconds
	 */
	public int getSessionExpireTime() {
		return sessionExpireTime;
	}

	/**
	 * Set the session expire value
	 * 
	 * @param sessionExpireTime Session expire time in seconds
	 */
	public void setSessionExpireTime(int sessionExpireTime) {
		this.sessionExpireTime = sessionExpireTime;
	}
	
	/**
	 * Set the min session expire value
	 * 
	 * @param sessionExpireTime Session expire time in seconds
	 */
	public void setMinSessionExpireTime(int sessionExpireTime) {
		RegistryFactory.getFactory().writeInteger(REGISTRY_MIN_SESSION_EXPIRE_PERIOD, sessionExpireTime);		
	}
	
	/**
	 * Get stack dialog
	 * 
	 * @return Dialog or null
	 */
	public Dialog getStackDialog() {
		if (invite != null) {
			return invite.getStackTransaction().getDialog();
		} else {
			return null;
		}
	}
}
