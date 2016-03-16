/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.text.ParseException;

import javax2.sip.Dialog;
import javax2.sip.InvalidArgumentException;
import javax2.sip.message.Response;

/**
 * Update session manager
 * 
 * @author owom5460
 */
public class UpdateSessionManager {

    /**
     * Session to be renegociated
     */
    private ImsServiceSession mSession;

    /**
     * Re-Invite invitation status
     */
    private InvitationStatus mReInviteStatus = InvitationStatus.INVITATION_NOT_ANSWERED;

    /**
     * Wait user answer for reInvite invitation
     */
    private Object mWaitUserAnswer = new Object();

    /**
     * Ringing period (in milliseconds)
     */
    private final long mRingingPeriod;

    private static final Logger sLogger = Logger.getLogger(UpdateSessionManager.class.getName());

    /**
     * Constructor
     * 
     * @param session Session to be refreshed
     * @param rcsSettings
     */
    public UpdateSessionManager(ImsServiceSession session, RcsSettings rcsSettings) {
        mSession = session;
        mRingingPeriod = rcsSettings.getRingingPeriod();
    }

    /**
     * Create ReInvite
     * 
     * @param featureTags featureTags to set in reInvite
     * @param content reInvite content
     * @return reInvite request
     * @throws PayloadException
     */
    public SipRequest createReInvite(String[] featureTags, String content)
            throws PayloadException {
        if (sLogger.isActivated()) {
            sLogger.debug("createReInvite()");
        }
        try {
            mSession.getDialogPath().incrementCseq();
            if (sLogger.isActivated()) {
                sLogger.info("Increment DialogPath CSeq - DialogPath CSeq =".concat(String
                        .valueOf(mSession.getDialogPath().getCseq())));
            }
            /* Increment internal stack CSeq (NIST stack issue?) */
            Dialog dlg = mSession.getDialogPath().getStackDialog();
            while ((dlg != null) && (dlg.getLocalSeqNumber() < mSession.getDialogPath().getCseq())) {
                dlg.incrementLocalSequenceNumber();
                if (sLogger.isActivated()) {
                    sLogger.info("Increment LocalSequenceNumber -  Dialog local Seq Number ="
                            .concat(String.valueOf(dlg.getLocalSeqNumber())));
                }
            }
            SipRequest reInvite = SipMessageFactory.createReInvite(mSession.getDialogPath(),
                    featureTags, content);
            if (sLogger.isActivated()) {
                sLogger.info("reInvite created -  reInvite CSeq =".concat(String.valueOf(reInvite
                        .getCSeq())));
            }
            mSession.getAuthenticationAgent().setAuthorizationHeader(reInvite);
            mSession.getAuthenticationAgent().setProxyAuthorizationHeader(reInvite);
            return reInvite;

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Unable to create re-invite request!", e);

        } catch (ParseException e) {
            throw new PayloadException("Unable to create re-invite request!", e);
        }

    }

