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

package com.gsma.rcs.core.ims.network.sip;

import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.KeepAliveManager;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipInterface;
import com.gsma.rcs.core.ims.protocol.sip.SipMessage;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.util.ListIterator;

import javax2.sip.header.ViaHeader;
import javax2.sip.header.WarningHeader;
import javax2.sip.message.Request;
import javax2.sip.message.Response;

/**
 * SIP manager
 * 
 * @author JM. Auffret
 */
public class SipManager {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * SIP timeout for SIP transaction (in milliseconds)
     */
    private static long sTimeout = 30000;

    private final ImsNetworkInterface mNetworkInterface;

    private SipInterface mSipInterface;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(SipManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS network interface
     * @param rcsSettings the RCS settings accessor
     */
    public SipManager(ImsNetworkInterface parent, RcsSettings rcsSettings) {
        mNetworkInterface = parent;
        mRcsSettings = rcsSettings;
        if (sLogger.isActivated()) {
            sLogger.info("SIP manager started");
        }
    }

    /**
     * Returns the network interface
     * 
     * @return Network interface
     */
    public ImsNetworkInterface getNetworkInterface() {
        return mNetworkInterface;
    }

    /**
     * Returns the SIP stack
     * 
     * @return SIP stack
     */
    public SipInterface getSipStack() {
        return mSipInterface;
    }

    /**
     * Terminate the manager
     */
    public void terminate() {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the SIP manager");
        }
        // Close the SIP stack
        if (mSipInterface != null) {
            closeStack();
        }
        if (sLogger.isActivated()) {
            sLogger.info("SIP manager has been terminated");
        }
    }

    /**
     * Initialize the SIP stack
     * 
     * @param localAddr Local IP address
     * @param proxyAddr Outbound proxy address
     * @param proxyPort Outbound proxy port
     * @param protocol the protocol
     * @param tcpFallback TCP fallback according to RFC3261 chapter 18.1.1
     * @throws PayloadException
     */
    public synchronized void initStack(String localAddr, String proxyAddr, int proxyPort,
            String protocol, boolean tcpFallback) throws PayloadException {
        closeStack();
        mSipInterface = new SipInterface(localAddr, proxyAddr, proxyPort, protocol, tcpFallback,
                mRcsSettings);
        mSipInterface.initialize();
    }

    /**
     * Close the SIP stack
     */
    public synchronized void closeStack() {
        if (mSipInterface == null) {
            // Already closed
            return;
        }
        // Close the SIP stack
        mSipInterface.close();
        mSipInterface = null;
    }

