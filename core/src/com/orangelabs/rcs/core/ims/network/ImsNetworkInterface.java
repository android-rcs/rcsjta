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

import org.apache.http.conn.util.InetAddressUtils;
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
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Abstract IMS network interface
 *
 * @author Jean-Marc AUFFRET
 */
public abstract class ImsNetworkInterface {
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
	protected String imsAuthentMode;

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
            String proxyAddr, int proxyPort, String proxyProtocol, String authentMode) {
		this.imsModule = imsModule;
		this.type = type;
		this.access = access;
        this.imsProxyAddr = proxyAddr;
        this.imsProxyPort = proxyPort;
        this.imsProxyProtocol = proxyProtocol;
		this.imsAuthentMode = authentMode;
		
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
	public String getAuthenticationMode() {
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
		if (imsAuthentMode.equals(RcsSettingsData.GIBA_AUTHENT)) {
			if (logger.isActivated()) {
				logger.debug("Load GIBA authentication procedure");
			}
			this.registrationProcedure = new GibaRegistrationProcedure();
		} else
		if (imsAuthentMode.equals(RcsSettingsData.DIGEST_AUTHENT)) {
			if (logger.isActivated()) {
				logger.debug("Load HTTP Digest authentication procedure");
			}
			this.registrationProcedure = new HttpDigestRegistrationProcedure();
        }
	}

	/**
     * Returns the user profile associated to the network access
     *
     * @return User profile
     */
	public UserProfile getUserProfile() {
		UserProfileInterface intf;
		if (imsAuthentMode.equals(RcsSettingsData.GIBA_AUTHENT)) {
			if (logger.isActivated()) {
				logger.debug("Load user profile derived from IMSI (GIBA)");
			}
    		intf = new GibaUserProfileInterface();
    	} else {
			if (logger.isActivated()) {
				logger.debug("Load user profile from RCS settings database");
			}
            intf = new SettingsUserProfileInterface();
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
	
	/**
     * Register to the IMS
     *
     * @return Registration result
     */
    public boolean register() {
		if (logger.isActivated()) {
			logger.debug("Register to IMS");
		}

		try {
			// Use IMS proxy address by default
			String resolvedIpAddress = imsProxyAddr;

			// Use default port by default
			int resolvedPort = imsProxyPort;

            if (!InetAddressUtils.isIPv4Address(imsProxyAddr) && !InetAddressUtils.isIPv6Address(imsProxyAddr)) {
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
								resolvedIpAddress = getDnsA(srvRecord.getTarget().toString());
								resolvedPort = srvRecord.getPort();
							} else {
								// Direct DNS A lookup
								resolvedIpAddress = getDnsA(imsProxyAddr);
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
						resolvedIpAddress = getDnsA(srvRecord.getTarget().toString());
						resolvedPort = srvRecord.getPort();
				    	resolved = true;
					}
				}
				
				if (!resolved) {
					// If not resolved: direct DNS A lookup
					if (logger.isActivated()) {
						logger.debug("No SRV record found: use DNS A instead");
					}
					resolvedIpAddress = getDnsA(imsProxyAddr);
				}
			}
			
	        if (resolvedIpAddress == null) {
		        throw new SipException("Proxy IP address not found");        	
	        }
	        
			if (logger.isActivated()) {
				logger.debug("SIP outbound proxy configuration: " +
						resolvedIpAddress + ":" + resolvedPort + ";" + imsProxyProtocol);
			}
			
			// Initialize the SIP stack
            sip.initStack(access.getIpAddress(), resolvedIpAddress, resolvedPort, imsProxyProtocol, getType());
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
