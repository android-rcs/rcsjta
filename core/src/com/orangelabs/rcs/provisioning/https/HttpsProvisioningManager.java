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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provisioning.ProvisioningFailureReasons;
import com.orangelabs.rcs.provisioning.ProvisioningInfo;
import com.orangelabs.rcs.provisioning.ProvisioningParser;
import com.orangelabs.rcs.provisioning.TermsAndConditionsRequest;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.HttpUtils;
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
     * Retry counter
     */
    private int retryCount = 0;

    /**
     * Check if a provisioning request is already pending
     */
    private boolean isPending = false;

    /**
     * The Service Context
     */
	private Context context;

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
     */
    public HttpsProvisioningManager(Context applicationContext, PendingIntent retryIntent) {
		this.context = applicationContext;
		this.retryIntent = retryIntent;
		
		smsManager = new HttpsProvisioningSMS(this);
		networkConnection = new HttpsProvisioningConnection(this);
	}

    /**
     * @return the context
     */
    protected Context getContext() {
        return context;
    }

    /**
     * Set the first launch boolean
     *
     * @param isFirstLaunch Boolean to set
     */
	protected void setIsFirstLaunch(boolean isFirstLaunch) {
		first = isFirstLaunch;
	}

	/**
     * Get if it is the first launch
     *
     * @param isFirstLaunch Boolean to set
     */
	protected boolean getIsFirstLaunch() {
		return first;
	}

    /**
     * Set the first launch boolean with the extra in the intent, if the extra is not found, sets to false
     *
     * @param intent Intent in which to look for the first Extra value
     * @return first The boolean value set, returns false if the intent does not content the extra
     */
    protected boolean setIsFirstLaunchFromIntent(Intent intent) {
		// Get intent parameter
        if (intent != null) {
            first = intent.getBooleanExtra(HttpsProvisioningUtils.FIRST_KEY, false);
        } else {
            first = false;
        }
        return first;
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
    			if ((networkInfo != null) &&
    			     // Changed by Deutsche Telekom (workaround)
					 //(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) &&
						 networkInfo.isConnected()) {
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
     * @return {@link String} with the HTTPS request arguments. 
     */
    private String getHttpsRequestArguments(String imsi, String imei, String smsPort, String token, String msisdn) {
        String args = "?vers=" + RcsSettings.getInstance().getProvisioningVersion()
        		+ "&rcs_version=" + HttpsProvisioningUtils.getRcsVersion()
                + "&rcs_profile=" + HttpsProvisioningUtils.getRcsProfile()
                + "&client_vendor=" + HttpsProvisioningUtils.getClientVendorFromContext(context)
                + "&client_version=" + HttpsProvisioningUtils.getClientVersionFromContext(context)
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
	 * @param imsi Imsi
	 * @param imei Imei
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
                response = executeRequest("http", request, client, localContext);
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
                    msisdn = HttpsProvionningMSISDNInput.getInstance().displayPopupAndWaitResponse(context);

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
                smsManager.registerSmsProvisioningReceiver(smsPortForOTP, primaryUri, client, localContext);
                
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
     * Start retry alarm
     * 
     * @param delay (ms)
     */
    protected void startRetryAlarm(long delay) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, retryIntent);
    }

    /**
     * Cancel retry alarm
     */
    protected void cancelRetryAlarm() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(retryIntent);
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
        cancelRetryAlarm();

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
            TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
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
			if (result.code != 200) {
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
    	cancelRetryAlarm();

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
                if (parser.parse()) {
                    // Successfully provisioned, 1st time reg finalized
                    first = false;
                    ProvisioningInfo info = parser.getProvisioningInfo();

                    // Save version
                    String version = info.getVersion();
                    long validity = info.getValidity();
                    if (logger.isActivated()) {
                        logger.debug("Provisioning version " + version + ", validity " + validity);
                    }
                    RcsSettings.getInstance().setProvisioningVersion(version);
                    
                    // Save token
                    String token = info.getToken();
                    long tokenValidity = info.getTokenValidity();
                    if (logger.isActivated()) {
                        logger.debug("Provisioning Token " + token + ", validity " + tokenValidity);
                    }
                    RcsSettings.getInstance().setProvisioningToken(token);
                    
                    if (version.equals("-1") && validity == -1) {
                        // Forbidden: reset account + version = 0-1 (doesn't restart)
                        if (logger.isActivated()) {
                            logger.debug("Provisioning forbidden: reset account");
                        }

                        // Reset config
                        LauncherUtils.resetRcsConfig(context);
                        
                        // Force version to "-1" (resetRcs set version to "0")
                        RcsSettings.getInstance().setProvisioningVersion(version);
                    } else if (version.equals("0") && validity == 0) {
                        // Forbidden: reset account + version = 0
                        if (logger.isActivated()) {
                            logger.debug("Provisioning forbidden: reset account");
                        }

                        // Reset config
                        LauncherUtils.resetRcsConfig(context);
                    } else {
                        // Start retry alarm
                        if (validity > 0) {
                            if (logger.isActivated()) {
                                logger.debug("Provisioning retry after validity " + validity);
                            }
                            retryCount = 0;
                            startRetryAlarm(validity * 1000);
                        }

                        // Terms request
                        if (info.getMessage() != null && !RcsSettings.getInstance().isProvisioningTermsAccepted()) {
                            showTermsAndConditions(info);
                        }

                        // Start the RCS core service
                        if (first) {
                            LauncherUtils.forceLaunchRcsCoreService(context);
                        } else {
                            LauncherUtils.launchRcsCoreService(context);
                        }
                    }
                } else {
                    if (logger.isActivated()) {
                        logger.debug("Can't parse provisioning document");
                    }
                    if (first){
                    	if (logger.isActivated()){
                    		logger.debug("As this is first launch and we do not have a valid configuration yet, retry later");
                    	}

						// Reason: Invalid configuration
						provisioningFails(ProvisioningFailureReasons.INVALID_CONFIGURATION);
                    	retry();
                    }else{
                    	if (logger.isActivated()){
                    		logger.debug("This is not first launch, use old configuration to register");
                    	}
                        // Start the RCS service
                    	LauncherUtils.launchRcsCoreService(context);
                    }
                }
            } else if (result.code == 503) {
                // Retry after
                if (logger.isActivated()) {
                    logger.debug("Provisioning retry after " + result.retryAfter);
                }

                // Start retry alarm
                if (result.retryAfter > 0) {
                	startRetryAlarm(result.retryAfter * 1000);
                }

                // Start the RCS service
                if (!first) {
                    LauncherUtils.launchRcsCoreService(context);
				} else {
					// Reason: Unable to get configuration
					provisioningFails(ProvisioningFailureReasons.UNABLE_TO_GET_CONFIGURATION);
				}
            } else if (result.code == 403) {
                // Forbidden: reset account + version = 0
                if (logger.isActivated()) {
                    logger.debug("Provisioning forbidden: reset account");
                }

                // Reset version to "0"
                RcsSettings.getInstance().setProvisioningVersion("0");

                // Reset config
                LauncherUtils.resetRcsConfig(context);

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
                if (!first) {
                    LauncherUtils.launchRcsCoreService(context);
				} else {
					// Reason: No configuration present
					provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
				}
                retry();
            }
        } else { // result is null
            // Start the RCS service
            if (!first) {
                LauncherUtils.launchRcsCoreService(context);
			} else {
				// Reason: No configuration present
				if (logger.isActivated()) {
                    logger.error("### Provisioning fails and first = true!");
                }
				provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
			}
            retry();
        }
    }

    /**
     * Show the terms and conditions request
     *
     * @param info Provisioning info
     */
    private void showTermsAndConditions(ProvisioningInfo info) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(context, TermsAndConditionsRequest.class);

        // Required as the activity is started outside of an Activity context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // Add intent parameters
        intent.putExtra(TermsAndConditionsRequest.ACCEPT_BTN_KEY, info.getAcceptBtn());
        intent.putExtra(TermsAndConditionsRequest.REJECT_BTN_KEY, info.getRejectBtn());
        intent.putExtra(TermsAndConditionsRequest.TITLE_KEY, info.getTitle());
        intent.putExtra(TermsAndConditionsRequest.MESSAGE_KEY, info.getMessage());

        context.startActivity(intent);
    }
    
    /**
     * Retry after 511 "Network authentication required" procedure
     * 
     * @return <code>true</code> if retry is performed, otherwise <code>false</code>
     */
    private boolean retryAfter511Error() {
        if (retryAfter511ErrorCount < HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_MAX_COUNT) {
            retryAfter511ErrorCount++;
            startRetryAlarm(HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_TIMEOUT);
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
            startRetryAlarm(retryDelay);
            if (logger.isActivated()) {
                logger.debug("Retry (" + retryCount +  ") provisionning after " + retryDelay + "ms");
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
