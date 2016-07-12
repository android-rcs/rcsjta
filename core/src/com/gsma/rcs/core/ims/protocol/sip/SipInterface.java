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

package com.gsma.rcs.core.ims.protocol.sip;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext.INotifySipProvisionalResponse;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.InetAddressUtils;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;

import gov2.nist.javax2.sip.address.AddressImpl;
import gov2.nist.javax2.sip.message.SIPMessage;

import java.io.File;
import java.security.KeyStoreException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.Vector;

import javax2.sip.ClientTransaction;
import javax2.sip.DialogTerminatedEvent;
import javax2.sip.IOExceptionEvent;
import javax2.sip.InvalidArgumentException;
import javax2.sip.ListeningPoint;
import javax2.sip.ObjectInUseException;
import javax2.sip.RequestEvent;
import javax2.sip.ResponseEvent;
import javax2.sip.ServerTransaction;
import javax2.sip.SipException;
import javax2.sip.SipFactory;
import javax2.sip.SipListener;
import javax2.sip.SipProvider;
import javax2.sip.SipStack;
import javax2.sip.TimeoutEvent;
import javax2.sip.TransactionAlreadyExistsException;
import javax2.sip.TransactionTerminatedEvent;
import javax2.sip.TransactionUnavailableException;
import javax2.sip.address.Address;
import javax2.sip.address.SipURI;
import javax2.sip.address.URI;
import javax2.sip.header.ContactHeader;
import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.Header;
import javax2.sip.header.RouteHeader;
import javax2.sip.header.ViaHeader;
import javax2.sip.message.Request;
import javax2.sip.message.Response;

/**
 * SIP interface which manage the SIP stack. The NIST stack is used statefully (i.e. messages are
 * sent via a SIP transaction). NIST release is nist-sip-96f517a (2010-10-29)
 *
 * @author JM. Auffret
 */
public class SipInterface implements SipListener {

    private final static String TRACE_SEPARATOR = "-----------------------------------------------------------------------------";

    /**
     * SIP traces activation
     */
    private boolean mSipTraceEnabled;

    private final String mSipTraceFile;

    private final String mLocalIpAddress;

    private final String mOutboundProxyAddr;

    private int mOutboundProxyPort;

    private final Vector<String> mDefaultRoutePath;

    private final Vector<String> mServiceRoutePath;

    private final int mListeningPort;

    private final String mDefaultProtocol;

    /**
     * TCP fallback according to RFC3261 chapter 18.1.1
     */
    private final boolean mTcpFallback;

    /**
     * List of current SIP transactions
     */
    private final SipTransactionList mTransactions;

    private final List<SipEventListener> mListeners;

    private SipStack mSipStack;

    private SipProvider mDefaultSipProvider;

    private final List<SipProvider> mSipProviders;

    private final KeepAliveManager mKeepAliveManager;

    private String mPublicGruu;

    private String mTempGruu;

    private String mInstanceId;

    /**
     * Base timer T1 (in ms)
     */
    private long mTimerT1 = 500;

    /**
     * Base timer T2 (in ms)
     */
    private long mTimerT2 = 4000;

    /**
     * Base timer T4 (in ms)
     */
    private long mTimerT4 = 5000;

