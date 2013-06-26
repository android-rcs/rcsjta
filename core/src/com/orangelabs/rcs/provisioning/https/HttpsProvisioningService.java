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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provisioning.ProvisioningInfo;
import com.orangelabs.rcs.provisioning.ProvisioningParser;
import com.orangelabs.rcs.provisioning.TermsAndConditionsRequest;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.AppUtils;
import com.orangelabs.rcs.utils.HttpUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * HTTPS auto configuration service
 * 
 * @author hlxn7157
 * @author G. LE PESSOT
 * @author Deutsche Telekom AG
 */
public class HttpsProvisioningService extends Service {
    /**
     * Intent key
     */
    public static final String FIRST_KEY = "first";

    /**
	 * Unknown value
	 */
	private static final String UNKNOWN = "unknown";

    /**
     * Retry base timeout - 5min 
     */
    private static final int RETRY_BASE_TIMEOUT = 300000;

    /**
     * Retry max count
     */
    private static final int RETRY_MAX_COUNT = 5;

	/**
	 * Connection manager
	 */
	private ConnectivityManager connMgr = null;

	/**
     * Retry intent
     */
    private PendingIntent retryIntent;

    /**
     * First launch flag
     */
    private boolean first = false;

    /**
     * Check if a provisioning request is already pending
     */
    private boolean isPending = false;

    /**
     * Network state listener
     */
    private BroadcastReceiver networkStateListener = null;

    /**
     * Retry counter
     */
    private int retryCount = 0;

	/**
	 * The logger
	 */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public void onCreate() {
        // Instantiate RcsSettings
        RcsSettings.createInstance(getApplicationContext());

    	// Get connectivity manager
        if (connMgr == null) {
            connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        // Register the retry listener
        retryIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(this.toString()), 0);
        registerReceiver(retryReceiver, new IntentFilter(this.toString()));
	}

    @Override
    public void onDestroy() {
		// Unregister network state listener
        if (networkStateListener != null) {
        	try {
	            unregisterReceiver(networkStateListener);
	        } catch (IllegalArgumentException e) {
	        	// Nothing to do
	        }
        }

        // Unregister retry receiver
        try {
	        unregisterReceiver(retryReceiver);
	    } catch (IllegalArgumentException e) {
	    	// Nothing to do
	    }
    }

    @Override
    public IBinder onBind(Intent intent) {
    	return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if (logger.isActivated()) {
			logger.debug("Start HTTPS provisioning");
		}

		// Get intent parameter
        if (intent != null) {
            first = intent.getBooleanExtra(HttpsProvisioningService.FIRST_KEY, false);
        } else {
            first = false;
        }

        String version = RcsSettings.getInstance().getProvisioningVersion();
        if (logger.isActivated()) {
        	logger.debug("Provisioning parameter: first=" + first + ", version= " + version);
        }

        // Send default connection event
        if (!connectionEvent(ConnectivityManager.CONNECTIVITY_ACTION)) {
            // If the UpdateConfig has NOT been done: 
            // Instantiate the network listener
            networkStateListener = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, final Intent intent) {
                    Thread t = new Thread() {
                        public void run() {
                            connectionEvent(intent.getAction());
                        }
                    };
                    t.start();
                }
            };

