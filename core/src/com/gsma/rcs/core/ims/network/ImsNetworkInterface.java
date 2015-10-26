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

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.access.NetworkAccess;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.registration.GibaRegistrationProcedure;
import com.gsma.rcs.core.ims.network.registration.HttpDigestRegistrationProcedure;
import com.gsma.rcs.core.ims.network.registration.RegistrationManager;
import com.gsma.rcs.core.ims.network.registration.RegistrationProcedure;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.userprofile.GibaUserProfileInterface;
import com.gsma.rcs.core.ims.userprofile.SettingsUserProfileInterface;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.core.ims.userprofile.UserProfileInterface;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceRegistration;

import org.xbill.DNS.Cache;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax2.sip.ListeningPoint;

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

    private ImsModule mImsModule;

    /**
     * Network interface type
     */
    private int mType;

    private NetworkAccess mAccess;

    private final SipManager mSip;

    /**
     * IMS authentication mode associated to the network interface
     */
    protected AuthenticationProcedure mImsAuthentMode;

    protected String mImsProxyProtocol;

    private String mImsProxyAddr;

    private int mImsProxyPort;

    /**
     * Registration procedure associated to the network interface
     */
    protected RegistrationProcedure mRegistrationProcedure;

    private RegistrationManager mRegistration;

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
     * Holds retry duration value obtained from Retry-After header
     * <p>
     * Default value = 0L
     * </p>
     */
    private long mRetryDuration = 0;

    private static Logger sLogger = Logger.getLogger(ImsNetworkInterface.class.getName());

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
        if (ListeningPoint.UDP.equals(mImsProxyProtocol)) {
            mTcpFallback = mRcsSettings.isTcpFallback();
        }

        mSip = new SipManager(this, mRcsSettings);

        loadRegistrationProcedure();

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
        return mImsProxyAddr != null;
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
     * Get the Retry header time
     * 
     * @return retryHeader Retry-After duration value
     */
    public long getRetryAfterHeaderDuration() {
        return mRetryDuration;
    }

    /**
     * Sets the Retry header time
     * 
     * @param retryHeader Retry-After duration value
     */
    public void setRetryAfterHeaderDuration(long retryValue) {
        mRetryDuration = retryValue;
    }

    /**
     * Load the registration procedure associated to the network access
     */
    public void loadRegistrationProcedure() {
        switch (mImsAuthentMode) {
            case GIBA:
                if (sLogger.isActivated()) {
                    sLogger.debug("Load GIBA authentication procedure");
                }
                mRegistrationProcedure = new GibaRegistrationProcedure();
                break;
            case DIGEST:
            default:
                if (sLogger.isActivated()) {
                    sLogger.debug("Load HTTP Digest authentication procedure");
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
                if (sLogger.isActivated()) {
                    sLogger.debug("Load user profile derived from IMSI (GIBA)");
                }
                intf = new GibaUserProfileInterface(mRcsSettings);
                break;
            case DIGEST:
            default:
                if (sLogger.isActivated()) {
                    sLogger.debug("Load user profile from RCS settings database");
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
     * @throws TextParseException
     */
    private Record[] getDnsRequest(String domain, ExtendedResolver resolver, int type)
            throws TextParseException {
        if (sLogger.isActivated()) {
            if (type == Type.SRV) {
                sLogger.debug("DNS SRV lookup for " + domain);
            } else if (type == Type.NAPTR) {
                sLogger.debug("DNS NAPTR lookup for " + domain);
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
            if (sLogger.isActivated()) {
                sLogger.warn("Lookup error: " + code + "/" + lookup.getErrorString());
            }
        }
        return result;
    }

    /**
     * Get DNS A record
     * 
     * @param domain Domain
     * @return IP address or null if no record
     * @throws UnknownHostException
     */
    private String getDnsA(String domain) throws UnknownHostException {
        if (sLogger.isActivated()) {
            sLogger.debug("DNS A lookup for " + domain);
        }
        return InetAddress.getByName(domain).getHostAddress();
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
            if (sLogger.isActivated()) {
                sLogger.debug("SRV record: " + srv.toString());
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
        StringBuilder query = new StringBuilder();
        if (DNS_SIP_TLS_SERVICE.equalsIgnoreCase(sipService)) {
            query.append(DNS_SIPS_PREFIX).append(TCP_PROTOCOL);
        } else {
            query.append(DNS_SIP_PREFIX).append(mImsProxyProtocol.toLowerCase());
        }
        query.append(DOT).append(mImsProxyAddr);
        return query.toString();
    }

    // Changed by Deutsche Telekom
    /**
     * Get the DNS resolved fields.
     * 
     * @return The {@link DnsResolvedFields} object containing the DNS resolved fields.
     * @throws PayloadException
     * @throws UnknownHostException
     */
    protected DnsResolvedFields getDnsResolvedFields() throws PayloadException,
            UnknownHostException {
        try {
            // Changed by Deutsche Telekom
            DnsResolvedFields dnsResolvedFields;
            boolean useDns = true;
            if (mImsProxyAddr.matches(REGEX_IPV4)) {
                useDns = false;
                dnsResolvedFields = new DnsResolvedFields(mImsProxyAddr, mImsProxyPort);

                if (sLogger.isActivated()) {
                    sLogger.warn("IP address found instead of FQDN!");
                }
            } else {
                dnsResolvedFields = new DnsResolvedFields(null, mImsProxyPort);
            }

            if (useDns) {
                ResolverConfig.refresh();
                ExtendedResolver resolver = new ExtendedResolver();

                /*
                 * Resolve the IMS proxy configuration: first try to resolve via a NAPTR query, then
                 * a SRV query and finally via A query
                 */
                if (sLogger.isActivated()) {
                    sLogger.debug("Resolve IMS proxy address ".concat(mImsProxyAddr));
                }

                /* DNS NAPTR lookup */
                String service;
                if (ListeningPoint.UDP.equals(mImsProxyProtocol)) {
                    service = DNS_SIP_UDP_SERVICE;
                } else if (ListeningPoint.TCP.equals(mImsProxyProtocol)) {
                    service = DNS_SIP_TCP_SERVICE;
                } else if (ListeningPoint.TLS.equals(mImsProxyProtocol)) {
                    service = DNS_SIP_TLS_SERVICE;
                } else {
                    throw new PayloadException(new StringBuilder("Unkown SIP protocol : ").append(
                            mImsProxyProtocol).toString());
                }

                boolean resolved = false;
                Record[] naptrRecords = getDnsRequest(mImsProxyAddr, resolver, Type.NAPTR);
                if ((naptrRecords != null) && (naptrRecords.length > 0)) {
                    /* First try with NAPTR */
                    if (sLogger.isActivated()) {
                        sLogger.debug(new StringBuilder("NAPTR records found: ").append(
                                naptrRecords.length).toString());
                    }
                    for (int i = 0; i < naptrRecords.length; i++) {
                        NAPTRRecord naptr = (NAPTRRecord) naptrRecords[i];
                        if (sLogger.isActivated()) {
                            sLogger.debug("NAPTR record: ".concat(naptr.toString()));
                        }
                        if ((naptr != null) && naptr.getService().equalsIgnoreCase(service)) {
                            /* DNS SRV lookup */
                            Record[] srvRecords = getDnsRequest(naptr.getReplacement().toString(),
                                    resolver, Type.SRV);
                            if ((srvRecords != null) && (srvRecords.length > 0)) {
                                SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                                dnsResolvedFields.mIpAddress = getDnsA(srvRecord.getTarget()
                                        .toString());
                                dnsResolvedFields.mPort = srvRecord.getPort();
                            } else {
                                /* Direct DNS A lookup */
                                dnsResolvedFields.mIpAddress = getDnsA(mImsProxyAddr);
                            }
                            resolved = true;
                        }
                    }
                }

                if (!resolved) {
                    /* If no NAPTR: direct DNS SRV lookup */
                    if (sLogger.isActivated()) {
                        sLogger.debug("No NAPTR record found: use DNS SRV instead");
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
                        /* If not resolved: direct DNS A lookup */
                        if (sLogger.isActivated()) {
                            sLogger.debug("No SRV record found: use DNS A instead");
                        }
                        dnsResolvedFields.mIpAddress = getDnsA(mImsProxyAddr);
                    }
                }
            }

            if (dnsResolvedFields.mIpAddress == null) {
                // Changed by Deutsche Telekom
                /* Try to use IMS proxy address as a fallback */
                String imsProxyAddrResolved = getDnsA(mImsProxyAddr);
                if (imsProxyAddrResolved == null) {
                    throw new PayloadException(new StringBuilder("Proxy IP address : ")
                            .append(mImsProxyAddr).append(" not found!").toString());
                }
                dnsResolvedFields = new DnsResolvedFields(imsProxyAddrResolved, mImsProxyPort);
            }

            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("SIP outbound proxy configuration: ")
                        .append(dnsResolvedFields.mIpAddress).append(":")
                        .append(dnsResolvedFields.mPort).append(";").append(mImsProxyProtocol)
                        .toString());
            }
            return dnsResolvedFields;

        } catch (TextParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Failed to resolve dns for proxy configuration: ").append(mImsProxyAddr)
                    .append(" with protocol: ").append(mImsProxyProtocol).append("!").toString(), e);
        }
    }

    /**
     * Register to the IMS
     * 
     * @param dnsResolvedFields The {@link DnsResolvedFields} object containing the DNS resolved
     *            fields.
     * @return Registration result
     * @throws PayloadException
     * @throws NetworkException
     */
    // Changed by Deutsche Telekom
    public void register(DnsResolvedFields dnsResolvedFields) throws PayloadException,
            NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Register to IMS");
            }

            // Changed by Deutsche Telekom
            if (dnsResolvedFields == null) {
                dnsResolvedFields = getDnsResolvedFields();
            }

            // Changed by Deutsche Telekom
            mSip.initStack(mAccess.getIpAddress(), dnsResolvedFields.mIpAddress,
                    dnsResolvedFields.mPort, mImsProxyProtocol, mTcpFallback, getType());
            mSip.getSipStack().addSipEventListener(mImsModule);

            mRegistration.register();

            if (sLogger.isActivated()) {
                sLogger.debug("IMS registered: ".concat(Boolean.toString(mRegistration
                        .isRegistered())));
            }

            /**
             * Even if DUT is not behind NAT (Network Address Translation) and PROTOCOL !=
             * ListeningPoint.UDP, it should still send the keep-Alive (double CRLF).
             */
            if (mRcsSettings.isSipKeepAliveEnabled()
                    && !ListeningPoint.UDP.equals(mImsProxyProtocol)) {
                mSip.getSipStack().getKeepAliveManager().start();
            }
        } catch (UnknownHostException e) {
            throw new PayloadException(new StringBuilder(
                    "Unable to register due to stack initialization failure for address : ")
                    .append(mImsProxyAddr).toString(), e);
        }
    }

    /**
     * Unregister from the IMS
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public void unregister() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Unregister from IMS");
        }

        mRegistration.deRegister();

        mSip.closeStack();
    }

    /**
     * Registration terminated
     */
    public void registrationTerminated() {
        if (sLogger.isActivated()) {
            sLogger.debug("Registration has been terminated");
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
