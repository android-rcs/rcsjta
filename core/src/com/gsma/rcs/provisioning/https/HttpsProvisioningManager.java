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

package com.gsma.rcs.provisioning.https;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.TerminalInfo;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.gsma.rcs.provisioning.ProvisioningFailureReasons;
import com.gsma.rcs.provisioning.ProvisioningInfo;
import com.gsma.rcs.provisioning.ProvisioningInfo.Version;
import com.gsma.rcs.provisioning.ProvisioningParser;
import com.gsma.rcs.provisioning.TermsAndConditionsRequest;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.NetworkUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.RcsService;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * Provisioning via network manager
 * 
 * @author hlxn7157
 * @author G. LE PESSOT
 * @author Deutsche Telekom AG
 */
public class HttpsProvisioningManager {

    private static final int HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED = 511;

    private static final String PROVISIONING_URI_FILENAME = "rcs_provisioning_uri.txt";

    private static final String PARAM_VERS = "vers";

    private static final String PARAM_RCS_VERSION = "rcs_version";

    private static final String PARAM_RCS_PROFILE = "rcs_profile";

    private static final String PARAM_CLIENT_VENDOR = "client_vendor";

    private static final String PARAM_CLIENT_VERSION = "client_version";

    private static final String PARAM_TERMINAL_VENDOR = "terminal_vendor";

    private static final String PARAM_TERMINAL_MODEL = "terminal_model";

    private static final String PARAM_TERMINAL_SW_VERSION = "terminal_sw_version";

    private static final String PARAM_IMSI = "IMSI";

    private static final String PARAM_IMEI = "IMEI";

    private static final String PARAM_SMS_PORT = "SMS_port";

    private static final String PARAM_MSISDN = "msisdn";

    private static final String PARAM_TOKEN = "token";

    /**
     * First launch flag
     */
    private boolean mFirst = false;

    /**
     * User action flag
     */
    private boolean mUser = false;
    /**
     * Retry counter
     */
    private int mRetryCount = 0;

    /**
     * Check if a provisioning request is already pending
     */
    private boolean mProvisioningPending = false;

    private final LocalContentResolver mLocalContentResolver;

    /**
     * The Service Context
     */
    private final Context mCtx;

    /**
     * Provisioning SMS manager
     */
    private HttpsProvisioningSMS mSmsManager;

    /**
     * Provisioning Connection manager
     */
    private HttpsProvisioningConnection mNetworkCnx;

    /**
     * Retry after 511 "Network authentication required" counter
     */
    private int mRetryAfter511ErrorCount = 0;

    /**
     * Retry intent
     */
    private PendingIntent mRetryIntent;

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final ContactsManager mContactManager;

