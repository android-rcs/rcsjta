/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.orangelabs.rcs.provisioning.https;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.gsma.services.rcs.RcsService;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.MessagingMode;
import com.orangelabs.rcs.provisioning.ProvisioningFailureReasons;
import com.orangelabs.rcs.provisioning.ProvisioningInfo;
import com.orangelabs.rcs.provisioning.ProvisioningInfo.Version;
import com.orangelabs.rcs.provisioning.ProvisioningParser;
import com.orangelabs.rcs.provisioning.TermsAndConditionsRequest;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.HttpUtils;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.NetworkUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Provisioning via network manager
 *
 * @author hlxn7157
 * @author G. LE PESSOT
 * @author Deutsche Telekom AG
 */
public class HttpsProvisioningManager {

    /**
     * First launch flag
     */
    private boolean first = false;

    /**
     * User action flag
     */
    private boolean user = false;
    /**
     * Retry counter
     */
    private int retryCount = 0;

    /**
     * Check if a provisioning request is already pending
     */
    private boolean isPending = false;

    private final LocalContentResolver mLocalContentResolver;

    /**
     * The Service Context
     */
	private final Context mCtx;

    /**
     * Provisioning SMS manager
     */
    HttpsProvisioningSMS smsManager;

    /**
     * Provisioning Connection manager
     */
    HttpsProvisioningConnection networkConnection;

    /**
     * Retry after 511 "Network authentication required" counter
     */
    private int retryAfter511ErrorCount = 0;

    /**
     * Retry intent
     */
    private PendingIntent retryIntent;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param applicationContext
	 * @param retryIntent
	 *            pending intent to update periodically the configuration
	 * @param first
	 *            is provisioning service launch after (re)boot ?
	 * @param user
	 *            is provisioning service launch after user action ?
	 */
	public HttpsProvisioningManager(Context applicationContext,
			LocalContentResolver localContentResolver, final PendingIntent retryIntent,
			boolean first, boolean user) {
		mCtx = applicationContext;
		mLocalContentResolver = localContentResolver;
		this.retryIntent = retryIntent;
		this.first = first;
		this.user = user;
		this.smsManager = new HttpsProvisioningSMS(this);
		this.networkConnection = new HttpsProvisioningConnection(this);
	}

    /**
     * @return the context
     */
    protected Context getContext() {
        return mCtx;
    }

