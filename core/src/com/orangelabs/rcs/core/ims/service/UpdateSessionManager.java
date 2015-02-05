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

import javax2.sip.Dialog;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallError;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Update session manager
 * 
 * @author owom5460
 */
public class UpdateSessionManager {

    /**
     * Session to be renegociated
     */
    private ImsServiceSession session;

    /**
     * Re-Invite invitation status
     */
    private int reInviteStatus = ImsServiceSession.INVITATION_NOT_ANSWERED;

    /**
     * Wait user answer for reInvite invitation
     */
    private Object waitUserAnswer = new Object();

    /**
     * Ringing period (in seconds)
     */
    private int ringingPeriod = RcsSettings.getInstance().getRingingPeriod();

    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param session Session to be refreshed
     */
    public UpdateSessionManager(ImsServiceSession mysession) {
        this.session = mysession;
    }

    /**
     * Create ReInvite
     * 
     * @param featureTags featureTags to set in reInvite
     * @param content reInvite content
     * @return reInvite request
     */
    public SipRequest createReInvite(String[] featureTags, String content) {
        if (logger.isActivated()) {
            logger.debug("createReInvite()");
        }

        SipRequest reInvite = null;

        try {
            // Increment the Cseq number of the dialog path
            session.getDialogPath().incrementCseq();
            if (logger.isActivated()) {
                logger.info("Increment DialogPath CSeq - DialogPath CSeq ="
                        + session.getDialogPath().getCseq());
            }

            // Increment internal stack CSeq (NIST stack issue?)
            Dialog dlg = session.getDialogPath().getStackDialog();
            while ((dlg != null) && (dlg.getLocalSeqNumber() < session.getDialogPath().getCseq())) {
                dlg.incrementLocalSequenceNumber();
                if (logger.isActivated()) {
                    logger.info("Increment LocalSequenceNumber -  Dialog local Seq Number ="
                            + dlg.getLocalSeqNumber());
                }
            }

            // create ReInvite
            reInvite = SipMessageFactory.createReInvite(session.getDialogPath(), featureTags,
                    content);
            if (logger.isActivated()) {
                logger.info("reInvite created -  reInvite CSeq =" + reInvite.getCSeq());
            }

            // Set the Authorization header
            session.getAuthenticationAgent().setAuthorizationHeader(reInvite);

            // Set the Proxy-Authorization header
            session.getAuthenticationAgent().setProxyAuthorizationHeader(reInvite);

        } catch (SipException e) {
            // Unexpected error
            session.handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (CoreException e) {
            // Unexpected error
            session.handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }

        return reInvite;

    }

    /**
     * Send ReInvite
     * 
     * @param request ReInvite request
     * @param serviceContext service context of ReInvite
     */
    public void sendReInvite(SipRequest request, int serviceContext) {
        if (logger.isActivated()) {
            logger.debug("sendReInvite()");
        }

        final SipRequest reInvite = request;
        final int reInviteContext = serviceContext;

        Thread thread = new Thread() {
            public void run() {
                SipTransactionContext ctx;
                try {
                    // Send ReINVITE request
                    ctx = session.getImsService().getImsModule().getSipManager()
                            .sendSipMessageAndWait(reInvite, session.getResponseTimeout());

                    if (ctx.isSipResponse()) { // Analyze the received response
                        if (ctx.getStatusCode() == 200) {

                            // // set received sdp response as remote sdp content
                            session.getDialogPath().setRemoteContent(
                                    ctx.getSipResponse().getSdpContent());

                            // notify session with 200OK response
                            session.handleReInviteResponse(200, ctx.getSipResponse(),
                                    reInviteContext);

                            // send SIP ACK
                            session.getImsService().getImsModule().getSipManager()
                                    .sendSipAck(session.getDialogPath());

                        } else if (ctx.getStatusCode() == 603) {
                            // notify session with 603 response
                            session.handleReInviteResponse(ImsServiceSession.INVITATION_REJECTED,
                                    ctx.getSipResponse(), reInviteContext);
                        } else if (ctx.getStatusCode() == 408) {
                            // notify session with 408 response
                            session.handleReInviteResponse(
                                    ImsServiceSession.TERMINATION_BY_TIMEOUT, ctx.getSipResponse(),
                                    reInviteContext);
                        } else if (ctx.getStatusCode() == 407) {
                            // notify session with 407 Proxy Authent required
                            session.handleReInvite407ProxyAuthent(ctx.getSipResponse(),
                                    reInviteContext);
                        } else {
                            // Other error response => generate call error
                            session.handleError(new ImsSessionBasedServiceError(
                                    ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, ctx
                                            .getSipResponse().getStatusCode()
                                            + " "
                                            + ctx.getSipResponse().getReasonPhrase()));
                        }
                    } else {
                        // No response received: timeout => notify session
                        session.handleReInviteResponse(ImsServiceSession.TERMINATION_BY_TIMEOUT,
                                ctx.getSipResponse(), reInviteContext);
                    }
                } catch (SipException e) {
                    // Unexpected error => generate call error
                    if (logger.isActivated()) {
                        logger.error("Send ReInvite has failed", e);
                    }
                    session.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                }
            }
        };
        thread.start();

    }

    /**
     * Receive RE-INVITE request
     * 
     * @param request RE-INVITE request
     * @param featureTags featureTags to set in request
     * @param serviceContext service context of reInvite request
     */
    public void send200OkReInviteResp(SipRequest request, String[] featureTags, String sdpResponse,
            int serviceContext) {
        if (logger.isActivated()) {
            logger.debug("receiveReInvite()");
        }

        final SipRequest reInvite = request;
        final String sdp = sdpResponse;
        final int reInviteContext = serviceContext;
        final String[] respFeatureTags = featureTags;

        Thread thread = new Thread() {
            public void run() {
                try {
                    if (logger.isActivated()) {
                        logger.debug("Send 200 OK");
                    }
                    // Create 200 OK response
                    SipResponse resp = SipMessageFactory.create200OkReInviteResponse(
                            session.getDialogPath(), reInvite, respFeatureTags, sdp);
                    // Send 200 OK response
                    SipTransactionContext ctx = session.getImsService().getImsModule()
                            .getSipManager().sendSipMessageAndWait(resp);

                    // Analyze the received response
                    if (ctx.isSipAck()) {
                        // ACK received
                        if (logger.isActivated()) {
                            logger.info("ACK request received");
                        }
                        // notify local listener
                        session.handleReInviteAck(200, reInviteContext);
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("No ACK received for ReINVITE");
                        }
                        // No ACK received => generate call error for local client
                        session.handleError(new ImsSessionBasedServiceError(
                                ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                "ack not received"));
                    }
                } catch (Exception e) {
                    // Unexpected error => generate call error for local client
                    if (logger.isActivated()) {
                        logger.error("Session update refresh has failed", e);
                    }
                    session.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                }
            }
        };

        thread.start();

    }

