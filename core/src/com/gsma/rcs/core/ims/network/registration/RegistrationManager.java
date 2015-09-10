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

package com.gsma.rcs.core.ims.network.registration;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.ImsError;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipInterface;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.DeviceUtils;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.RcsServiceRegistration.ReasonCode;

import java.util.ListIterator;

import javax2.sip.header.ContactHeader;
import javax2.sip.header.ExpiresHeader;
import javax2.sip.header.Header;
import javax2.sip.header.RetryAfterHeader;
import javax2.sip.header.ViaHeader;
import javax2.sip.message.Response;

/**
 * Registration manager (register, re-register, un-register)
 * 
 * @author JM. Auffret
 */
public class RegistrationManager extends PeriodicRefresher {

    private static final int MAX_REGISTRATION_FAILURES = 3;

    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    private static final long DEFAULT_EXPIRE_PERIOD = 1200 * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;

    private static final long SUBSTRACT_EXPIRE_PERIOD = DEFAULT_EXPIRE_PERIOD / 2;

    /**
     * First C Sequence
     */
    private static final int CSEQ_ONE = 1;

    private long mExpirePeriod;

    private SipDialogPath mDialogPath;

    /**
     * Supported feature tags
     */
    private String[] mFeatureTags;

    private final ImsNetworkInterface mNetworkInterface;

    private RegistrationProcedure mRegistrationProcedure;

    private String mInstanceId;

    private boolean mRegistered = false;

    /**
     * Reason code for un-registration
     */
    private RcsServiceRegistration.ReasonCode mReasonCode = RcsServiceRegistration.ReasonCode.UNSPECIFIED;

    private boolean mPendingUnRegister = false;

    private int mNb401Failures = 0;

    /**
     * Number of 4xx5xx6xx failures
     */
    private int mNb4xx5xx6xxFailures;

    private final RcsSettings mRcsSettings;

    private final Core mCore;

    private static final Logger sLogger = Logger.getLogger(RegistrationManager.class.getName());

    /**
     * Constructor
     * 
     * @param networkInterface IMS network interface
     * @param registrationProcedure Registration procedure
     * @param rcsSettings The RCS settings accessor
     */
    public RegistrationManager(ImsNetworkInterface networkInterface,
            RegistrationProcedure registrationProcedure, RcsSettings rcsSettings) {
        mCore = networkInterface.getImsModule().getCore();
        mNetworkInterface = networkInterface;
        mRegistrationProcedure = registrationProcedure;
        mFeatureTags = RegistrationUtils.getSupportedFeatureTags(rcsSettings);
        mRcsSettings = rcsSettings;
        mExpirePeriod = mRcsSettings.getRegisterExpirePeriod();
        if (mRcsSettings.isGruuSupported()) {
            mInstanceId = DeviceUtils.getInstanceId(AndroidFactory.getApplicationContext(),
                    rcsSettings);
        }
    }

    /**
     * Get the expiry value duration for the next SIP register
     * 
     * @return value of the expiry period in milliseconds
     */
    private long getExpiryValue() {
        if (CSEQ_ONE == mDialogPath.getCseq()) {
            return mRcsSettings.getRegisterExpirePeriod();
        }
        return mExpirePeriod;
    }

    /**
     * Init the registration procedure
     */
    public void init() {
    }

    /**
     * Returns registration procedure
     * 
     * @return Registration procedure
     */
    public RegistrationProcedure getRegistrationProcedure() {
        return mRegistrationProcedure;
    }

    /**
     * Is registered
     * 
     * @return Return True if the terminal is registered, else return False
     */
    public boolean isRegistered() {
        return mRegistered;
    }

    /**
     * Gets reason code for RCS registration
     * 
     * @return reason code
     */
    public RcsServiceRegistration.ReasonCode getReasonCode() {
        return mReasonCode;
    }