    /**
     * Connection event
     *
     * @param action Connectivity action
     * @return true if the updateConfig has been done
     */
    protected boolean connectionEvent(String action) {
        if (!isPending) {
    		if (logger.isActivated()) {
    			logger.debug("Connection event " + action);
    		}

    		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
    			
    			// Check received network info
    	    	NetworkInfo networkInfo = networkConnection.getConnectionMngr().getActiveNetworkInfo();
    			if ((networkInfo != null) && networkInfo.isConnected()) {
                    isPending = true;
    				if (logger.isActivated()) {
    					logger.debug("Connected to data network");
    				}
    	            Thread t = new Thread() {
    	                public void run() {
    	                    updateConfig();
    	                }
    	            };
    	            t.start();

                    // Unregister network state listener
    	            networkConnection.unregisterNetworkStateListener();
                    isPending = false;
                    return true;
    			}
    		}
        }
        return false;
    } 

	/**
     * Execute an HTTP request
     *
     * @param protocol HTTP protocol
     * @param request HTTP request
     * @return HTTP response
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    protected HttpResponse executeRequest(String protocol, String request, DefaultHttpClient client, HttpContext localContext) throws URISyntaxException, ClientProtocolException, IOException {
        try {
            HttpGet get = new HttpGet();
            get.setURI(new URI(protocol + "://" + request));
            get.addHeader("Accept-Language", HttpsProvisioningUtils.getUserLanguage());
            if (logger.isActivated()) {
                logger.debug("HTTP request: " + get.getURI().toString());
            }
            
            HttpResponse response = client.execute(get, localContext);
            if (logger.isActivated()) {
                logger.debug("HTTP response: " + response.getStatusLine().toString());
            }
            return response;
        } catch (UnknownHostException e) {
            if (logger.isActivated()) {
                logger.debug("The server " + request + " can't be reached!");
            }
            return null;
        }
    }

    /**
     * Get the HTTPS request arguments
     * 
     * @param imsi Imsi
     * @param imei Imei
     * @return {@link String} with the HTTPS request arguments.
     */
    protected String getHttpsRequestArguments(String imsi, String imei) {
        return getHttpsRequestArguments(imsi, imei, null, null, null);
    }

    /**
     * Get the HTTPS request arguments
     * 
     * @param imsi Imsi
     * @param imei Imei
     * @param smsPort SMS port
     * @param token Provisioning token
     * @param msisdn MSISDN
     * @return {@link String} with the HTTPS request arguments. 
     */
    private String getHttpsRequestArguments(String imsi, String imei, String smsPort, String token, String msisdn) {
    	String vers = RcsSettings.getInstance().getProvisioningVersion();
    	if (this.user && ProvisioningInfo.Version.DISABLED_DORMANT.equals(vers)) {
    		vers = LauncherUtils.getProvisioningVersion(mCtx);
    		this.user = false;
    	}
    		
        String args = "?vers=" + vers
        		+ "&rcs_version=" + HttpsProvisioningUtils.getRcsVersion()
                + "&rcs_profile=" + HttpsProvisioningUtils.getRcsProfile()
                + "&client_vendor=" + HttpsProvisioningUtils.getClientVendorFromContext(mCtx)
                + "&client_version=" + HttpsProvisioningUtils.getClientVersionFromContext(mCtx)
                + "&terminal_vendor=" + HttpUtils.encodeURL(HttpsProvisioningUtils.getTerminalVendor())
                + "&terminal_model=" + HttpUtils.encodeURL(HttpsProvisioningUtils.getTerminalModel())
                + "&terminal_sw_version=" + HttpUtils.encodeURL(HttpsProvisioningUtils.getTerminalSoftwareVersion());
        if (imsi != null) {
        	// Add optional parameter IMSI only if available
            args += "&IMSI=" + imsi;
        }
        if (imei != null) {
        	// Add optional parameter IMEI only if available
            args += "&IMEI=" + imei;
        }
        if (smsPort != null) {
        	// Add SMS port if available
            args += "&SMS_port=" + smsPort;
        }
        if (msisdn != null) {
        	// Add token if available
            args += "&msisdn=" + msisdn;
        }
        if (token != null) {
        	// Add token if available
            args += "&token=" + token;
        }
        return args;
    }

	/**
	 * Send the first HTTPS request to require the one time password (OTP)
	 * 
	 * @param imsi IMSI
	 * @param imei IMEI
	 * @param requestUri Request URI
	 * @param client Instance of {@link DefaultHttpClient} 
	 * @param localContext Instance of {@link HttpContext}
	 * @return Instance of {@link HttpsProvisioningResult} or null in case of internal exception
	 */
    protected HttpsProvisioningResult sendFirstRequestsToRequireOTP(String imsi, String imei, String msisdn, String primaryUri, String secondaryUri, DefaultHttpClient client, HttpContext localContext) {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        try {
            if (logger.isActivated()) {
                logger.debug("HTTP provisioning - Send first HTTPS request to require OTP");
            }

            // Generate the SMS port for provisioning
            String smsPortForOTP = HttpsProvisioningSMS.generateSmsPortForProvisioning();

            // Format first HTTPS request with extra parameters (IMSI and IMEI if available plus SMS_port and token)
            String token = (!TextUtils.isEmpty(RcsSettings.getInstance().getProvisioningToken()) ? RcsSettings.getInstance().getProvisioningToken() : "");
            String args = getHttpsRequestArguments(imsi, imei, smsPortForOTP, token, msisdn);

            // Execute first HTTPS request with extra parameters
            String request = primaryUri + args;
            HttpResponse response = executeRequest("https", request, client, localContext);
            if (response == null && !StringUtils.isEmpty(secondaryUri)) {
                // First server not available, try the secondaryUri
                request = secondaryUri + args;
                response = executeRequest("https", request, client, localContext);
            }
            if (response == null) {
                return null;
            }

            result.code = response.getStatusLine().getStatusCode();
            result.content = new String(EntityUtils.toByteArray(response.getEntity()), "UTF-8");
            if (result.code != 200) {
                if (result.code == 403) {
                    if (logger.isActivated()) {
                        logger.debug("First HTTPS request to require OTP failed: Forbidden (request status code: 403) for msisdn "+msisdn);
                    }
                    
                    msisdn = RcsSettings.getInstance().getMsisdn();
                    msisdn = HttpsProvionningMSISDNInput.getInstance().displayPopupAndWaitResponse(mCtx);

                    if (msisdn == null) {
                        return null;
                    } else {
                        return sendFirstRequestsToRequireOTP(imsi, imei, msisdn, primaryUri, secondaryUri, client, localContext);
                    }

                } else if (result.code == 503) {
                    if (logger.isActivated()) {
                        logger.debug("First HTTPS request to require OTP failed: Retry After (request status code: 503)");
                    }
                    result.retryAfter = getRetryAfter(response);
                } else if (result.code == 511) {
                    if (logger.isActivated()) {
                        logger.debug("First HTTPS request to require OTP failed: Invalid token (request status code: 511)");
                    }
                }

            } else {
                if (logger.isActivated()) {
                    logger.debug("HTTPS request returns with 200 OK.");
                }
                
                // Register SMS provisioning receiver
                smsManager.registerSmsProvisioningReceiver(mLocalContentResolver, smsPortForOTP, primaryUri, client, localContext);
                
                // Save the MSISDN
                RcsSettings.getInstance().setMsisdn(msisdn);
                
                // If the content is empty, means that the configuration XML is not present 
                // and the Token is invalid then we need to wait for the SMS with OTP
                if (TextUtils.isEmpty(result.content)) {
                    // Wait for SMS OTP
                    result.waitingForSMSOTP = true;
                }
            }

            // If not waiting for the sms with OTP
            if (!result.waitingForSMSOTP) {
                // Unregister SMS provisioning receiver
            	smsManager.unregisterSmsProvisioningReceiver();
            }

            return result;
        } catch (UnknownHostException e) {
            if (logger.isActivated()) {
                logger.warn("First HTTPS request to require OTP failed: Provisioning server not reachable");
            }
            return null;
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("First HTTPS request to require OTP failed: Can't get config via HTTPS", e);
            }
            return null;
        }
    }

    
    /**
     * Update provisioning config with OTP
     * 
     * @param otp One time password
     * @param requestUri Request URI
     * @param client Instance of {@link DefaultHttpClient}
     * @param localContext Instance of {@link HttpContext}
     */
    protected void updateConfigWithOTP(String otp, String requestUri, DefaultHttpClient client, HttpContext localContext) {
		// Cancel previous retry alarm
		HttpsProvisioningService.cancelRetryAlarm(mCtx, retryIntent);

        // Get config via HTTPS with OTP
        HttpsProvisioningResult result = sendSecondHttpsRequestWithOTP(otp, requestUri, client, localContext);
        
        // Process HTTPS provisioning result
        processProvisioningResult(result);
    }
    

    /**
     * Build the provisioning address with SIM information
     *
     * @return provisioning URI
     */
    private String buildProvisioningAddress(TelephonyManager tm) {
        // Get SIM info
        String ope = tm.getSimOperator();
        if (ope == null || ope.length() < 4) {
            if (logger.isActivated()) {
                logger.warn("Can not read network operator from SIM card!");
            }
            return null;
        }
        String mnc = ope.substring(3);
        String mcc = ope.substring(0, 3);
        while (mnc.length() < 3) { // Set mnc on 3 digits
            mnc = "0" + mnc;
        }
        return "config.rcs." + "mnc" + mnc + ".mcc" + mcc + ".pub.3gppnetwork.org";
    }

    /**
	 * Get configuration
	 * 
	 * @return Result or null in case of internal exception
	 */
	private HttpsProvisioningResult getConfig() {
		HttpsProvisioningResult result = new HttpsProvisioningResult();
		try {
			if (logger.isActivated()) {
				logger.debug("Request config via HTTPS");
			}

            // Get provisioning address
            TelephonyManager tm = (TelephonyManager)mCtx.getSystemService(Context.TELEPHONY_SERVICE);
            String primaryUri = null;
            String secondaryUri = null;
            if (RcsSettings.getInstance().isSecondaryProvisioningAddressOnly()) {
                primaryUri = RcsSettings.getInstance().getSecondaryProvisioningAddress();
            } else {
                primaryUri = buildProvisioningAddress(tm);
                secondaryUri = RcsSettings.getInstance().getSecondaryProvisioningAddress();
            }

            // Check if a configuration file for HTTPS provisioning exists
            String PROVISIONING_FILE = Environment.getExternalStorageDirectory().getPath() + "/joyn_provisioning.txt";
            try {
                File file = new File(PROVISIONING_FILE);
                if (file.exists()) {
                	if (logger.isActivated()) {
                        logger.debug("Provisioning file found !");
                    }
                    FileInputStream fis = new FileInputStream(PROVISIONING_FILE);
                    DataInputStream in = new DataInputStream(fis);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    primaryUri = br.readLine();
                    secondaryUri = null;
                    in.close();
                }
            } catch (Exception e) {
                // Nothing to do
            }

            if (logger.isActivated()) {
                logger.debug("HCS/RCS Uri to connect: "+ primaryUri + " or " + secondaryUri);
            }

			String imsi = tm.getSubscriberId();
			String imei = tm.getDeviceId();
	    	tm = null;

	    	// Format HTTP request
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

			HttpParams params = new BasicHttpParams();
			params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
			params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, 
					new ConnPerRouteBean(30));
			params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            NetworkInfo networkInfo = networkConnection.getConnectionMngr().getActiveNetworkInfo();

            if (networkInfo != null) {
                String proxyHost = Proxy.getDefaultHost();
                if (proxyHost != null && proxyHost.length() > 1) {
                    int proxyPort = Proxy.getDefaultPort();
                    params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, proxyPort));
                }
            }
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

			// Support broad variety of different cookie types (not just Netscape but RFC 2109 and RFC2965 compliant ones, too)  
			HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);

			ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
			DefaultHttpClient client = new DefaultHttpClient(cm, params);
			CookieStore cookieStore = (CookieStore) new BasicCookieStore();
			HttpContext localContext = new BasicHttpContext();
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // If network is not mobile network, use request with OTP
            if (networkInfo != null && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
                // Proceed with non mobile network registration
                return sendFirstRequestsToRequireOTP(imsi, imei, null, primaryUri, secondaryUri, client, localContext);
            }

            if (logger.isActivated()) {
                logger.debug("HTTP provisioning on mobile network");
            }

			// Execute first HTTP request
            String requestUri = primaryUri;
            HttpResponse response = executeRequest("http", requestUri, client, localContext);
            if (response == null && !StringUtils.isEmpty(secondaryUri)) {
                // First server not available, try the secondaryUri
                requestUri = secondaryUri;
                response = executeRequest("http", requestUri, client, localContext);
            }
            if (response == null) {
                return null;
            }

            result.code = response.getStatusLine().getStatusCode(); 
			result.content = new String(EntityUtils.toByteArray(response.getEntity()), "UTF-8");
            if (result.code == 511) {
                // Blackbird guidelines ID_2_6 Configuration mechanism over PS without Header Enrichment
                // Use SMS provisioning on PS data network if server reply 511 NETWORK AUTHENTICATION REQUIRED 
                return sendFirstRequestsToRequireOTP(imsi, imei, null, primaryUri, secondaryUri, client, localContext);
            } else if (result.code != 200) {
                if (result.code == 503) {
                    result.retryAfter = getRetryAfter(response);
                }
                return result;
            }

			// Format second HTTPS request
			String args = getHttpsRequestArguments(imsi, imei);
            String request = requestUri + args;
            if (logger.isActivated()) {
                logger.info("Request provisioning: "+ request);
            }

            // Execute second HTTPS request
            response = executeRequest("https", request, client, localContext);
            if (response == null) {
                return null;
            }
			result.code = response.getStatusLine().getStatusCode();
			if (result.code != 200) {
                if (result.code == 503) {
                    result.retryAfter = getRetryAfter(response);
                }
				return result;
			}
			result.content = new String(EntityUtils.toByteArray(response.getEntity()), "UTF-8");
			return result;
		} catch(UnknownHostException e) {
			if (logger.isActivated()) {
				logger.warn("Provisioning server not reachable");
			}
			return null;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't get config via HTTPS", e);
			}
			return null;
		}
	}

    /**
     * Update provisioning config
     */
    protected void updateConfig() {
		// Cancel previous retry alarm
		HttpsProvisioningService.cancelRetryAlarm(mCtx, retryIntent);

        // Get config via HTTPS
        HttpsProvisioningResult result = getConfig();
        
        // Process HTTPS provisioning result
        processProvisioningResult(result);
    }
    
    /**
     * Send the second HTTPS request with the one time password (OTP)
     * 
     * @param otp One time password
     * @param requestUri Request URI
     * @param client Instance of {@link DefaultHttpClient} 
     * @param localContext Instance of {@link HttpContext}
     * @return Instance of {@link HttpsProvisioningResult} or null in case of internal exception
     */
	protected HttpsProvisioningResult sendSecondHttpsRequestWithOTP(String otp, String requestUri, DefaultHttpClient client, HttpContext localContext) {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        try {
            if (logger.isActivated()) {
                logger.debug("Send second HTTPS with OTP");
            }

            // Format second HTTPS request
            String args = "?OTP=" + otp;
            String request = requestUri + args;

            if (logger.isActivated()) {
                logger.info("Request provisioning with OTP: " + request);
            }

            // Execute second HTTPS request
            HttpResponse response = executeRequest("https", request, client, localContext);
            if (response == null) {
                return null;
            }
            result.code = response.getStatusLine().getStatusCode();
            if (result.code != 200) {
                if (result.code == 503) {
                    result.retryAfter = getRetryAfter(response);
                } else if (result.code == 511) {
                    if (logger.isActivated()) {
                        logger.debug("Second HTTPS request with OTP failed: Invalid one time password (request status code: 511)");
                    }
                }
                return result;
            }
            result.content = new String(EntityUtils.toByteArray(response.getEntity()), "UTF-8");

            return result;
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Second HTTPS request with OTP failed: Can't get config via HTTPS", e);
            }
            return null;
        }
    }
	
    /**
     * Get retry-after value
     * 
     * @return retry-after value
     */
    protected int getRetryAfter(HttpResponse response) {
        Header[] headers = response.getHeaders("Retry-After");
        if (headers.length > 0) {
            try {
                return Integer.parseInt(headers[0].getValue());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Process provisioning result
     * 
     * @param result Instance of {@link HttpsProvisioningResult}
     */
	private void processProvisioningResult(HttpsProvisioningResult result) {
		if (result != null) {
			if (result.code == 200) {
				// Reset after 511 counter
				retryAfter511ErrorCount = 0;

				if (result.waitingForSMSOTP) {
					if (logger.isActivated()) {
						logger.debug("Waiting for SMS with OTP.");
					}
					return;
				}

				if (logger.isActivated()) {
					logger.debug("Provisioning request successful");
				}

				// Parse the received content
				ProvisioningParser parser = new ProvisioningParser(result.content);
				
				// Save GSMA release set into the provider
				GsmaRelease gsmaRelease = RcsSettings.getInstance().getGsmaRelease();
				// Save client Messaging Mode set into the provider
				MessagingMode messagingMode = RcsSettings.getInstance().getMessagingMode();
				
				// Before parsing the provisioning, the GSMA release is set to Albatros
				RcsSettings.getInstance().setGsmaRelease(GsmaRelease.ALBATROS);
				// Before parsing the provisioning, the client Messaging mode is set to NONE 
				RcsSettings.getInstance().setMessagingMode(MessagingMode.NONE);
				
				if (parser.parse(gsmaRelease, first)) {
					// Successfully provisioned, 1st time reg finalized
					first = false;
					ProvisioningInfo info = parser.getProvisioningInfo();
					
					// Save version
					String version = info.getVersion();
					long validity = info.getValidity();
					if (logger.isActivated()) {
						logger.debug("Provisioning version=" + version + ", validity=" + validity);
					}
					
					// Save the latest positive version of the configuration
					LauncherUtils.saveProvisioningVersion(mCtx, version);
					
					// Save the validity of the configuration
					LauncherUtils.saveProvisioningValidity(mCtx, validity);
					RcsSettings.getInstance().setProvisioningVersion(version);

					// Save token
					String token = info.getToken();
					long tokenValidity = info.getTokenValidity();
					if (logger.isActivated()) {
						logger.debug("Provisioning Token=" + token + ", validity=" + tokenValidity);
					}
					RcsSettings.getInstance().setProvisioningToken(token);
					
					// Reset retry alarm counter
			        retryCount = 0;
					if (ProvisioningInfo.Version.DISABLED_DORMANT.equals(version)) {
						// -3 : Put RCS client in dormant state
						if (logger.isActivated()) {
							logger.debug("Provisioning: RCS client in dormant state");
						}
						// Start retry alarm
						if (validity > 0) {
							HttpsProvisioningService.startRetryAlarm(mCtx, retryIntent, validity * 1000);
						}
						// We parsed successfully the configuration
						RcsSettings.getInstance().setConfigurationValid(true);
						// Stop the RCS core service. Provisioning is still running.
						LauncherUtils.stopRcsCoreService(mCtx);
					} else {
						if (ProvisioningInfo.Version.DISABLED_NOQUERY.equals(version)) {
							// -2 : Disable RCS client and stop configuration query
							if (logger.isActivated()) {
								logger.debug("Provisioning: disable RCS client");
							}
							// We parsed successfully the configuration
							RcsSettings.getInstance().setConfigurationValid(true);
							// Disable and stop RCS service
							RcsSettings.getInstance().setServiceActivationState(false);
							LauncherUtils.stopRcsService(mCtx);
						} else {
							if (ProvisioningInfo.Version.RESETED_NOQUERY.equals(version)) {
								// -1 Forbidden: reset account + version = 0-1 (doesn't restart)
								if (logger.isActivated()) {
									logger.debug("Provisioning forbidden: reset account");
								}
								// Reset config
								LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver);
								// Force version to "-1" (resetRcs set version to "0")
								RcsSettings.getInstance().setProvisioningVersion(version);
								// Disable the RCS service
								RcsSettings.getInstance().setServiceActivationState(false);
							} else {
								if (ProvisioningInfo.Version.RESETED.equals(version)) {
									if (logger.isActivated()) {
										logger.debug("Provisioning forbidden: no account");
									}
									// Reset config
									LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver);
								} else {
									// Start retry alarm
									if (validity > 0) {
										HttpsProvisioningService.startRetryAlarm(mCtx, retryIntent, validity * 1000);
									}
									// Terms request
									if (info.getMessage() != null && !RcsSettings.getInstance().isProvisioningTermsAccepted()) {
										showTermsAndConditions(info);
									}
									// We parsed successfully the configuration
									RcsSettings.getInstance().setConfigurationValid(true);
									// Start the RCS core service
									LauncherUtils.launchRcsCoreService(mCtx);
								}
							}
						}
					}

					// Send service provisioning intent
					Intent serviceProvisioned = new Intent(RcsService.ACTION_SERVICE_PROVISIONED);
					IntentUtils.tryToSetReceiverForegroundFlag(serviceProvisioned);
					mCtx.sendBroadcast(serviceProvisioned);
				} else {
					if (logger.isActivated()) {
						logger.debug("Can't parse provisioning document");
					}
					// Restore GSMA release saved before parsing of the provisioning
					RcsSettings.getInstance().setGsmaRelease(gsmaRelease);
					
					// Restore the client messaging mode saved before parsing of the provisioning
					RcsSettings.getInstance().setMessagingMode(messagingMode);
					
					if (first) {
						if (logger.isActivated()) {
							logger.debug("As this is first launch and we do not have a valid configuration yet, retry later");
						}
						// Reason: Invalid configuration
						provisioningFails(ProvisioningFailureReasons.INVALID_CONFIGURATION);
						retry();
					} else {
						if (logger.isActivated()) {
							logger.debug("This is not first launch, use old configuration to register");
						}
						tryLaunchRcsCoreService(mCtx, -1);
					}
				}
			} else if (result.code == 503) {
				// Server Unavailable
				if (logger.isActivated()) {
					logger.debug("Server Unavailable. Retry after: " + result.retryAfter);
				}
				if (first) {
					// Reason: Unable to get configuration
					provisioningFails(ProvisioningFailureReasons.UNABLE_TO_GET_CONFIGURATION);
					if (result.retryAfter > 0) {
						HttpsProvisioningService.startRetryAlarm(mCtx, retryIntent, result.retryAfter * 1000);
					}
				} else {
					tryLaunchRcsCoreService(mCtx, result.retryAfter * 1000);
				}
			} else if (result.code == 403) {
				// Forbidden: reset account + version = 0
				if (logger.isActivated()) {
					logger.debug("Provisioning forbidden: reset account");
				}
				// Reset version to "0"
				RcsSettings.getInstance().setProvisioningVersion(Version.RESETED.toString());
				// Reset config
				LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver);
				// Reason: Provisioning forbidden
				provisioningFails(ProvisioningFailureReasons.PROVISIONING_FORBIDDEN);
			} else if (result.code == 511) {
				// Provisioning authentication required
				if (logger.isActivated()) {
					logger.debug("Provisioning authentication required");
				}
				// Reset provisioning token
				RcsSettings.getInstance().setProvisioningToken("");
				// Retry after reseting provisioning token
				if (!retryAfter511Error()) {
					// Reason: Provisioning authentication required
					provisioningFails(ProvisioningFailureReasons.PROVISIONING_AUTHENTICATION_REQUIRED);
				}
			} else {
				// Other error
				if (logger.isActivated()) {
					logger.debug("Provisioning error " + result.code);
				}
				// Start the RCS service
				if (first) {
					// Reason: No configuration present
					provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
					retry();
				} else {
					tryLaunchRcsCoreService(mCtx, -1);
				}
			}
		} else { // result is null
			// Start the RCS service
			if (first) {
				// Reason: No configuration present
				if (logger.isActivated()) {
					logger.error("### Provisioning fails and first = true!");
				}
				provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
				retry();
			} else {
				tryLaunchRcsCoreService(mCtx, -1);
			}
		}
	}
    
	/**
	 * Try to launch RCS Core Service. RCS Service is only launched if version is positive.
	 * 
	 * @param context
	 * @param timerRetry
	 *            timer to trigger next provisioning request. Only applicable if greater than 0.
	 */
	private void tryLaunchRcsCoreService(Context context, int timerRetry) {
		try {
			int version = Integer.parseInt(RcsSettings.getInstance().getProvisioningVersion());
			// Only launch service if version is positive
			if (version > 0) {
				// Start the RCS service
				LauncherUtils.launchRcsCoreService(context);
				if (timerRetry > 0) {
					HttpsProvisioningService.startRetryAlarm(context, retryIntent, timerRetry);
				} else
					retry();
			} else {
				// Only retry provisioning if service is disabled dormant (-3)
				if (ProvisioningInfo.Version.DISABLED_DORMANT.getVersion() == version) {
					if (timerRetry > 0) {
						HttpsProvisioningService.startRetryAlarm(context, retryIntent, timerRetry);
					} else
						retry();
				}
			}
		} catch (NumberFormatException e) {
		}
	}

    /**
     * Show the terms and conditions request
     *
     * @param info Provisioning info
     */
    private void showTermsAndConditions(ProvisioningInfo info) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(mCtx, TermsAndConditionsRequest.class);

        // Required as the activity is started outside of an Activity context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // Add intent parameters
        intent.putExtra(TermsAndConditionsRequest.ACCEPT_BTN_KEY, info.getAcceptBtn());
        intent.putExtra(TermsAndConditionsRequest.REJECT_BTN_KEY, info.getRejectBtn());
        intent.putExtra(TermsAndConditionsRequest.TITLE_KEY, info.getTitle());
        intent.putExtra(TermsAndConditionsRequest.MESSAGE_KEY, info.getMessage());

        mCtx.startActivity(intent);
    }
    
    /**
     * Retry after 511 "Network authentication required" procedure
     * 
     * @return <code>true</code> if retry is performed, otherwise <code>false</code>
     */
    private boolean retryAfter511Error() {
        if (retryAfter511ErrorCount < HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_MAX_COUNT) {
            retryAfter511ErrorCount++;
            HttpsProvisioningService.startRetryAlarm(mCtx, retryIntent, HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_TIMEOUT);
            if (logger.isActivated()) {
                logger.debug("Retry after 511 error (" + retryAfter511ErrorCount + "/" + HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_MAX_COUNT + ") provisionning after " + HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_TIMEOUT + "ms");
            }
            return true;
        }

        if (logger.isActivated()) {
            logger.debug("No more retry after 511 error for provisionning");
        }
        
        // Reset after 511 counter
        retryAfter511ErrorCount = 0;

        return false;
    }
    
    /**
     * Provisioning fails.
     * 
     * @param reason Reason of failure
     */
    public void provisioningFails(int reason) {
        // If wifi is active network access type
        if (NetworkUtils.getNetworkAccessType() == NetworkUtils.NETWORK_ACCESS_WIFI) {
            // Register Wifi disabling listener
        	networkConnection.registerWifiDisablingListener();
        }
    }

    /**
     * Retry procedure
     */
    private void retry() {
        if (retryCount < HttpsProvisioningUtils.RETRY_MAX_COUNT) {
            retryCount++;
            int retryDelay = HttpsProvisioningUtils.RETRY_BASE_TIMEOUT + 2 * (retryCount - 1) * HttpsProvisioningUtils.RETRY_BASE_TIMEOUT;
            HttpsProvisioningService.startRetryAlarm(mCtx, retryIntent, retryDelay);
            if (logger.isActivated()) {
                logger.debug("Retry provisionning count: "+retryCount );
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("No more retry for provisionning");
            }
        }
    }
    
    /**
     * Transmit to SMS unregister method
     */
	public void unregisterSmsProvisioningReceiver() {
		smsManager.unregisterSmsProvisioningReceiver();
	}

    /**
     * Transmit to Network unregister method
     */
	public void unregisterNetworkStateListener() {
		networkConnection.unregisterNetworkStateListener();
	}

    /**
     * Transmit to Network unregister wifi method
     */
	public void unregisterWifiDisablingListener() {
		networkConnection.unregisterWifiDisablingListener();
	}

    /**
     * Transmit to Network register method
     */
	public void registerNetworkStateListener() {
		networkConnection.registerNetworkStateListener();
	}

    /**
     * Retry procedure
     */
	public void resetCounters() {
		 // Reset retry alarm counter
        retryCount = 0;

        // Reset after 511 counter
        retryAfter511ErrorCount = 0;
	}
}