    /**
     * Receive RE-INVITE request
     * 
     * @param request RE-INVITE request
     * @param featureTags featureTags to set in request
     * @param serviceContext service context of reInvite request
     */
    public void waitUserAckAndSendReInviteResp(SipRequest request, String[] featureTags,
            int serviceContext) {
        if (logger.isActivated()) {
            logger.debug("receiveReInviteAndWait()");
        }

        reInviteStatus = ImsServiceSession.INVITATION_NOT_ANSWERED;
        final SipRequest reInvite = request;
        final int reInviteContext = serviceContext;
        final String[] respFeatureTags = featureTags;

        Thread thread = new Thread() {
            public void run() {
                try {
                    // wait user answer
                    int answer = waitInvitationAnswer();

                    if (answer == ImsServiceSession.INVITATION_REJECTED) {
                        // Invitation declined by user
                        if (logger.isActivated()) {
                            logger.debug("reInvite has been rejected by user");
                        }

                        // send error to remote client
                        session.sendErrorResponse(reInvite, session.getDialogPath().getLocalTag(),
                                603);
                        session.handleReInviteUserAnswer(ImsServiceSession.INVITATION_REJECTED,
                                reInviteContext);
                    } else if (answer == ImsServiceSession.INVITATION_NOT_ANSWERED) {
                        if (logger.isActivated()) {
                            logger.debug("Session has been rejected on timeout");
                        }

                        // send error to remote client
                        session.sendErrorResponse(reInvite, session.getDialogPath().getLocalTag(),
                                603);
                        session.handleReInviteUserAnswer(ImsServiceSession.INVITATION_NOT_ANSWERED,
                                reInviteContext);

                    } else if (answer == ImsServiceSession.INVITATION_ACCEPTED) {
                        if (logger.isActivated()) {
                            logger.debug("Send 200 OK");
                        }

                        // build sdp response
                        String sdp = session.buildReInviteSdpResponse(reInvite, reInviteContext);
                        if (sdp == null) {
                            // sdp null - terminate session and send error
                            session.handleError(new ImsSessionBasedServiceError(
                                    ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                    "error on sdp building, sdp is null "));
                            return;
                        }

                        // set sdp response as local content
                        session.getDialogPath().setLocalContent(sdp);

                        session.handleReInviteUserAnswer(ImsServiceSession.INVITATION_ACCEPTED,
                                reInviteContext);

                        // create 200OK response
                        SipResponse resp = SipMessageFactory.create200OkReInviteResponse(
                                session.getDialogPath(), reInvite, respFeatureTags, sdp);

                        // Send response
                        SipTransactionContext ctx = session.getImsService().getImsModule()
                                .getSipManager().sendSipMessageAndWait(resp);

                        // Analyze the received response
                        if (ctx.isSipAck()) {
                            // ACK received
                            if (logger.isActivated()) {
                                logger.info("ACK request received");
                                logger.info("ACK status code = " + ctx.getStatusCode());
                            }

                            // notify local listener
                            session.handleReInviteAck(200, reInviteContext);
                        } else {
                            if (logger.isActivated()) {
                                logger.debug("No ACK received for INVITE");
                            }
                            // No ACK received: send error
                            session.handleError(new ImsSessionBasedServiceError(
                                    ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                    "ack not received"));
                        }
                    } else {
                        session.handleError(new ImsSessionBasedServiceError(
                                ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                "ack not received"));
                    }
                } catch (Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Session update refresh has failed", e);
                    }
                    // Unexpected error
                    session.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                }
            }
        };

