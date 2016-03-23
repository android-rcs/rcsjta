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

package com.gsma.rcs.core.ims.service.presence;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;

import java.text.ParseException;
import java.util.Vector;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ExpiresHeader;
import javax2.sip.message.Response;

/**
 * Generic subscribe manager
 * 
 * @author jexa7410
 */
public abstract class SubscribeManager extends PeriodicRefresher {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * Last min expire period (in milliseconds)
     */
    private static final String REGISTRY_MIN_EXPIRE_PERIOD = "MinSubscribeExpirePeriod";

    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Dialog path
     */
    private SipDialogPath mDialogPath;

    /**
     * Expire period in milliseconds
     */
    private long mExpirePeriod;

    /**
     * Subscription flag
     */
    private boolean mSubscribed = false;

    /**
     * Authentication agent
     */
    private SessionAuthenticationAgent mAuthenticationAgent;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings
     */
    public SubscribeManager(ImsModule parent, RcsSettings rcsSettings) {
        mImsModule = parent;
        mRcsSettings = rcsSettings;

        long defaultExpirePeriod = rcsSettings.getSubscribeExpirePeriod();
        long minExpireValue = RegistryFactory.getFactory().readLong(REGISTRY_MIN_EXPIRE_PERIOD, -1);
        if ((minExpireValue != -1) && (defaultExpirePeriod < minExpireValue)) {
            mExpirePeriod = minExpireValue;
        } else {
            mExpirePeriod = defaultExpirePeriod;
        }
    }

    public void initialize() {
        mAuthenticationAgent = new SessionAuthenticationAgent(mImsModule);
    }

    /**
     * Returns the IMS module
     * 
     * @return IMS module
     */
    public ImsModule getImsModule() {
        return mImsModule;
    }

    /**
     * Is subscribed
     * 
     * @return Boolean
     */
    public boolean isSubscribed() {
        return mSubscribed;
    }

    /**
     * Returns the presentity
     * 
     * @return Presentity
     */
    public abstract String getPresentity();

    /**
     * Receive a notification
     * 
     * @param notify Received notify
     * @throws PayloadException
     * @throws NetworkException
     */
    public abstract void receiveNotification(SipRequest notify) throws PayloadException,
            NetworkException;

    /**
     * Check if the received notification if for this subscriber
     * 
     * @param notify
     * @return Boolean
     */
    public boolean isNotifyForThisSubscriber(SipRequest notify) {
        boolean result = false;
        if ((mDialogPath != null) && notify.getCallId().equals(mDialogPath.getCallId())) {
            result = true;
        }
        return result;
    }

    /**
     * Subscription has been terminated by server
     */
    public void terminatedByServer() {
        if (!mSubscribed) {
            // Already unsubscribed
            return;
        }

        if (logger.isActivated()) {
            logger.info("Subscription has been terminated by server");
        }

        // Stop periodic subscription
        stopTimer();

        // Reset dialog path attributes
        resetDialogPath();

        // Force subscription flag to false
        mSubscribed = false;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
        if (logger.isActivated()) {
            logger.info("Terminate the subscribe manager");
        }

        // Stop periodic subscription
        stopTimer();

        // Unsubscribe before to quit
        if ((mImsModule.getCurrentNetworkInterface() != null)
                && mImsModule.getCurrentNetworkInterface().isRegistered()) {
            unSubscribe();
        }

        if (logger.isActivated()) {
            logger.info("Subscribe manager is terminated");
        }
    }

    /**
     * Create a SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period in milliseconds
     * @return SIP request
     * @throws PayloadException
     */
    public abstract SipRequest createSubscribe(SipDialogPath dialog, long expirePeriod)
            throws PayloadException;

    /**
     * Subscription refresh processing
     */
    public void periodicProcessing() {
        // Make a subscribe
        if (logger.isActivated()) {
            logger.info("Execute re-subscribe");
        }

        // Send SUBSCRIBE request
        subscribe();
    }

    /**
     * Subscribe
     * 
     * @return Boolean
     */
    public synchronized boolean subscribe() {
        if (logger.isActivated()) {
            logger.info("Subscribe to " + getPresentity());
        }

        try {
            // Create a dialog path if necessary
            if (mDialogPath == null) {
                // Set Call-Id
                String callId = mImsModule.getSipManager().getSipStack().generateCallId();

                // Set target
                String target = getPresentity();

                // Set local party
                String localParty = ImsModule.getImsUserProfile().getPublicUri();

                // Set remote party
                String remoteParty = getPresentity();

                // Set the route path
                Vector<String> route = mImsModule.getSipManager().getSipStack()
                        .getServiceRoutePath();

                // Create a dialog path
                mDialogPath = new SipDialogPath(mImsModule.getSipManager().getSipStack(), callId,
                        1, target, localParty, remoteParty, route, mRcsSettings);
            } else {
                // Increment the Cseq number of the dialog path
                mDialogPath.incrementCseq();
            }

            // Create a SUBSCRIBE request
            SipRequest subscribe = createSubscribe(mDialogPath, mExpirePeriod);

            // Send SUBSCRIBE request
            sendSubscribe(subscribe);

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Subscribe has failed", e);
            }
            handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
        return mSubscribed;
    }

    /**
     * Unsubscribe
     */
    public synchronized void unSubscribe() {
        if (!mSubscribed) {
            // Already unsubscribed
            return;
        }

        if (logger.isActivated()) {
            logger.info("Unsubscribe to " + getPresentity());
        }

        try {
            // Stop periodic subscription
            stopTimer();

            // Increment the Cseq number of the dialog path

            mDialogPath.incrementCseq();

            // Create a SUBSCRIBE with expire 0
            SipRequest subscribe = createSubscribe(mDialogPath, 0);

            // Send SUBSCRIBE request
            sendSubscribe(subscribe);

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("UnSubscribe has failed", e);
            }
        }