    private static final Logger sLogger = Logger.getLogger(SipInterface.class.getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     *
     * @param localIpAddress Local IP address
     * @param proxyAddr Outbound proxy address
     * @param proxyPort Outbound proxy port
     * @param defaultProtocol Default protocol
     * @param tcpFallback TCP fallback according to RFC3261 chapter 18.1.1
     * @param rcsSettings The RCS settings accessor
     */
    public SipInterface(String localIpAddress, String proxyAddr, int proxyPort,
            String defaultProtocol, boolean tcpFallback, RcsSettings rcsSettings) {
        mLocalIpAddress = localIpAddress;
        mDefaultProtocol = defaultProtocol;
        mTcpFallback = tcpFallback;
        mListeningPort = NetworkRessourceManager.generateLocalSipPort(rcsSettings);
        mOutboundProxyAddr = proxyAddr;
        mOutboundProxyPort = proxyPort;
        mKeepAliveManager = new KeepAliveManager(this, rcsSettings);
        mSipTraceEnabled = rcsSettings.isSipTraceActivated();
        mSipTraceFile = rcsSettings.getSipTraceFile();
        /* Set timers value from provisioning */
        mTimerT1 = rcsSettings.getSipTimerT1();
        mTimerT2 = rcsSettings.getSipTimerT2();
        mTimerT4 = rcsSettings.getSipTimerT4();
        /* Set the default route path */
        mDefaultRoutePath = new Vector<>();
        mDefaultRoutePath.addElement(getDefaultRoute());
        /* Set the default service route path */
        mServiceRoutePath = new Vector<>();
        mServiceRoutePath.addElement(getDefaultRoute());
        mRcsSettings = rcsSettings;
        mSipProviders = new ArrayList<>();
        mListeners = new ArrayList<>();
        mTransactions = new SipTransactionList();
    }

    /**
     * Initialize sip stack
     *
     * @throws PayloadException
     */
    public void initialize() throws PayloadException {
        try {
            /* Init SIP factories */
            SipFactory sipFactory = SipFactory.getInstance();
            SipUtils.HEADER_FACTORY = sipFactory.createHeaderFactory();
            SipUtils.ADDR_FACTORY = sipFactory.createAddressFactory();
            SipUtils.MSG_FACTORY = sipFactory.createMessageFactory();
            /* Set SIP stack properties */
            Properties properties = new Properties();
            properties.setProperty("javax2.sip.STACK_NAME", mLocalIpAddress);
            properties.setProperty("gov2.nist.javax2.sip.THREAD_POOL_SIZE", "1");
            properties.setProperty("javax2.sip.OUTBOUND_PROXY", getOutboundProxy());
            if (mSipTraceEnabled) {
                /* Activate SIP stack traces */
                boolean cleanLog = true;
                /* Remove previous log file */
                File fs = new File(mSipTraceFile);
                if (fs.exists()) {
                    cleanLog = fs.delete();
                }
                if (cleanLog) {
                    properties.setProperty("gov2.nist.javax2.sip.TRACE_LEVEL", "DEBUG");
                    properties.setProperty("gov2.nist.javax2.sip.SERVER_LOG", mSipTraceFile);
                    properties.setProperty("gov2.nist.javax2.sip.LOG_MESSAGE_CONTENT", "true");
                    properties.setProperty("gov2.nist.javax2.sip.LOG_STACK_TRACE_ON_MESSAGE_SEND",
                            "true");
                }
            }
            if (mDefaultProtocol.equals(ListeningPoint.TLS)) {
                /* Set SSL properties */
                properties.setProperty("gov2.nist.javax2.sip.TLS_CLIENT_PROTOCOLS", "SSLv3, TLSv1");
                if (KeyStoreManager.isOwnCertificateUsed(mRcsSettings)) {
                    properties.setProperty("javax2.net.ssl.keyStoreType",
                            KeyStoreManager.getKeystoreType());
                    String keyStorePath = KeyStoreManager.getKeystore().getPath();
                    properties.setProperty("javax2.net.ssl.keyStore", keyStorePath);
                    properties.setProperty("javax2.net.ssl.keyStorePassword",
                            KeyStoreManager.getKeystorePassword());
                    properties.setProperty("javax2.net.ssl.trustStore", keyStorePath);
                } else {
                    properties.setProperty("gov2.nist.javax2.sip.NETWORK_LAYER",
                            "gov2.nist.core.net.SslNetworkLayer");
                }
            }
            mSipStack = sipFactory.createSipStack(properties);
            ListeningPoint udp = mSipStack.createListeningPoint(mLocalIpAddress, mListeningPort,
                    ListeningPoint.UDP);
            SipProvider udpSipProvider = mSipStack.createSipProvider(udp);
            udpSipProvider.addSipListener(this);
            mSipProviders.add(udpSipProvider);
            /* Set the default SIP provider */
            switch (mDefaultProtocol) {
                case ListeningPoint.TLS:
                    ListeningPoint tls = mSipStack.createListeningPoint(mLocalIpAddress,
                            mListeningPort, ListeningPoint.TLS);
                    SipProvider tlsSipProvider = mSipStack.createSipProvider(tls);
                    tlsSipProvider.addSipListener(this);
                    mSipProviders.add(tlsSipProvider);
                    mDefaultSipProvider = tlsSipProvider;
                    break;

                case ListeningPoint.TCP: {
                    ListeningPoint tcp = mSipStack.createListeningPoint(mLocalIpAddress,
                            mListeningPort, ListeningPoint.TCP);
                    SipProvider tcpSipProvider = mSipStack.createSipProvider(tcp);
                    tcpSipProvider.addSipListener(this);
                    mSipProviders.add(tcpSipProvider);
                    mDefaultSipProvider = tcpSipProvider;
                    break;
                }
                default: {
                    ListeningPoint tcp = mSipStack.createListeningPoint(mLocalIpAddress,
                            mListeningPort, ListeningPoint.TCP);
                    if (!mTcpFallback) {
                        SipProvider tcpSipProvider = mSipStack.createSipProvider(tcp);
                        tcpSipProvider.addSipListener(this);
                        mSipProviders.add(tcpSipProvider);
                    }
                    mDefaultSipProvider = udpSipProvider;
                    if (mTcpFallback) {
                        /* prepare 2nd listening point for TCP fallback */
                        mDefaultSipProvider.addListeningPoint(tcp);
                    }
                    break;
                }
            }
            if (sLogger.isActivated()) {
                if (mDefaultProtocol.equals(ListeningPoint.UDP))
                    sLogger.debug("Default SIP provider is UDP (TCP fallback=" + mTcpFallback + ")");
                else
                    sLogger.debug("Default SIP provider is ".concat(mDefaultProtocol));
            }
            mSipStack.start();

        } catch (TooManyListenersException | SipException | KeyStoreException e) {
            throw new PayloadException("Unable to instantiate SIP stack for localIpAddress : "
                    + mLocalIpAddress + " with defaultProtocol : " + mDefaultProtocol, e);
        }
        if (sLogger.isActivated()) {
            sLogger.debug("SIP stack initialized at " + mLocalIpAddress + ":" + mListeningPort);
        }
    }

    private String getOutboundProxy() {
        if (InetAddressUtils.isIPv6Address(mOutboundProxyAddr)) {
            return "[" + mOutboundProxyAddr + "]" + ':' + mOutboundProxyPort + '/'
                    + mDefaultProtocol;
        }
        return mOutboundProxyAddr + ':' + mOutboundProxyPort + '/' + mDefaultProtocol;
    }

    /**
     * Close the SIP stack
     */
    public void close() {
        try {
            mKeepAliveManager.stop();
            mListeners.clear();
            for (SipProvider sipProvider : mSipProviders) {
                sipProvider.removeSipListener(this);
                sipProvider.removeListeningPoints();
                try {
                    mSipStack.deleteSipProvider(sipProvider);

                } catch (ObjectInUseException e) {
                    /* Nothing to be done here */
                    sLogger.error("SipProvider still has an associated SipListener!", e);
                }
            }
        } finally {
            if (mSipStack != null) {
                mSipStack.stop();
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("SIP stack is null");
                }
            }
            SipFactory.getInstance().resetFactory();
        }
    }