    /**
     * Builds HTTPS request parameters that are related to Terminal, PARAM_RCS_VERSION &
     * PARAM_RCS_PROFILE.
     * <p>
     * EXCLUDE - PARAM_VERS & OPTIONAL ARGS
     * </p>
     * <p>
     * OPTIONAL ARGS = PARAM_IMSI, PARAM_IMEI, PARAM_SMS_PORT, PARAM_MSISDN & PARAM_TOKEN
     * </p>
     */
    private static Uri.Builder sHttpsReqUriBuilder;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param applicationContext
     * @param localContentResolver
     * @param retryIntent pending intent to update periodically the configuration
     * @param first is provisioning service launch after (re)boot ?
     * @param user is provisioning service launch after user action ?
     * @param rcsSettings
     * @param messagingLog
     * @param contactManager
     */
    public HttpsProvisioningManager(Context applicationContext,
            LocalContentResolver localContentResolver, final PendingIntent retryIntent,
            boolean first, boolean user, RcsSettings rcsSettings, MessagingLog messagingLog,
            ContactsManager contactManager) {
        mCtx = applicationContext;
        mLocalContentResolver = localContentResolver;
        mRetryIntent = retryIntent;
        mFirst = first;
        mUser = user;
        mNetworkCnx = new HttpsProvisioningConnection(this);
        mRcsSettings = rcsSettings;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;
        mSmsManager = new HttpsProvisioningSMS(this, localContentResolver, rcsSettings,
                messagingLog, contactManager);
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
        if (mProvisioningPending) {
            return false;
        }
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            return false;
        }
        // Check received network info
        NetworkInfo networkInfo = mNetworkCnx.getConnectionMngr().getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }
        if (!networkInfo.isConnected()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Disconnection from network");
            }
            return false;
        }
        mProvisioningPending = true;
        if (sLogger.isActivated()) {
            sLogger.debug("Connected to data network");
        }
        new Thread() {
            public void run() {
                updateConfig();
            }
        }.start();

        // Unregister network state listener
        mNetworkCnx.unregisterNetworkStateListener();
        mProvisioningPending = false;
        return true;
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
    protected HttpResponse executeRequest(String protocol, String request,
            DefaultHttpClient client, HttpContext localContext) throws URISyntaxException,
            ClientProtocolException, IOException {
        boolean logActivated = sLogger.isActivated();
        try {
            HttpGet get = new HttpGet();
            get.setURI(new URI(new StringBuilder(protocol).append("://").append(request).toString()));
            get.addHeader("Accept-Language", HttpsProvisioningUtils.getUserLanguage());
            if (logActivated) {
                sLogger.debug("HTTP request: ".concat(get.getURI().toString()));
            }

            HttpResponse response = client.execute(get, localContext);
            if (logActivated) {
                sLogger.debug("HTTP response: ".concat(response.getStatusLine().toString()));
            }
            return response;
        } catch (UnknownHostException e) {
            if (logActivated) {
                sLogger.debug(new StringBuilder("The server ").append(request)
                        .append(" can't be reached!").toString());
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
    private String getHttpsRequestArguments(String imsi, String imei, String smsPort, String token,
            String msisdn) {

        if (sHttpsReqUriBuilder == null) {
            sHttpsReqUriBuilder = new Uri.Builder();
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_RCS_VERSION,
                    HttpsProvisioningUtils.getRcsVersion());
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_RCS_PROFILE,
                    HttpsProvisioningUtils.getRcsProfile());
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_CLIENT_VENDOR,
                    TerminalInfo.getClientVendor());
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_CLIENT_VERSION,
                    TerminalInfo.getClientVersion(mCtx));
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_TERMINAL_VENDOR,
                    TerminalInfo.getTerminalVendor());
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_TERMINAL_MODEL,
                    TerminalInfo.getTerminalModel());
            sHttpsReqUriBuilder.appendQueryParameter(PARAM_TERMINAL_SW_VERSION,
                    TerminalInfo.getTerminalSoftwareVersion());
        }

        String provisioningVersion = mRcsSettings.getProvisioningVersion();
        if (mUser && ProvisioningInfo.Version.DISABLED_DORMANT.equals(provisioningVersion)) {
            provisioningVersion = LauncherUtils.getProvisioningVersion(mCtx);
            mUser = false;
        }

        final Uri.Builder uriBuilder = sHttpsReqUriBuilder.build().buildUpon();
        uriBuilder.appendQueryParameter(PARAM_VERS, provisioningVersion);

        /**
         * Add optional parameters only if available
         */
        if (imsi != null) {
            uriBuilder.appendQueryParameter(PARAM_IMSI, imsi);
        }
        if (imei != null) {
            uriBuilder.appendQueryParameter(PARAM_IMEI, imei);
        }
        if (smsPort != null) {
            uriBuilder.appendQueryParameter(PARAM_SMS_PORT, smsPort);
        }
        if (msisdn != null) {
            uriBuilder.appendQueryParameter(PARAM_MSISDN, msisdn);
        }
        if (token != null) {
            uriBuilder.appendQueryParameter(PARAM_TOKEN, token);
        }

        return uriBuilder.toString();
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
    protected HttpsProvisioningResult sendFirstRequestsToRequireOTP(String imsi, String imei,
            String msisdn, String primaryUri, String secondaryUri, DefaultHttpClient client,
            HttpContext localContext) {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.debug("HTTP provisioning - Send first HTTPS request to require OTP");
            }

            // Generate the SMS port for provisioning
            String smsPortForOTP = HttpsProvisioningSMS.generateSmsPortForProvisioning();

            // Format first HTTPS request with extra parameters (IMSI and IMEI if available plus
            // SMS_port and token)
            String token = (!TextUtils.isEmpty(mRcsSettings.getProvisioningToken()) ? mRcsSettings
                    .getProvisioningToken() : "");
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
            result.content = new String(EntityUtils.toByteArray(response.getEntity()), UTF8);
            if (result.code != 200) {
                if (result.code == 403) {
                    if (logActivated) {
                        sLogger.debug("First HTTPS request to require OTP failed: Forbidden (request status code: 403) for msisdn "
                                + msisdn);
                    }

                    msisdn = mRcsSettings.getMsisdn();
                    msisdn = HttpsProvionningMSISDNInput.getInstance().displayPopupAndWaitResponse(
                            mCtx);

                    if (msisdn == null) {
                        return null;
                    } else {
                        return sendFirstRequestsToRequireOTP(imsi, imei, msisdn, primaryUri,
                                secondaryUri, client, localContext);
                    }

                } else if (result.code == 503) {
                    if (logActivated) {
                        sLogger.debug("First HTTPS request to require OTP failed: Retry After (request status code: 503)");
                    }
                    result.retryAfter = getRetryAfter(response);
                } else if (HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED == result.code) {
                    if (logActivated) {
                        sLogger.debug("First HTTPS request to require OTP failed: Invalid token (request status code: 511)");
                    }
                }

            } else {
                if (logActivated) {
                    sLogger.debug("HTTPS request returns with 200 OK.");
                }

                // Register SMS provisioning receiver
                mSmsManager.registerSmsProvisioningReceiver(smsPortForOTP, primaryUri, client,
                        localContext);

                // Save the MSISDN
                mRcsSettings.setMsisdn(msisdn);

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
                mSmsManager.unregisterSmsProvisioningReceiver();
            }

            return result;
        } catch (UnknownHostException e) {
            if (logActivated) {
                sLogger.warn("First HTTPS request to require OTP failed: Provisioning server not reachable");
            }
            return null;
        } catch (Exception e) {
            if (logActivated) {
                sLogger.error(
                        "First HTTPS request to require OTP failed: Can't get config via HTTPS", e);
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
    protected void updateConfigWithOTP(String otp, String requestUri, DefaultHttpClient client,
            HttpContext localContext) {
        // Cancel previous retry alarm
        HttpsProvisioningService.cancelRetryAlarm(mCtx, mRetryIntent);

        // Get config via HTTPS with OTP
        HttpsProvisioningResult result = sendSecondHttpsRequestWithOTP(otp, requestUri, client,
                localContext);

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
            if (sLogger.isActivated()) {
                sLogger.warn("Can not read network operator from SIM card!");
            }
            return null;
        }
        String mnc = ope.substring(3);
        String mcc = ope.substring(0, 3);
        while (mnc.length() < 3) { // Set mnc on 3 digits
            mnc = "0".concat(mnc);
        }
        return new StringBuilder("config.rcs.mnc").append(mnc).append(".mcc").append(mcc)
                .append(".pub.3gppnetwork.org").toString();
    }

    /**
     * Get configuration
     * 
     * @return Result or null in case of internal exception
     */
    private HttpsProvisioningResult getConfig() {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.debug("Request config via HTTPS");
            }

            // Get provisioning address
            TelephonyManager tm = (TelephonyManager) mCtx
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String primaryUri = null;
            String secondaryUri = null;
            if (mRcsSettings.isSecondaryProvisioningAddressOnly()) {
                primaryUri = mRcsSettings.getSecondaryProvisioningAddress();
            } else {
                primaryUri = buildProvisioningAddress(tm);
                secondaryUri = mRcsSettings.getSecondaryProvisioningAddress();
            }

            /*
             * Check if a file containing URI for HTTPS provisioning exists
             */
            String primaryUriFromFile = getPrimaryProvisionigServerUriFromFile();
            if (primaryUriFromFile != null) {
                primaryUri = primaryUriFromFile;
                secondaryUri = null;
            }

            if (logActivated) {
                sLogger.debug(new StringBuilder("HCS/RCS Uri to connect: ").append(primaryUri)
                        .append(" or ").append(secondaryUri).toString());
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
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(
                    30));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            NetworkInfo networkInfo = mNetworkCnx.getConnectionMngr().getActiveNetworkInfo();

            if (networkInfo != null) {
                String proxyHost = Proxy.getDefaultHost();
                if (proxyHost != null && proxyHost.length() > 1) {
                    int proxyPort = Proxy.getDefaultPort();
                    params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost,
                            proxyPort));
                }
            }
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

            // Support broad variety of different cookie types (not just Netscape but RFC 2109 and
            // RFC2965 compliant ones, too)
            HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);

            ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
            DefaultHttpClient client = new DefaultHttpClient(cm, params);
            CookieStore cookieStore = (CookieStore) new BasicCookieStore();
            HttpContext localContext = new BasicHttpContext();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // If network is not mobile network, use request with OTP
            if (networkInfo != null && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
                // Proceed with non mobile network registration
                return sendFirstRequestsToRequireOTP(imsi, imei, null, primaryUri, secondaryUri,
                        client, localContext);
            }

            if (logActivated) {
                sLogger.debug("HTTP provisioning on mobile network");
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
            result.content = new String(EntityUtils.toByteArray(response.getEntity()), UTF8);
            if (HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED == result.code) {
                // Blackbird guidelines ID_2_6 Configuration mechanism over PS without Header
                // Enrichment
                // Use SMS provisioning on PS data network if server reply 511 NETWORK
                // AUTHENTICATION REQUIRED
                return sendFirstRequestsToRequireOTP(imsi, imei, null, primaryUri, secondaryUri,
                        client, localContext);
            } else if (HttpStatus.SC_OK != result.code) {
                if (HttpStatus.SC_SERVICE_UNAVAILABLE == result.code) {
                    result.retryAfter = getRetryAfter(response);
                }
                return result;
            }

            // Format second HTTPS request
            String args = getHttpsRequestArguments(imsi, imei);
            String request = requestUri + args;
            if (logActivated) {
                sLogger.info("Request provisioning: " + request);
            }

            // Execute second HTTPS request
            response = executeRequest("https", request, client, localContext);
            if (response == null) {
                return null;
            }
            result.code = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != result.code) {
                if (HttpStatus.SC_SERVICE_UNAVAILABLE == result.code) {
                    result.retryAfter = getRetryAfter(response);
                }
                return result;
            }
            result.content = new String(EntityUtils.toByteArray(response.getEntity()), UTF8);
            return result;
        } catch (UnknownHostException e) {
            if (logActivated) {
                sLogger.warn("Provisioning server not reachable");
            }
            return null;
        } catch (Exception e) {
            if (logActivated) {
                sLogger.error("Can't get config via HTTPS", e);
            }
            return null;
        }
    }

    private String getPrimaryProvisionigServerUriFromFile() {
        boolean logActivated = sLogger.isActivated();
        DataInputStream dataInputStream = null;
        try {
            File primaryUri = new File(mCtx.getFilesDir(), PROVISIONING_URI_FILENAME);
            if (!primaryUri.exists()) {
                return null;
            }
            if (logActivated) {
                sLogger.debug("Provisioning URI file found !");
            }
            FileInputStream fis = new FileInputStream(primaryUri);
            dataInputStream = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dataInputStream));
            return br.readLine();
        } catch (Exception e) {
            if (logActivated) {
                sLogger.error("Failed to locate URI provisioning file", e);
            }
        } finally {
            if (dataInputStream != null) {
                try {
                    dataInputStream.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }
        return null;
    }

    /**
     * Update provisioning config
     */
    protected void updateConfig() {
        // Cancel previous retry alarm
        HttpsProvisioningService.cancelRetryAlarm(mCtx, mRetryIntent);

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
    protected HttpsProvisioningResult sendSecondHttpsRequestWithOTP(String otp, String requestUri,
            DefaultHttpClient client, HttpContext localContext) {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.debug("Send second HTTPS with OTP");
            }

            // Format second HTTPS request
            String args = "?OTP=" + otp;
            String request = requestUri + args;

            if (logActivated) {
                sLogger.info("Request provisioning with OTP: " + request);
            }

            // Execute second HTTPS request
            HttpResponse response = executeRequest("https", request, client, localContext);
            if (response == null) {
                return null;
            }
            result.code = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK != result.code) {
                if (HttpStatus.SC_SERVICE_UNAVAILABLE == result.code) {
                    result.retryAfter = getRetryAfter(response);
                } else if (HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED == result.code) {
                    if (logActivated) {
                        sLogger.debug("Second HTTPS request with OTP failed: Invalid one time password (request status code: 511)");
                    }
                }
                return result;
            }
            result.content = new String(EntityUtils.toByteArray(response.getEntity()), UTF8);

            return result;
        } catch (Exception e) {
            if (logActivated) {
                sLogger.error("Second HTTPS request with OTP failed: Can't get config via HTTPS", e);
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
        boolean logActivated = sLogger.isActivated();
        if (result != null) {
            if (HttpStatus.SC_OK == result.code) {
                // Reset after 511 counter
                mRetryAfter511ErrorCount = 0;

                if (result.waitingForSMSOTP) {
                    if (logActivated) {
                        sLogger.debug("Waiting for SMS with OTP.");
                    }
                    return;
                }

                if (logActivated) {
                    sLogger.debug("Provisioning request successful");
                }

                // Parse the received content
                ProvisioningParser parser = new ProvisioningParser(result.content, mRcsSettings);

                /*
                 * Save GSMA release set into the provider. The Node "SERVICES" is mandatory in GSMA
                 * release Blackbird and not present in previous one (i.e. Albatros). It is the
                 * absence of this node in the configuration which allows us to determine that
                 * current release is Albatros
                 */
                GsmaRelease gsmaRelease = mRcsSettings.getGsmaRelease();
                /*
                 * Save client Messaging Mode set into the provider. The message mode NONE value is
                 * not defined in the standard. It is the absence of the messagingUx parameter which
                 * allows us to determine that client Message Mode is set to NONE.
                 */
                MessagingMode messagingMode = mRcsSettings.getMessagingMode();

                /* Before parsing the provisioning, the GSMA release is set to Albatros */
                mRcsSettings.setGsmaRelease(GsmaRelease.ALBATROS);
                /* Before parsing the provisioning, the client Messaging mode is set to NONE */
                mRcsSettings.setMessagingMode(MessagingMode.NONE);

                if (parser.parse(gsmaRelease, messagingMode, mFirst)) {
                    // Successfully provisioned, 1st time reg finalized
                    mFirst = false;
                    ProvisioningInfo info = parser.getProvisioningInfo();

                    // Save version
                    String version = info.getVersion();
                    long validity = info.getValidity();
                    if (logActivated) {
                        sLogger.debug("Provisioning version=" + version + ", validity=" + validity);
                    }

                    // Save the latest positive version of the configuration
                    LauncherUtils.saveProvisioningVersion(mCtx, version);

                    // Save the validity of the configuration
                    LauncherUtils.saveProvisioningValidity(mCtx, validity);
                    mRcsSettings.setProvisioningVersion(version);

                    // Save token
                    String token = info.getToken();
                    mRcsSettings.setProvisioningToken(token);

                    mRcsSettings.setFileTransferHttpSupported(mRcsSettings.getFtHttpServer()
                            .length() > 0
                            && mRcsSettings.getFtHttpLogin().length() > 0
                            && mRcsSettings.getFtHttpPassword().length() > 0);

                    // Reset retry alarm counter
                    mRetryCount = 0;
                    if (ProvisioningInfo.Version.DISABLED_DORMANT.equals(version)) {
                        // -3 : Put RCS client in dormant state
                        if (logActivated) {
                            sLogger.debug("Provisioning: RCS client in dormant state");
                        }
                        // Start retry alarm
                        if (validity > 0) {
                            HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent, validity);
                        }
                        // We parsed successfully the configuration
                        mRcsSettings.setConfigurationValid(true);
                        // Stop the RCS core service. Provisioning is still running.
                        LauncherUtils.stopRcsCoreService(mCtx);
                    } else {
                        if (ProvisioningInfo.Version.DISABLED_NOQUERY.equals(version)) {
                            // -2 : Disable RCS client and stop configuration query
                            if (logActivated) {
                                sLogger.debug("Provisioning: disable RCS client");
                            }
                            // We parsed successfully the configuration
                            mRcsSettings.setConfigurationValid(true);
                            // Disable and stop RCS service
                            mRcsSettings.setServiceActivationState(false);
                            LauncherUtils.stopRcsService(mCtx);
                        } else {
                            if (ProvisioningInfo.Version.RESETED_NOQUERY.equals(version)) {
                                // -1 Forbidden: reset account + version = 0-1 (doesn't restart)
                                if (logActivated) {
                                    sLogger.debug("Provisioning forbidden: reset account");
                                }
                                // Reset config
                                LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver,
                                        mRcsSettings, mMessagingLog, mContactManager);
                                // Force version to "-1" (resetRcs set version to "0")
                                mRcsSettings.setProvisioningVersion(version);
                                // Disable the RCS service
                                mRcsSettings.setServiceActivationState(false);
                            } else {
                                if (ProvisioningInfo.Version.RESETED.equals(version)) {
                                    if (logActivated) {
                                        sLogger.debug("Provisioning forbidden: no account");
                                    }
                                    // Reset config
                                    LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver,
                                            mRcsSettings, mMessagingLog, mContactManager);
                                } else {
                                    // Start retry alarm
                                    if (validity > 0) {
                                        HttpsProvisioningService.startRetryAlarm(mCtx,
                                                mRetryIntent, validity);
                                    }
                                    // Terms request
                                    if (info.getMessage() != null
                                            && !mRcsSettings.isProvisioningTermsAccepted()) {
                                        showTermsAndConditions(info);
                                    } else {
                                        if (logActivated) {
                                            sLogger.debug("No special terms and conditions");
                                        }
                                        mRcsSettings.setProvisioningTermsAccepted(true);
                                    }
                                    // We parsed successfully the configuration
                                    mRcsSettings.setConfigurationValid(true);
                                    // Start the RCS core service
                                    LauncherUtils.launchRcsCoreService(mCtx, mRcsSettings);
                                }
                            }
                        }
                    }

                    // Send service provisioning intent
                    Intent serviceProvisioned = new Intent(
                            RcsService.ACTION_SERVICE_PROVISIONING_DATA_CHANGED);
                    IntentUtils.tryToSetReceiverForegroundFlag(serviceProvisioned);
                    mCtx.sendBroadcast(serviceProvisioned);
                } else {
                    if (logActivated) {
                        sLogger.debug("Can't parse provisioning document");
                    }
                    // Restore GSMA release saved before parsing of the provisioning
                    mRcsSettings.setGsmaRelease(gsmaRelease);

                    // Restore the client messaging mode saved before parsing of the provisioning
                    mRcsSettings.setMessagingMode(messagingMode);

                    if (mFirst) {
                        if (logActivated) {
                            sLogger.debug("As this is first launch and we do not have a valid configuration yet, retry later");
                        }
                        // Reason: Invalid configuration
                        provisioningFails(ProvisioningFailureReasons.INVALID_CONFIGURATION);
                        retry();
                    } else {
                        if (logActivated) {
                            sLogger.debug("This is not first launch, use old configuration to register");
                        }
                        tryLaunchRcsCoreService(mCtx, -1);
                    }
                }
            } else if (HttpStatus.SC_SERVICE_UNAVAILABLE == result.code) {
                // Server Unavailable
                if (logActivated) {
                    sLogger.debug("Server Unavailable. Retry after: " + result.retryAfter);
                }
                if (mFirst) {
                    // Reason: Unable to get configuration
                    provisioningFails(ProvisioningFailureReasons.UNABLE_TO_GET_CONFIGURATION);
                    if (result.retryAfter > 0) {
                        HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent,
                                result.retryAfter * 1000);
                    }
                } else {
                    tryLaunchRcsCoreService(mCtx, result.retryAfter * 1000);
                }
            } else if (HttpStatus.SC_FORBIDDEN == result.code) {
                // Forbidden: reset account + version = 0
                if (logActivated) {
                    sLogger.debug("Provisioning forbidden: reset account");
                }
                // Reset version to "0"
                mRcsSettings.setProvisioningVersion(Version.RESETED.toString());
                // Reset config
                LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver, mRcsSettings,
                        mMessagingLog, mContactManager);
                // Reason: Provisioning forbidden
                provisioningFails(ProvisioningFailureReasons.PROVISIONING_FORBIDDEN);
            } else if (HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED == result.code) {
                // Provisioning authentication required
                if (logActivated) {
                    sLogger.debug("Provisioning authentication required");
                }
                // Reset provisioning token
                mRcsSettings.setProvisioningToken("");
                // Retry after reseting provisioning token
                if (!retryAfter511Error()) {
                    // Reason: Provisioning authentication required
                    provisioningFails(ProvisioningFailureReasons.PROVISIONING_AUTHENTICATION_REQUIRED);
                }
            } else {
                // Other error
                if (logActivated) {
                    sLogger.debug("Provisioning error " + result.code);
                }
                // Start the RCS service
                if (mFirst) {
                    // Reason: No configuration present
                    provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
                    retry();
                } else {
                    tryLaunchRcsCoreService(mCtx, -1);
                }
            }
        } else { // result is null
            // Start the RCS service
            if (mFirst) {
                // Reason: No configuration present
                if (logActivated) {
                    sLogger.error("### Provisioning fails and first = true!");
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
     * @param timerRetry timer to trigger next provisioning request. Only applicable if greater than
     *            0.
     */
    private void tryLaunchRcsCoreService(Context context, int timerRetry) {
        try {
            int version = Integer.parseInt(mRcsSettings.getProvisioningVersion());
            // Only launch service if version is positive
            if (version > 0) {
                // Start the RCS service
                LauncherUtils.launchRcsCoreService(context, mRcsSettings);
                if (timerRetry > 0) {
                    HttpsProvisioningService.startRetryAlarm(context, mRetryIntent, timerRetry);
                } else
                    retry();
            } else {
                // Only retry provisioning if service is disabled dormant (-3)
                if (ProvisioningInfo.Version.DISABLED_DORMANT.getVersion() == version) {
                    if (timerRetry > 0) {
                        HttpsProvisioningService.startRetryAlarm(context, mRetryIntent, timerRetry);
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
        intent.putExtra(TermsAndConditionsRequest.EXTRA_ACCEPT_BTN, info.getAcceptBtn());
        intent.putExtra(TermsAndConditionsRequest.EXTRA_REJECT_BTN, info.getRejectBtn());
        intent.putExtra(TermsAndConditionsRequest.EXTRA_TITLE, info.getTitle());
        intent.putExtra(TermsAndConditionsRequest.EXTRA_MESSAGE, info.getMessage());

        mCtx.startActivity(intent);
    }

    /**
     * Retry after 511 "Network authentication required" procedure
     * 
     * @return <code>true</code> if retry is performed, otherwise <code>false</code>
     */
    private boolean retryAfter511Error() {
        if (mRetryAfter511ErrorCount < HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_MAX_COUNT) {
            mRetryAfter511ErrorCount++;
            HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent,
                    HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_TIMEOUT);
            if (sLogger.isActivated()) {
                sLogger.debug("Retry after 511 error (" + mRetryAfter511ErrorCount + "/"
                        + HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_MAX_COUNT
                        + ") provisionning after "
                        + HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_TIMEOUT + "ms");
            }
            return true;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("No more retry after 511 error for provisionning");
        }

        // Reset after 511 counter
        mRetryAfter511ErrorCount = 0;

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
            mNetworkCnx.registerWifiDisablingListener();
        }
    }

    /**
     * Retry procedure
     */
    private void retry() {
        if (mRetryCount < HttpsProvisioningUtils.RETRY_MAX_COUNT) {
            mRetryCount++;
            int retryDelay = HttpsProvisioningUtils.RETRY_BASE_TIMEOUT + 2 * (mRetryCount - 1)
                    * HttpsProvisioningUtils.RETRY_BASE_TIMEOUT;
            HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent, retryDelay);
            if (sLogger.isActivated()) {
                sLogger.debug("Retry provisionning count: " + mRetryCount);
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("No more retry for provisionning");
            }
        }
    }

    /**
     * Transmit to SMS unregister method
     */
    public void unregisterSmsProvisioningReceiver() {
        mSmsManager.unregisterSmsProvisioningReceiver();
    }

    /**
     * Transmit to Network unregister method
     */
    public void unregisterNetworkStateListener() {
        mNetworkCnx.unregisterNetworkStateListener();
    }

    /**
     * Transmit to Network unregister wifi method
     */
    public void unregisterWifiDisablingListener() {
        mNetworkCnx.unregisterWifiDisablingListener();
    }

    /**
     * Transmit to Network register method
     */
    public void registerNetworkStateListener() {
        mNetworkCnx.registerNetworkStateListener();
    }

    /**
     * Retry procedure
     */
    public void resetCounters() {
        // Reset retry alarm counter
        mRetryCount = 0;

        // Reset after 511 counter
        mRetryAfter511ErrorCount = 0;
    }
}
