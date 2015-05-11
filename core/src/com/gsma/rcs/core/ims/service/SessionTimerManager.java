/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipMessage;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import javax2.sip.Dialog;

/**
 * Session timer manager (see RFC 4028)
 * 
 * @author jexa7410
 */
public class SessionTimerManager extends PeriodicRefresher {
    /**
     * Minimum value of expire period in milliseconds
     */
    public final static long MIN_EXPIRE_PERIOD = 90000;

    /**
     * UAC role
     */
    public final static String UAC_ROLE = "uac";

    /**
     * UAC role
     */
    public final static String UAS_ROLE = "uas";

    /**
     * Session to be refreshed
     */
    private ImsServiceSession mSession;

    /**
     * Expire period
     */
    private long mExpirePeriod;

    /**
     * Refresher
     */
    private String mRefresher = "uas";

    /**
     * Last session refresh time
     */
    private long mLastSessionRefresh;

    /**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param session Session to be refreshed
     */
    public SessionTimerManager(ImsServiceSession session) {
        mSession = session;
    }

    /**
     * Is session timer activated
     * 
     * @param msg SIP message
     * @return Boolean
     */
    public boolean isSessionTimerActivated(SipMessage msg) {
        // Check the Session-Expires header
        if (msg.getSessionTimerExpire() < MIN_EXPIRE_PERIOD) {
            if (mLogger.isActivated()) {
                mLogger.debug("Session timer not activated");
            }
            return false;
        }

        return true;
    }

    /**
     * Start the session timer
     * 
     * @param refresher Refresher role
     * @param expirePeriod Expire period in milliseconds
     */
    public void start(String refresher, long expirePeriod) {
        if (mLogger.isActivated()) {
            mLogger.debug(new StringBuilder("Start session timer for session ")
                    .append(mSession.getId()).append(" (role=").append(refresher)
                    .append(", expire=").append(expirePeriod).append("ms)").toString());
        }

        // If the session timer is set to 0 value, it may have not been set, so take the expire
        // period as value
        SipDialogPath path = mSession.getDialogPath();
        if (path.getSessionExpireTime() == 0) {
            path.setSessionExpireTime(expirePeriod);
        }

        // Set refresher role
        mRefresher = refresher;

        // Set expire period
        mExpirePeriod = expirePeriod;

        // Reset last session refresh time
        mLastSessionRefresh = System.currentTimeMillis();

        // Start processing the session timer
        startProcessing();
    }

    /**
     * Start processing the session timer
     */
    private void startProcessing() {
        long currentTime = System.currentTimeMillis();
        if (UAC_ROLE.equals(mRefresher)) {
            startTimer(currentTime, mExpirePeriod, 0.5);
        } else {
            startTimer(currentTime, mExpirePeriod);
        }
    }

    /**
     * Stop the session timer
     */
    public void stop() {
        if (mLogger.isActivated()) {
            mLogger.debug("Stop session timer for session " + mSession.getId());
        }
        stopTimer();
    }

    /**
     * Periodic session timer processing
     */
    public void periodicProcessing() {
        if (UAC_ROLE.equals(mRefresher)) {
            // Refresher role
            sessionRefreshForUAC();
        } else {
            // Refreshee role
            sessionRefreshForUAS();
        }
    }