    /**
     * Return the default SIP provider
     *
     * @return SIP provider
     */
    public SipProvider getDefaultSipProvider() {
        return mDefaultSipProvider;
    }

    /**
     * Create a transaction; either default or fallback provider is used (depending on request size)
     *
     * @param request the SIP request
     * @return ClientTransaction
     * @throws ParseException
     * @throws SipException
     */
    private ClientTransaction createNewTransaction(SipRequest request) throws ParseException,
            SipException {
        // fall back to TCP if channel is UDP and request size exceeds the limit
        // according to RFC3261, chapter 18.1.1:
        // If a request is within 200 bytes of the path MTU, or if it is larger
        // than 1300 bytes and the path MTU is unknown, the request MUST be sent
        // using an RFC 2914 [43] congestion controlled transport protocol, such
        // as TCP. If this causes a change in the transport protocol from the
        // one indicated in the top Via, the value in the top Via MUST be
        // changed.
        if (ListeningPoint.UDP.equals(mDefaultProtocol) && mTcpFallback
                && (request.getStackMessage().toString().length() > (mSipStack.getMtuSize() - 200))) {
            if (sLogger.isActivated()) {
                sLogger.debug("Transaction falls back to TCP as request size is "
                        + request.getStackMessage().toString().length() + " and MTU size is "
                        + mSipStack.getMtuSize());
            }
            // Change Via header
            ViaHeader topViaHeader = ((SIPMessage) request.getStackMessage()).getTopmostViaHeader();
            if (topViaHeader != null) {
                topViaHeader.setTransport("TCP");
            } else {
                topViaHeader = SipUtils.HEADER_FACTORY.createViaHeader(mLocalIpAddress,
                        mListeningPort, "TCP", null);
            }
            request.getStackMessage().removeFirst(ViaHeader.NAME);
            request.getStackMessage().addFirst(topViaHeader);
            // Change Route header
            RouteHeader topRouteHeader = (RouteHeader) request.getStackMessage().getHeader(
                    RouteHeader.NAME);
            if (topRouteHeader != null) {
                URI uri = topRouteHeader.getAddress().getURI();
                if (uri.isSipURI()) {
                    SipURI sipUri = (SipURI) uri;
                    sipUri.setTransportParam("tcp");
                    AddressImpl address = new AddressImpl();
                    address.setURI(sipUri);
                    topRouteHeader = SipUtils.HEADER_FACTORY.createRouteHeader(address);
                } else {
                    // TODO
                    // check whether this could happen anyhow and if so whether this would be valid
                    sLogger.error("Update of route header due to TCP fallback failed due to wrong address format!");
                }
                request.getStackMessage().removeFirst(RouteHeader.NAME);
                request.getStackMessage().addFirst(topRouteHeader);
            }
        }
        ClientTransaction transaction = mDefaultSipProvider.getNewClientTransaction(request
                .getStackMessage());
        /* NOTE: External API limiting timers that should be in type 'long' to 'int'. */
        transaction.setRetransmitTimers((int) mTimerT1, (int) mTimerT2, (int) mTimerT4);
        return transaction;
    }