        // Force subscription flag to false
        mSubscribed = false;

        // Reset dialog path attributes
        resetDialogPath();
    }

    /**
     * Reset the dialog path
     */
    private void resetDialogPath() {
        mDialogPath = null;
    }

    /**
     * Retrieve the expire period
     * 
     * @param resp SIP response
     */
    private void retrieveExpirePeriod(SipResponse response) {
        // Extract expire value from Expires header
        ExpiresHeader expiresHeader = (ExpiresHeader) response.getHeader(ExpiresHeader.NAME);
        if (expiresHeader != null) {
            int expires = expiresHeader.getExpires();
            if (expires != -1) {
                mExpirePeriod = expires * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
            }
        }
    }

    /**
     * Send SUBSCRIBE message
     * 
     * @param subscribe SIP SUBSCRIBE
     * @throws NetworkException
     * @throws PayloadException
     */
    private void sendSubscribe(SipRequest subscribe) throws PayloadException,
            NetworkException {
        try {
            if (logger.isActivated()) {
                logger.info(new StringBuilder("Send SUBSCRIBE, expire=")
                        .append(subscribe.getExpires()).append("ms").toString());
            }

            if (mSubscribed) {
                mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);
            }

            SipTransactionContext ctx = mImsModule.getSipManager().sendSipMessageAndWait(subscribe);
            if (ctx.isSipResponse()) {
                switch (ctx.getStatusCode()) {
                    case Response.OK:
                        if (subscribe.getExpires() != 0) {
                            handle200OK(ctx);
                        } else {
                            handle200OkUnsubscribe(ctx);
                        }
                        return;
                    case Response.ACCEPTED:
                        handle200OK(ctx);
                        return;
                    case Response.PROXY_AUTHENTICATION_REQUIRED:
                        handle407Authentication(ctx);
                        return;
                    case Response.INTERVAL_TOO_BRIEF:
                        handle423IntervalTooBrief(ctx);
                        return;
                    default:
                        handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED,
                                ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
                        return;
                }
            }
            if (logger.isActivated()) {
                logger.debug("No response received for SUBSCRIBE");
            }
            handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED));
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't send sip subscribe!", e);

        } catch (ParseException e) {
            throw new PayloadException("Can't send sip subscribe!", e);
        }
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
        if (logger.isActivated()) {
            logger.info("200 OK response received");
        }
        mSubscribed = true;

        SipResponse resp = ctx.getSipResponse();

        // Set the remote tag
        mDialogPath.setRemoteTag(resp.getToTag());

        // Set the target
        mDialogPath.setTarget(resp.getContactURI());

        // Set the Proxy-Authorization header
        mAuthenticationAgent.readProxyAuthenticateHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);

        // Start the periodic subscribe
        startTimer(System.currentTimeMillis(), mExpirePeriod, 0.5);
    }

    /**
     * Handle 200 0K response of UNSUBSCRIBE
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OkUnsubscribe(SipTransactionContext ctx) {
        // 200 OK response received
        if (logger.isActivated()) {
            logger.info("200 OK response received");
        }
    }

    /**
     * Handle 407 response
     * 
     * @param ctx SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle407Authentication(SipTransactionContext ctx) throws PayloadException,
            NetworkException {
        try {
            if (logger.isActivated()) {
                logger.info("407 response received");
            }
            SipResponse resp = ctx.getSipResponse();
            mAuthenticationAgent.readProxyAuthenticateHeader(resp);
            mDialogPath.incrementCseq();

            if (logger.isActivated()) {
                logger.info("Send second SUBSCRIBE");
            }
            SipRequest subscribe = createSubscribe(mDialogPath, ctx.getTransaction().getRequest()
                    .getExpires().getExpires()
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
            mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);
            sendSubscribe(subscribe);
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);
        }
    }

    /**
     * Handle 423 response
     * 
     * @param ctx SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle423IntervalTooBrief(SipTransactionContext ctx) throws PayloadException,
            NetworkException {
        try {
            if (logger.isActivated()) {
                logger.info("423 interval too brief response received");
            }
            SipResponse resp = ctx.getSipResponse();
            mDialogPath.incrementCseq();

            long minExpire = SipUtils.getMinExpiresPeriod(resp);
            if (minExpire == -1) {
                if (logger.isActivated()) {
                    logger.error("Can't read the Min-Expires value");
                }
                handleError(new PresenceError(PresenceError.SUBSCRIBE_FAILED,
                        "No Min-Expires value found"));
                return;
            }
            RegistryFactory.getFactory().writeLong(REGISTRY_MIN_EXPIRE_PERIOD, minExpire);
            mExpirePeriod = minExpire;
            SipRequest subscribe = createSubscribe(mDialogPath, mExpirePeriod);
            mAuthenticationAgent.setProxyAuthorizationHeader(subscribe);
            sendSubscribe(subscribe);
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle 423 interval too brief response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle 423 interval too brief response!", e);
        }
    }

    /**
     * Handle error response
     * 
     * @param error Error
     */
    private void handleError(PresenceError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Subscribe has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        mSubscribed = false;

        // Subscribe has failed, stop the periodic subscribe
        stopTimer();

        // Reset dialog path attributes
        resetDialogPath();
    }
}
