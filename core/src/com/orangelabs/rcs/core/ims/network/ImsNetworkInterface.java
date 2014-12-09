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

package com.orangelabs.rcs.core.ims.network;



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

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.access.NetworkAccess;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.registration.GibaRegistrationProcedure;
import com.orangelabs.rcs.core.ims.network.registration.HttpDigestRegistrationProcedure;
import com.orangelabs.rcs.core.ims.network.registration.RegistrationManager;
import com.orangelabs.rcs.core.ims.network.registration.RegistrationProcedure;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.userprofile.GibaUserProfileInterface;
import com.orangelabs.rcs.core.ims.userprofile.SettingsUserProfileInterface;
import com.orangelabs.rcs.core.ims.userprofile.UserProfile;
import com.orangelabs.rcs.core.ims.userprofile.UserProfileInterface;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.orangelabs.rcs.utils.logger.Logger;

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
	
	// Changed by Deutsche Telekom
	private static final String REGEX_IPV4 = "\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b";
    
    // Changed by Deutsche Telekom
    /**
     * Class containing the resolved fields
     */
    public class DnsResolvedFields {
        public String ipAddress = null;
        public int port = -1;

        public DnsResolvedFields(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }
    }
    
	/**
	 * IMS module
	 */
	private ImsModule imsModule;

	/**
	 * Network interface type
	 */
	private int type;

    /**
	 * Network access
	 */
	private NetworkAccess access;

    /**
     * SIP manager
     */
    private SipManager sip;

	/**
	 * IMS authentication mode associated to the network interface
	 */
	protected AuthenticationProcedure imsAuthentMode;

    /**
     * IMS proxy protocol
     */
    protected String imsProxyProtocol;

    /**
     * IMS proxy address
     */
    private String imsProxyAddr;

    /**
     * IMS proxy port
     */
    private int imsProxyPort;

    /**
	 * Registration procedure associated to the network interface
	 */
	protected RegistrationProcedure registrationProcedure;

	/**
     * Registration manager
     */
    private RegistrationManager registration;
    
	/**
	 * NAT traversal
	 */
	private boolean natTraversal = false;    
    
	/**
     * NAT public IP address for last registration
     */
	private String natPublicAddress = null;
	
	/**
	 * NAT public UDP port
	 */
	private int natPublicPort = -1;
	
	
	/**
	 * TCP fallback according to RFC3261 chapter 18.1.1
	 */
	private boolean tcpFallback = false;

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
     */
	public ImsNetworkInterface(ImsModule imsModule, int type, NetworkAccess access,
            String proxyAddr, int proxyPort, String proxyProtocol, AuthenticationProcedure authentMode) {
		this.imsModule = imsModule;
		this.type = type;
		this.access = access;
        this.imsProxyAddr = proxyAddr;
        this.imsProxyPort = proxyPort;
        this.imsProxyProtocol = proxyProtocol;
		this.imsAuthentMode = authentMode;
		if (proxyProtocol.equalsIgnoreCase(ListeningPoint.UDP))
			this.tcpFallback = RcsSettings.getInstance().isTcpFallback();
		
        // Instantiates the SIP manager
        sip = new SipManager(this);

        // Load the registration procedure
        loadRegistrationProcedure();

        // Instantiates the registration manager
        registration = new RegistrationManager(this, registrationProcedure);
	}

    /**
     * Is behind a NAT
     *
     * @return Boolean
     */
    public boolean isBehindNat() {
		return natTraversal;
    }
    
    /**
     * Set NAT traversal flag
     *
     * @return Boolean
     */
    public void setNatTraversal(boolean flag) {
		natTraversal = flag;
    }
    
    /**
     * Returns last known NAT public address as discovered by UAC. Returns null if unknown, UAC is not registered or
	 * no NAT traversal is detected.
	 * 
	 * @return Last known NAT public address discovered by UAC or null if UAC is not registered
	 */
	public String getNatPublicAddress() {
		return natPublicAddress;
	}

	/**
	 * Sets the last known NAT public address as discovered by UAC. Set to null on unregistering or if no
	 * NAT traversal is detected.
	 * 
	 * @param publicAddress Public address
	 */
	public void setNatPublicAddress(String publicAddress) {
		this.natPublicAddress = publicAddress;
	}	
	
	/**
	 * Returns last known NAT public UDP port as discovered by UAC. Returns -1 if unknown, UAC is not registered or
	 * no NAT traversal is detected.
	 * 
	 * @return Last known NAT public UDP port discovered by UAC or -1 if UAC is not registered
	 */
	public int getNatPublicPort() {
		return natPublicPort;
	}    
	
	/**
	 * Sets the last known NAT public address as discovered by UAC. Set to -1 on unregistering or if no
	 * NAT traversal is detected.
	 * 
	 * @param publicPort Public port
	 */
	public void setNatPublicPort(int publicPort) {
		this.natPublicPort = publicPort;
	}	
	
    /**
     * Is network interface configured
     *
     * @return Boolean
     */
    public boolean isInterfaceConfigured() {
    	return (imsProxyAddr != null) && (imsProxyAddr.length() > 0);
    }
    
	/**
     * Returns the IMS authentication mode
     *
     * @return Authentication mode
     */
	public AuthenticationProcedure getAuthenticationMode() {
		return imsAuthentMode;
	}

	/**
     * Returns the registration manager
     *
     * @return Registration manager
     */
	public RegistrationManager getRegistrationManager() {
		return registration;
	}
	
	/**
     * Load the registration procedure associated to the network access
     */
	public void loadRegistrationProcedure() {
		switch (imsAuthentMode) {
		case GIBA:
			if (logger.isActivated()) {
				logger.debug("Load GIBA authentication procedure");
			}
			this.registrationProcedure = new GibaRegistrationProcedure();
			break;
		case DIGEST:
			if (logger.isActivated()) {
				logger.debug("Load HTTP Digest authentication procedure");
			}
			this.registrationProcedure = new HttpDigestRegistrationProcedure();
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
		switch (imsAuthentMode) {
		case GIBA:
			if (logger.isActivated()) {
				logger.debug("Load user profile derived from IMSI (GIBA)");
			}
    		intf = new GibaUserProfileInterface();
			break;
		case DIGEST:
		default:
			if (logger.isActivated()) {
				logger.debug("Load user profile from RCS settings database");
			}
            intf = new SettingsUserProfileInterface();
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
		return imsModule;
	}

    /**
     * Returns the network interface type
     *
     * @return Type (see ConnectivityManager class)
     */
	public int getType() {
		return type;
	}

	/**
     * Returns the network access
     *
     * @return Network access
     */
    public NetworkAccess getNetworkAccess() {
    	return access;
    }

    /**
     * Returns the SIP manager
     *
     * @return SIP manager
     */
    public SipManager getSipManager() {
    	return sip;
    }

    /**
     * Is registered
     *
     * @return Return True if the terminal is registered, else return False
     */
    public boolean isRegistered() {
        return registration.isRegistered();
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
        } catch(TextParseException e) {
            if (logger.isActivated()) {
                logger.debug("Not a valid DNS name");
            }
            return null;
        } catch(IllegalArgumentException e) {
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
        } catch(UnknownHostException e) {
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
        	SRVRecord srv = (SRVRecord)records[i];
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
				} else
				if (srv.getPriority() == result.getPriority()) {
					// Highest weight
					if (srv.getWeight() > result.getWeight()) {
						result = srv;
					}
				}
			}
        }
        return result;
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
		if (imsProxyAddr.matches(REGEX_IPV4)) {
        	useDns = false;
        	dnsResolvedFields = new DnsResolvedFields(imsProxyAddr, imsProxyPort);
        
        	  if (logger.isActivated()) {
                  logger.warn("IP address found instead of FQDN!");
              }
        }
		else {
			dnsResolvedFields = new DnsResolvedFields(null, imsProxyPort);
		}
          
        if (useDns) {
            // Set DNS resolver
            ResolverConfig.refresh();
            ExtendedResolver resolver = new ExtendedResolver(); 

            // Resolve the IMS proxy configuration: first try to resolve via
            // a NAPTR query, then a SRV query and finally via A query
            if (logger.isActivated()) {
                logger.debug("Resolve IMS proxy address " + imsProxyAddr);
            }
            
            // DNS NAPTR lookup
            String service;
            if (imsProxyProtocol.equalsIgnoreCase(ListeningPoint.UDP)) {
                service = "SIP+D2U";
            } else
            if (imsProxyProtocol.equalsIgnoreCase(ListeningPoint.TCP)) {
                service = "SIP+D2T";
            } else
            if (imsProxyProtocol.equalsIgnoreCase(ListeningPoint.TLS)) {
                service = "SIPS+D2T";
            } else {
                throw new SipException("Unkown SIP protocol");
            }

            boolean resolved = false;
            Record[] naptrRecords = getDnsRequest(imsProxyAddr, resolver, Type.NAPTR);
            if ((naptrRecords != null) && (naptrRecords.length > 0)) {
                // First try with NAPTR
                if (logger.isActivated()) {
                    logger.debug("NAPTR records found: " + naptrRecords.length);
                }
                for (int i = 0; i < naptrRecords.length; i++) {
                    NAPTRRecord naptr = (NAPTRRecord)naptrRecords[i];
                    if (logger.isActivated()) {
                        logger.debug("NAPTR record: " + naptr.toString());
                    }
                    if ((naptr != null) && naptr.getService().equalsIgnoreCase(service)) {
                        // DNS SRV lookup
						Record[] srvRecords = getDnsRequest(naptr.getReplacement().toString(), resolver, Type.SRV);
                        if ((srvRecords != null) && (srvRecords.length > 0)) {
                            SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                            dnsResolvedFields.ipAddress = getDnsA(srvRecord.getTarget().toString());
                            dnsResolvedFields.port = srvRecord.getPort();
                        } else {
                            // Direct DNS A lookup
                            dnsResolvedFields.ipAddress = getDnsA(imsProxyAddr);
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
                String query;
                if (imsProxyAddr.startsWith("_sip.")) {
                    query = imsProxyAddr;
                } else {
                    query = "_sip._" + imsProxyProtocol.toLowerCase() + "." + imsProxyAddr;
                }
				Record[] srvRecords = getDnsRequest(query, resolver, Type.SRV);
                if ((srvRecords != null) && (srvRecords.length > 0)) {
                    SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                    dnsResolvedFields.ipAddress = getDnsA(srvRecord.getTarget().toString());
                    dnsResolvedFields.port = srvRecord.getPort();
                    resolved = true;
                }

                if (!resolved) {
                    // If not resolved: direct DNS A lookup
                    if (logger.isActivated()) {
                        logger.debug("No SRV record found: use DNS A instead");
                    }
                    dnsResolvedFields.ipAddress = getDnsA(imsProxyAddr);
                }
            }       
        }
        
        if (dnsResolvedFields.ipAddress == null) {
            // Changed by Deutsche Telekom
            // Try to use IMS proxy address as a fallback
            String imsProxyAddrResolved = getDnsA(imsProxyAddr);
            if (imsProxyAddrResolved != null){
                dnsResolvedFields = new DnsResolvedFields(imsProxyAddrResolved, imsProxyPort);
            } else {
                throw new SipException("Proxy IP address not found");
            }
        }
        
        if (logger.isActivated()) {
            logger.debug("SIP outbound proxy configuration: " +
                    dnsResolvedFields.ipAddress + ":" + dnsResolvedFields.port + ";" + imsProxyProtocol);
        }
        
        return dnsResolvedFields;
	}
	
	/**
     * Register to the IMS
     *
     * @param dnsResolvedFields The {@link DnsResolvedFields} object containing the DNS resolved fields.
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
            sip.initStack(access.getIpAddress(), dnsResolvedFields.ipAddress, dnsResolvedFields.port, imsProxyProtocol, tcpFallback, getType());
	    	sip.getSipStack().addSipEventListener(imsModule);
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't instanciate the SIP stack", e);
			}
			return false;
		}

    	// Register to IMS
		boolean registered = registration.registration();
		if (registered) {
			if (logger.isActivated()) {
				logger.debug("IMS registration successful");
			}

            // Start keep-alive for NAT if activated
            if (isBehindNat() && RcsSettings.getInstance().isSipKeepAliveEnabled()) {
                sip.getSipStack().getKeepAliveManager().start();
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
     * Check if the DNS fields has changed. If it has, return them. Otherwise, return <code>null</code>.
     * 
     * @return The {@link DnsResolvedFields} object containing the new DNS resolved fields, otherwise <code>null</code>.
     */
    public DnsResolvedFields checkDnsResolvedFieldsChanged() throws Exception {
        // Check DNS resolved fields
        DnsResolvedFields dnsResolvedFields = getDnsResolvedFields();

        if (sip.getSipStack() == null) {
            if (logger.isActivated()) {
                logger.debug("Registration state has changed: sip stack not initialized yet.");
            }
            return dnsResolvedFields;
        } else if (!sip.getSipStack().getOutboundProxyAddr().equals(dnsResolvedFields.ipAddress)) {
            if (logger.isActivated()) {
                logger.debug("Registration state has changed: proxy ip address has changed (old: " + sip.getSipStack().getOutboundProxyAddr()
                        + " - new: " + dnsResolvedFields.ipAddress + ").");
            }
            return dnsResolvedFields;
        } else if (sip.getSipStack().getOutboundProxyPort() != dnsResolvedFields.port) {
            if (logger.isActivated()) {
                logger.debug("Registration state has changed: proxy port has changed (old: " + sip.getSipStack().getOutboundProxyPort() + " - new: "
                        + dnsResolvedFields.port + ").");
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
		registration.unRegistration();

    	// Close the SIP stack
    	sip.closeStack();
    }

	/**
     * Registration terminated
     */
    public void registrationTerminated() {
		if (logger.isActivated()) {
			logger.debug("Registration has been terminated");
		}

		// Stop registration
		registration.stopRegistration();

		// Close the SIP stack
    	sip.closeStack();
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