    /**
     * Restart registration procedure
     */
    public void restart() {
        mCore.scheduleForBackgroundExecution(new Runnable() {

            @Override
            public void run() {
                // Stop the current registration
                stopRegistration();
                mFeatureTags = RegistrationUtils.getSupportedFeatureTags(mRcsSettings);

                try {
                    register();
                } catch (SipPayloadException e) {
                    sLogger.error("Registration has failed!", e);
                } catch (SipNetworkException e) {
                    /* Nothing to be handled here */
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Registration has failed!", e);
                    handleError(new ImsError(ImsError.REGISTRATION_FAILED, e));
                }

            }
        });
    }

    /**
     * Attempts to perform a registration to IMS
     * 
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public synchronized void register() throws SipPayloadException, SipNetworkException {
        try {
            if (mDialogPath == null) {
                /* Reset the registration authentication procedure */
                mRegistrationProcedure.init();

                SipInterface sipInterface = mNetworkInterface.getSipManager().getSipStack();
                String callId = sipInterface.generateCallId();

                String target = PhoneUtils.SIP_URI_HEADER.concat(mRegistrationProcedure
                        .getHomeDomain());

                String uri = mRegistrationProcedure.getPublicUri();
                mDialogPath = new SipDialogPath(sipInterface, callId, 1, target, uri, uri,
                        sipInterface.getDefaultRoutePath(), mRcsSettings);
            } else {
                mDialogPath.incrementCseq();
            }

            mNb401Failures = 0;

            mNb4xx5xx6xxFailures = 0;

            mNetworkInterface.setRetryAfterHeaderDuration(0);

            SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags,
                    getExpiryValue(), mInstanceId);

            sendRegister(register);
        } catch (SipPayloadException e) {
            handleError(new ImsError(ImsError.REGISTRATION_FAILED, e));
            throw e;
        } catch (SipNetworkException e) {
            handleError(new ImsError(ImsError.REGISTRATION_FAILED, e));
            throw e;
        }

    }

    private boolean isBatteryLow() {
        return mNetworkInterface.getImsModule().getImsConnectionManager().isDisconnectedByBattery();
    }

    /**
     * Stop the registration manager without unregistering from IMS
     */
    public synchronized void stopRegistration() {
        if (!mRegistered) {
            // Already unregistered
            return;
        }

        // Stop periodic registration
        stopTimer();

        // Force registration flag to false
        mRegistered = false;

        // Reset dialog path attributes
        resetDialogPath();

        mReasonCode = isBatteryLow() ? RcsServiceRegistration.ReasonCode.BATTERY_LOW
                : RcsServiceRegistration.ReasonCode.CONNECTION_LOST;

        // Notify event listener
        mCore.getListener().handleRegistrationTerminated(mReasonCode);
    }

    /**
     * Performs a de-registration to IMS
     * 
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public synchronized void deRegister() throws SipPayloadException, SipNetworkException {
        if (mRegistered) {
            mPendingUnRegister = false;

            stopTimer();

            mDialogPath.incrementCseq();

            mNb4xx5xx6xxFailures = 0;

            /* Create REGISTER request with expire 0 */
            SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, 0,
                    mInstanceId);
            sendRegister(register);

            mRegistered = false;

            resetDialogPath();

            mReasonCode = isBatteryLow() ? RcsServiceRegistration.ReasonCode.BATTERY_LOW
                    : RcsServiceRegistration.ReasonCode.CONNECTION_LOST;

            mCore.getListener().handleRegistrationTerminated(mReasonCode);
        } else {
            mPendingUnRegister = true;
        }
    }

    /**
     * Send REGISTER message
     * 
     * @param register SIP REGISTER
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void sendRegister(SipRequest register) throws SipPayloadException, SipNetworkException {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Send REGISTER, expire=").append(register.getExpires())
                    .append("ms").toString());
        }

        // Set the security header
        mRegistrationProcedure.writeSecurityHeader(register);

        // Send REGISTER request
        SipTransactionContext ctx = mNetworkInterface.getSipManager().sendSipMessageAndWait(
                register);

        // Analyze the received response
        if (ctx.isSipResponse()) {
            // A response has been received
            switch (ctx.getStatusCode()) {
                case Response.OK:
                    /**
                     * 200 OK
                     */
                    if (register.getExpires() != 0) {
                        handle200OK(ctx);
                    } else {
                        handle200OkUnregister();
                    }
                    break;
                case Response.MOVED_TEMPORARILY:
                    /**
                     * 302 Moved Temporarily
                     */
                    handle302MovedTemporarily(ctx);
                    break;
                case Response.UNAUTHORIZED:
                    /**
                     * 401 Unauthorized
                     */
                    handle401Unauthorized(ctx);
                    break;
                case Response.INTERVAL_TOO_BRIEF:
                    /**
                     * 423 Interval Too Brief
                     */
                    handle423IntervalTooBrief(ctx);
                    break;
                case Response.NOT_FOUND:
                case Response.REQUEST_TIMEOUT:
                case Response.TEMPORARILY_UNAVAILABLE:
                case Response.SERVER_INTERNAL_ERROR:
                case Response.SERVICE_UNAVAILABLE:
                case Response.SERVER_TIMEOUT:
                case Response.BUSY_EVERYWHERE:
                    /**
                     * Intentional fall-through for 4xx, 5xx & 6xx SIP error responses.
                     */
                    handle4xx5xx6xxNoRetryAfterHeader(ctx);
                    break;
                default:
                    /**
                     * Other error response
                     */
                    handleError(new ImsError(ImsError.REGISTRATION_FAILED, ctx.getStatusCode()
                            + " " + ctx.getReasonPhrase()));
                    break;
            }
        } else {
            // No response received: timeout
            handleError(new ImsError(ImsError.REGISTRATION_FAILED, "timeout"));
        }
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle200OK(SipTransactionContext ctx) throws SipPayloadException,
            SipNetworkException {
        // 200 OK response received
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Set the associated URIs
        ListIterator<Header> associatedHeader = resp.getHeaders(SipUtils.HEADER_P_ASSOCIATED_URI);
        ImsModule.getImsUserProfile().setAssociatedUri(associatedHeader);

        // Set the GRUU
        SipInterface sipInterface = mNetworkInterface.getSipManager().getSipStack();
        sipInterface.setInstanceId(mInstanceId);
        ListIterator<Header> contacts = resp.getHeaders(ContactHeader.NAME);
        while (contacts.hasNext()) {
            ContactHeader contact = (ContactHeader) contacts.next();
            String contactInstanceId = contact.getParameter(SipUtils.SIP_INSTANCE_PARAM);
            if ((contactInstanceId != null) && (mInstanceId != null)
                    && (mInstanceId.contains(contactInstanceId))) {
                String pubGruu = contact.getParameter(SipUtils.PUBLIC_GRUU_PARAM);
                sipInterface.setPublicGruu(pubGruu);
                String tempGruu = contact.getParameter(SipUtils.TEMP_GRUU_PARAM);
                sipInterface.setTemporaryGruu(tempGruu);
            }
        }

        // Set the service route path
        ListIterator<Header> routes = resp.getHeaders(SipUtils.HEADER_SERVICE_ROUTE);
        sipInterface.setServiceRoutePath(routes);

        // If the IP address of the Via header in the 200 OK response to the initial
        // SIP REGISTER request is different than the local IP address then there is a NAT
        String localIpAddr = mNetworkInterface.getNetworkAccess().getIpAddress();
        ViaHeader respViaHeader = ctx.getSipResponse().getViaHeaders().next();
        String received = respViaHeader.getParameter("received");
        if (!respViaHeader.getHost().equals(localIpAddr)
                || ((received != null) && !received.equals(localIpAddr))) {
            mNetworkInterface.setNatTraversal(true);
            mNetworkInterface.setNatPublicAddress(received);
            String viaRportStr = respViaHeader.getParameter("rport");
            int viaRport = -1;
            if (viaRportStr != null) {
                try {
                    viaRport = Integer.parseInt(viaRportStr);
                } catch (NumberFormatException e) {
                    if (sLogger.isActivated()) {
                        sLogger.warn("Non-numeric rport value \"" + viaRportStr + "\"");
                    }
                }
            }
            mNetworkInterface.setNatPublicPort(viaRport);
            if (sLogger.isActivated()) {
                sLogger.debug("NAT public interface detected: " + received + ":" + viaRport);
            }
        } else {
            mNetworkInterface.setNatTraversal(false);
            mNetworkInterface.setNatPublicAddress(null);
            mNetworkInterface.setNatPublicPort(-1);
        }
        if (sLogger.isActivated()) {
            sLogger.debug("NAT traversal detection: " + mNetworkInterface.isBehindNat());
        }

        // Read the security header
        mRegistrationProcedure.readSecurityHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);
        mRegistered = true;
        mReasonCode = ReasonCode.UNSPECIFIED;

        // Start the periodic registration
        long currentTime = System.currentTimeMillis();
        if (mExpirePeriod <= DEFAULT_EXPIRE_PERIOD) {
            startTimer(currentTime, mExpirePeriod, 0.5);
        } else {
            startTimer(currentTime, mExpirePeriod - SUBSTRACT_EXPIRE_PERIOD);
        }

        // Notify event listener
        mCore.getListener().handleRegistrationSuccessful();

        /* Start deregister procedure if necessary */
        if (mPendingUnRegister) {
            deRegister();
        }
    }

    private void handle200OkUnregister() {
        // 200 OK response received
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }

        // Reset the NAT parameters as we are not expecting any more messages
        // for this registration
        mNetworkInterface.setNatPublicAddress(null);
        mNetworkInterface.setNatPublicPort(-1);
    }

    /**
     * Handle 302 response
     * 
     * @param ctx SIP transaction context
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle302MovedTemporarily(SipTransactionContext ctx) throws SipPayloadException,
            SipNetworkException {
        // 302 Moved Temporarily response received
        if (sLogger.isActivated()) {
            sLogger.info("302 Moved Temporarily response received");
        }

        // Extract new target URI from Contact header of the received response
        SipResponse resp = ctx.getSipResponse();
        ContactHeader contactHeader = (ContactHeader) resp.getStackMessage().getHeader(
                ContactHeader.NAME);
        String newUri = contactHeader.getAddress().getURI().toString();
        mDialogPath.setTarget(newUri);

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Create REGISTER request with security token
        if (sLogger.isActivated()) {
            sLogger.info("Send REGISTER to new address");
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, ctx
                .getTransaction().getRequest().getExpires().getExpires()
                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE, mInstanceId);

        // Send REGISTER request
        sendRegister(register);
    }

    /**
     * Handle 401 response
     * 
     * @param ctx SIP transaction context
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle401Unauthorized(SipTransactionContext ctx) throws SipPayloadException,
            SipNetworkException {
        /**
         * Increment the number of 401 failures
         */
        mNb401Failures++;

        // 401 response received
        if (sLogger.isActivated()) {
            sLogger.info("401 response received, nbFailures=" + mNb401Failures);
        }

        if (mNb401Failures >= MAX_REGISTRATION_FAILURES) {
            /**
             * We reached MAX_REGISTRATION_FAILURES, stop registration retries
             */
            handleError(new ImsError(ImsError.REGISTRATION_FAILED, "too many 401"));
            return;
        }

        SipResponse resp = ctx.getSipResponse();

        // Read the security header
        mRegistrationProcedure.readSecurityHeader(resp);

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Create REGISTER request with security token
        if (sLogger.isActivated()) {
            sLogger.info("Send REGISTER with security token");
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, ctx
                .getTransaction().getRequest().getExpires().getExpires()
                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE, mInstanceId);

        // Send REGISTER request
        sendRegister(register);
    }

    /**
     * Handle 423 response
     * 
     * @param ctx SIP transaction context
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle423IntervalTooBrief(SipTransactionContext ctx) throws SipPayloadException,
            SipNetworkException {
        // 423 response received
        if (sLogger.isActivated()) {
            sLogger.info("423 response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Extract the Min-Expire value
        long minExpire = SipUtils.getMinExpiresPeriod(resp);

        // Set the expire value
        mExpirePeriod = minExpire;

        // Create a new REGISTER with the right expire period
        if (sLogger.isActivated()) {
            sLogger.info("Send new REGISTER");
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags,
                mExpirePeriod, mInstanceId);

        // Send REGISTER request
        sendRegister(register);
    }

    /**
     * Handle error response
     * 
     * @param error Error
     */
    private void handleError(ImsError error) {
        // Error
        if (sLogger.isActivated()) {
            sLogger.info("Registration has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        mRegistered = false;
        mReasonCode = ReasonCode.CONNECTION_LOST;

        // Registration has failed, stop the periodic registration
        stopTimer();

        // Reset dialog path attributes
        resetDialogPath();

        // Notify event listener
        mCore.getListener().handleRegistrationFailed(error);
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
     * @param response SIP response
     */
    private void retrieveExpirePeriod(SipResponse response) {
        // Extract expire value from Contact header
        ListIterator<Header> contacts = response.getHeaders(ContactHeader.NAME);
        if (contacts != null) {
            while (contacts.hasNext()) {
                ContactHeader contact = (ContactHeader) contacts.next();
                if (contact.getAddress().getHost()
                        .equals(mNetworkInterface.getNetworkAccess().getIpAddress())) {
                    int expires = contact.getExpires();
                    if (expires != -1) {
                        mExpirePeriod = expires * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
                    }
                    return;
                }
            }
        }

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
     * Registration processing
     * 
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public void periodicProcessing() throws SipPayloadException, SipNetworkException {
        // Make a registration
        if (sLogger.isActivated()) {
            sLogger.info("Execute re-registration");
        }
        register();
    }

    /**
     * Handle 4xx5xx6xx response without retry header
     * 
     * @param ctx SIP transaction context
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle4xx5xx6xxNoRetryAfterHeader(SipTransactionContext ctx)
            throws SipPayloadException, SipNetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("4xx5xx6xx response without retry after header received");
        }
        final SipResponse response = ctx.getSipResponse();
        final RetryAfterHeader retryHeader = (RetryAfterHeader) response.getStackMessage()
                .getHeader(RetryAfterHeader.NAME);
        if (retryHeader != null) {
            final long durationInMillis = retryHeader.getDuration()
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
            if (durationInMillis > 0) {
                mNetworkInterface.setRetryAfterHeaderDuration(durationInMillis);
                handleError(new ImsError(ImsError.REGISTRATION_FAILED, new StringBuilder(
                        "retry after").append(durationInMillis).append(" for 4xx/5xx/6xx")
                        .toString()));
            } else {
                mNb4xx5xx6xxFailures++;
                if (mNb4xx5xx6xxFailures >= MAX_REGISTRATION_FAILURES) {
                    /**
                     * We reached MAX_REGISTRATION_FAILURES, stop registration retries
                     */
                    handleError(new ImsError(ImsError.REGISTRATION_FAILED, "too many 4xx/5xx/6xx"));
                }
            }
            return;
        }
        mNb4xx5xx6xxFailures++;
        if (mNb4xx5xx6xxFailures >= MAX_REGISTRATION_FAILURES) {
            /**
             * We reached MAX_REGISTRATION_FAILURES, stop registration retries
             */
            handleError(new ImsError(ImsError.REGISTRATION_FAILED, "too many 4xx/5xx/6xx"));
            return;
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, ctx
                .getTransaction().getRequest().getExpires().getExpires()
                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE, mInstanceId);
        sendRegister(register);
    }
}