            // Register network state listener
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkStateListener, intentFilter);
        }
		
    	// We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Connection event
     * 
     * @param action Connectivity action
     * @return true if the updateConfig has been done
     */
    private boolean connectionEvent(String action) {
        if (!isPending) {
    		if (logger.isActivated()) {
    			logger.debug("Connection event " + action);
    		}

    		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
    			// Check received network info
    	    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    			if ((networkInfo != null) &&
					 (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) &&
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
                    if (networkStateListener != null) {
                    	try {
	                        unregisterReceiver(networkStateListener);
	                    } catch (IllegalArgumentException e) {
	                    	// Nothing to do
	                    }
                        networkStateListener = null;
                    }
                    isPending = false;
                    return true;
    			}
    		}
        }
        return false;
    }    

    /**
     * Update provisioning config
     */
    private void updateConfig() {
        // Cancel previous retry alarm 
        cancelRetryAlarm();

        // Get config via HTTPS
        HttpsProvisioningResult result = getConfig();
        if (result != null) {
            if (result.code == 200) {
                // Success
                if (logger.isActivated()) {
                    logger.debug("Provisioning successful");
                }

                /* Just for debugging
                try {
        		File file = new File(Environment.getExternalStorageDirectory() + "/provisioning.xml");
        		OutputStream os = new FileOutputStream(file);
        		os.write(result.content.getBytes());
        		os.flush();
                os.close();
                } catch(Exception e) {}*/
                
                // Parse the received content
                ProvisioningParser parser = new ProvisioningParser(result.content);
                if (parser.parse()) {
                    ProvisioningInfo info = parser.getProvisioningInfo();

                    // Save version
                    String version = info.getVersion();
                    long validity = info.getValidity();
                    if (logger.isActivated()) {
                        logger.debug("Provisioning version " + version + ", validity " + validity);
                    }
                    RcsSettings.getInstance().setProvisioningVersion(version);

                    if (version.equals("-1") && validity == -1) {
                        // Forbidden: reset account + version = 0-1 (doesn't restart)
                        if (logger.isActivated()) {
                            logger.debug("Provisioning forbidden: reset account");
                        }

                        // Reset config
                        LauncherUtils.resetRcsConfig(getApplicationContext());
                        
                        // Force version to "-1" (resetRcs set version to "0")
                        RcsSettings.getInstance().setProvisioningVersion(version);
                    } else if (version.equals("0") && validity == 0) {
                        // Forbidden: reset account + version = 0
                        if (logger.isActivated()) {
                            logger.debug("Provisioning forbidden: reset account");
                        }

                        // Reset config
                        LauncherUtils.resetRcsConfig(getApplicationContext());
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
                            LauncherUtils.forceLaunchRcsCoreService(getApplicationContext());
                        } else {
                            LauncherUtils.launchRcsCoreService(getApplicationContext());
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
                    	retry();
                    }else{
                    	if (logger.isActivated()){
                    		logger.debug("This is not first launch, use old configuration to register");
                    	}
                        // Start the RCS service
                    	LauncherUtils.launchRcsCoreService(getApplicationContext());
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
                    LauncherUtils.launchRcsCoreService(getApplicationContext());
                }
            } else if (result.code == 403) {
                // Forbidden: reset account + version = 0
                if (logger.isActivated()) {
                    logger.debug("Provisioning forbidden: reset account");
                }

                // Reset version to "0"
                RcsSettings.getInstance().setProvisioningVersion("0");

                // Reset config
                LauncherUtils.resetRcsConfig(getApplicationContext());
            } else {
                // Other error
                if (logger.isActivated()) {
                    logger.debug("Provisioning error " + result.code);
                }
                // Start the RCS service
                if (!first) {
                    LauncherUtils.launchRcsCoreService(getApplicationContext());
                }
                retry();
            }
        } else {
            // Start the RCS service
            if (!first) {
                LauncherUtils.launchRcsCoreService(getApplicationContext());
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
        intent.setClass(getApplicationContext(), TermsAndConditionsRequest.class);

        // Required as the activity is started outside of an Activity context
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // Add intent parameters
        intent.putExtra(TermsAndConditionsRequest.ACCEPT_BTN_KEY, info.getAcceptBtn());
        intent.putExtra(TermsAndConditionsRequest.REJECT_BTN_KEY, info.getRejectBtn());
        intent.putExtra(TermsAndConditionsRequest.TITLE_KEY, info.getTitle());
        intent.putExtra(TermsAndConditionsRequest.MESSAGE_KEY, info.getMessage());

        getApplicationContext().startActivity(intent);
    }


    /**
     * Retry receiver
     */
    private BroadcastReceiver retryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Thread t = new Thread() {
                public void run() {
                    updateConfig();
                }
            };
            t.start();
        }
    };

    /**
     * Retry procedure
     */
    private void retry() {
        if (retryCount < RETRY_MAX_COUNT) {
            retryCount++;
            int retryDelay = RETRY_BASE_TIMEOUT + 2 * (retryCount - 1) * RETRY_BASE_TIMEOUT;
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
     * Start retry alarm
     * 
     * @param delay (ms)
     */
    private void startRetryAlarm(long delay) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, retryIntent);
    }

    /**
     * Cancel retry alarm
     */
    private void cancelRetryAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(retryIntent);
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

	    	// Get SIM info
	    	TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            // Build provisioning address if empty in settings 
            String requestUri = RcsSettings.getInstance().getProvisioningAddress();
            String oldRequestUri = "";
            if (requestUri.length() == 0) {
    	    	String ope = tm.getSimOperator();
                // Cancel operation if no valid SIM operator
                if (ope == null || ope.length() < 4) {
                    if (logger.isActivated()) {
                        logger.warn("Can not read network operator from SIM card!");
                    }
                    return null;
                }
                String mnc = ope.substring(3);
                String mcc = ope.substring(0, 3);
                oldRequestUri = "config." + mcc + mnc + ".rcse";
                while (mnc.length() < 3) { // Set mnc on 3 digits
                    mnc = "0" + mnc;
                }
                requestUri = "config.rcs." + "mnc" + mnc + ".mcc" + mcc + ".pub.3gppnetwork.org";
            }
			String imsi = tm.getSubscriberId();
			String imei = tm.getDeviceId();
	    	tm = null;

	    	// Format HTTP request
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http",
					PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https",
					new EasySSLSocketFactory(), 443));

			HttpParams params = new BasicHttpParams();
			params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
			params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE,
					new ConnPerRouteBean(30));
			params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                String proxyHost = Proxy.getDefaultHost();
                if (proxyHost != null && proxyHost.length() > 1) {
                    int proxyPort = Proxy.getDefaultPort();
                    params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, proxyPort));
                }
            }
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

			ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
			DefaultHttpClient client = new DefaultHttpClient(cm, params);

			// Create a local instance of cookie store
			CookieStore cookieStore = (CookieStore) new BasicCookieStore();

			// Create local HTTP context
			HttpContext localContext = new BasicHttpContext();

			// Bind custom cookie store to the local context
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			// Execute first HTTP request
            HttpResponse response = null;
            try {
                response = executeRequest("http", requestUri, client, localContext);
            } catch (UnknownHostException e) {
                // If the new URI is not reachable, try the old
                if (logger.isActivated()) {
                    logger.debug("The server " + requestUri + " can't be reachable, try with the old URI");
                }
                requestUri = oldRequestUri;
                response = executeRequest("http",requestUri, client, localContext);
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
			String args = "?vers=" + RcsSettings.getInstance().getProvisioningVersion()
                    + "&rcs_version=" + getRcsVersion()
                    + "&rcs_profile=" + getRcsProfile()
                    + "&client_vendor=" + getClientVendor()
                    + "&client_version=" + getClientVersion()
                    + "&terminal_vendor=" + HttpUtils.encodeURL(getTerminalVendor())
                    + "&terminal_model=" + HttpUtils.encodeURL(getTerminalModel())
                    + "&terminal_sw_version=" + HttpUtils.encodeURL(getTerminalSoftwareVersion());
            if (imsi != null) { // add optional parameter IMSI only if available
                args += "&IMSI=" + imsi;
            }
            if (imei != null) { // add optional parameter IMEI only if available
                args += "&IMEI=" + imei;
            } 
            String request = requestUri + args;
            if (logger.isActivated()) {
                logger.info("Request provisioning: "+ request);
            }

            // Execute second HTTPS request
            response = executeRequest("https", request, client, localContext);
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
     * Execute an HTTP request
     *
     * @param protocol HTTP protocol
     * @param request HTTP request
     * @return HTTP response
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    private HttpResponse executeRequest(String protocol, String request, DefaultHttpClient client, HttpContext localContext) throws URISyntaxException, ClientProtocolException, IOException {
        HttpGet get = new HttpGet();
        get.setURI(new URI(protocol + "://" + request));
        get.addHeader("Accept-Language", getUserLanguage());
        if (logger.isActivated()) {
            logger.debug("HTTP request: " + get.getURI().toString());
        }
        HttpResponse response = client.execute(get, localContext);
        if (logger.isActivated()) {
            logger.debug("HTTP response: " + response.getStatusLine().toString());
        }
        return response;
    }

    /**
     * Get retry-after value
     * 
     * @return retry-after value
     */
    private int getRetryAfter(HttpResponse response) {
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
     * Returns the client vendor
     * 
     * @return String(4)
     */
	private String getClientVendor() {
		String result = UNKNOWN;
		String version = getString(R.string.rcs_client_vendor);
		if (version != null && version.length() > 0) {
			result = version;
		}
		return StringUtils.truncate(result, 4);
	}    

	/**
     * Returns the client version
     * 
     * @return String(15)
     */
	private String getClientVersion() {
		String result = UNKNOWN;

		// Read version from manifest file
		String appVersion = AppUtils.getApplicationVersion(getApplicationContext());

		// Extract version in x.y format
		String version = appVersion;
		if (appVersion != null && appVersion.length() > 0) {
			String[] values = appVersion.split("\\.");
			if (values.length > 2) { 
				version = values[0] + "." + values[1];
			}
		}
		
		// Format version like specified in ID_2_4 Impl guideline 
		result = getString(R.string.rcs_client_version, version);
		
		return StringUtils.truncate(result, 15);
	}

	/**
     * Returns the terminal vendor
     * 
     * @return String(4)
     */
	private String getTerminalVendor() {
		String result = UNKNOWN;
		String productmanufacturer = getSystemProperties("ro.product.manufacturer");
		if (productmanufacturer != null && productmanufacturer.length() > 0) {
			result = productmanufacturer;
		}
		return StringUtils.truncate(result, 4);
	}    

    /**
     * Returns the terminal model
     * 
     * @return String(10)
     */
	private String getTerminalModel() {
		String result = UNKNOWN;
		String devicename = getSystemProperties("ro.product.device");
		if (devicename != null && devicename.length() > 0) {
			result = devicename;
		}
		return StringUtils.truncate(result, 10);
	}

    /**
     * Returns the terminal software version
     * 
     * @return String(10)
     */
	private String getTerminalSoftwareVersion() {
		String result = UNKNOWN;
		String productversion = getSystemProperties("ro.product.version");
		if (productversion != null && productversion.length() > 0) {
			result = productversion;
		}
		return StringUtils.truncate(result, 10);
	}

	/**
	 * Returns a system parameter
	 * 
	 * @param key Key parameter
	 * @return Parameter value
	 */
	private String getSystemProperties(String key) {
		String value = null;
		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class);
			value = (String)get.invoke(c, key);
			return value;
		} catch(Exception e) {
			return UNKNOWN;
		}		
	}

    /**
     * Get the current device language
     *
     * @return device language (like fr-FR)
     */
    private String getUserLanguage() {
        return Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
    }
    
	/**
     * Returns the RCS version
     * 
     * @return String(4)
     */
	private String getRcsVersion() {
		return "5.1B";
	}

	/**
     * Returns the RCS profile
     * 
     * @return String(15)
     */
	private String getRcsProfile() {
		return "joyn_blackbird";
	}    
}