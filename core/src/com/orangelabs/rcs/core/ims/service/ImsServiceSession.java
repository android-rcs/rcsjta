/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service;

import java.util.Collection;
import java.util.Vector;

import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IMS service session
 * 
 * @author jexa7410
 */
public abstract class ImsServiceSession extends Thread {
	/**
	 * Session invitation status
	 */
	public final static int INVITATION_NOT_ANSWERED = 0;
	public final static int INVITATION_ACCEPTED = 1;
	public final static int INVITATION_REJECTED = 2;
    public final static int INVITATION_CANCELED = 3; 

	/**
	 * Session termination reason
	 */
    public final static int TERMINATION_BY_SYSTEM = 0;
    public final static int TERMINATION_BY_USER = 1;
    public final static int TERMINATION_BY_TIMEOUT = 2;
    
	private final static int SESSION_INTERVAL_TOO_SMALL = 422;
    
	/**
     * IMS service
     */
    private ImsService imsService;
    
    /**
     * Session ID
     */
    private String sessionId =  SessionIdGenerator.getNewId();

	/**
	 * Remote contactId
	 */
	private ContactId contact;
		
	/**
	 * Remote contactUri
	 */
	private String remoteUri;

    /**
     * Remote display name
     */
    private String remoteDisplayName;

    /**
	 * Dialog path
	 */
    private SipDialogPath dialogPath;

	/**
	 * Authentication agent
	 */
	private SessionAuthenticationAgent authenticationAgent;

	/**
	 * Session invitation status
	 */
	protected int invitationStatus = INVITATION_NOT_ANSWERED;
	
	/**
	 * Wait user answer for session invitation
	 */
	protected Object waitUserAnswer = new Object();

	/**
	 * Session listeners
	 */
	private Vector<ImsSessionListener> listeners = new Vector<ImsSessionListener>();

	/**
	 * Session timer manager
	 */
	private SessionTimerManager sessionTimer = new SessionTimerManager(this);

	/**
	 * Update session manager
	 */
	protected UpdateSessionManager updateMgr;
	
    /**
     * Ringing period (in seconds)
     */
    private int ringingPeriod = RcsSettings.getInstance().getRingingPeriod();

    /**
     * Session interrupted flag 
     */
    private boolean sessionInterrupted = false;

    /**
     * Session terminated by remote flag
     */
    private boolean sessionTerminatedByRemote = false;