    /**
     * Returns the local IP address
     *
     * @return IP address
     */
    public String getLocalIpAddress() {
        return mLocalIpAddress;
    }

    /**
     * Returns the outbound proxy address
     *
     * @return Outbound proxy address
     */
    public String getOutboundProxyAddr() {
        return mOutboundProxyAddr;
    }

    /**
     * Returns the outbound proxy port
     *
     * @return Outbound proxy port
     */
    public int getOutboundProxyPort() {
        return mOutboundProxyPort;
    }

    /**
     * Returns the proxy protocol
     *
     * @return Outbound proxy protocol
     */
    public String getProxyProtocol() {
        return mDefaultProtocol;
    }

    /**
     * Returns the keep-alive manager
     *
     * @return Keep-alive manager
     */
    public KeepAliveManager getKeepAliveManager() {
        return mKeepAliveManager;
    }

    /**
     * Get public GRUU
     *
     * @return GRUU
     */
    public String getPublicGruu() {
        return mPublicGruu;
    }

    /**
     * Set public GRUU
     *
     * @param gruu GRUU
     */
    public void setPublicGruu(String gruu) {
        mPublicGruu = gruu;
    }

    /**
     * Set temporary GRUU
     *
     * @param gruu GRUU
     */
    public void setTemporaryGruu(String gruu) {
        mTempGruu = gruu;
    }

    /**
     * Get instance ID
     *
     * @return ID
     */
    public String getInstanceId() {
        return mInstanceId;
    }

    /**
     * Set instance ID
     *
     * @param id Instance ID
     */
    public void setInstanceId(String id) {
        mInstanceId = id;
    }

