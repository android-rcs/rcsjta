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

package com.gsma.rcs.core.ims.service;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

/**
 * IMS service session
 * 
 * @author jexa7410
 */
public abstract class ImsServiceSession extends Thread {
    /**
     * Session invitation status
     */
    public enum InvitationStatus {

        INVITATION_NOT_ANSWERED, INVITATION_ACCEPTED, INVITATION_REJECTED, INVITATION_CANCELED, INVITATION_TIMEOUT, INVITATION_REJECTED_BY_SYSTEM, INVITATION_DELETED, INVITATION_REJECTED_DECLINE, INVITATION_REJECTED_BUSY_HERE, INVITATION_REJECTED_FORBIDDEN;
    }

    /**
     * Session termination reason
     */
    public enum TerminationReason {
        TERMINATION_BY_SYSTEM, TERMINATION_BY_USER, TERMINATION_BY_TIMEOUT, TERMINATION_BY_INACTIVITY, TERMINATION_BY_CONNECTION_LOST, TERMINATION_BY_LOW_BATTERY, TERMINATION_BY_REMOTE;
    }

    private final static int SESSION_INTERVAL_TOO_SMALL = 422;

    private final ImsService mImsService;

    private String mSessionId = SessionIdGenerator.getNewId();

    /**
     * Remote contactId
     */
    private ContactId mContact;

    /**
     * Remote contactUri
     */
    private Uri mRemoteId;

    private String mRemoteDisplayName;

    private SipDialogPath mDialogPath;

    /**
     * Authentication agent
     */
    private SessionAuthenticationAgent mAuthenticationAgent;

    /**
     * Session invitation status
     */
    protected InvitationStatus mInvitationStatus = InvitationStatus.INVITATION_NOT_ANSWERED;

    /**
     * Wait user answer for session invitation
     */
    protected Object mWaitUserAnswer = new Object();

    private List<ImsSessionListener> mListeners = new ArrayList<ImsSessionListener>();

    /**
     * Session timer manager
     */
    private SessionTimerManager mSessionTimer = new SessionTimerManager(this);

    /**
     * Update session manager
     */
    protected UpdateSessionManager mUpdateMgr;

    /**
     * Ringing period (in milliseconds)
     */
    private final long mRingingPeriod;

    /**
     * Session interrupted flag
     */
    private boolean mSessionInterrupted = false;

    /**
     * Session terminated by remote flag
     */
    private boolean mSessionTerminatedByRemote = false;

    /**
     * Session accepting flag
     */
    private boolean mSessionAccepted = false;

    protected final RcsSettings mRcsSettings;

    protected final ContactManager mContactManager;

    /**
     * Session timestamp
     */
    private long mTimestamp;

