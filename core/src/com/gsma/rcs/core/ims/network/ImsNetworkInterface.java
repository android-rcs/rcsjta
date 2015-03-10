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

package com.gsma.rcs.core.ims.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax2.sip.ListeningPoint;

import org.xbill.DNS.Cache;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.access.NetworkAccess;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.registration.GibaRegistrationProcedure;
import com.gsma.rcs.core.ims.network.registration.HttpDigestRegistrationProcedure;
import com.gsma.rcs.core.ims.network.registration.RegistrationManager;
import com.gsma.rcs.core.ims.network.registration.RegistrationProcedure;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.userprofile.GibaUserProfileInterface;
import com.gsma.rcs.core.ims.userprofile.SettingsUserProfileInterface;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.core.ims.userprofile.UserProfileInterface;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Abstract IMS network interface
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class ImsNetworkInterface {

    /**
     * The maximum time in seconds that a negative response will be stored in this DNS Cache.
     */
    private static int DNS_NEGATIVE_CACHING_TIME = 5;

    /**
     * IPv4 address format
     */
    private static final String REGEX_IPV4 = "\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b";

    /**
     * Dot constant
     */
    private static final char DOT = '.';

    /**
     * DNS SIP TLS service
     */
    private static final String DNS_SIP_TLS_SERVICE = "SIPS+D2T";

    /**
     * DNS SIP TCP service
     */
    private static final String DNS_SIP_TCP_SERVICE = "SIP+D2T";

    /**
     * DNS SIP UDP service
     */
    private static final String DNS_SIP_UDP_SERVICE = "SIP+D2U";

    /**
     * DNS SIP prefix service
     */
    private static final String DNS_SIP_PREFIX = "_sip._";

    /**
     * DNS SIPS prefix service
     */
    private static final String DNS_SIPS_PREFIX = "_sips._";

    /**
     * TCP protocol
     */
    private static final String TCP_PROTOCOL = "tcp";

    /**
     * Class containing the resolved fields
     */
    public class DnsResolvedFields {
        /**
         * DNS resoled IP address
         */
        public String mIpAddress;
        /**
         * DNS resolved port
         */
        public int mPort = -1;

        /**
         * Constructor
         * 
         * @param ipAddress
         * @param port
         */
        public DnsResolvedFields(String ipAddress, int port) {
            mIpAddress = ipAddress;
            mPort = port;
        }
    }

    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Network interface type
     */
    private int mType;

    /**
     * Network access
     */
    private NetworkAccess mAccess;

    /**
     * SIP manager
     */
    private SipManager mSip;

    /**
     * IMS authentication mode associated to the network interface
     */
    protected AuthenticationProcedure mImsAuthentMode;

    /**
     * IMS proxy protocol
     */
    protected String mImsProxyProtocol;

    /**
     * IMS proxy address
     */
    private String mImsProxyAddr;

    /**
     * IMS proxy port
     */
    private int mImsProxyPort;

    /**
     * Registration procedure associated to the network interface
     */
    protected RegistrationProcedure mRegistrationProcedure;

    /**
     * Registration manager
     */
    private RegistrationManager mRegistration;

    /**
     * NAT traversal
     */
    private boolean mNatTraversal = false;

    /**
     * NAT public IP address for last registration
     */
    private String mNatPublicAddress;

    /**
     * NAT public UDP port
     */
    private int mNatPublicPort = -1;

    /**
     * TCP fallback according to RFC3261 chapter 18.1.1
     */
    private boolean mTcpFallback = false;

    private final RcsSettings mRcsSettings;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param imsModule IMS module
     * @param type Network interface type
     * @param access Network access
     * @param proxyAddr IMS proxy address
     * @param proxyPort IMS proxy port
     * @param proxyProtocol IMS proxy protocol
     * @param authentMode IMS authentication mode
     * @param rcsSettings
     */
    public ImsNetworkInterface(ImsModule imsModule, int type, NetworkAccess access,
            String proxyAddr, int proxyPort, String proxyProtocol,
            AuthenticationProcedure authentMode, RcsSettings rcsSettings) {
        mImsModule = imsModule;
        mType = type;
        mAccess = access;
        mImsProxyAddr = proxyAddr;
        mImsProxyPort = proxyPort;
        mImsProxyProtocol = proxyProtocol;
        mImsAuthentMode = authentMode;
        mRcsSettings = rcsSettings;
        if (proxyProtocol.equalsIgnoreCase(ListeningPoint.UDP))
            mTcpFallback = mRcsSettings.isTcpFallback();

        // Instantiates the SIP manager
        mSip = new SipManager(this, mRcsSettings);

        // Load the registration procedure
        loadRegistrationProcedure();

        // Instantiates the registration manager
        mRegistration = new RegistrationManager(this, mRegistrationProcedure, mRcsSettings);
    }

    /**
     * Is behind a NAT
     * 
     * @return Boolean
     */
    public boolean isBehindNat() {
        return mNatTraversal;
    }

    /**
     * Set NAT traversal flag
     * 
     * @param flag
     */
    public void setNatTraversal(boolean flag) {
        mNatTraversal = flag;
    }

    /**
     * Returns last known NAT public address as discovered by UAC. Returns null if unknown, UAC is
     * not registered or no NAT traversal is detected.
     * 
     * @return Last known NAT public address discovered by UAC or null if UAC is not registered
     */
    public String getNatPublicAddress() {
        return mNatPublicAddress;
    }

    /**
     * Sets the last known NAT public address as discovered by UAC. Set to null on unregistering or
     * if no NAT traversal is detected.
     * 
     * @param publicAddress Public address
     */
    public void setNatPublicAddress(String publicAddress) {
        mNatPublicAddress = publicAddress;
    }

    /**
     * Returns last known NAT public UDP port as discovered by UAC. Returns -1 if unknown, UAC is
     * not registered or no NAT traversal is detected.
     * 
     * @return Last known NAT public UDP port discovered by UAC or -1 if UAC is not registered
     */
    public int getNatPublicPort() {
        return mNatPublicPort;
    }

    /**
     * Sets the last known NAT public address as discovered by UAC. Set to -1 on unregistering or if
     * no NAT traversal is detected.
     * 
     * @param publicPort Public port
     */
    public void setNatPublicPort(int publicPort) {
        mNatPublicPort = publicPort;
    }

    /**
     * Is network interface configured
     * 
     * @return Boolean
     */
    public boolean isInterfaceConfigured() {
        return (mImsProxyAddr != null) && (mImsProxyAddr.length() > 0);
    }

    /**
     * Returns the IMS authentication mode
     * 
     * @return Authentication mode
     */
    public AuthenticationProcedure getAuthenticationMode() {
        return mImsAuthentMode;
    }

    /**
     * Returns the registration manager
     * 
     * @return Registration manager
     */
    public RegistrationManager getRegistrationManager() {
        return mRegistration;
    }

    /**
     * Load the registration procedure associated to the network access
     */
    public void loadRegistrationProcedure() {
        switch (mImsAuthentMode) {
            case GIBA:
                if (logger.isActivated()) {
                    logger.debug("Load GIBA authentication procedure");
                }
                mRegistrationProcedure = new GibaRegistrationProcedure();
                break;
            case DIGEST:
                if (logger.isActivated()) {
                    logger.debug("Load HTTP Digest authentication procedure");
                }
                mRegistrationProcedure = new HttpDigestRegistrationProcedure();
                break;
        }
    }

    /**
     * Returns the user profile associated to the network access
     * 
     * @return User profile
     */
    public UserProfile getUserProfile() {
        UserProfileInterface intf;
        switch (mImsAuthentMode) {
            case GIBA:
                if (logger.isActivated()) {
                    logger.debug("Load user profile derived from IMSI (GIBA)");
                }
                intf = new GibaUserProfileInterface(mRcsSettings);
                break;
            case DIGEST:
            default:
                if (logger.isActivated()) {
                    logger.debug("Load user profile from RCS settings database");
                }
                intf = new SettingsUserProfileInterface(mRcsSettings);
                break;
        }
        return intf.read();
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
     * Returns the network interface type
     * 
     * @return Type (see ConnectivityManager class)
     */
    public int getType() {
        return mType;
    }

    /**
     * Returns the network access
     * 
     * @return Network access
     */
    public NetworkAccess getNetworkAccess() {
        return mAccess;
    }

    /**
     * Returns the SIP manager
     * 
     * @return SIP manager
     */
    public SipManager getSipManager() {
        return mSip;
    }

    /**
     * Is registered
     * 
     * @return Return True if the terminal is registered, else return False
     */
    public boolean isRegistered() {
        return mRegistration.isRegistered();
    }

    /**
     * Gets reason code for RCS registration
     * 
     * @return reason code
     */
    public RcsServiceRegistration.ReasonCode getRegistrationReasonCode() {
        return mRegistration.getReasonCode();
    }

    /**
     * Get DNS records
     * 
     * @param domain Domain
     * @param resolver Resolver
     * @param type (Type.SRV or Type.NAPTR)
     * @return SRV records or null if no record
     */
    private Record[] getDnsRequest(String domain, ExtendedResolver resolver, int type) {
        try {
            if (logger.isActivated()) {
                if (type == Type.SRV) {
                    logger.debug("DNS SRV lookup for " + domain);
                } else if (type == Type.NAPTR) {
                    logger.debug("DNS NAPTR lookup for " + domain);
                }
            }
            Lookup lookup = new Lookup(domain, type);
            lookup.setResolver(resolver);
            // Default negative cache TTL value is "cache forever". We do not want that.
            Cache cache = Lookup.getDefaultCache(type);
            cache.setMaxNCache(DNS_NEGATIVE_CACHING_TIME);
            lookup.setCache(cache);
            Record[] result = lookup.run();
            int code = lookup.getResult();
            if (code != Lookup.SUCCESSFUL) {
                if (logger.isActivated()) {
                    logger.warn("Lookup error: " + code + "/" + lookup.getErrorString());
                }
            }
            return result;
        } catch (TextParseException e) {
            if (logger.isActivated()) {
                logger.debug("Not a valid DNS name");
            }
            return null;
        } catch (IllegalArgumentException e) {
            if (logger.isActivated()) {
                logger.debug("Not a valid DNS type");
            }
            return null;
        }
    }

    /**
     * Get DNS A record
     * 
     * @param domain Domain
     * @return IP address or null if no record
     */
    private String getDnsA(String domain) {
        try {
            if (logger.isActivated()) {
                logger.debug("DNS A lookup for " + domain);
            }
            return InetAddress.getByName(domain).getHostAddress();
        } catch (UnknownHostException e) {
            if (logger.isActivated()) {
                logger.debug("Unknown host for " + domain);
            }
            return null;
        }
    }

    /**
     * Get best DNS SRV record
     * 
     * @param records SRV records
     * @return IP address
     */
    private SRVRecord getBestDnsSRV(Record[] records) {
        SRVRecord result = null;
        for (int i = 0; i < records.length; i++) {
            SRVRecord srv = (SRVRecord) records[i];
            if (logger.isActivated()) {
                logger.debug("SRV record: " + srv.toString());
            }
            if (result == null) {
                // First record
                result = srv;
            } else {
                // Next record
                if (srv.getPriority() < result.getPriority()) {
                    // Lowest priority
                    result = srv;
                } else if (srv.getPriority() == result.getPriority()) {
                    // Highest weight
                    if (srv.getWeight() > result.getWeight()) {
                        result = srv;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get the SRV Query
     *
     * @return constructed srv query
     */
    private String getSrvQuery(String sipService) {
        if (DNS_SIP_TLS_SERVICE.equalsIgnoreCase(sipService)) {
            return new StringBuilder(DNS_SIPS_PREFIX).append(TCP_PROTOCOL).append(DOT)
                    .append(mImsProxyAddr).toString();
        } else {
            return new StringBuilder(DNS_SIP_PREFIX).append(mImsProxyProtocol.toLowerCase())
                    .append(DOT).append(mImsProxyAddr).toString();
        }
    }

    // Changed by Deutsche Telekom
    /**
     * Get the DNS resolved fields.
     * 
     * @return The {@link DnsResolvedFields} object containing the DNS resolved fields.
     */
    protected DnsResolvedFields getDnsResolvedFields() throws Exception {
        // Changed by Deutsche Telekom
        DnsResolvedFields dnsResolvedFields;
        boolean useDns = true;
        if (mImsProxyAddr.matches(REGEX_IPV4)) {
            useDns = false;
            dnsResolvedFields = new DnsResolvedFields(mImsProxyAddr, mImsProxyPort);

            if (logger.isActivated()) {
                logger.warn("IP address found instead of FQDN!");
            }
        } else {
            dnsResolvedFields = new DnsResolvedFields(null, mImsProxyPort);
        }

        if (useDns) {
            // Set DNS resolver
            ResolverConfig.refresh();
            ExtendedResolver resolver = new ExtendedResolver();

            // Resolve the IMS proxy configuration: first try to resolve via
            // a NAPTR query, then a SRV query and finally via A query
            if (logger.isActivated()) {
                logger.debug("Resolve IMS proxy address " + mImsProxyAddr);
            }

            // DNS NAPTR lookup
            String service;
            if (mImsProxyProtocol.equalsIgnoreCase(ListeningPoint.UDP)) {
                service = DNS_SIP_UDP_SERVICE;
            } else if (mImsProxyProtocol.equalsIgnoreCase(ListeningPoint.TCP)) {
                service = DNS_SIP_TCP_SERVICE;
            } else if (mImsProxyProtocol.equalsIgnoreCase(ListeningPoint.TLS)) {
                service = DNS_SIP_TLS_SERVICE;
            } else {
                throw new SipException("Unkown SIP protocol");
            }

            boolean resolved = false;
            Record[] naptrRecords = getDnsRequest(mImsProxyAddr, resolver, Type.NAPTR);
            if ((naptrRecords != null) && (naptrRecords.length > 0)) {
                // First try with NAPTR
                if (logger.isActivated()) {
                    logger.debug("NAPTR records found: " + naptrRecords.length);
                }
                for (int i = 0; i < naptrRecords.length; i++) {
                    NAPTRRecord naptr = (NAPTRRecord) naptrRecords[i];
                    if (logger.isActivated()) {
                        logger.debug("NAPTR record: " + naptr.toString());
                    }
                    if ((naptr != null) && naptr.getService().equalsIgnoreCase(service)) {
                        // DNS SRV lookup
                        Record[] srvRecords = getDnsRequest(naptr.getReplacement().toString(),
                                resolver, Type.SRV);
                        if ((srvRecords != null) && (srvRecords.length > 0)) {
                            SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                            dnsResolvedFields.mIpAddress = getDnsA(srvRecord.getTarget().toString());
                            dnsResolvedFields.mPort = srvRecord.getPort();
                        } else {
                            // Direct DNS A lookup
                            dnsResolvedFields.mIpAddress = getDnsA(mImsProxyAddr);
                        }
                        resolved = true;
                    }
                }
            }

            if (!resolved) {
                // If no NAPTR: direct DNS SRV lookup
                if (logger.isActivated()) {
                    logger.debug("No NAPTR record found: use DNS SRV instead");
                }
                String srvQuery;
                if (mImsProxyAddr.startsWith(DNS_SIP_PREFIX)
                        || mImsProxyAddr.startsWith(DNS_SIPS_PREFIX)) {
                    srvQuery = mImsProxyAddr;
                } else {
                    srvQuery = getSrvQuery(service);
                }
                Record[] srvRecords = getDnsRequest(srvQuery, resolver, Type.SRV);
                if ((srvRecords != null) && (srvRecords.length > 0)) {
                    SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                    dnsResolvedFields.mIpAddress = getDnsA(srvRecord.getTarget().toString());
                    dnsResolvedFields.mPort = srvRecord.getPort();
                    resolved = true;
                }

                if (!resolved) {
                    // If not resolved: direct DNS A lookup
                    if (logger.isActivated()) {
                        logger.debug("No SRV record found: use DNS A instead");
                    }
                    dnsResolvedFields.mIpAddress = getDnsA(mImsProxyAddr);
                }
            }
        }

        if (dnsResolvedFields.mIpAddress == null) {
            // Changed by Deutsche Telekom
            // Try to use IMS proxy address as a fallback
            String imsProxyAddrResolved = getDnsA(mImsProxyAddr);
            if (imsProxyAddrResolved != null) {
                dnsResolvedFields = new DnsResolvedFields(imsProxyAddrResolved, mImsProxyPort);
            } else {
                throw new SipException("Proxy IP address not found");
            }
        }

        if (logger.isActivated()) {
            logger.debug("SIP outbound proxy configuration: " + dnsResolvedFields.mIpAddress + ":"
                    + dnsResolvedFields.mPort + ";" + mImsProxyProtocol);
        }

        return dnsResolvedFields;
    }

    /**
     * Register to the IMS
     * 
     * @param dnsResolvedFields The {@link DnsResolvedFields} object containing the DNS resolved
     *            fields.
     * @return Registration result
     */
    // Changed by Deutsche Telekom
    public boolean register(DnsResolvedFields dnsResolvedFields) {
        if (logger.isActivated()) {
            logger.debug("Register to IMS");
        }

        try {
            // Changed by Deutsche Telekom
            if (dnsResolvedFields == null) {
                dnsResolvedFields = getDnsResolvedFields();
            }

            // Initialize the SIP stack
            // Changed by Deutsche Telekom
            mSip.initStack(mAccess.getIpAddress(), dnsResolvedFields.mIpAddress,
                    dnsResolvedFields.mPort, mImsProxyProtocol, mTcpFallback, getType());
            mSip.getSipStack().addSipEventListener(mImsModule);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't instanciate the SIP stack", e);
            }
            return false;
        }

        // Register to IMS
        boolean registered = mRegistration.registration();
        if (registered) {
            if (logger.isActivated()) {
                logger.debug("IMS registration successful");
            }

            /**
             * Even if DUT is not behind NAT (Network Address Translation) and PROTOCOL !=
             * ListeningPoint.UDP, it should still send the keep-Alive (double CRLF).
             */
            if (mRcsSettings.isSipKeepAliveEnabled()
                    && !ListeningPoint.UDP.equalsIgnoreCase(mImsProxyProtocol)) {
                mSip.getSipStack().getKeepAliveManager().start();
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("IMS registration has failed");
            }
        }

        return registered;
    }

    // Changed by Deutsche Telekom
    /**
     * Register to the IMS
     * 
     * @return Registration result
     */
    public boolean register() {
        return register(null);
    }

    // Changed by Deutsche Telekom
    /**
     * Check if the DNS fields has changed. If it has, return them. Otherwise, return
     * <code>null</code>.
     * 
     * @return The {@link DnsResolvedFields} object containing the new DNS resolved fields,
     *         otherwise <code>null</code>.
     * @throws Exception
     */
    public DnsResolvedFields checkDnsResolvedFieldsChanged() throws Exception {
        // Check DNS resolved fields
        DnsResolvedFields dnsResolvedFields = getDnsResolvedFields();

        if (mSip.getSipStack() == null) {
            if (logger.isActivated()) {
                logger.debug("Registration state has changed: sip stack not initialized yet.");
            }
            return dnsResolvedFields;
        } else if (!mSip.getSipStack().getOutboundProxyAddr().equals(dnsResolvedFields.mIpAddress)) {
            if (logger.isActivated()) {
                logger.debug("Registration state has changed: proxy ip address has changed (old: "
                        + mSip.getSipStack().getOutboundProxyAddr() + " - new: "
                        + dnsResolvedFields.mIpAddress + ").");
            }
            return dnsResolvedFields;
        } else if (mSip.getSipStack().getOutboundProxyPort() != dnsResolvedFields.mPort) {
            if (logger.isActivated()) {
                logger.debug("Registration state has changed: proxy port has changed (old: "
                        + mSip.getSipStack().getOutboundProxyPort() + " - new: "
                        + dnsResolvedFields.mPort + ").");
            }
            return dnsResolvedFields;
        }

        return null;
    }

    /**
     * Unregister from the IMS
     */
    public void unregister() {
        if (logger.isActivated()) {
            logger.debug("Unregister from IMS");
        }

        // Unregister from IMS
        mRegistration.unRegistration();

        // Close the SIP stack
        mSip.closeStack();
    }

    /**
     * Registration terminated
     */
    public void registrationTerminated() {
        if (logger.isActivated()) {
            logger.debug("Registration has been terminated");
        }

        // Stop registration
        mRegistration.stopRegistration();

        // Close the SIP stack
        mSip.closeStack();
    }

    /**
     * Returns the network access info
     * 
     * @return String
     * @throws CoreException
     */
    public String getAccessInfo() throws CoreException {
        return getNetworkAccess().getType();
    }
}