    /**
     * Session accepting flag
     */
    private boolean mSessionAccepted = false;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ImsServiceSession.class.getSimpleName());

    /**
	 * Constructor
	 * 
	 * @param imsService IMS service
	 * @param contact Remote contact Identifier
	 * @param remoteUri Remote URI
	 */
	public ImsServiceSession(ImsService imsService, ContactId contact, String remoteUri) {
        this.imsService = imsService;
		this.contact = contact;
		this.remoteUri =  remoteUri;
		this.authenticationAgent = new SessionAuthenticationAgent(imsService.getImsModule());
		this.updateMgr = new UpdateSessionManager(this);
	}

	/**
	 * Create originating dialog path
	 */
	public void createOriginatingDialogPath() {
        // Set Call-Id
    	String callId = getImsService().getImsModule().getSipManager().getSipStack().generateCallId();

    	// Set the route path
    	Vector<String> route = getImsService().getImsModule().getSipManager().getSipStack().getServiceRoutePath();

    	// Create a dialog path
    	dialogPath = new SipDialogPath(
    			getImsService().getImsModule().getSipManager().getSipStack(),
    			callId,
				1,
				remoteUri,
				ImsModule.IMS_USER_PROFILE.getPublicAddress(),
				remoteUri,
				route);
    	
    	// Set the authentication agent in the dialog path 
    	dialogPath.setAuthenticationAgent(getAuthenticationAgent());
    	
    	if (contact != null) {
    		try {
        		remoteDisplayName = ContactsManager.getInstance().getContactDisplayName(contact);
			} catch (Exception e) {
				// RCS account does not exist
			}
    	}
	}
		
	/**
	 * Create terminating dialog path
	 * 
	 * @param invite Incoming invite
	 */
	public void createTerminatingDialogPath(SipRequest invite) {
	    // Set the call-id
		String callId = invite.getCallId();
	
	    // Set target
	    String target = invite.getContactURI();
	
	    // Set local party
	    String localParty = invite.getTo();
	
	    // Set remote party
	    String remoteParty = invite.getFrom();
	
	    // Get the CSeq value
	    long cseq = invite.getCSeq();
	    
	    // Set the route path with the Record-Route 
	    Vector<String> route = SipUtils.routeProcessing(invite, false);
	    
	   	// Create a dialog path
		dialogPath = new SipDialogPath(
				getImsService().getImsModule().getSipManager().getSipStack(),
				callId,
				cseq,
				target,
				localParty,
				remoteParty,
				route);
	
	    // Set the INVITE request
		dialogPath.setInvite(invite);
	
	    // Set the remote tag
		dialogPath.setRemoteTag(invite.getFromTag());
	
	    // Set the remote content part
		dialogPath.setRemoteContent(invite.getContent());
		
		// Set the session timer expire
		dialogPath.setSessionExpireTime(invite.getSessionTimerExpire());
		
		remoteDisplayName = SipUtils.getDisplayNameFromUri(remoteParty);
	}
	
	/**
	 * Add a listener for receiving events
	 * 
	 * @param listener Listener
	 */
	public void addListener(ImsSessionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener
	 */
	public void removeListener(ImsSessionListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Remove all listeners
	 */
	public void removeListeners() {
		listeners.removeAllElements();
	}

	/**
	 * Returns the event listeners
	 * 
	 * @return Listeners
	 */
	public Vector<ImsSessionListener> getListeners() {
		return listeners;
	}
	
	/**
	 * Get the session timer manager
	 * 
	 * @return Session timer manager
	 */
	public SessionTimerManager getSessionTimerManager() {
		return sessionTimer;
	}
	
	/**
	 * Get the update session manager
	 * 
	 * @return UpdateSessionManager
	 */
	public UpdateSessionManager getUpdateSessionManager() {
		return updateMgr;
	}

    /**
     * Is behind a NAT
     *
     * @return Boolean
     */
    public boolean isBehindNat() {
		return getImsService().getImsModule().getCurrentNetworkInterface().isBehindNat();
    }	

	/**
	 * Start the session in background
	 */
	public void startSession() {
		// Add the session in the session manager
		imsService.addSession(this);
		
		// Start the session
		start();
	}
	
	/**
	 * Return the IMS service
	 * 
	 * @return IMS service
	 */
	public ImsService getImsService() {
		return imsService;
	}
	
	/**
	 * Return the session ID
	 * 
	 * @return Session ID
	 */
	public String getSessionID() {
		return sessionId;
	}

	/**
	 * Set the session ID
	 * 
	 * @param sessionId
	 *            <p>
	 *            <b>Be Careful:</b><br />
	 *            Should only be called to resume session (like for FT HTTP).
	 *            </p>
	 */
	public void setSessionID(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns the remote contactId
	 * 
	 * @return ContactId
	 */
	public ContactId getRemoteContact() {
		return contact;
	}

	/**
	 * Returns the remote Uri
	 * 
	 * @return remoteUri
	 */
	public String getRemoteUri() {
		return remoteUri;
	}
	
	/**
	 * Returns display name of the remote contact
	 * 
	 * @return String
	 */
	public String getRemoteDisplayName() {
	    return remoteDisplayName;
	}

    /**
     * Set display name of the remote contact
     * 
     * @param String
     */
    public void setRemoteDisplayName(String remoteDisplayName) {
        this.remoteDisplayName = remoteDisplayName;
    }

	/**
	 * Get the dialog path of the session
	 * 
	 * @return Dialog path object
	 */
	public SipDialogPath getDialogPath() {
		return dialogPath;
	}

	/**
	 * Set the dialog path of the session
	 * 
	 * @param dialog Dialog path
	 */
	public void setDialogPath(SipDialogPath dialog) {
		dialogPath = dialog;
	}
	
    /**
     * Returns the authentication agent
     * 
     * @return Authentication agent
     */
	public SessionAuthenticationAgent getAuthenticationAgent() {
		return authenticationAgent;
	}
	
	/**
	 * Reject the session invitation
	 * 
	 * @param code Error code
	 */
	public void rejectSession(int code) {
		if (logger.isActivated()) {
			logger.debug("Session invitation has been rejected");
		}
		invitationStatus = INVITATION_REJECTED;

		// Unblock semaphore
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}

		// Decline the invitation
		sendErrorResponse(getDialogPath().getInvite(), getDialogPath().getLocalTag(), code);
			
		// Remove the session in the session manager
		imsService.removeSession(this);
	}	
	
	/**
	 * Accept the session invitation
	 */
	public void acceptSession() {
		if (logger.isActivated()) {
			logger.debug("Session invitation has been accepted");
		}
		invitationStatus = INVITATION_ACCEPTED;

		// Unblock semaphore
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}
	}

	/**
	 * Wait session invitation answer
	 * 
	 * @return Answer
	 */
	public int waitInvitationAnswer() {
		if (invitationStatus != INVITATION_NOT_ANSWERED) {
			return invitationStatus;
		}
		
		if (logger.isActivated()) {
			logger.debug("Wait session invitation answer");
		}
		
		// Wait until received response or received timeout
		try {
			synchronized(waitUserAnswer) {
				waitUserAnswer.wait(ringingPeriod * 1000);
			}
		} catch(InterruptedException e) {
			sessionInterrupted = true;
		}
		
		return invitationStatus;
	}
	
	/**
	 * Interrupt session
	 */
	public void interruptSession() {
		if (logger.isActivated()) {
			logger.debug("Interrupt the session");
		}
		
		try {
			// Unblock semaphore
			synchronized(waitUserAnswer) {
				waitUserAnswer.notifyAll();
			}
			
			if (!isSessionInterrupted()) {
				// Interrupt thread
                sessionInterrupted = true;
                interrupt();
			}
		} catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't interrupt the session correctly", e);
        	}
		}
    	if (logger.isActivated()) {
    		logger.debug("Session has been interrupted");
    	}
	}
	
	/**
	 * Abort the session
	 * 
	 * @param reason Termination reason
	 */
	public void abortSession(int abortedReason) {
		if (logger.isActivated()) {
			logger.info("Abort the session " + abortedReason);
		}

		interruptSession();

		terminateSession(abortedReason);

		closeMediaSession();

		getImsService().removeSession(this);

		/* TODO: This will be changed anyway by the implementation of CR018 */
		Collection<ImsSessionListener> listeners = getListeners();
		/* Handles the case of REJECTED_BY_USER on originating session */
		if (abortedReason == ImsServiceSession.TERMINATION_BY_USER && dialogPath != null
				&& !dialogPath.isSigEstablished()) {
			for (ImsSessionListener listener : listeners) {
				listener.handleSessionRejectedByUser();
			}
			return;
		}

		for (ImsSessionListener listener : listeners) {
			listener.handleSessionAborted(abortedReason);
		}
	}
	
	/**
	 * Terminate session
	 * 
	 * @param reason Reason
	 */
	public void terminateSession(int reason) {
		if (logger.isActivated()) {
			logger.debug("Terminate the session (reason " + reason + ")");
		}
		
		if ((dialogPath == null) || dialogPath.isSessionTerminated()) {
			// Already terminated
			return;
		}
		
    	// Stop session timer
    	getSessionTimerManager().stop();		

		// Update dialog path
    	if (reason == ImsServiceSession.TERMINATION_BY_USER) {
    		dialogPath.sessionTerminated(200, "Call completed");
    	} else {
    		dialogPath.sessionTerminated();
    	}

		// Unblock semaphore (used for terminating side only)
		synchronized(waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}

		try {
			// Terminate the session
        	if (dialogPath.isSigEstablished()) {
		        // Increment the Cseq number of the dialog path
		        getDialogPath().incrementCseq();
	
		        // Send BYE without waiting a response
		        getImsService().getImsModule().getSipManager().sendSipBye(getDialogPath());
        	} else {
		        // Send CANCEL without waiting a response
		        getImsService().getImsModule().getSipManager().sendSipCancel(getDialogPath());
        	}
        	
        	if (logger.isActivated()) {
        		logger.debug("SIP session has been terminated");
        	}
		} catch(Exception e) { 
        	if (logger.isActivated()) {
        		logger.error("Session termination has failed", e);
        	}
		}
	}

	/**
	 * Receive BYE request 
	 * 
	 * @param bye BYE request
	 */
	public void receiveBye(SipRequest bye) {
    	if (logger.isActivated()) {
    		logger.info("Receive a BYE message from the remote");
    	}

    	// Close media session
    	closeMediaSession();
    	
        // Update the dialog path status
		getDialogPath().sessionTerminated();
        sessionTerminatedByRemote = true;

    	// Remove the current session
    	getImsService().removeSession(this);
	
    	// Stop session timer
    	getSessionTimerManager().stop();		

    	// Notify listeners
    	for(int i=0; i < getListeners().size(); i++) {
    		getListeners().get(i).handleSessionTerminatedByRemote();
        }

		try {
			ContactId remote = ContactUtils.createContactId(getDialogPath().getRemoteParty());
			// Request capabilities to the remote
			getImsService().getImsModule().getCapabilityService().requestContactCapabilities(remote);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.debug("Cannot parse contact " + getDialogPath().getRemoteParty());
			}
		}
	}
	
	/**
	 * Receive CANCEL request
	 * 
	 * @param cancel
	 *            CANCEL request
	 */
	public void receiveCancel(SipRequest cancel) {
		if (logger.isActivated()) {
			logger.info("Receive a CANCEL message from the remote");
		}

		if (getDialogPath().isSigEstablished()) {
			if (logger.isActivated()) {
				logger.info("Ignore the received CANCEL message from the remote (session already established)");
			}
			return;
		}

		// Close media session
		closeMediaSession();

		// Update dialog path
		getDialogPath().sessionCancelled();

		// Send a 487 Request terminated
		try {
			if (logger.isActivated()) {
				logger.info("Send 487 Request terminated");
			}
			SipResponse terminatedResp = SipMessageFactory.createResponse(getDialogPath().getInvite(), getDialogPath()
					.getLocalTag(), 487);
			getImsService().getImsModule().getSipManager().sendSipResponse(terminatedResp);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 487 error response", e);
			}
		}

		// Remove the current session
		getImsService().removeSession(this);

		// Set invitation status
		invitationStatus = ImsServiceSession.INVITATION_CANCELED;

		// Unblock semaphore
		synchronized (waitUserAnswer) {
			waitUserAnswer.notifyAll();
		}
	}

	/**
	 * Receive re-INVITE request 
	 *
	 * @param reInvite re-INVITE request
	 */
	public void receiveReInvite(SipRequest reInvite) {
		// Session refresh management
		sessionTimer.receiveReInvite(reInvite);
	}

	/**
	 * Receive UPDATE request 
	 * 
	 * @param update UPDATE request
	 */
	public void receiveUpdate(SipRequest update) {
		sessionTimer.receiveUpdate(update);
	}

	/**
	 * Set session accepted
	 */
	public void setSessionAccepted() {
		mSessionAccepted = true;
	}

    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
    public abstract void prepareMediaSession() throws Exception;

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public abstract void startMediaSession() throws Exception;

	/**
	 * Close media session
	 */
	public abstract void closeMediaSession();

	/**
     * Send a 180 Ringing response to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     */
	public void send180Ringing(SipRequest request, String localTag) {
    	try {
	    	SipResponse progress = SipMessageFactory.createResponse(request, localTag, 180);
            getImsService().getImsModule().getSipManager().sendSipResponse(progress);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Can't send a 180 Ringing response");
    		}
    	}
    }

    /**
     * Send an error response to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     * @param code Response code
     */
	public void sendErrorResponse(SipRequest request, String localTag, int code) {
		try {
	        // Send  error
	    	if (logger.isActivated()) {
	    		logger.info("Send " + code + " error response");
	    	}
	        SipResponse resp = SipMessageFactory.createResponse(request, localTag, code);
	        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send error response", e);
			}
		}
	}
	
	/**
     * Send a 603 "Decline" to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     */
	public void send603Decline(SipRequest request, String localTag) {
		try {
            // Send a 603 Decline error
            if (logger.isActivated()) {
                logger.info("Send 603 Decline");
            }
            SipResponse resp = SipMessageFactory.createResponse(request, localTag, 603);
            getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 603 Decline response", e);
			}
		}
	}
	
	/**
	 * Send a 403 "Forbidden" to the remote party
	 * 
	 * @param request
	 *            SIP request
	 * @param localTag
	 *            Local tag
	 * @param warning
	 *            the warning message
	 */
	public void send403Forbidden(SipRequest request, String localTag, String warning) {
		try {
			// Send a 403 Forbidden
			if (logger.isActivated()) {
				logger.info("Send 403 Forbidden (warning=" + warning + ")");
			}
			SipResponse resp = SipMessageFactory.createResponse(request, localTag, 403, warning);
			getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 403 Forbidden response", e);
			}
		}
	}
	
    /**
     * Send a 486 "Busy" to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     */
	public void send486Busy(SipRequest request, String localTag) {
		try {
            // Send a 486 Busy error
            if (logger.isActivated()) {
                logger.info("Send 486 Busy");
            }
            SipResponse resp = SipMessageFactory.createResponse(request, localTag, 486);
            getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 486 Busy response", e);
			}
		}
	}	
	
    /**
     * Send a 415 "Unsupported Media Type" to the remote party
     * 
     * @param request SIP request
     */
	public void send415Error(SipRequest request) {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Send 415 Unsupported Media Type");
	    	}
	        SipResponse resp = SipMessageFactory.createResponse(request, 415);
	        // TODO: set Accept-Encoding header
	        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't send 415 error response", e);
			}
		}
	}	
	
	/**
	 * Create SDP setup offer (see RFC6135, RFC4145)
	 * 
	 * @return Setup offer
	 */
	public String createSetupOffer() {
    	if (isBehindNat()) {
    		// Active mode by default if there is a NAT
    		return "active";
    	} else {
        	// Active/passive mode is exchanged in order to be compatible
    		// with UE not supporting COMEDIA
        	return "actpass";
    	}
	}
	
	/**
	 * Create SDP setup offer for mobile to mobile (see RFC6135, RFC4145)
	 * 
	 * @return Setup offer
	 */
	public String createMobileToMobileSetupOffer() {
		// Always active mode proposed here
		return "active";
	}
	
	/**
	 * Create SDP setup answer (see RFC6135, RFC4145)
	 * 
	 * @param offer setup offer
	 * @return Setup answer ("active" or "passive")
	 */
	public String createSetupAnswer(String offer) {
    	if (offer.equals("actpass")) {
        	// Active mode by default if there is a NAT or AS IM
    		return "active";
    	} else
        if (offer.equals("active")) {
        	// Passive mode
			return "passive";
        } else 
        if (offer.equals("passive")) {
        	// Active mode
			return "active";
        } else {
        	// Passive mode by default
			return "passive";
        }
	}
	
	/**
	 * Returns the response timeout (in seconds) 
	 * 
	 * @return Timeout
	 */
	public int getResponseTimeout() {
		return ringingPeriod + SipManager.TIMEOUT;
	}
	
	/**
	 * Is session interrupted
	 * 
	 * @return Boolean
	 */
	public boolean isSessionInterrupted() {
		return sessionInterrupted || isInterrupted() ||
				(getDialogPath() != null && getDialogPath().isSessionTerminated());
	}

    /**
     * Is session terminated by remote
     * 
     * @return Boolean
     */
    public boolean isSessionTerminatedByRemote() {
        return sessionTerminatedByRemote;
    }

    /**
     * Is session accepted
     *
     * @return Boolean
     */
    public boolean isSessionAccepted() {
        return mSessionAccepted;
    }

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException
     */
    public abstract SipRequest createInvite() throws SipException;

    /**
     * Send INVITE message
     *
     * @param invite SIP INVITE
     * @throws SipException
     */
    public void sendInvite(SipRequest invite) throws SipException {
		// Send INVITE request
		SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
				.sendSipMessageAndWait(invite, getResponseTimeout(), new SipTransactionContext.INotifySipProvisionalResponse() {
					public void handle180Ringing(SipResponse response) {
						ImsServiceSession.this.handle180Ringing(response);
					}

				});

        // Analyze the received response 
		if (ctx.isSipResponse()) {
			// A response has been received
			switch (ctx.getStatusCode()) {
			case Response.OK:
				// 200 OK
				handle200OK(ctx.getSipResponse());
				break;
			case Response.NOT_FOUND:
				// 404 session not found
				handle404SessionNotFound(ctx.getSipResponse());
				break;
			case Response.PROXY_AUTHENTICATION_REQUIRED:
				// 407 Proxy Authentication Required
				handle407Authentication(ctx.getSipResponse());
				break;
			case SESSION_INTERVAL_TOO_SMALL:
				// 422 Session Interval Too Small
				handle422SessionTooSmall(ctx.getSipResponse());
				break;
			case Response.TEMPORARILY_UNAVAILABLE:
                // 480 Temporarily Unavailable 
                handle480Unavailable(ctx.getSipResponse());
                break;
			case Response.BUSY_HERE:
                // 486 busy  
				handle486Busy(ctx.getSipResponse());
				break;
			case Response.REQUEST_TERMINATED:
                // 487 Invitation cancelled
                handle487Cancel(ctx.getSipResponse());
                break;
			case Response.DECLINE:
                // 603 Invitation declined
                handle603Declined(ctx.getSipResponse());
                break;
			default:
				// Other error response
                handleDefaultError(ctx.getSipResponse());
				break;
			}
        } else {
            // No response received: timeout
            handleError(new ImsSessionBasedServiceError(ImsSessionBasedServiceError.SESSION_INITIATION_FAILED, "timeout"));
        }
    }

    /**
     * Handle 200 0K response 
     *
     * @param resp 200 OK response
     */
    public void handle200OK(SipResponse resp) {
        try {
            // 200 OK received
            if (logger.isActivated()) {
                logger.info("200 OK response received");
            }

            // The signaling is established
            getDialogPath().sigEstablished();

            // Set the remote tag
            getDialogPath().setRemoteTag(resp.getToTag());
            
            // Set the target
            getDialogPath().setTarget(resp.getContactURI());

            // Set the route path with the Record-Route header
            Vector<String> newRoute = SipUtils.routeProcessing(resp, true);
            getDialogPath().setRoute(newRoute);

            // Set the remote SDP part
            getDialogPath().setRemoteContent(resp.getContent());

            // Set the remote SIP instance ID
            ContactHeader remoteContactHeader = (ContactHeader)resp.getHeader(ContactHeader.NAME);
            if (remoteContactHeader != null) {
                getDialogPath().setRemoteSipInstance(remoteContactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM));
            }

            // Prepare Media Session
            prepareMediaSession();

            // Send ACK request
            if (logger.isActivated()) {
                logger.info("Send ACK");
            }
            getImsService().getImsModule().getSipManager().sendSipAck(getDialogPath());

            // The session is established
            getDialogPath().sessionEstablished();

            // Start Media Session
            startMediaSession();

            // Notify listeners
            for(int i=0; i < getListeners().size(); i++) {
                getListeners().get(i).handleSessionStarted();
            }

            // Start session timer
            if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                getSessionTimerManager().start(resp.getSessionTimerRefresher(), resp.getSessionTimerExpire());
            }
        } catch(Exception e) {
            // Unexpected error
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }
            handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

    /**
     * Handle default error
     *
     * @param resp Error response
     */
    public void handleDefaultError(SipResponse resp) {
        // Default handle
        handleError(new ImsSessionBasedServiceError(ImsSessionBasedServiceError.SESSION_INITIATION_FAILED,
                resp.getStatusCode() + " " + resp.getReasonPhrase()));
    }

    /**
     * Handle 403 Forbidden
     *
     * @param resp 403 response
     */
    public void handle403Forbidden(SipResponse resp) {
        handleDefaultError(resp);
    }

    /**
     * Handle 404 Session Not Found
     *
     * @param resp 404 response
     */
    public void handle404SessionNotFound(SipResponse resp) {
        handleDefaultError(resp);
    }

    /**
     * Handle 407 Proxy Authentication Required
     *
     * @param resp 407 response
     */
    public void handle407Authentication(SipResponse resp) {
        try {
            if (logger.isActivated()) {
                logger.info("407 response received");
            }

            // Set the remote tag
            getDialogPath().setRemoteTag(resp.getToTag());

            // Update the authentication agent
            getAuthenticationAgent().readProxyAuthenticateHeader(resp);

            // Increment the Cseq number of the dialog path
            getDialogPath().incrementCseq();

            // Create the invite request
            SipRequest invite = createInvite();

            // Reset initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Set the Proxy-Authorization header
            getAuthenticationAgent().setProxyAuthorizationHeader(invite);

            // Send INVITE request
            sendInvite(invite);

        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ImsServiceError(ImsServiceError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }

    /**
     * Handle 422 response 
     *
     * @param resp 422 response
     */
    public void handle422SessionTooSmall(SipResponse resp) {
        try {
            // 422 response received
            if (logger.isActivated()) {
                logger.info("422 response received");
            }

            // Extract the Min-SE value
            int minExpire = SipUtils.getMinSessionExpirePeriod(resp);
            if (minExpire == -1) {
                if (logger.isActivated()) {
                    logger.error("Can't read the Min-SE value");
                }
                handleError(new ImsSessionBasedServiceError(ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, "No Min-SE value found"));
                return;
            }

            // Set the min expire value
            getDialogPath().setMinSessionExpireTime(minExpire);

            // Set the expire value
            getDialogPath().setSessionExpireTime(minExpire);

            // Increment the Cseq number of the dialog path
            getDialogPath().incrementCseq();

            // Create a new INVITE with the right expire period
            if (logger.isActivated()) {
                logger.info("Send new INVITE");
            }
            SipRequest invite = createInvite();

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Reset initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Send INVITE request
            sendInvite(invite);
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ImsSessionBasedServiceError(ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                    e.getMessage()));
        }
    }
    
    /**
     * Handle 480 Temporarily Unavailable
     *
     * @param resp 480 response
     */
    public void handle480Unavailable(SipResponse resp) {
        handleDefaultError(resp);
    }

    /**
     * Handle 486 Busy
     *
     * @param resp 486 response
     */
    public void handle486Busy(SipResponse resp) {
        handleDefaultError(resp);
    }

    /**
     * Handle 487 Cancel
     *
     * @param resp 487 response
     */
    public void handle487Cancel(SipResponse resp) {
        handleError(new ImsSessionBasedServiceError(ImsSessionBasedServiceError.SESSION_INITIATION_CANCELLED,
                resp.getStatusCode() + " " + resp.getReasonPhrase()));
    }

    /**
     * Handle 603 Decline
     *
     * @param resp 603 response
     */
    public void handle603Declined(SipResponse resp) {
        handleError(new ImsSessionBasedServiceError(ImsSessionBasedServiceError.SESSION_INITIATION_DECLINED,
                resp.getStatusCode() + " " + resp.getReasonPhrase()));
    }

    /**
     * Handle Error 
     *
     * @param error ImsServiceError
     */
    public abstract void handleError(ImsServiceError error);
    
    /**
     * Handle ReInvite Sip Response
     *
     * @param response Sip response to reInvite
     * @param int code response code
     * @param reInvite reInvite SIP request
     */
    public void handleReInviteResponse(int  code, SipResponse response, int requestType) {   	
    }
    
    /**
     * Handle User Answer in Response to Session Update notification 
     *
     * @param int code response code
     * @param reInvite reInvite SIP request
     */
    public void handleReInviteUserAnswer(int  code, int requestType) {   	
    }
    
    /**
     * Handle ACK sent in Response to 200Ok ReInvite 
     *
     * @param int code response code
     * @param reInvite reInvite SIP request
     */
    public void handleReInviteAck(int  code, int requestType) {   	
    }
    
    /**
     * Handle 407 Proxy Authent error ReInvite Response
     *
     * @param response reInvite SIP response
     * @param int requestType  service context of reInvite 
     */
    public void handleReInvite407ProxyAuthent(SipResponse response, int serviceContext){	
    }
 
    public String buildReInviteSdpResponse(SipRequest ReInvite, int serviceContext){
    	return null;
    }
 
	/**
	 * Verify if session is initiated by remote part
	 * 
	 * @return true if session is initiated by remote part
	 */
    abstract public boolean isInitiatedByRemote();
    
	/**
	 * Handle 180 Ringing
	 *
	 * @param response
	 */
	public void handle180Ringing(SipResponse response) {
	}
}