    /**
     * Returns the local via path
     *
     * @return List of headers
     * @throws PayloadException
     */
    public List<ViaHeader> getViaHeaders() throws PayloadException {
        try {
            List<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader via = SipUtils.HEADER_FACTORY.createViaHeader(mLocalIpAddress,
                    mListeningPort, getProxyProtocol(), null);
            viaHeaders.add(via);
            return viaHeaders;

        } catch (ParseException | InvalidArgumentException e) {
            throw new PayloadException("Can't create Via headers!", e);
        }
    }

    /**
     * Generate a unique call-ID
     *
     * @return Call-Id
     */
    public String generateCallId() {
        // Call-ID value follows RFC 3261, section 25.1
        return IdGenerator.getIdentifier() + "@" + mLocalIpAddress;
    }

    /**
     * Get local contact
     *
     * @return Header
     * @throws PayloadException
     */
    public ContactHeader getLocalContact() throws PayloadException {
        try {
            // Set the contact with the terminal IP address, port and transport
            SipURI contactURI = SipUtils.ADDR_FACTORY.createSipURI(null, mLocalIpAddress);
            contactURI.setPort(mListeningPort);
            contactURI.setParameter("transport", mDefaultProtocol);
            // Create the Contact header
            Address contactAddress = SipUtils.ADDR_FACTORY.createAddress(contactURI);
            return SipUtils.HEADER_FACTORY.createContactHeader(contactAddress);

        } catch (ParseException e) {
            throw new PayloadException("Unable to create SIP URI : " + mLocalIpAddress, e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Unable to set port : " + mListeningPort
                    + " for contact with ip address : " + mLocalIpAddress, e);
        }
    }

    /**
     * Get contact based on local contact info and multidevice infos (GRUU, sip.instance)
     *
     * @return Header
     * @throws PayloadException
     */
    public ContactHeader getContact() throws PayloadException {
        try {
            ContactHeader contactHeader;
            if (mPublicGruu != null) {
                // Create a contact with GRUU
                SipURI contactURI = SipUtils.ADDR_FACTORY.createSipURI(mPublicGruu);
                contactURI.setTransportParam(mDefaultProtocol);
                Address contactAddress = SipUtils.ADDR_FACTORY.createAddress(contactURI);
                contactHeader = SipUtils.HEADER_FACTORY.createContactHeader(contactAddress);

            } else if (mInstanceId != null) {
                // Create a local contact with an instance ID
                contactHeader = getLocalContact();
                contactHeader.setParameter(SipUtils.SIP_INSTANCE_PARAM, mInstanceId);
            } else {
                // Create a local contact
                contactHeader = getLocalContact();
            }
            return contactHeader;

        } catch (ParseException e) {
            throw new PayloadException("Unable to create SIP URI : " + mPublicGruu, e);
        }
    }

    /**
     * Returns the default route
     *
     * @return Route
     */
    public String getDefaultRoute() {
        String defaultRoute;
        if (InetAddressUtils.isIPv6Address(mOutboundProxyAddr)) {
            defaultRoute = String.format("<sip:[%s]:%s;transport=%s;lr>", mOutboundProxyAddr,
                    mOutboundProxyPort, getProxyProtocol());
        } else {
            defaultRoute = String.format("<sip:%s:%s;transport=%s;lr>", mOutboundProxyAddr,
                    mOutboundProxyPort, getProxyProtocol());
        }
        return defaultRoute.toLowerCase();
    }

    /**
     * Returns the default route path
     *
     * @return Route path
     */
    public Vector<String> getDefaultRoutePath() {
        return mDefaultRoutePath;
    }

    /**
     * Returns the service route path
     *
     * @return Route path
     */
    public Vector<String> getServiceRoutePath() {
        return mServiceRoutePath;
    }

    /**
     * Set the service route path
     *
     * @param routes List of routes
     */
    public void setServiceRoutePath(ListIterator<Header> routes) {
        mServiceRoutePath.clear();
        // Always add the outbound proxy
        mServiceRoutePath.addElement(getDefaultRoute());
        if (routes != null) {
            // Add the received service route path
            while (routes.hasNext()) {
                ExtensionHeader route = (ExtensionHeader) routes.next();
                String rt = route.getValue().toLowerCase();
                if (!mServiceRoutePath.contains(rt)) {
                    mServiceRoutePath.addElement(rt);
                }
            }
        }
    }