    private static final Logger sLogger = Logger.getLogger(ImsServiceSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param imsService IMS service
     * @param contact Remote contact Identifier
     * @param remoteContact Remote contact URI
     * @param rcsSettings RCS settings accessor
     * @param timestamp Local timestamp for the session
     * @param contactManager Contact manager accessor
     */
    public ImsServiceSession(ImsService imsService, ContactId contact, Uri remoteId,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager) {
        mImsService = imsService;
        mContact = contact;
        mRemoteId = remoteId;
        mAuthenticationAgent = new SessionAuthenticationAgent(imsService.getImsModule());
        mUpdateMgr = new UpdateSessionManager(this, rcsSettings);
        mContactManager = contactManager;
        mRcsSettings = rcsSettings;
        mRingingPeriod = mRcsSettings.getRingingPeriod();
        mTimestamp = timestamp;
    }

    /**
     * Create originating dialog path
     */
    public void createOriginatingDialogPath() {
        // Set Call-Id
        String callId = getImsService().getImsModule().getSipManager().getSipStack()
                .generateCallId();

        // Set the route path
        Vector<String> route = getImsService().getImsModule().getSipManager().getSipStack()
                .getServiceRoutePath();

        // @FIXME: This method should take mRemoteId param as an URI instead of String
        // Create a dialog path
        mDialogPath = new SipDialogPath(getImsService().getImsModule().getSipManager()
                .getSipStack(), callId, 1, mRemoteId.toString(), ImsModule.getImsUserProfile()
                .getPublicAddress(), mRemoteId.toString(), route, mRcsSettings);

        // Set the authentication agent in the dialog path
        mDialogPath.setAuthenticationAgent(getAuthenticationAgent());

        if (mContact != null) {
            mRemoteDisplayName = mContactManager.getContactDisplayName(mContact);
        }
    }

    /**
     * Create terminating dialog path
     * 
     * @param invite Incoming invite
     */
    public void createTerminatingDialogPath(SipRequest invite) {
        String callId = invite.getCallId();
        String target = invite.getContactURI();
        String localParty = invite.getTo();
        String remoteParty = invite.getFrom();
        long cseq = invite.getCSeq();
        Vector<String> route = SipUtils.routeProcessing(invite, false);

        mDialogPath = new SipDialogPath(getImsService().getImsModule().getSipManager()
                .getSipStack(), callId, cseq, target, localParty, remoteParty, route, mRcsSettings);
        mDialogPath.setInvite(invite);
        mDialogPath.setRemoteTag(invite.getFromTag());
        mDialogPath.setRemoteContent(invite.getContent());
        mDialogPath.setSessionExpireTime(invite.getSessionTimerExpire());
        if (remoteParty != null) {
            mRemoteDisplayName = SipUtils.getDisplayNameFromUri(remoteParty);
        }
    }

    /**
     * Add a listener for receiving events
     * 
     * @param listener Listener
     */
    public void addListener(ImsSessionListener listener) {
        mListeners.add(listener);
    }

    /**
     * Remove a listener
     * 
     * @param listener Listener to remove
     */
    public void removeListener(ImsSessionListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Remove all listeners
     */
    public void removeListeners() {
        mListeners.clear();
    }

    /**
     * Returns the event listeners
     * 
     * @return Listeners
     */
    public List<ImsSessionListener> getListeners() {
        return mListeners;
    }

    /**
     * Get the session timer manager
     * 
     * @return Session timer manager
     */
    public SessionTimerManager getSessionTimerManager() {
        return mSessionTimer;
    }

    /**
     * Get the update session manager
     * 
     * @return UpdateSessionManager
     */
    public UpdateSessionManager getUpdateSessionManager() {
        return mUpdateMgr;
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
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public abstract void startSession() throws PayloadException, NetworkException;

    /**
     * Removes the session
     */
    public abstract void removeSession();

    /**
     * Return the IMS service
     * 
     * @return IMS service
     */
    public ImsService getImsService() {
        return mImsService;
    }

    /**
     * Returns the timestamp of the session
     * 
     * @return timestamp
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Return the session ID
     * 
     * @return Session ID
     */
    public String getSessionID() {
        return mSessionId;
    }

    /**
     * Set the session ID
     * 
     * @param sessionId <p>
     *            <b>Be Careful:</b><br />
     *            Should only be called to resume session (like for FT HTTP).
     *            </p>
     */
    public void setSessionID(String sessionId) {
        mSessionId = sessionId;
    }

    /**
     * Returns the remote contactId
     * 
     * @return ContactId
     */
    public ContactId getRemoteContact() {
        return mContact;
    }

    /**
     * Returns display name of the remote contact
     * 
     * @return String
     */
    public String getRemoteDisplayName() {
        return mRemoteDisplayName;
    }

    /**
     * Set display name of the remote contact
     * 
     * @param remoteDisplayName The remote display name
     */
    public void setRemoteDisplayName(String remoteDisplayName) {
        mRemoteDisplayName = remoteDisplayName;
    }

    /**
     * Get the dialog path of the session
     * 
     * @return Dialog path object
     */
    public SipDialogPath getDialogPath() {
        return mDialogPath;
    }

    /**
     * Returns the authentication agent
     * 
     * @return Authentication agent
     */
    public SessionAuthenticationAgent getAuthenticationAgent() {
        return mAuthenticationAgent;
    }

    /**
     * Reject the session invitation
     * 
     * @param status InvitationStatus
     */
    public void rejectSession(InvitationStatus status) {
        if (sLogger.isActivated()) {
            sLogger.debug("Session invitation has been rejected");
        }
        mInvitationStatus = status;

        synchronized (mWaitUserAnswer) {
            mWaitUserAnswer.notifyAll();
        }
    }

    /**
     * Accept the session invitation
     */
    public void acceptSession() {
        if (sLogger.isActivated()) {
            sLogger.debug("Session invitation has been accepted");
        }
        mInvitationStatus = InvitationStatus.INVITATION_ACCEPTED;

        synchronized (mWaitUserAnswer) {
            mWaitUserAnswer.notifyAll();
        }
    }

    /**
     * Wait session invitation answer
     * 
     * @param timeout value
     * @return Answer
     */
    public InvitationStatus waitInvitationAnswer(long timeout) {
        if (sLogger.isActivated()) {
            sLogger.debug("Wait session invitation answer delay=".concat(Long.toString(timeout)));
        }
        /* Wait until received response or received timeout */
        try {
            long timeToWait = (timeout > 0) ? timeout : mRingingPeriod;
            long startTime = System.currentTimeMillis();
            while (InvitationStatus.INVITATION_NOT_ANSWERED == mInvitationStatus) {
                synchronized (mWaitUserAnswer) {
                    mWaitUserAnswer.wait(timeToWait);
                    long waitedTime = System.currentTimeMillis() - startTime;
                    if (waitedTime < timeToWait) {
                        timeToWait -= waitedTime;
                    } else if (InvitationStatus.INVITATION_NOT_ANSWERED == mInvitationStatus) {
                        mInvitationStatus = InvitationStatus.INVITATION_TIMEOUT;
                    }
                }
            }
            return mInvitationStatus;

        } catch (InterruptedException e) {
            mSessionInterrupted = true;
            if (InvitationStatus.INVITATION_DELETED == mInvitationStatus) {
                return InvitationStatus.INVITATION_DELETED;
            }
            return InvitationStatus.INVITATION_REJECTED_BY_SYSTEM;
        }
    }

    /**
     * Wait session invitation answer
     * 
     * @return Answer
     */
    public InvitationStatus waitInvitationAnswer() {
        return waitInvitationAnswer(mRingingPeriod);
    }

    /**
     * Interrupt session
     */
    public void interruptSession() {
        if (sLogger.isActivated()) {
            sLogger.debug("Interrupt the session");
        }
        synchronized (mWaitUserAnswer) {
            mWaitUserAnswer.notifyAll();
        }

        if (!isSessionInterrupted()) {
            mSessionInterrupted = true;
            interrupt();
            if (sLogger.isActivated()) {
                sLogger.debug("Session has been interrupted");
            }
        }
    }

    /**
     * This function is used when session needs to terminated in both invitation pending and started
     * state. If an error has occurred and the session needs to closed, please call
     * closeSession(TerminationReason).
     * 
     * @param reason Termination reason
     * @throws NetworkException
     * @throws PayloadException
     */
    public void terminateSession(TerminationReason reason) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the session ".concat(reason.toString()));
        }
        boolean wasEstablished;
        if (isInitiatedByRemote()) {
            wasEstablished = mDialogPath != null && isSessionAccepted();
        } else {
            wasEstablished = mDialogPath != null && mDialogPath.isSigEstablished();
        }

        interruptSession();

        closeSession(reason);

        closeMediaSession();

        removeSession();

        Collection<ImsSessionListener> listeners = getListeners();
        if (wasEstablished) {
            for (ImsSessionListener listener : listeners) {
                listener.onSessionAborted(mContact, reason);
            }
            return;
        }
        for (ImsSessionListener listener : listeners) {
            listener.onSessionRejected(mContact, reason);
        }
    }

    /**
     * Force terminate and remove the session
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void deleteSession() throws PayloadException, NetworkException {
        mInvitationStatus = InvitationStatus.INVITATION_DELETED;
        interruptSession();
        closeSession(TerminationReason.TERMINATION_BY_USER);
        closeMediaSession();
        removeSession();
    }

    /**
     * This function is called when an error has occurred in the session and the session needs to be
     * closed. If you need to actively terminate the session, please call
     * terminateSession(TerminationReason).
     * 
     * @param reason Reason
     * @throws NetworkException
     * @throws PayloadException
     */
    public void closeSession(TerminationReason reason) throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Close the session (reason ").append(reason)
                    .append(")").toString());
        }
        if ((mDialogPath == null) || mDialogPath.isSessionTerminated()) {
            return;
        }
        mSessionTimer.stop();
        if (TerminationReason.TERMINATION_BY_USER == reason) {
            mDialogPath.setSessionTerminated(Response.OK, "Call completed");
        } else {
            mDialogPath.setSessionTerminated();
        }
        /* Unblock semaphore (used for terminating side only) */
        synchronized (mWaitUserAnswer) {
            mWaitUserAnswer.notifyAll();
        }
        if (TerminationReason.TERMINATION_BY_CONNECTION_LOST == reason) {
            /*
             * Since connection is lost , we should not even try to send a BYE or CANCEL as we will
             * anyway fail , Hence return from this point.
             */
            return;
        }
        /* Close the session */
        if (mDialogPath.isSigEstablished()) {
            getDialogPath().incrementCseq();

            // @FIXME:Inject SipManager instead
            /* Send BYE without waiting a response */
            getImsService().getImsModule().getSipManager().sendSipBye(getDialogPath());
        } else {
            /* Send CANCEL without waiting a response */
            getImsService().getImsModule().getSipManager().sendSipCancel(getDialogPath());
        }
        if (sLogger.isActivated()) {
            sLogger.debug("SIP session has been closed");
        }
    }

    /**
     * Receive BYE request
     * 
     * @param bye BYE request
     * @throws NetworkException
     * @throws PayloadException
     */
    public void receiveBye(SipRequest bye) throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Receive a BYE message from the remote");
        }
        closeMediaSession();

        getDialogPath().setSessionTerminated();

        mSessionTerminatedByRemote = true;

        removeSession();

        mSessionTimer.stop();
    }

    /**
     * Receive CANCEL request
     * 
     * @param cancel CANCEL request
     * @throws PayloadException
     * @throws NetworkException
     */
    public void receiveCancel(SipRequest cancel) throws NetworkException, PayloadException {
        if (sLogger.isActivated()) {
            sLogger.info("Receive a CANCEL message from the remote");
        }
        if (getDialogPath().isSigEstablished()) {
            if (sLogger.isActivated()) {
                sLogger.info("Ignore the received CANCEL message from the remote (session already established)");
            }
            return;
        }
        closeMediaSession();
        getDialogPath().setSessionCancelled();
        if (sLogger.isActivated()) {
            sLogger.info("Send 487 Request terminated");
        }
        getImsService()
                .getImsModule()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(getDialogPath().getInvite(),
                                getDialogPath().getLocalTag(), Response.REQUEST_TERMINATED));
        removeSession();
        mInvitationStatus = InvitationStatus.INVITATION_CANCELED;
        synchronized (mWaitUserAnswer) {
            mWaitUserAnswer.notifyAll();
        }
    }

    /**
     * Receive re-INVITE request
     * 
     * @param reInvite re-INVITE request
     * @throws NetworkException
     * @throws PayloadException
     */
    public void receiveReInvite(SipRequest reInvite) throws PayloadException, NetworkException {
        // Session refresh management
        mSessionTimer.receiveReInvite(reInvite);
    }

    /**
     * Receive UPDATE request
     * 
     * @param update UPDATE request
     * @throws PayloadException
     * @throws NetworkException
     */
    public void receiveUpdate(SipRequest update) throws NetworkException, PayloadException {
        mSessionTimer.receiveUpdate(update);
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
     * @throws NetworkException
     * @throws PayloadException
     */
    public abstract void prepareMediaSession() throws PayloadException, NetworkException;

    /**
     * Open media session
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public abstract void openMediaSession() throws PayloadException, NetworkException;

    /**
     * Start media transfer
     * 
     * @throws NetworkException
     * @throws PayloadException
     * @throws FileAccessException
     */
    public abstract void startMediaTransfer() throws PayloadException, NetworkException,
            FileAccessException;

    /**
     * Close media session
     */
    public abstract void closeMediaSession();

    /**
     * Send a 180 Ringing response to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     * @throws PayloadException
     * @throws NetworkException
     */
    public void send180Ringing(SipRequest request, String localTag) throws NetworkException,
            PayloadException {
        getImsService()
                .getImsModule()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(request, localTag, Response.RINGING));
    }

    /**
     * Send an error response to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     * @param status InvitationStatus
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendErrorResponse(SipRequest request, String localTag, InvitationStatus status)
            throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Send ").append(status).append(" error response")
                    .toString());
        }
        SipResponse resp;
        switch (status) {
            case INVITATION_REJECTED_BUSY_HERE:
                resp = SipMessageFactory.createResponse(request, localTag, Response.BUSY_HERE);
                break;
            case INVITATION_REJECTED_DECLINE:
                resp = SipMessageFactory.createResponse(request, localTag, Response.DECLINE);
                break;
            case INVITATION_REJECTED_FORBIDDEN:
                resp = SipMessageFactory.createResponse(request, localTag, Response.FORBIDDEN);
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder("Unknown InvitationStatus ")
                        .append(status).toString());
        }
        getImsService().getImsModule().getSipManager().sendSipResponse(resp);
    }

    /**
     * Send a 603 "Decline" to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     * @throws PayloadException
     * @throws NetworkException
     */
    public void send603Decline(SipRequest request, String localTag) throws NetworkException,
            PayloadException {
        if (sLogger.isActivated()) {
            sLogger.info("Send 603 Decline");
        }
        getImsService()
                .getImsModule()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(request, localTag, Response.DECLINE));
    }

    /**
     * Send a 486 "Busy" to the remote party
     * 
     * @param request SIP request
     * @param localTag Local tag
     * @throws PayloadException
     * @throws NetworkException
     */
    public void send486Busy(SipRequest request, String localTag) throws NetworkException,
            PayloadException {
        if (sLogger.isActivated()) {
            sLogger.info("Send 486 Busy");
        }
        getImsService()
                .getImsModule()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(request, localTag, Response.BUSY_HERE));
    }

    /**
     * Send a 415 "Unsupported Media Type" to the remote party
     * 
     * @param request SIP request
     * @throws PayloadException
     * @throws NetworkException
     */
    public void send415Error(SipRequest request) throws NetworkException, PayloadException {
        if (sLogger.isActivated()) {
            sLogger.info("Send 415 Unsupported Media Type");
        }
        // TODO: set Accept-Encoding header
        getImsService()
                .getImsModule()
                .getSipManager()
                .sendSipResponse(
                        SipMessageFactory.createResponse(request, Response.UNSUPPORTED_MEDIA_TYPE));
    }

    /**
     * Create SDP setup offer (see RFC6135, RFC4145)
     * 
     * @return Setup offer
     */
    public String createSetupOffer() {
        if (isBehindNat()) {
            /* Active mode by default if there is a NAT */
            return "active";
        }
        /*
         * Active/passive mode is exchanged in order to be compatible with UE not supporting
         * COMEDIA.
         */
        return "actpass";
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
        } else if (offer.equals("active")) {
            // Passive mode
            return "passive";
        } else if (offer.equals("passive")) {
            // Active mode
            return "active";
        } else {
            // Passive mode by default
            return "passive";
        }
    }

    /**
     * Returns the response timeout (in milliseconds)
     * 
     * @return Timeout
     */
    public long getResponseTimeout() {
        return mRingingPeriod + SipManager.getTimeout();
    }

    /**
     * Is session interrupted
     * 
     * @return Boolean
     */
    public boolean isSessionInterrupted() {
        return mSessionInterrupted || isInterrupted()
                || (getDialogPath() != null && getDialogPath().isSessionTerminated());
    }

    /**
     * Is session terminated by remote
     * 
     * @return Boolean
     */
    public boolean isSessionTerminatedByRemote() {
        return mSessionTerminatedByRemote;
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
     * @throws PayloadException
     */
    public abstract SipRequest createInvite() throws PayloadException;

    /**
     * Send INVITE message
     * 
     * @param invite SIP INVITE
     * @throws PayloadException
     * @throws NetworkException
     * @throws FileAccessException
     */
    public void sendInvite(SipRequest invite) throws PayloadException, NetworkException,
            FileAccessException {
        // Send INVITE request
        SipTransactionContext ctx = getImsService()
                .getImsModule()
                .getSipManager()
                .sendSipMessageAndWait(invite, getResponseTimeout(),
                        new SipTransactionContext.INotifySipProvisionalResponse() {
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
                case Response.FORBIDDEN:
                    // 403 Forbidden
                    handle403Forbidden(ctx.getSipResponse());
                    break;
                default:
                    // Other error response
                    handleDefaultError(ctx.getSipResponse());
                    break;
            }
        } else {
            // No response received: timeout
            handleError(new ImsSessionBasedServiceError(
                    ImsSessionBasedServiceError.SESSION_INITIATION_FAILED, "timeout"));
        }
    }

    /**
     * Perform next steps once 200 0K response is received
     * 
     * @param resp 200 OK response
     * @throws PayloadException
     * @throws NetworkException
     * @throws FileAccessException
     */
    public void handle200OK(SipResponse resp) throws PayloadException, NetworkException,
            FileAccessException {
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }
        getDialogPath().setSigEstablished();

        getDialogPath().setRemoteTag(resp.getToTag());

        getDialogPath().setTarget(resp.getContactURI());

        /* Set the route path with the Record-Route header */
        Vector<String> newRoute = SipUtils.routeProcessing(resp, true);
        getDialogPath().setRoute(newRoute);

        /* Set the remote SDP part */
        getDialogPath().setRemoteContent(resp.getContent());

        /* Set the remote SIP instance ID */
        ContactHeader remoteContactHeader = (ContactHeader) resp.getHeader(ContactHeader.NAME);
        if (remoteContactHeader != null) {
            getDialogPath().setRemoteSipInstance(
                    remoteContactHeader.getParameter(SipUtils.SIP_INSTANCE_PARAM));
        }

        prepareMediaSession();

        if (sLogger.isActivated()) {
            sLogger.info("Send ACK");
        }
        getImsService().getImsModule().getSipManager().sendSipAck(getDialogPath());

        getDialogPath().setSessionEstablished();

        openMediaSession();

        for (ImsSessionListener listener : getListeners()) {
            listener.onSessionStarted(mContact);
        }

        /* Start session timer */
        if (mSessionTimer.isSessionTimerActivated(resp)) {
            mSessionTimer.start(resp.getSessionTimerRefresher(), resp.getSessionTimerExpire());
        }
        startMediaTransfer();
    }

    /**
     * Session inactivity event
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public abstract void handleInactivityEvent() throws PayloadException, NetworkException;

    /**
     * Handle default error
     * 
     * @param resp Error response
     */
    public void handleDefaultError(SipResponse resp) {
        // Default handle
        handleError(new ImsSessionBasedServiceError(
                ImsSessionBasedServiceError.SESSION_INITIATION_FAILED, resp.getStatusCode() + " "
                        + resp.getReasonPhrase()));
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
     * @throws NetworkException
     * @throws PayloadException
     * @throws FileAccessException
     */
    public void handle407Authentication(SipResponse resp) throws PayloadException,
            NetworkException, FileAccessException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("407 response received");
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
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);
        }

    }

    /**
     * Handle 422 response
     * 
     * @param resp 422 response
     * @throws NetworkException
     * @throws PayloadException
     * @throws FileAccessException
     */
    public void handle422SessionTooSmall(SipResponse resp) throws PayloadException,
            NetworkException, FileAccessException {
        try {
            // 422 response received
            if (sLogger.isActivated()) {
                sLogger.info("422 response received");
            }

            // Extract the Min-SE value
            long minExpire = SipUtils.getMinSessionExpirePeriod(resp);
            if (minExpire == -1) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't read the Min-SE value");
                }
                handleError(new ImsSessionBasedServiceError(
                        ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, "No Min-SE value found"));
                return;
            }

            // Set the min expire value
            SipDialogPath sipDialogPath = getDialogPath();
            sipDialogPath.setMinSessionExpireTime(minExpire);

            // Set the expire value
            sipDialogPath.setSessionExpireTime(minExpire);

            // Increment the Cseq number of the dialog path
            sipDialogPath.incrementCseq();

            // Create a new INVITE with the right expire period
            if (sLogger.isActivated()) {
                sLogger.info("Send new INVITE");
            }
            SipRequest invite = createInvite();

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Reset initial request in the dialog path
            sipDialogPath.setInvite(invite);

            // Send INVITE request
            sendInvite(invite);
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Unable to handle session too small response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Unable to handle session too small response!", e);
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
        handleError(new ImsSessionBasedServiceError(
                ImsSessionBasedServiceError.SESSION_INITIATION_CANCELLED, resp.getStatusCode()
                        + " " + resp.getReasonPhrase()));
    }

    /**
     * Handle 603 Decline
     * 
     * @param resp 603 response
     */
    public void handle603Declined(SipResponse resp) {
        handleError(new ImsSessionBasedServiceError(
                ImsSessionBasedServiceError.SESSION_INITIATION_DECLINED, resp.getStatusCode() + " "
                        + resp.getReasonPhrase()));
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
     * @param code InvitationStatus
     * @param requestType
     */
    public void handleReInviteResponse(InvitationStatus code, SipResponse response, int requestType) {
    }

    /**
     * Handle User Answer in Response to Session Update notification
     * 
     * @param code InvitationStatus
     * @param requestType reInvite SIP request
     */
    public void handleReInviteUserAnswer(InvitationStatus code, int requestType) {
    }

    /**
     * Handle ACK sent in Response to 200Ok ReInvite
     * 
     * @param code InvitationStatus
     * @param requestType reInvite SIP request
     */
    public void handleReInviteAck(InvitationStatus code, int requestType) {
    }

    /**
     * Handle 407 Proxy Authent error ReInvite Response
     * 
     * @param response reInvite SIP response
     * @param serviceContext context of reInvite
     * @throws NetworkException
     * @throws PayloadException
     */
    public void handleReInvite407ProxyAuthent(SipResponse response, int serviceContext)
            throws PayloadException, NetworkException {
    }

    /**
     * @param ReInvite
     * @param serviceContext
     * @return SDP built
     * @throws PayloadException
     * @throws NetworkException
     */
    public String buildReInviteSdpResponse(SipRequest ReInvite, int serviceContext)
            throws PayloadException, NetworkException {
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