    /**
     * Send ReInvite
     * 
     * @param request ReInvite request
     * @param serviceContext service context of ReInvite
     * @throws NetworkException
     * @throws PayloadException
     */
    public void sendReInvite(SipRequest request, int serviceContext) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("sendReInvite()");
        }
        SipTransactionContext ctx = mSession.getImsService().getImsModule().getSipManager()
                .sendSipMessageAndWait(request, mSession.getResponseTimeout());

        if (ctx.isSipResponse()) {
            switch (ctx.getStatusCode()) {
                case Response.OK:
                    mSession.getDialogPath().setRemoteContent(ctx.getSipResponse().getSdpContent());
                    mSession.handleReInviteResponse(InvitationStatus.INVITATION_ACCEPTED,
                            ctx.getSipResponse(), serviceContext);

                    mSession.getImsService().getImsModule().getSipManager()
                            .sendSipAck(mSession.getDialogPath());
                    return;

                case Response.DECLINE:
                    mSession.handleReInviteResponse(InvitationStatus.INVITATION_REJECTED,
                            ctx.getSipResponse(), serviceContext);
                    return;

                case Response.REQUEST_TIMEOUT:
                    mSession.handleReInviteResponse(InvitationStatus.INVITATION_TIMEOUT,
                            ctx.getSipResponse(), serviceContext);
                    return;

                case Response.PROXY_AUTHENTICATION_REQUIRED:
                    mSession.handleReInvite407ProxyAuthent(ctx.getSipResponse(), serviceContext);
                    return;

                default:
                    mSession.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.SESSION_INITIATION_FAILED));
                    return;
            }
        }
        mSession.handleReInviteResponse(InvitationStatus.INVITATION_NOT_ANSWERED,
                ctx.getSipResponse(), serviceContext);
    }

    /**
     * Receive RE-INVITE request
     * 
     * @param request RE-INVITE request
     * @param featureTags featureTags to set in request
     * @param sdpResponse
     * @param serviceContext service context of reInvite request
     * @throws PayloadException
     * @throws NetworkException
     */
    public void send200OkReInviteResp(SipRequest request, String[] featureTags, String sdpResponse,
            int serviceContext) throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("receiveReInvite()");
        }
        SipResponse resp = SipMessageFactory.create200OkReInviteResponse(mSession.getDialogPath(),
                request, featureTags, sdpResponse);
        SipTransactionContext ctx = mSession.getImsService().getImsModule().getSipManager()
                .sendSipMessageAndWait(resp);
        if (ctx.isSipAck()) {
            if (sLogger.isActivated()) {
                sLogger.info("ACK request received");
            }
            mSession.handleReInviteAck(InvitationStatus.INVITATION_ACCEPTED, serviceContext);
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("No ACK received for ReINVITE");
            }
            mSession.handleError(new ImsSessionBasedServiceError(
                    ImsSessionBasedServiceError.SEND_RESPONSE_FAILED));
        }
    }

    /**
     * Receive RE-INVITE request
     * 
     * @param request RE-INVITE request
     * @param featureTags featureTags to set in request
     * @param serviceContext service context of reInvite request
     * @throws PayloadException
     * @throws NetworkException
     */
    public void waitUserAckAndSendReInviteResp(SipRequest request, String[] featureTags,
            int serviceContext) throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("waitUserAckAndSendReInviteResp()");
        }
        mReInviteStatus = InvitationStatus.INVITATION_NOT_ANSWERED;
        InvitationStatus answer = waitInvitationAnswer();

        switch (answer) {
            case INVITATION_REJECTED:
                if (sLogger.isActivated()) {
                    sLogger.debug("reInvite has been rejected by user");
                }
                mSession.sendErrorResponse(request, mSession.getDialogPath().getLocalTag(),
                        InvitationStatus.INVITATION_REJECTED_DECLINE);
                mSession.handleReInviteUserAnswer(InvitationStatus.INVITATION_REJECTED,
                        serviceContext);
                break;

            case INVITATION_NOT_ANSWERED:
                if (sLogger.isActivated()) {
                    sLogger.debug("Session has been rejected on timeout");
                }
                mSession.sendErrorResponse(request, mSession.getDialogPath().getLocalTag(),
                        InvitationStatus.INVITATION_REJECTED_DECLINE);
                mSession.handleReInviteUserAnswer(InvitationStatus.INVITATION_NOT_ANSWERED,
                        serviceContext);
                break;

            case INVITATION_ACCEPTED:
                if (sLogger.isActivated()) {
                    sLogger.debug("Send 200 OK");
                }
                String sdp = mSession.buildReInviteSdpResponse(request, serviceContext);
                if (sdp == null) {
                    mSession.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.SEND_RESPONSE_FAILED));
                    return;
                }
                mSession.getDialogPath().setLocalContent(sdp);

                mSession.handleReInviteUserAnswer(InvitationStatus.INVITATION_ACCEPTED,
                        serviceContext);
                SipResponse resp = SipMessageFactory.create200OkReInviteResponse(
                        mSession.getDialogPath(), request, featureTags, sdp);
                SipTransactionContext ctx = mSession.getImsService().getImsModule().getSipManager()
                        .sendSipMessageAndWait(resp);
                if (ctx.isSipAck()) {
                    if (sLogger.isActivated()) {
                        sLogger.info("ACK request received");
                        sLogger.info("ACK status code = " + ctx.getStatusCode());
                    }
                    mSession.handleReInviteAck(InvitationStatus.INVITATION_ACCEPTED, serviceContext);
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("No ACK received for INVITE");
                    }
                    mSession.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.SEND_RESPONSE_FAILED));
                }
                break;
            default:
                mSession.handleError(new ImsSessionBasedServiceError(
                        ImsSessionBasedServiceError.SEND_RESPONSE_FAILED));
                break;
        }
    }

    /**
     * Reject the session invitation
     * 
     * @param code Error code
     */
    public void rejectReInvite(int code) {
        if (sLogger.isActivated()) {
            sLogger.debug("ReInvite  has been rejected");
        }

        synchronized (mWaitUserAnswer) {
            mReInviteStatus = InvitationStatus.INVITATION_REJECTED;

            // Unblock semaphore
            mWaitUserAnswer.notifyAll();
        }

        // Decline the invitation
        // session.sendErrorResponse(session.getDialogPath().getInvite(),
        // session.getDialogPath().getLocalTag(), code);
    }

    /**
     * Accept the session invitation
     */
    public void acceptReInvite() {
        if (sLogger.isActivated()) {
            sLogger.debug("ReInvite has been accepted");
        }

        synchronized (mWaitUserAnswer) {
            mReInviteStatus = InvitationStatus.INVITATION_ACCEPTED;

            // Unblock semaphore
            mWaitUserAnswer.notifyAll();
        }
    }

    /**
     * Wait session invitation answer
     * 
     * @return Answer
     */
    public InvitationStatus waitInvitationAnswer() {
        if (InvitationStatus.INVITATION_NOT_ANSWERED != mReInviteStatus) {
            return mReInviteStatus;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Wait session invitation answer");
        }

        try {
            synchronized (mWaitUserAnswer) {
                // Wait until received response or received timeout
                mWaitUserAnswer.wait(mRingingPeriod / 2);
            }
        } catch (InterruptedException e) {
            /* Nothing to be handled here */
            sLogger.warn("Wait for timeout has been interrupted!", e);
        }

        return mReInviteStatus;
    }

}