    /**
     * Add a SIP event listener
     *
     * @param listener Listener
     */
    public void addSipEventListener(SipEventListener listener) {
        mListeners.add(listener);
    }

    /**
     * Remove a transaction context from its ID
     *
     * @param id Transaction ID
     */
    public synchronized void removeTransactionContext(String id) {
        mTransactions.remove(id);
    }

    /**
     * Notify the transaction context that a message has been received (response or ACK)
     *
     * @param transactionId Transaction ID
     * @param msg SIP message
     */
    private void notifyTransactionContext(String transactionId, SipMessage msg) {
        SipTransactionContext ctx = mTransactions.get(transactionId);
        if (ctx != null) {
            removeTransactionContext(transactionId);
            ctx.responseReceived(msg);
        }
    }

    /**
     * Send a SIP message and create a context to wait a response
     *
     * @param message SIP message
     * @param callbackSipProvisionalResponse a callback to handle SIP provisional response
     * @return Transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message,
            INotifySipProvisionalResponse callbackSipProvisionalResponse) throws PayloadException,
            NetworkException {
        try {
            if (message instanceof SipRequest) {
                SipRequest req = (SipRequest) message;
                ClientTransaction transaction = (ClientTransaction) req.getStackTransaction();
                if (transaction == null) {
                    transaction = createNewTransaction(req);
                    req.setStackTransaction(transaction);
                }
                SipTransactionContext ctx = new SipTransactionContext(transaction,
                        callbackSipProvisionalResponse);
                String id = SipTransactionContext.getTransactionContextId(req);
                mTransactions.put(id, ctx);
                if (sLogger.isActivated()) {
                    sLogger.debug("Create a transaction context ".concat(id));
                }
                if (sLogger.isActivated()) {
                    sLogger.debug(">>> Send SIP ".concat(req.getMethod()));
                }
                if (mSipTraceEnabled) {
                    System.out.println(">>> " + req.getStackMessage().toString());
                    System.out.println(TRACE_SEPARATOR);
                }
                transaction.sendRequest();
                return ctx;
            }
            SipResponse resp = (SipResponse) message;
            ServerTransaction transaction = (ServerTransaction) resp.getStackTransaction();
            if (transaction == null) {
                throw new NetworkException("No transaction exist for " + resp.getCallId()
                        + ": the response can't be sent!");
            }
            SipTransactionContext ctx = new SipTransactionContext(transaction);
            String id = SipTransactionContext.getTransactionContextId(resp);
            mTransactions.put(id, ctx);
            if (sLogger.isActivated()) {
                sLogger.debug("Create a transaction context ".concat(id));
            }
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP " + resp.getStatusCode() + " response");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + resp.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            transaction.sendResponse(resp.getStackMessage());
            return ctx;

        } catch (ParseException e) {
            throw new PayloadException("Unable to instantiate SIP transaction!", e);

        } catch (SipException e) {
            throw new NetworkException("Can't send SIP message!", e);
        }
    }

    /**
     * Send a SIP message and create a context to wait a response
     *
     * @param message SIP message
     * @return Transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message) throws PayloadException,
            NetworkException {
        return sendSipMessageAndWait(message, null);
    }

    /**
     * Send a SIP response
     *
     * @param response SIP response
     * @throws NetworkException
     */
    public void sendSipResponse(SipResponse response) throws NetworkException {
        try {
            ServerTransaction transaction = (ServerTransaction) response.getStackTransaction();
            if (transaction == null) {
                throw new NetworkException("No transaction available for sending response!");
            }
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP " + response.getStatusCode() + " response");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + response.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            transaction.sendResponse(response.getStackMessage());

        } catch (SipException e) {
            throw new NetworkException("Can't send SIP message!", e);
        }
    }