    /**
     * Send a SIP message and wait a response
     * 
     * @param message SIP message
     * @return Transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message) throws PayloadException,
            NetworkException {
        return sendSipMessageAndWait(message, SipManager.sTimeout);
    }

    /**
     * Send a SIP message and wait a response
     * 
     * @param message SIP message
     * @param timeout SIP timeout in milliseconds
     * @return Transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message, long timeout)
            throws PayloadException, NetworkException {
        return sendSipMessageAndWait(message, timeout, null);
    }

    /**
     * Send a SIP message and create a context to wait a response
     * 
     * @param message SIP message
     * @return Transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessage(SipMessage message) throws PayloadException,
            NetworkException {
        return sendSipMessage(message, null);
    }

    /**
     * Send a SIP message and wait a response
     * 
     * @param message the SIP message
     * @param timeout in milliseconds
     * @param callback callback to handle provisional response
     * @return SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message, long timeout,
            SipTransactionContext.INotifySipProvisionalResponse callback) throws NetworkException,
            PayloadException {
        SipTransactionContext ctx = mSipInterface.sendSipMessageAndWait(message, callback);
        ctx.waitResponse(timeout);

        if (!(message instanceof SipRequest) || !ctx.isSipResponse()) {
            return ctx;
        }
        String method = ((SipRequest) message).getMethod();
        SipResponse response = ctx.getSipResponse();
        if (response == null) {
            return ctx;
        }
        /* Analyze the received response */
        if (!Request.REGISTER.equals(method)) {
            /* Check if not registered and warning header */
            WarningHeader warn = (WarningHeader) response.getHeader(WarningHeader.NAME);
            if (Response.FORBIDDEN == ctx.getStatusCode() && warn == null) {
                /* Launch new registration */
                mNetworkInterface.getRegistrationManager().restart();
            }
        }
        if (!Request.INVITE.equals(method) && !Request.REGISTER.equals(method)) {
            return ctx;
        }
        KeepAliveManager keepAliveManager = mNetworkInterface.getSipManager().getSipStack()
                .getKeepAliveManager();
        if (keepAliveManager == null) {
            return ctx;
        }
        /* Message is a response to INVITE or REGISTER: analyze "keep" flag of "Via" header */
        ListIterator<ViaHeader> iterator = response.getViaHeaders();
        if (!iterator.hasNext()) {
            keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
            return ctx;
        }
        ViaHeader respViaHeader = iterator.next();
        String keepStr = respViaHeader.getParameter("keep");
        if (keepStr == null) {
            keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
            return ctx;
        }
        try {
            long viaKeep = Integer.parseInt(keepStr) * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
            if (viaKeep > 0) {
                keepAliveManager.setPeriod(viaKeep);
            } else {
                /* Set Default Value fetched from provisioning settings */
                keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
            }
        } catch (NumberFormatException e) {
            /*
             * If "keep" value is invalid or not present, Set Default Value fetched from
             * provisioning settings
             */
            keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
        }
        return ctx;
    }

    /**
     * Send a SIP message and create a context to wait a response
     * 
     * @param message the SIP message
     * @param callback callback to handle provisional response
     * @return SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessage(SipMessage message,
            SipTransactionContext.INotifySipProvisionalResponse callback) throws NetworkException,
            PayloadException {
        return mSipInterface.sendSipMessageAndWait(message, callback);
    }

    /**
     * Wait a response
     * 
     * @param ctx SIP transaction context
     */
    public void waitResponse(SipTransactionContext ctx) {
        ctx.waitResponse(SipManager.sTimeout);
    }

    /**
     * Wait a response
     * 
     * @param ctx SIP transaction context
     * @param timeout in milliseconds
     */
    public void waitResponse(SipTransactionContext ctx, long timeout) {
        ctx.waitResponse(timeout);

        SipMessage message = ctx.getMessageReceived();
        if (!(message instanceof SipRequest) || !ctx.isSipResponse()) {
            return;
        }
        String method = ((SipRequest) message).getMethod();
        SipResponse response = ctx.getSipResponse();
        if (response == null) {
            return;
        }
        /* Analyze the received response */
        if (!Request.REGISTER.equals(method)) {
            /* Check if not registered and warning header */
            WarningHeader warn = (WarningHeader) response.getHeader(WarningHeader.NAME);
            if (Response.FORBIDDEN == ctx.getStatusCode() && warn == null) {
                /* Launch new registration */
                mNetworkInterface.getRegistrationManager().restart();
            }
        }
        if (!Request.REGISTER.equals(method)) {
            return;
        }
        KeepAliveManager keepAliveManager = mSipInterface.getKeepAliveManager();
        if (keepAliveManager == null) {
            return;
        }
        /* Message is a response to REGISTER: analyze "keep" flag of "Via" header */
        ListIterator<ViaHeader> iterator = response.getViaHeaders();
        if (!iterator.hasNext()) {
            return;
        }
        ViaHeader respViaHeader = iterator.next();
        String keepStr = respViaHeader.getParameter("keep");
        if (keepStr == null) {
            return;
        }
        try {
            long viaKeep = Integer.parseInt(keepStr) * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
            if (viaKeep > 0) {
                keepAliveManager.setPeriod(viaKeep);

            } else if (viaKeep == 0) {
                /*
                 * If "keep" value is zero, set keep alive period to own discretion (i.e. default
                 * value from provisioning)
                 */
                keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
            }
        } catch (NumberFormatException e) {
            /*
             * If "keep" value is invalid , set Default Value fetched from provisioning settings
             */
            keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
        }
    }

    /**
     * Send a SIP response
     * 
     * @param response SIP response
     * @throws NetworkException
     */
    public void sendSipResponse(SipResponse response) throws NetworkException {
        mSipInterface.sendSipResponse(response);
    }

    /**
     * Send a SIP ACK
     * 
     * @param dialog Dialog path
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendSipAck(SipDialogPath dialog) throws PayloadException, NetworkException {
        mSipInterface.sendSipAck(dialog);
    }

    /**
     * Send a SIP BYE
     * 
     * @param dialog Dialog path
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendSipBye(SipDialogPath dialog) throws PayloadException, NetworkException {
        mSipInterface.sendSipBye(dialog);
    }

    /**
     * Send a SIP CANCEL
     * 
     * @param dialog Dialog path
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendSipCancel(SipDialogPath dialog) throws PayloadException, NetworkException {
        mSipInterface.sendSipCancel(dialog);
    }

    /**
     * Send a subsequent SIP request
     * 
     * @param dialog Dialog path
     * @param request Request
     * @return SipTransactionContext
     * @throws NetworkException
     * @throws PayloadException
     */
    public SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request)
            throws NetworkException, PayloadException {
        return sendSubsequentRequest(dialog, request, SipManager.sTimeout);
    }

    /**
     * Send a subsequent SIP request
     * 
     * @param dialog Dialog path
     * @param request Request
     * @param timeout SIP timeout in milliseconds
     * @return SipTransactionContext
     * @throws PayloadException
     * @throws NetworkException
     */
    private SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request,
            long timeout) throws NetworkException, PayloadException {
        SipTransactionContext ctx = mSipInterface.sendSubsequentRequest(dialog, request);
        ctx.waitResponse(timeout);
        if (ctx.isSipResponse()) {
            int code = ctx.getStatusCode();
            /* Check if not registered and warning header */
            WarningHeader warn = (WarningHeader) ctx.getSipResponse().getHeader(WarningHeader.NAME);
            if (Response.FORBIDDEN == code && warn == null) {
                mNetworkInterface.getRegistrationManager().restart();
                throw new PayloadException("Stack not properly registered with status code : "
                        + code);
            }
        }
        return ctx;
    }

    /**
     * Gets the timeout for SIP transaction (in milliseconds)
     * 
     * @return timeout for SIP transaction (in milliseconds)
     */
    public static long getTimeout() {
        return sTimeout;
    }

    /**
     * Sets the timeout for SIP transaction (in milliseconds)
     * 
     * @param timeout the timeout
     */
    public static void setTimeout(long timeout) {
        sTimeout = timeout;
    }

}
