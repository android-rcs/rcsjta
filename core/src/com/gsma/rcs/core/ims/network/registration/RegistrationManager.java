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

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsError;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
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
import java.util.Vector;

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

    private static final int MILLISEC_CONVERSION_RATE = 1000;

    /**
     * First C Sequence
     */
    private static final int CSEQ_ONE = 1;

    private int mExpirePeriod;

    private SipDialogPath mDialogPath;

    /**
     * Supported feature tags
     */
    private String[] mFeatureTags;

    private ImsNetworkInterface mNetworkInterface;

    private RegistrationProcedure mRegistrationProcedure;

    private String mInstanceId;

    private boolean mRegistered = false;

    /**
     * Reason code for un-registration
     */
    private RcsServiceRegistration.ReasonCode mReasonCode = RcsServiceRegistration.ReasonCode.UNSPECIFIED;

    private boolean mRegistering = false;

    private boolean mNeedUnregister = false;

    private int mNb401Failures = 0;

    /**
     * Number of 4xx5xx6xx failures
     */
    private int mNb4xx5xx6xxFailures;

    private final RcsSettings mRcsSettings;

    private Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param networkInterface IMS network interface
     * @param registrationProcedure Registration procedure
     * @param rcsSettings
     */
    public RegistrationManager(ImsNetworkInterface networkInterface,
            RegistrationProcedure registrationProcedure, RcsSettings rcsSettings) {
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
     * @return value of the expiry period
     */
    private int getExpiryValue() {
        if (CSEQ_ONE == mDialogPath.getCseq()) {
            return mRcsSettings.getRegisterExpirePeriod();
        } else {
            return mExpirePeriod;
        }
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
        new Thread() {
            /**
             * Processing
             */
            public void run() {
                // Stop the current registration
                stopRegistration();
                
                mFeatureTags = RegistrationUtils.getSupportedFeatureTags(mRcsSettings);

                // Start a new registration
                registration();
            }
        }.start();
    }

    /**
     * Registration
     * 
     * @return Boolean status
     */
    public synchronized boolean registration() {
        mRegistering = true;
        try {
            // Create a dialog path if necessary
            if (mDialogPath == null) {
                // Reset the registration authentication procedure
                mRegistrationProcedure.init();

                // Set Call-Id
                String callId = mNetworkInterface.getSipManager().getSipStack().generateCallId();

                // Set target
                String target = PhoneUtils.SIP_URI_HEADER.concat(mRegistrationProcedure
                        .getHomeDomain());

                // Set local party
                String localParty = mRegistrationProcedure.getPublicUri();

                // Set remote party
                String remoteParty = mRegistrationProcedure.getPublicUri();

                // Set the route path
                Vector<String> route = mNetworkInterface.getSipManager().getSipStack()
                        .getDefaultRoutePath();

                // Create a dialog path
                mDialogPath = new SipDialogPath(mNetworkInterface.getSipManager().getSipStack(),
                        callId, 1, target, localParty, remoteParty, route, mRcsSettings);
            } else {
                // Increment the Cseq number of the dialog path
                mDialogPath.incrementCseq();
            }

            // Reset the number of 401 failures
            mNb401Failures = 0;

            // Reset the number of 4xx5xx6xx failures
            mNb4xx5xx6xxFailures = 0;

            // Reset retry after header duration
            mNetworkInterface.setRetryAfterHeaderDuration(0);

            // Create REGISTER request
            SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags,
                    getExpiryValue(), mInstanceId);

            // Send REGISTER request
            sendRegister(register);

        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Registration has failed", e);
            }
            handleError(new ImsError(ImsError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
        mRegistering = false;
        return mRegistered;
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
        mNetworkInterface.getImsModule().getCore().getListener()
                .handleRegistrationTerminated(mReasonCode);
    }

    /**
     * Unregistration
     */
    public synchronized void unRegistration() {
        if (mRegistered) {
            doUnRegistration();
        } else if (mRegistering) {
            mNeedUnregister = true;
        }
    }

    /**
     * Unregistration
     */
    private synchronized void doUnRegistration() {
        mNeedUnregister = false;
        if (!mRegistered) {
            // Already unregistered
            return;
        }

        try {
            // Stop periodic registration
            stopTimer();

            // Increment the Cseq number of the dialog path
            mDialogPath.incrementCseq();

            // Reset the number of 4xx5xx6xx failures
            mNb4xx5xx6xxFailures = 0;

            // Create REGISTER request with expire 0
            SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, 0,
                    mInstanceId);

            // Send REGISTER request
            sendRegister(register);

        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Unregistration has failed", e);
            }
        }

        // Force registration flag to false
        mRegistered = false;

        // Reset dialog path attributes
        resetDialogPath();

        mReasonCode = isBatteryLow() ? RcsServiceRegistration.ReasonCode.BATTERY_LOW
                : RcsServiceRegistration.ReasonCode.CONNECTION_LOST;

        // Notify event listener
        mNetworkInterface.getImsModule().getCore().getListener()
                .handleRegistrationTerminated(mReasonCode);
    }

    /**
     * Send REGISTER message
     * 
     * @param register SIP REGISTER
     * @throws SipException
     * @throws CoreException
     */
    private void sendRegister(SipRequest register) throws SipException, CoreException {
        if (mLogger.isActivated()) {
            mLogger.info("Send REGISTER, expire=" + register.getExpires());
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
                        handle200OkUnregister(ctx);
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
     * @throws SipException
     * @throws CoreException
     */
    private void handle200OK(SipTransactionContext ctx) throws SipException, CoreException {
        // 200 OK response received
        if (mLogger.isActivated()) {
            mLogger.info("200 OK response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Set the associated URIs
        ListIterator<Header> associatedHeader = resp.getHeaders(SipUtils.HEADER_P_ASSOCIATED_URI);
        ImsModule.IMS_USER_PROFILE.setAssociatedUri(associatedHeader);

        // Set the GRUU
        mNetworkInterface.getSipManager().getSipStack().setInstanceId(mInstanceId);
        ListIterator<Header> contacts = resp.getHeaders(ContactHeader.NAME);
        while (contacts.hasNext()) {
            ContactHeader contact = (ContactHeader) contacts.next();
            String contactInstanceId = contact.getParameter(SipUtils.SIP_INSTANCE_PARAM);
            if ((contactInstanceId != null) && (mInstanceId != null)
                    && (mInstanceId.contains(contactInstanceId))) {
                String pubGruu = contact.getParameter(SipUtils.PUBLIC_GRUU_PARAM);
                mNetworkInterface.getSipManager().getSipStack().setPublicGruu(pubGruu);
                String tempGruu = contact.getParameter(SipUtils.TEMP_GRUU_PARAM);
                mNetworkInterface.getSipManager().getSipStack().setTemporaryGruu(tempGruu);
            }
        }

        // Set the service route path
        ListIterator<Header> routes = resp.getHeaders(SipUtils.HEADER_SERVICE_ROUTE);
        mNetworkInterface.getSipManager().getSipStack().setServiceRoutePath(routes);

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
                    if (mLogger.isActivated()) {
                        mLogger.warn("Non-numeric rport value \"" + viaRportStr + "\"");
                    }
                }
            }
            mNetworkInterface.setNatPublicPort(viaRport);
            if (mLogger.isActivated()) {
                mLogger.debug("NAT public interface detected: " + received + ":" + viaRport);
            }
        } else {
            mNetworkInterface.setNatTraversal(false);
            mNetworkInterface.setNatPublicAddress(null);
            mNetworkInterface.setNatPublicPort(-1);
        }
        if (mLogger.isActivated()) {
            mLogger.debug("NAT traversal detection: " + mNetworkInterface.isBehindNat());
        }

        // Read the security header
        mRegistrationProcedure.readSecurityHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);
        mRegistered = true;
        mReasonCode = ReasonCode.UNSPECIFIED;

        // Start the periodic registration
        if (mExpirePeriod <= 1200) {
            startTimer(mExpirePeriod, 0.5);
        } else {
            startTimer(mExpirePeriod - 600);
        }

        // Notify event listener
        mNetworkInterface.getImsModule().getCore().getListener().handleRegistrationSuccessful();

        // Start unregister procedure if necessary
        if (mNeedUnregister) {
            doUnRegistration();
        }
    }

    /**
     * Handle 200 0K response of UNREGISTER
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OkUnregister(SipTransactionContext ctx) {
        // 200 OK response received
        if (mLogger.isActivated()) {
            mLogger.info("200 OK response received");
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
     * @throws SipException
     * @throws CoreException
     */
    private void handle302MovedTemporarily(SipTransactionContext ctx) throws SipException,
            CoreException {
        // 302 Moved Temporarily response received
        if (mLogger.isActivated()) {
            mLogger.info("302 Moved Temporarily response received");
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
        if (mLogger.isActivated()) {
            mLogger.info("Send REGISTER to new address");
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, ctx
                .getTransaction().getRequest().getExpires().getExpires(), mInstanceId);

        // Send REGISTER request
        sendRegister(register);
    }

    /**
     * Handle 401 response
     * 
     * @param ctx SIP transaction context
     * @throws SipException
     * @throws CoreException
     */
    private void handle401Unauthorized(SipTransactionContext ctx) throws SipException,
            CoreException {
        /**
         * Increment the number of 401 failures
         */
        mNb401Failures++;

        // 401 response received
        if (mLogger.isActivated()) {
            mLogger.info("401 response received, nbFailures=" + mNb401Failures);
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
        if (mLogger.isActivated()) {
            mLogger.info("Send REGISTER with security token");
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, ctx
                .getTransaction().getRequest().getExpires().getExpires(), mInstanceId);

        // Send REGISTER request
        sendRegister(register);
    }

    /**
     * Handle 423 response
     * 
     * @param ctx SIP transaction context
     * @throws SipException
     * @throws CoreException
     */
    private void handle423IntervalTooBrief(SipTransactionContext ctx) throws SipException,
            CoreException {
        // 423 response received
        if (mLogger.isActivated()) {
            mLogger.info("423 response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Extract the Min-Expire value
        int minExpire = SipUtils.getMinExpiresPeriod(resp);
        if (minExpire == -1) {
            if (mLogger.isActivated()) {
                mLogger.error("Can't read the Min-Expires value");
            }
            handleError(new ImsError(ImsError.UNEXPECTED_EXCEPTION, "No Min-Expires value found"));
            return;
        }

        // Set the expire value
        mExpirePeriod = minExpire;

        // Create a new REGISTER with the right expire period
        if (mLogger.isActivated()) {
            mLogger.info("Send new REGISTER");
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
        if (mLogger.isActivated()) {
            mLogger.info("Registration has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        mRegistered = false;
        mReasonCode = ReasonCode.CONNECTION_LOST;

        // Registration has failed, stop the periodic registration
        stopTimer();

        // Reset dialog path attributes
        resetDialogPath();

        // Notify event listener
        mNetworkInterface.getImsModule().getCore().getListener().handleRegistrationFailed(error);
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
                        mExpirePeriod = expires;
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
                mExpirePeriod = expires;
            }
        }
    }

    /**
     * Registration processing
     */
    public void periodicProcessing() {
        // Make a registration
        if (mLogger.isActivated()) {
            mLogger.info("Execute re-registration");
        }
        registration();
    }

    /**
     * Handle 4xx5xx6xx response without retry header
     * 
     * @param ctx SIP transaction context
     * @throws SipException
     * @throws CoreException
     */
    private void handle4xx5xx6xxNoRetryAfterHeader(SipTransactionContext ctx) throws SipException,
            CoreException {
        if (mLogger.isActivated()) {
            mLogger.info("4xx5xx6xx response without retry after header received");
        }
        final SipResponse response = ctx.getSipResponse();
        final RetryAfterHeader retryHeader = (RetryAfterHeader) response.getStackMessage()
                .getHeader(RetryAfterHeader.NAME);
        final int durationInMillis = retryHeader.getDuration() * MILLISEC_CONVERSION_RATE;
        if (retryHeader != null && durationInMillis > 0) {
            mNetworkInterface.setRetryAfterHeaderDuration(durationInMillis);
            handleError(new ImsError(ImsError.REGISTRATION_FAILED, new StringBuilder("retry after")
                    .append(durationInMillis).append(" for 4xx/5xx/6xx").toString()));
            return;
        } else {
            mNb4xx5xx6xxFailures++;
            if (mNb4xx5xx6xxFailures >= MAX_REGISTRATION_FAILURES) {
                /**
                 * We reached MAX_REGISTRATION_FAILURES, stop registration retries
                 */
                handleError(new ImsError(ImsError.REGISTRATION_FAILED, "too many 4xx/5xx/6xx"));
                return;
            }
        }
        SipRequest register = SipMessageFactory.createRegister(mDialogPath, mFeatureTags, ctx
                .getTransaction().getRequest().getExpires().getExpires(), mInstanceId);
        sendRegister(register);
    }
}