    /**
     * Send a SIP ACK
     *
     * @param dialog Dialog path
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendSipAck(SipDialogPath dialog) throws PayloadException, NetworkException {
        try {
            SipRequest ack = SipMessageFactory.createAck(dialog);
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP ACK");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + ack.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            /* Re-use INVITE transaction */
            dialog.getStackDialog().sendAck(ack.getStackMessage());

        } catch (SipException e) {
            throw new NetworkException("Can't send SIP message!", e);
        }
    }

    /**
     * Send a SIP CANCEL
     *
     * @param dialog Dialog path
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendSipCancel(SipDialogPath dialog) throws PayloadException, NetworkException {
        try {
            if (dialog.getInvite().getStackTransaction() instanceof ServerTransaction) {
                /* Server transaction can't send a cancel */
                return;
            }
            SipRequest cancel = SipMessageFactory.createCancel(dialog);
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            if (agent != null) {
                agent.setProxyAuthorizationHeader(cancel);
            }
            ClientTransaction transaction = createNewTransaction(cancel);
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP CANCEL");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + cancel.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            transaction.sendRequest();

        } catch (ParseException e) {
            throw new PayloadException("Unable to instantiate SIP transaction!", e);

        } catch (SipException e) {
            throw new NetworkException("Can't send SIP message!", e);
        }
    }

    /**
     * Send a SIP BYE
     *
     * @param dialog Dialog path
     * @throws PayloadException
     * @throws NetworkException
     */
    public void sendSipBye(SipDialogPath dialog) throws PayloadException, NetworkException {
        boolean loggerActivated = sLogger.isActivated();
        try {
            SipRequest bye = SipMessageFactory.createBye(dialog);
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            if (agent != null) {
                agent.setProxyAuthorizationHeader(bye);
            }
            ClientTransaction transaction = createNewTransaction(bye);
            if (loggerActivated) {
                sLogger.debug(">>> Send SIP BYE");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + bye.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            dialog.getStackDialog().sendRequest(transaction);

        } catch (ParseException e) {
            throw new PayloadException("Unable to instantiate SIP transaction!", e);

        } catch (SipException e) {
            throw new NetworkException("Can't send SIP message!", e);
        }
    }

    /**
     * Send a subsequent SIP request and create a context to wait a response
     *
     * @param dialog Dialog path
     * @param request Request
     * @return SipTransactionContext
     * @throws PayloadException
     * @throws NetworkException
     */
    public SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request)
            throws PayloadException, NetworkException {
        boolean loggerActivated = sLogger.isActivated();
        try {
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            if (agent != null) {
                agent.setProxyAuthorizationHeader(request);
            }
            ClientTransaction transaction = createNewTransaction(request);
            if (loggerActivated) {
                sLogger.debug(">>> Send SIP ".concat(request.getMethod().toUpperCase()));
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + request.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            dialog.getStackDialog().sendRequest(transaction);
            SipTransactionContext ctx = new SipTransactionContext(transaction);
            String id = SipTransactionContext.getTransactionContextId(request);
            mTransactions.put(id, ctx);
            return ctx;

        } catch (ParseException e) {
            throw new PayloadException("Unable to instantiate SIP transaction!", e);

        } catch (SipException e) {
            throw new NetworkException("Can't send SIP message!", e);
        }
    }

    /**
     * Process an asynchronously reported DialogTerminatedEvent
     *
     * @param dialogTerminatedEvent Event
     */
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        if (sLogger.isActivated()) {
            sLogger.debug("Dialog terminated");
        }
    }

    /**
     * Process an asynchronously reported IO Exception
     *
     * @param exceptionEvent Event
     */
    public void processIOException(IOExceptionEvent exceptionEvent) {
        if (sLogger.isActivated()) {
            sLogger.debug("IO Exception on " + exceptionEvent.getTransport() + " transport");
        }
    }

    /**
     * Processes a Request received on a SipProvider upon which this SipListener is registered.
     *
     * @param requestEvent Event
     */
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.debug("<<< Receive SIP " + request.getMethod());
        }
        if (mSipTraceEnabled) {
            System.out.println("<<< " + request.toString());
            System.out.println(TRACE_SEPARATOR);
        }
        try {
            // Get transaction
            ServerTransaction transaction = requestEvent.getServerTransaction();
            if (transaction == null) {
                // Create a transaction for this new incoming request
                SipProvider srcSipProvider = (SipProvider) requestEvent.getSource();
                transaction = srcSipProvider.getNewServerTransaction(request);
            }
            // Create received request with its associated transaction
            SipRequest req = new SipRequest(request);
            req.setStackTransaction(transaction);
            if (Request.ACK.equals(req.getMethod())) {
                // Search the context associated to the received ACK and notify it
                String transactionId = SipTransactionContext.getTransactionContextId(req);
                notifyTransactionContext(transactionId, req);
                return;
            }
            // Notify event listeners
            for (SipEventListener listener : mListeners) {
                listener.receiveSipRequest(req);
            }
        } catch (TransactionAlreadyExistsException | TransactionUnavailableException e) {
            /**
             * Intentionally consuming this exception as no need to create a new transaction in case
             * it already exists.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
    }

    /**
     * Processes a Response received on a SipProvider upon which this SipListener is registered
     *
     * @param responseEvent Event
     */
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        int responseStatusCode = response.getStatusCode();
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.debug("<<< Receive SIP " + responseStatusCode + " response");
        }
        if (mSipTraceEnabled) {
            System.out.println("<<< " + response.toString());
            System.out.println(TRACE_SEPARATOR);
        }
        // Search transaction
        ClientTransaction transaction = responseEvent.getClientTransaction();
        if (transaction == null) {
            if (loggerActivated) {
                sLogger.debug("No transaction exist for this response: by-pass it");
            }
            return;
        }
        // Create received response with its associated transaction
        SipResponse resp = new SipResponse(response);
        resp.setStackTransaction(transaction);
        // Search the context associated to the received response and notify it
        String transactionId = SipTransactionContext.getTransactionContextId(resp);
        if (Response.OK <= responseStatusCode) {
            notifyTransactionContext(transactionId, resp);
        } else {
            // Is the response provisional ?
            if (Response.TRYING <= responseStatusCode) {
                notifyProvisionalResponse(transactionId, resp);
            }
        }
    }

    /**
     * Processes a retransmit or expiration Timeout of an underlying Transaction handled by this
     * SipListener
     *
     * @param timeoutEvent Event
     */
    public void processTimeout(TimeoutEvent timeoutEvent) {
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.debug("Transaction timeout " + timeoutEvent.getTimeout().toString());
        }
        if (timeoutEvent.isServerTransaction()) {
            if (loggerActivated) {
                sLogger.warn("Unexpected timeout for a server transaction: should never arrives");
            }
            return;
        }
        ClientTransaction transaction = timeoutEvent.getClientTransaction();
        if (transaction == null) {
            if (loggerActivated) {
                sLogger.debug("No transaction exist for this transaction: by-pass it");
            }
            return;
        }
        // Search the context associated to the received timeout and notify it
        String transactionId = SipTransactionContext.getTransactionContextId(transaction
                .getRequest());
        notifyTransactionContext(transactionId, null);
    }

    /**
     * Process an asynchronously reported TransactionTerminatedEvent
     *
     * @param transactionTerminatedEvent Event
     */
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        // if (sLogger.isActivated()) {
        // sLogger.debug("Transaction terminated");
        // }
    }

    /**
     * Notify provisional SIP response
     *
     * @param transactionId Transaction ID
     * @param response SIP response
     */
    private void notifyProvisionalResponse(String transactionId, SipResponse response) {
        SipTransactionContext ctx = mTransactions.get(transactionId);
        if (ctx == null) {
            return;
        }
        boolean loggerActivated = sLogger.isActivated();
        if (loggerActivated) {
            sLogger.debug("Callback object found for transaction " + transactionId);
        }
        INotifySipProvisionalResponse callback = ctx.getCallbackSipProvisionalResponse();
        // Only consider ringing event
        if (callback != null && Response.RINGING == response.getStatusCode()) {
            callback.handle180Ringing(response);
        } else {
            if (loggerActivated) {
                sLogger.debug("By pass provisional response");
            }
        }
    }

}
