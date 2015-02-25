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

package com.gsma.rcs.core.ims.network.sip;

import java.util.ListIterator;

import javax2.sip.header.ViaHeader;
import javax2.sip.header.WarningHeader;
import javax2.sip.message.Request;
import javax2.sip.message.Response;

import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.protocol.sip.KeepAliveManager;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipInterface;
import com.gsma.rcs.core.ims.protocol.sip.SipMessage;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * SIP manager
 * 
 * @author JM. Auffret
 */
public class SipManager {

    /**
     * SIP timeout for SIP transaction (in seconds)
     */
    public static int TIMEOUT = 30;

    /**
     * IMS network interface
     */
    private ImsNetworkInterface mNetworkInterface;

    /**
     * SIP stack
     */
    private SipInterface sipstack;

    private final RcsSettings mRcsSettings;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(SipManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS network interface
     * @param rcsSettings
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
        return sipstack;
    }

    /**
     * Terminate the manager
     */
    public void terminate() {
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the SIP manager");
        }

        // Close the SIP stack
        if (sipstack != null) {
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
     * @param protocol
     * @param tcpFallback TCP fallback according to RFC3261 chapter 18.1.1
     * @param networkType type of network
     * @throws SipException
     */
    public synchronized void initStack(String localAddr, String proxyAddr, int proxyPort,
            String protocol, boolean tcpFallback, int networkType) throws SipException {
        // Close the stack if necessary
        closeStack();

        // Create the SIP stack
        sipstack = new SipInterface(localAddr, proxyAddr, proxyPort, protocol, tcpFallback,
                networkType, mRcsSettings);
    }

    /**
     * Close the SIP stack
     */
    public synchronized void closeStack() {
        if (sipstack == null) {
            // Already closed
            return;
        }

        try {
            // Close the SIP stack
            sipstack.close();
            sipstack = null;
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't close SIP stack properly", e);
            }
        }
    }

    /**
     * Send a SIP message and create a context to wait a response
     * 
     * @param message SIP message
     * @return Transaction context
     * @throws SipException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message) throws SipException {
        return sendSipMessageAndWait(message, SipManager.TIMEOUT);
    }

    /**
     * Send a SIP message and create a context to wait for response
     * 
     * @param message
     * @param timeout
     * @param callback callback to handle provisional response
     * @return SIP transaction context
     * @throws SipException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message, int timeout,
            SipTransactionContext.INotifySipProvisionalResponse callback) throws SipException {
        if (sipstack == null) {
            throw new SipException("Stack not initialized");
        }
        SipTransactionContext ctx = sipstack.sendSipMessageAndWait(message, callback);

        // wait the response
        ctx.waitResponse(timeout);

        if (!(message instanceof SipRequest) || !ctx.isSipResponse()) {
            // Return the transaction context
            return ctx;

        }
        String method = ((SipRequest) message).getMethod();
        SipResponse response = ctx.getSipResponse();
        if (response == null) {
            return ctx;

        }
        // Analyze the received response
        if (!Request.REGISTER.equals(method)) {
            // Check if not registered and warning header
            WarningHeader warn = (WarningHeader) response.getHeader(WarningHeader.NAME);
            if (Response.FORBIDDEN == ctx.getStatusCode() && warn == null) {
                // Launch new registration
                mNetworkInterface.getRegistrationManager().restart();

                if (callback == null) {
                    throw new SipException("Not registered");

                }
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

        // Message is a response to INVITE or REGISTER: analyze "keep" flag of "Via" header
        int viaKeep = -1;
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
            viaKeep = Integer.parseInt(keepStr);
            if (viaKeep > 0) {
                // If "keep" value is valid, set keep alive period
                keepAliveManager.setPeriod(viaKeep);
            } else {
                if (sLogger.isActivated())
                    sLogger.warn("Non positive keep value \"" + keepStr + "\"");
            }
        } catch (NumberFormatException e) {
            if (sLogger.isActivated())
                sLogger.warn("Non-numeric keep value \"" + keepStr + "\"");
        }
        // If "keep" value is invalid or not present, set keep alive period to default value
        if (viaKeep <= 0) {
            keepAliveManager.setPeriod(mRcsSettings.getSipKeepAlivePeriod());
        }

        // Return the transaction context
        return ctx;
    }

    /**
     * Send a SIP message and create a context to wait a response
     * 
     * @param message SIP message
     * @param timeout SIP timeout
     * @return Transaction context
     * @throws SipException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message, int timeout)
            throws SipException {
        return sendSipMessageAndWait(message, timeout, null);
    }

    /**
     * Send a SIP response
     * 
     * @param response SIP response
     * @throws SipException
     */
    public void sendSipResponse(SipResponse response) throws SipException {
        if (sipstack != null) {
            sipstack.sendSipResponse(response);
        } else {
            throw new SipException("Stack not initialized");
        }
    }

    /**
     * Send a SIP ACK
     * 
     * @param dialog Dialog path
     * @throws SipException
     */
    public void sendSipAck(SipDialogPath dialog) throws SipException {
        if (sipstack != null) {
            sipstack.sendSipAck(dialog);
        } else {
            throw new SipException("Stack not initialized");
        }
    }

    /**
     * Send a SIP BYE
     * 
     * @param dialog Dialog path
     * @throws SipException
     */
    public void sendSipBye(SipDialogPath dialog) throws SipException {
        if (sipstack != null) {
            sipstack.sendSipBye(dialog);
        } else {
            throw new SipException("Stack not initialized");
        }
    }

    /**
     * Send a SIP CANCEL
     * 
     * @param dialog Dialog path
     * @throws SipException
     */
    public void sendSipCancel(SipDialogPath dialog) throws SipException {
        if (sipstack != null) {
            sipstack.sendSipCancel(dialog);
        } else {
            throw new SipException("Stack not initialized");
        }
    }

    /**
     * Send a subsequent SIP request
     * 
     * @param dialog Dialog path
     * @param request Request
     * @return SipTransactionContext
     * @throws SipException
     */
    public SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request)
            throws SipException {
        return sendSubsequentRequest(dialog, request, SipManager.TIMEOUT);
    }

    /**
     * Send a subsequent SIP request
     * 
     * @param dialog Dialog path
     * @param request Request
     * @param timeout SIP timeout
     * @return SipTransactionContext
     * @throws SipException
     */
    public SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request,
            int timeout) throws SipException {
        if (sipstack != null) {
            SipTransactionContext ctx = sipstack.sendSubsequentRequest(dialog, request);

            // wait the response
            ctx.waitResponse(timeout);

            // Analyze the received response
            if (ctx.isSipResponse()) {
                int code = ctx.getStatusCode();
                // Check if not registered and warning header
                WarningHeader warn = (WarningHeader) ctx.getSipResponse().getHeader(
                        WarningHeader.NAME);
                if ((code == 403) && (warn == null)) {
                    // Launch new registration
                    mNetworkInterface.getRegistrationManager().restart();

                    // Throw not registered exception
                    throw new SipException("Not registered");
                }
            }
            return ctx;
        } else {
            throw new SipException("Stack not initialized");
        }
    }
}
