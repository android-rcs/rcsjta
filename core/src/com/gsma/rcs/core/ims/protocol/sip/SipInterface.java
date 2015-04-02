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

package com.gsma.rcs.core.ims.protocol.sip;

import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext.INotifySipProvisionalResponse;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.IpAddressUtils;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.logger.Logger;

import android.net.ConnectivityManager;

import gov2.nist.javax2.sip.address.AddressImpl;
import gov2.nist.javax2.sip.message.SIPMessage;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Vector;

import javax2.sip.ClientTransaction;
import javax2.sip.DialogTerminatedEvent;
import javax2.sip.IOExceptionEvent;
import javax2.sip.InvalidArgumentException;
import javax2.sip.ListeningPoint;
import javax2.sip.RequestEvent;
import javax2.sip.ResponseEvent;
import javax2.sip.ServerTransaction;
import javax2.sip.SipFactory;
import javax2.sip.SipListener;
import javax2.sip.SipProvider;
import javax2.sip.SipStack;
import javax2.sip.TimeoutEvent;
import javax2.sip.TransactionTerminatedEvent;
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
    /**
     * Trace separator
     */
    private final static String TRACE_SEPARATOR = "-----------------------------------------------------------------------------";

    /**
     * Default SIP port
     */
    public final static int DEFAULT_SIP_PORT = 5062;

    /**
     * SIP traces activation
     */
    private boolean mSipTraceEnabled;

    /**
     * SIP traces filename
     */
    private String mSipTraceFile;

    /**
     * Local IP address
     */
    private String mLocalIpAddress;

    /**
     * Outbound proxy address
     */
    private String mOutboundProxyAddr;

    /**
     * Outbound proxy port
     */
    private int mOutboundProxyPort;

    /**
     * Default route path
     */
    private Vector<String> mDefaultRoutePath;

    /**
     * Service route path
     */
    private Vector<String> mServiceRoutePath;

    /**
     * SIP listening port
     */
    private int mListeningPort;

    /**
     * SIP default protocol
     */
    private String mDefaultProtocol;

    /*
     * TCP fallback according to RFC3261 chapter 18.1.1
     */
    private boolean mTcpFallback;

    /**
     * List of current SIP transactions
     */
    private SipTransactionList mTransactions = new SipTransactionList();

    /**
     * SIP interface listeners
     */
    private Vector<SipEventListener> mListeners = new Vector<SipEventListener>();

    /**
     * SIP stack
     */
    private SipStack mSipStack;

    /**
     * Default SIP stack provider
     */
    private SipProvider mDefaultSipProvider;

    /**
     * SIP stack providers
     */
    private Vector<SipProvider> mSipProviders = new Vector<SipProvider>();

    /**
     * Keep-alive manager
     */
    private KeepAliveManager mKeepAliveManager;

    /**
     * Public GRUU
     */
    private String mPublicGruu;

    /**
     * Temporary GRUU
     */
    private String mTempGruu;

    /**
     * Instance ID
     */
    private String mInstanceId;

    /**
     * Base timer T1 (in ms)
     */
    private int mTimerT1 = 500;

    /**
     * Base timer T2 (in ms)
     */
    private int mTimerT2 = 4000;

    /**
     * Base timer T4 (in ms)
     */
    private int mTimerT4 = 5000;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(SipInterface.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param localIpAddress Local IP address
     * @param proxyAddr Outbound proxy address
     * @param proxyPort Outbound proxy port
     * @param defaultProtocol Default protocol
     * @param tcpFallback TCP fallback according to RFC3261 chapter 18.1.1
     * @param networkType Type of network
     * @param rcsSettings
     * @throws SipException
     */
    public SipInterface(String localIpAddress, String proxyAddr, int proxyPort,
            String defaultProtocol, boolean tcpFallback, int networkType, RcsSettings rcsSettings)
            throws SipException {
        mLocalIpAddress = localIpAddress;
        mDefaultProtocol = defaultProtocol;
        mTcpFallback = tcpFallback;
        mListeningPort = NetworkRessourceManager.generateLocalSipPort(rcsSettings);
        mOutboundProxyAddr = proxyAddr;
        mOutboundProxyPort = proxyPort;

        mKeepAliveManager = new KeepAliveManager(this, rcsSettings);
        mSipTraceEnabled = rcsSettings.isSipTraceActivated();
        mSipTraceFile = rcsSettings.getSipTraceFile();

        // Set timers value from provisioning for 3G or default for Wifi
        if (networkType == ConnectivityManager.TYPE_MOBILE) {
            mTimerT1 = rcsSettings.getSipTimerT1();
            mTimerT2 = rcsSettings.getSipTimerT2();
            mTimerT4 = rcsSettings.getSipTimerT4();
        }

        // Set the default route path
        mDefaultRoutePath = new Vector<String>();
        mDefaultRoutePath.addElement(getDefaultRoute());

        // Set the default service route path
        mServiceRoutePath = new Vector<String>();
        mServiceRoutePath.addElement(getDefaultRoute());

        try {
            // Init SIP factories
            SipFactory sipFactory = SipFactory.getInstance();
            SipUtils.HEADER_FACTORY = sipFactory.createHeaderFactory();
            SipUtils.ADDR_FACTORY = sipFactory.createAddressFactory();
            SipUtils.MSG_FACTORY = sipFactory.createMessageFactory();

            // Set SIP stack properties
            Properties properties = new Properties();
            properties.setProperty("javax2.sip.STACK_NAME", localIpAddress);
            properties.setProperty("gov2.nist.javax2.sip.THREAD_POOL_SIZE", "1");
            final String outboundProxy = new StringBuilder().append(mOutboundProxyAddr).append(':')
                    .append(mOutboundProxyPort).append('/').append(defaultProtocol).toString();
            properties.setProperty("javax2.sip.OUTBOUND_PROXY", outboundProxy);
            if (mSipTraceEnabled) {
                // Activate SIP stack traces
                boolean cleanLog = true;

                // Remove previous log file
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
            if (defaultProtocol.equals(ListeningPoint.TLS)) {
                // Set SSL properties
                properties.setProperty("gov2.nist.javax2.sip.TLS_CLIENT_PROTOCOLS", "SSLv3, TLSv1");
                if (KeyStoreManager.isOwnCertificateUsed(rcsSettings)) {
                    properties.setProperty("javax2.net.ssl.keyStoreType",
                            KeyStoreManager.getKeystoreType());
                    properties.setProperty("javax2.net.ssl.keyStore",
                            KeyStoreManager.getKeystorePath());
                    properties.setProperty("javax2.net.ssl.keyStorePassword",
                            KeyStoreManager.getKeystorePassword());
                    properties.setProperty("javax2.net.ssl.trustStore",
                            KeyStoreManager.getKeystorePath());
                } else {
                    properties.setProperty("gov2.nist.javax2.sip.NETWORK_LAYER",
                            "gov2.nist.core.net.SslNetworkLayer");
                }
            }

            // Create the SIP stack
            mSipStack = sipFactory.createSipStack(properties);

            // Create UDP provider
            ListeningPoint udp = mSipStack.createListeningPoint(localIpAddress, mListeningPort,
                    ListeningPoint.UDP);
            SipProvider udpSipProvider = mSipStack.createSipProvider(udp);
            udpSipProvider.addSipListener(this);
            mSipProviders.addElement(udpSipProvider);

            // Set the default SIP provider
            if (defaultProtocol.equals(ListeningPoint.TLS)) {
                // Create TLS provider
                ListeningPoint tls = mSipStack.createListeningPoint(localIpAddress, mListeningPort,
                        ListeningPoint.TLS);
                SipProvider tlsSipProvider = mSipStack.createSipProvider(tls);
                tlsSipProvider.addSipListener(this);
                mSipProviders.addElement(tlsSipProvider);

                // TLS protocol used by default
                mDefaultSipProvider = tlsSipProvider;
            } else if (defaultProtocol.equals(ListeningPoint.TCP)) {
                // Create TCP provider
                ListeningPoint tcp = mSipStack.createListeningPoint(localIpAddress, mListeningPort,
                        ListeningPoint.TCP);
                SipProvider tcpSipProvider = mSipStack.createSipProvider(tcp);
                tcpSipProvider.addSipListener(this);
                mSipProviders.addElement(tcpSipProvider);

                // TCP protocol used by default
                mDefaultSipProvider = tcpSipProvider;
            } else {
                // Create TCP provider
                ListeningPoint tcp = mSipStack.createListeningPoint(localIpAddress, mListeningPort,
                        ListeningPoint.TCP);
                // Changed by Deutsche Telekom
                if (this.mTcpFallback == false) {
                    SipProvider tcpSipProvider = mSipStack.createSipProvider(tcp);
                    tcpSipProvider.addSipListener(this);
                    mSipProviders.addElement(tcpSipProvider);
                }

                // UDP protocol used by default
                mDefaultSipProvider = udpSipProvider;

                // Changed by Deutsche Telekom
                if (this.mTcpFallback) {
                    // prepare 2nd listening point for TCP fallback
                    mDefaultSipProvider.addListeningPoint(tcp);
                }
            }

            if (sLogger.isActivated()) {
                if (defaultProtocol.equals(ListeningPoint.UDP))
                    sLogger.debug("Default SIP provider is UDP (TCP fallback=" + this.mTcpFallback
                            + ")");
                else
                    sLogger.debug("Default SIP provider is " + defaultProtocol);
            }

            // Start the stack
            mSipStack.start();

        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("SIP stack initialization has failed", e);
            }

            // discard unusable stack
            close();

            throw new SipException("Can't create the SIP stack");
        }

        if (sLogger.isActivated()) {
            sLogger.debug("SIP stack started at " + localIpAddress + ":" + mListeningPort);
        }
    }

    /**
     * Close the SIP stack
     */
    public void close() {
        try {
            // Stop keep alive
            mKeepAliveManager.stop();

            // Remove all application listeners
            mListeners.removeAllElements();

            // Delete SIP providers
            for (int i = 0; i < mSipProviders.size(); i++) {
                SipProvider sipProvider = (SipProvider) mSipProviders.elementAt(i);
                sipProvider.removeSipListener(this);
                sipProvider.removeListeningPoints();
                mSipStack.deleteSipProvider(sipProvider);
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't cleanup SIP stack correctly", e);
            }
        } finally {
            // Stop the stack
            try {
                if (mSipStack != null) {
                    mSipStack.stop();
                } else {
                    if (sLogger.isActivated()) {
                        sLogger.debug("SIP stack is null");
                    }
                }
                SipFactory.getInstance().resetFactory();
            } catch (Exception e) {
                if (sLogger.isActivated()) {
                    sLogger.error("Can't stop SIP stack correctly", e);
                }
            }
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

    // Changed by Deutsche Telekom
    /**
     * Create a transaction; either default or fallback provider is used (depending on request size)
     * 
     * @param request
     * @return
     * @throws ParseException
     * @throws javax2.sip.SipException
     * @throws NullPointerException
     */
    private ClientTransaction createNewTransaction(SipRequest request) throws ParseException,
            NullPointerException, javax2.sip.SipException {
        // fall back to TCP if channel is UDP and request size exceeds the limit
        // according to RFC3261, chapter 18.1.1:
        // If a request is within 200 bytes of the path MTU, or if it is larger
        // than 1300 bytes and the path MTU is unknown, the request MUST be sent
        // using an RFC 2914 [43] congestion controlled transport protocol, such
        // as TCP. If this causes a change in the transport protocol from the
        // one indicated in the top Via, the value in the top Via MUST be
        // changed.
        if (ListeningPoint.UDP.equals(mDefaultProtocol) && this.mTcpFallback
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
        transaction.setRetransmitTimers(mTimerT1, mTimerT2, mTimerT4);
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
     * Returns the listening port
     * 
     * @return Port number
     */
    public int getListeningPort() {
        return mListeningPort;
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
        this.mPublicGruu = gruu;
    }

    /**
     * Get temporary GRUU
     * 
     * @return GRUU
     */
    public String getTemporaryGruu() {
        return mTempGruu;
    }

    /**
     * Set temporary GRUU
     * 
     * @param gruu GRUU
     */
    public void setTemporaryGruu(String gruu) {
        this.mTempGruu = gruu;
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
        this.mInstanceId = id;
    }

    /**
     * Returns the local via path
     *
     * @return List of headers
     * @throws SipException
     */
    public ArrayList<ViaHeader> getViaHeaders() throws SipException {
        try {
            ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
            ViaHeader via = SipUtils.HEADER_FACTORY.createViaHeader(mLocalIpAddress,
                    mListeningPort, getProxyProtocol(), null);
            viaHeaders.add(via);
            return viaHeaders;

        } catch (ParseException e) {
            throw new SipException("Can't create Via headers!", e);

        } catch (InvalidArgumentException e) {
            throw new SipException("Can't create Via headers!", e);
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
     * @throws SipException
     */
    public ContactHeader getLocalContact() throws SipException {
        try {
            // Set the contact with the terminal IP address, port and transport
            SipURI contactURI = (SipURI) SipUtils.ADDR_FACTORY.createSipURI(null, mLocalIpAddress);
            contactURI.setPort(mListeningPort);
            contactURI.setParameter("transport", mDefaultProtocol);

            // Create the Contact header
            Address contactAddress = SipUtils.ADDR_FACTORY.createAddress(contactURI);
            ContactHeader contactHeader = SipUtils.HEADER_FACTORY
                    .createContactHeader(contactAddress);

            return contactHeader;

        } catch (ParseException e) {
            throw new SipException("Can't create local contact!", e);

        } catch (InvalidArgumentException e) {
            throw new SipException("Can't create local contact!", e);
        }
    }

    /**
     * Get contact based on local contact info and multidevice infos (GRUU, sip.instance)
     * 
     * @return Header
     * @throws ParseException
     * @throws SipException
     */
    public ContactHeader getContact() throws ParseException, SipException {
        ContactHeader contactHeader;
        if (mPublicGruu != null) {
            // Create a contact with GRUU
            SipURI contactURI = (SipURI) SipUtils.ADDR_FACTORY.createSipURI(mPublicGruu);
            // Changed by Deutsche Telekom
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
    }

    /**
     * Returns the default route
     * 
     * @return Route
     */
    public String getDefaultRoute() {
        String defaultRoute;
        if (IpAddressUtils.isIPv6(mOutboundProxyAddr)) {
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
        if (sLogger.isActivated()) {
            sLogger.debug("Add a SIP listener");
        }
        mListeners.addElement(listener);
    }

    /**
     * Remove a SIP event listener
     * 
     * @param listener Listener
     */
    public void removeSipEventListener(SipEventListener listener) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a SIP listener");
        }
        mListeners.removeElement(listener);
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
     * @param id Transaction ID
     * @param msg SIP message
     */
    private void notifyTransactionContext(String transactionId, SipMessage msg) {
        SipTransactionContext ctx = (SipTransactionContext) mTransactions.get(transactionId);
        if (ctx != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Callback object found for transaction " + transactionId);
            }
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
     * @throws SipException
     */
    public SipTransactionContext sendSipMessageAndWait(SipMessage message,
            INotifySipProvisionalResponse callbackSipProvisionalResponse) throws SipException {
        try {
            if (message instanceof SipRequest) {
                // Send a request
                SipRequest req = (SipRequest) message;

                // Get stack transaction
                ClientTransaction transaction = (ClientTransaction) req.getStackTransaction();
                if (transaction == null) {
                    // Create a new transaction
                    // Changed by Deutsche Telekom
                    transaction = createNewTransaction(req);
                    req.setStackTransaction(transaction);
                }

                // Create a transaction context
                SipTransactionContext ctx = new SipTransactionContext(transaction,
                        callbackSipProvisionalResponse);
                String id = SipTransactionContext.getTransactionContextId(req);
                mTransactions.put(id, ctx);
                if (sLogger.isActivated()) {
                    sLogger.debug("Create a transaction context " + id);
                }

                // Send the SIP message to the network
                if (sLogger.isActivated()) {
                    sLogger.debug(">>> Send SIP " + req.getMethod());
                }
                if (mSipTraceEnabled) {
                    System.out.println(">>> " + req.getStackMessage().toString());
                    System.out.println(TRACE_SEPARATOR);
                }
                transaction.sendRequest();

                // Returns the created transaction to wait synchronously the response
                return ctx;
            } else {
                // Send a response
                SipResponse resp = (SipResponse) message;

                // Get stack transaction
                ServerTransaction transaction = (ServerTransaction) resp.getStackTransaction();
                if (transaction == null) {
                    // No transaction exist
                    if (sLogger.isActivated()) {
                        sLogger.warn("No transaction exist for " + resp.getCallId()
                                + ": the response can't be sent");
                    }
                    return null;
                }

                // Create a transaction context
                SipTransactionContext ctx = new SipTransactionContext(transaction);
                String id = SipTransactionContext.getTransactionContextId(resp);
                mTransactions.put(id, ctx);
                if (sLogger.isActivated()) {
                    sLogger.debug("Create a transaction context " + id);
                }

                // Send the SIP message to the network
                if (sLogger.isActivated()) {
                    sLogger.debug(">>> Send SIP " + resp.getStatusCode() + " response");
                }
                if (mSipTraceEnabled) {
                    System.out.println(">>> " + resp.getStackMessage().toString());
                    System.out.println(TRACE_SEPARATOR);
                }
                transaction.sendResponse(resp.getStackMessage());

                // Returns the created transaction to wait synchronously the response
                return ctx;
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
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
        return sendSipMessageAndWait(message, null);
    }

    /**
     * Send a SIP response
     * 
     * @param response SIP response
     * @throws SipException
     */
    public void sendSipResponse(SipResponse response) throws SipException {
        try {
            // Get stack transaction
            ServerTransaction transaction = (ServerTransaction) response.getStackTransaction();
            if (transaction == null) {
                // No transaction exist
                if (sLogger.isActivated()) {
                    sLogger.warn("No transaction exist for " + response.getCallId()
                            + ": the response can't be sent");
                }
                throw new SipException("No transaction found");
            }

            // Send the SIP message to the network
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP " + response.getStatusCode() + " response");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + response.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            transaction.sendResponse(response.getStackMessage());
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
        }
    }

    /**
     * Send a SIP ACK
     * 
     * @param dialog Dialog path
     * @throws SipException
     */
    public void sendSipAck(SipDialogPath dialog) throws SipException {
        try {
            // Create the SIP request
            SipRequest ack = SipMessageFactory.createAck(dialog);

            // Send the SIP message to the network
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP ACK");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + ack.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }

            // Re-use INVITE transaction
            dialog.getStackDialog().sendAck(ack.getStackMessage());
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
        }
    }

    /**
     * Send a SIP CANCEL
     * 
     * @param dialog Dialog path
     * @throws SipException
     */
    public void sendSipCancel(SipDialogPath dialog) throws SipException {
        try {
            if (dialog.getInvite().getStackTransaction() instanceof ServerTransaction) {
                // Server transaction can't send a cancel
                return;
            }

            // Create the SIP request
            SipRequest cancel = SipMessageFactory.createCancel(dialog);

            // Set the Proxy-Authorization header
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            if (agent != null) {
                agent.setProxyAuthorizationHeader(cancel);
            }

            // Create a new transaction
            // Changed by Deutsche Telekom
            ClientTransaction transaction = createNewTransaction(cancel);

            // Send the SIP message to the network
            if (sLogger.isActivated()) {
                sLogger.debug(">>> Send SIP CANCEL");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + cancel.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            transaction.sendRequest();
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
        }
    }

    /**
     * Send a SIP BYE
     * 
     * @param dialog Dialog path
     * @throws SipException
     */
    public void sendSipBye(SipDialogPath dialog) throws SipException {
        boolean loggerActivated = sLogger.isActivated();
        try {
            // Create the SIP request
            SipRequest bye = SipMessageFactory.createBye(dialog);

            // Set the Proxy-Authorization header
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            if (agent != null) {
                agent.setProxyAuthorizationHeader(bye);
            }

            // Create a new transaction
            // Changed by Deutsche Telekom
            ClientTransaction transaction = createNewTransaction(bye);

            // Send the SIP message to the network
            if (loggerActivated) {
                sLogger.debug(">>> Send SIP BYE");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + bye.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            dialog.getStackDialog().sendRequest(transaction);
        } catch (Exception e) {
            if (loggerActivated) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
        }
    }

    /**
     * Send a SIP UPDATE
     * 
     * @param dialog Dialog path
     * @return Transaction context
     * @throws SipException
     */
    public SipTransactionContext sendSipUpdate(SipDialogPath dialog) throws SipException {
        boolean loggerActivated = sLogger.isActivated();
        try {
            // Create the SIP request
            SipRequest update = SipMessageFactory.createUpdate(dialog);

            // Set the Proxy-Authorization header
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            if (agent != null) {
                agent.setProxyAuthorizationHeader(update);
            }

            // Get stack transaction
            // Changed by Deutsche Telekom
            ClientTransaction transaction = createNewTransaction(update);

            // Create a transaction context
            SipTransactionContext ctx = new SipTransactionContext(transaction);
            String id = SipTransactionContext.getTransactionContextId(update);
            mTransactions.put(id, ctx);

            if (loggerActivated) {
                sLogger.debug("Create a transaction context " + id);
            }

            // Send the SIP message to the network
            if (loggerActivated) {
                sLogger.debug(">>> Send SIP UPDATE");
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + update.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            transaction.sendRequest();

            // Returns the created transaction to wait synchronously the response
            return ctx;
        } catch (Exception e) {
            if (loggerActivated) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
        }
    }

    /**
     * Send a subsequent SIP request and create a context to wait a response
     * 
     * @param dialog Dialog path
     * @param request Request
     * @return SipTransactionContext
     * @throws SipException
     */
    public SipTransactionContext sendSubsequentRequest(SipDialogPath dialog, SipRequest request)
            throws SipException {
        boolean loggerActivated = sLogger.isActivated();
        try {
            SessionAuthenticationAgent agent = dialog.getAuthenticationAgent();
            // Set the Proxy-Authorization header
            if (agent != null) {
                agent.setProxyAuthorizationHeader(request);
            }

            // Get stack transaction
            // Changed by Deutsche Telekom
            ClientTransaction transaction = createNewTransaction(request);

            // Send the SIP message to the network
            if (loggerActivated) {
                sLogger.debug(">>> Send SIP " + request.getMethod().toUpperCase());
            }
            if (mSipTraceEnabled) {
                System.out.println(">>> " + request.getStackMessage().toString());
                System.out.println(TRACE_SEPARATOR);
            }
            dialog.getStackDialog().sendRequest(transaction);

            // Create a transaction context
            SipTransactionContext ctx = new SipTransactionContext(transaction);
            String id = SipTransactionContext.getTransactionContextId(request);
            mTransactions.put(id, ctx);

            // Returns the created transaction to wait synchronously the response
            return ctx;
        } catch (Exception e) {
            if (loggerActivated) {
                sLogger.error("Can't send SIP message", e);
            }
            throw new SipException("Can't send SIP message");
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

        // Get transaction
        ServerTransaction transaction = requestEvent.getServerTransaction();
        if (transaction == null) {
            try {
                // Create a transaction for this new incoming request
                SipProvider srcSipProvider = (SipProvider) requestEvent.getSource();
                transaction = srcSipProvider.getNewServerTransaction(request);
            } catch (Exception e) {
                if (loggerActivated) {
                    sLogger.error("Unable to create a new server transaction for an incoming request");
                }
                return;
            }
        }

        // Create received request with its associated transaction
        SipRequest req = new SipRequest(request);
        req.setStackTransaction(transaction);

        if ("ACK".equals(req.getMethod())) {
            // Search the context associated to the received ACK and notify it
            String transactionId = SipTransactionContext.getTransactionContextId(req);
            notifyTransactionContext(transactionId, req);
            return;
        }

        // Notify event listeners
        for (SipEventListener listener : mListeners) {
            if (loggerActivated) {
                sLogger.debug("Notify a SIP listener");
            }
            listener.receiveSipRequest(req);
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

        ClientTransaction transaction = (ClientTransaction) timeoutEvent.getClientTransaction();
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
     * @param transactionId
     * @param response
     */
    private void notifyProvisionalResponse(String transactionId, SipResponse response) {
        SipTransactionContext ctx = (SipTransactionContext) mTransactions.get(transactionId);
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