    /**
     * Session refresh processing for UAC role. If the refresher never sends a session refresh
     * request then the session should be terminated.
     */
    private void sessionRefreshForUAC() {
        try {
            if (mLogger.isActivated()) {
                mLogger.debug("Session timer refresh (UAC role)");
            }

            // Increment the Cseq number of the dialog path
            mSession.getDialogPath().incrementCseq();

            // Create RE-INVITE request
            SipRequest reInvite = SipMessageFactory.createReInvite(mSession.getDialogPath());

            // Set the Authorization header
            mSession.getAuthenticationAgent().setAuthorizationHeader(reInvite);

            // Send RE-INVITE request
            sendReInvite(reInvite);
        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Session timer refresh has failed", e);
            }
        }
    }

    /**
     * Send RE-INVITE message
     * 
     * @param reInvite SIP RE-INVITE
     * @throws SipException
     */
    private void sendReInvite(SipRequest reInvite) throws SipException, CoreException {
        if (mLogger.isActivated()) {
            mLogger.debug("Send RE-INVITE");
        }
        // Send RE-INVITE request
        SipTransactionContext ctx = mSession.getImsService().getImsModule().getSipManager()
                .sendSipMessageAndWait(reInvite, mSession.getResponseTimeout());

        // Analyze the received response
        if (ctx.isSipResponse()) {
            // A response has been received
            if (ctx.getStatusCode() == 200) {
                // Success
                if (mLogger.isActivated()) {
                    mLogger.debug("Session timer refresh with success");
                }

                // The signalisation is established
                mSession.getDialogPath().sigEstablished();

                // Update last session refresh time
                mLastSessionRefresh = System.currentTimeMillis();

                // Send ACK request
                if (mLogger.isActivated()) {
                    mLogger.debug("Send ACK");
                }
                mSession.getImsService().getImsModule().getSipManager()
                        .sendSipAck(mSession.getDialogPath());

                // The session is established
                mSession.getDialogPath().sessionEstablished();

                // Increment internal stack CSeq (NIST stack issue?)
                Dialog dlg = mSession.getDialogPath().getStackDialog();
                if (dlg != null) {
                    dlg.incrementLocalSequenceNumber();
                }

                // Restart processing the session timer
                startProcessing();
            } else if (ctx.getStatusCode() == 405) {
                if (mLogger.isActivated()) {
                    mLogger.debug("Session timer refresh not supported");
                }
            } else if (ctx.getStatusCode() == 407) {
                // 407 Proxy Authentication Required
                if (mLogger.isActivated()) {
                    mLogger.info("407 response received. Send second RE-INVITE");
                }
                // Increment the Cseq number of the dialog path
                mSession.getDialogPath().incrementCseq();

                // Create new RE-INVITE request
                SipRequest newReInvite = SipMessageFactory.createReInvite(mSession.getDialogPath());

                // Update the authentication agent
                mSession.getAuthenticationAgent().readProxyAuthenticateHeader(ctx.getSipResponse());

                // Set the Proxy-Authorization header
                mSession.getAuthenticationAgent().setProxyAuthorizationHeader(newReInvite);

                // Send RE-INVITE request
                sendReInvite(newReInvite);
            } else {
                if (mLogger.isActivated()) {
                    mLogger.debug("Session timer refresh has failed: close the session");
                }

                mSession.terminateSession(TerminationReason.TERMINATION_BY_TIMEOUT);

                ContactId contact = mSession.getRemoteContact();
                mSession.getImsService().getImsModule().getCapabilityService()
                        .requestContactCapabilities(contact);
            }
        } else {
            // No response received: timeout
            throw new SipException("No response received: timeout");
        }
    }

    /**
     * Session refresh processing for UAS role. If the refresher never gets a response from the
     * remote then the session should be terminated.
     */
    private void sessionRefreshForUAS() {
        try {
            if (mLogger.isActivated()) {
                mLogger.debug("Session timer refresh (UAS role)");
            }

            if ((System.currentTimeMillis() - mLastSessionRefresh) >= mExpirePeriod) {
                // Session has expired
                if (mLogger.isActivated()) {
                    mLogger.debug("Session timer refresh has failed: close the session");
                }

                mSession.terminateSession(TerminationReason.TERMINATION_BY_TIMEOUT);

                ContactId contact = mSession.getRemoteContact();
                mSession.getImsService().getImsModule().getCapabilityService()
                        .requestContactCapabilities(contact);
            } else {
                // Success
                if (mLogger.isActivated()) {
                    mLogger.debug("Session timer refresh with success");
                }

                // Start processing the session timer
                startProcessing();
            }
        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Session timer refresh has failed", e);
            }

            mSession.terminateSession(TerminationReason.TERMINATION_BY_SYSTEM);
        }
    }

    /**
     * Receive RE-INVITE request
     * 
     * @param reInvite RE-INVITE request
     */
    public void receiveReInvite(SipRequest reInvite) {
        try {
            if (mLogger.isActivated()) {
                mLogger.debug("Session refresh request received");
            }

            // Update last session refresh time
            mLastSessionRefresh = System.currentTimeMillis();

            // Send 200 OK response
            if (mLogger.isActivated()) {
                mLogger.debug("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkReInviteResponse(
                    mSession.getDialogPath(), reInvite);

            // The signalisation is established
            mSession.getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = mSession.getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (mLogger.isActivated()) {
                    mLogger.info("ACK request received");
                }
                // The session is established
                mSession.getDialogPath().sessionEstablished();
            } else {
                if (mLogger.isActivated()) {
                    mLogger.debug("No ACK received for INVITE");
                }
                // TODO ?
            }
        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Session timer refresh has failed", e);
            }
        }
    }

    /**
     * Receive UPDATE request
     * 
     * @param update UPDATE request
     */
    public void receiveUpdate(SipRequest update) {
        try {
            if (mLogger.isActivated()) {
                mLogger.debug("Session refresh request received");
            }

            // Update last session refresh time
            mLastSessionRefresh = System.currentTimeMillis();

            // Send 200 OK response
            if (mLogger.isActivated()) {
                mLogger.debug("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkUpdateResponse(
                    mSession.getDialogPath(), update);
            mSession.getImsService().getImsModule().getSipManager().sendSipResponse(resp);
        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Session timer refresh has failed", e);
            }
        }
    }
}