        thread.start();
    }

    /**
     * Reject the session invitation
     * 
     * @param code Error code
     */
    public void rejectReInvite(int code) {
        if (logger.isActivated()) {
            logger.debug("ReInvite  has been rejected");
        }

        synchronized (waitUserAnswer) {
            reInviteStatus = ImsServiceSession.INVITATION_REJECTED;

            // Unblock semaphore
            waitUserAnswer.notifyAll();
        }

        // Decline the invitation
        // session.sendErrorResponse(session.getDialogPath().getInvite(),
        // session.getDialogPath().getLocalTag(), code);
    }

    /**
     * Accept the session invitation
     */
    public void acceptReInvite() {
        if (logger.isActivated()) {
            logger.debug("ReInvite has been accepted");
        }

        synchronized (waitUserAnswer) {
            reInviteStatus = ImsServiceSession.INVITATION_ACCEPTED;

            // Unblock semaphore
            waitUserAnswer.notifyAll();
        }
    }

    /**
     * Wait session invitation answer
     * 
     * @return Answer
     */
    public int waitInvitationAnswer() {
        if (reInviteStatus != ImsServiceSession.INVITATION_NOT_ANSWERED) {
            return reInviteStatus;
        }

        if (logger.isActivated()) {
            logger.debug("Wait session invitation answer");
        }

        try {
            synchronized (waitUserAnswer) {
                // Wait until received response or received timeout
                waitUserAnswer.wait(ringingPeriod * 500);
            }
        } catch (InterruptedException e) {

        }

        return reInviteStatus;
    }

}
