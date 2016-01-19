/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.R;
import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.addressbook.RcsAccountManager;
import com.gsma.rcs.core.TerminalInfo;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.gsma.rcs.provider.settings.RcsSettingsData.TermsAndConditionsResponse;
import com.gsma.rcs.provisioning.ProvisioningFailureReasons;
import com.gsma.rcs.provisioning.ProvisioningInfo;
import com.gsma.rcs.provisioning.ProvisioningInfo.Version;
import com.gsma.rcs.provisioning.ProvisioningParser;
import com.gsma.rcs.provisioning.TermsAndConditionsRequest;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.NetworkUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import android.app.PendingIntent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Provisioning via network manager
 * 
 * @author hlxn7157
 * @author G. LE PESSOT
 * @author Deutsche Telekom AG
 */
public class HttpsProvisioningManager {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

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

    private static final String PROVISIONING_OPERATIONS_THREAD_NAME = "ProvisioningOps";

    /**
     * First launch flag
     */
    private boolean mFirstProvAfterBoot = false;

    /**
     * User action flag
     */
    private boolean mUser = false;

    private int mRetryCount = 0;

    private final LocalContentResolver mLocalContentResolver;

    private final Context mCtx;

    /**
     * Handler to process messages & runnable associated with background thread.
     */
    private final Handler mProvisioningOperationHandler;

    private final HttpsProvisioningSMS mSmsManager;

    private final HttpsProvisioningConnection mNetworkCnx;

    /**
     * Retry after 511 "Network authentication required" counter
     */
    private int mRetryAfter511ErrorCount = 0;

    private final PendingIntent mRetryIntent;

    private final RcsSettings mRcsSettings;

    private final MessagingLog mMessagingLog;

    private final ContactManager mContactManager;

    private final RcsAccountManager mRcsAccountManager;

    private final String mImsi;

    private final String mImei;

    private final static int BUFFER_READER_SIZE = 1000;

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

    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param imei International Mobile Equipment Identity
     * @param imsi International Mobile Subscriber Identity
     * @param context application context
     * @param localContentResolver Local content resolver
     * @param retryIntent pending intent to update periodically the configuration
     * @param first is provisioning service launch after (re)boot ?
     * @param user is provisioning service launch after user action ?
     * @param rcsSettings RCS settings accessor
     * @param messagingLog Message log accessor
     * @param contactManager Contact manager accessor
     */
    public HttpsProvisioningManager(String imei, String imsi, Context context,
            LocalContentResolver localContentResolver, final PendingIntent retryIntent,
            boolean first, boolean user, RcsSettings rcsSettings, MessagingLog messagingLog,
            ContactManager contactManager) {
        mImsi = imsi;
        mImei = imei;
        mCtx = context;
        mLocalContentResolver = localContentResolver;
        mRetryIntent = retryIntent;
        mFirstProvAfterBoot = first;
        mUser = user;
        mRcsSettings = rcsSettings;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;

        mProvisioningOperationHandler = allocateBgHandler(PROVISIONING_OPERATIONS_THREAD_NAME);

        mNetworkCnx = new HttpsProvisioningConnection(this, context);
        mSmsManager = new HttpsProvisioningSMS(this, context, localContentResolver, rcsSettings,
                messagingLog, contactManager);
        mRcsAccountManager = new RcsAccountManager(mCtx, contactManager);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    /**
     * Connection event
     * 
     * @param action Connectivity action
     * @return true if the updateConfig has been done
     * @throws RcsAccountException
     * @throws IOException
     */
    /* package private */boolean connectionEvent(String action) throws RcsAccountException,
            IOException {
        try {
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
            if (sLogger.isActivated()) {
                sLogger.debug("Connected to data network");
            }
            updateConfig();
            return true;

        } finally {
            mNetworkCnx.unregisterNetworkStateListener();
        }
    }

    private HttpURLConnection executeHttpRequest(boolean secured, String request)
            throws IOException {
        String protocol = (secured) ? "https" : "http";
        URL url = new URL(protocol + "://" + request);
        HttpURLConnection cnx = (HttpURLConnection) url.openConnection();
        cnx.setRequestProperty("Accept-Language", HttpsProvisioningUtils.getUserLanguage());
        return cnx;
    }

    /**
     * Get the HTTPS request arguments
     * 
     * @param smsPort SMS port
     * @param token Provisioning token
     * @param msisdn MSISDN
     * @return {@link String} with the HTTPS request arguments.
     */
    private String getHttpsRequestArguments(String smsPort, String token, ContactId msisdn) {
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

        int provisioningVersion = mRcsSettings.getProvisioningVersion();
        if (mUser && Version.DISABLED_DORMANT.toInt() == provisioningVersion) {
            provisioningVersion = LauncherUtils.getProvisioningVersion(mCtx);
            mUser = false;
        }
        final Uri.Builder uriBuilder = sHttpsReqUriBuilder.build().buildUpon();
        uriBuilder.appendQueryParameter(PARAM_VERS, String.valueOf(provisioningVersion));
        uriBuilder.appendQueryParameter(PARAM_IMSI, mImsi);
        uriBuilder.appendQueryParameter(PARAM_IMEI, mImei);
        /*
         * Add optional parameters only if available
         */
        if (smsPort != null) {
            uriBuilder.appendQueryParameter(PARAM_SMS_PORT, smsPort);
        }
        if (msisdn != null) {
            uriBuilder.appendQueryParameter(PARAM_MSISDN, msisdn.toString());
        }

        /*
         * RCS standard:
         * "The token shall be stored on the device so it can be used in subsequent configuration requests over non-3GPP access."
         * <br> In 3GPP access, the token is only compulsory for non 3GPP access and is not used for
         * 3GPP access. It shall then not be inserted as a URI parameter for 3GPP access.
         */
        if (NetworkUtils.getNetworkAccessType() == NetworkUtils.NETWORK_ACCESS_WIFI) {
            /*
             * According to the standard the token-parameter name should be part of the Uri even if
             * the token is null here but only for non 3gpp access.
             */
            uriBuilder.appendQueryParameter(PARAM_TOKEN, token == null ? "" : token);
        }

        return uriBuilder.toString();
    }

    /**
     * Send the first HTTPS request to require the one time password (OTP)
     *
     * @param msisdn the phone number
     * @param primaryUri the primary URI
     * @param secondaryUri the secondary URI
     * @return Instance of {@link HttpsProvisioningResult} or null in case of internal exception
     * @throws IOException
     */
    private HttpsProvisioningResult sendFirstRequestsToRequireOTP(ContactId msisdn,
            String primaryUri, String secondaryUri) throws IOException {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("HTTP provisioning - Send first HTTPS request to get OTP");
        }
        HttpURLConnection urlConnection = null;
        try {
            if (msisdn == null) {
                msisdn = HttpsProvisioningMSISDNInput.getInstance().displayPopupAndWaitResponse(
                        mCtx);
                if (msisdn == null) {
                    if (logActivated) {
                        sLogger.warn("No MSISDN set by end user: cannot authenticate!");
                    }
                    result.code = HttpsProvisioningResult.UNKNOWN_MSISDN_CODE;
                    return result;
                }
                if (logActivated) {
                    sLogger.debug("MSISDN set by end user=".concat(msisdn.toString()));
                }
            }

            /* Generate the SMS port for provisioning */
            String smsPortForOTP = HttpsProvisioningSMS.generateSmsPortForProvisioning();
            /*
             * Format first HTTPS request with extra parameters (IMSI and IMEI if available plus
             * SMS_port and token).
             */
            String token = mRcsSettings.getProvisioningToken();
            String args = getHttpsRequestArguments(smsPortForOTP, token, msisdn);

            /* Execute first HTTPS request with extra parameters */
            String request = primaryUri + args;
            urlConnection = executeHttpRequest(true, request);
            result.code = urlConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK != result.code && !StringUtils.isEmpty(secondaryUri)) {
                /* First server not available, try the secondaryUri */
                request = secondaryUri + args;
                urlConnection.disconnect();
                urlConnection = null;
                urlConnection = executeHttpRequest(true, request);
                result.code = urlConnection.getResponseCode();
            }
            switch (result.code) {
                case HttpURLConnection.HTTP_OK:
                    if (logActivated) {
                        sLogger.debug("HTTPS request returns with 200 OK");
                    }
                    result.content = readStream(urlConnection.getInputStream());

                    /*
                     * If the content is empty, means that the configuration XML is not present and
                     * the Token is invalid then we need to wait for the SMS with OTP.
                     */
                    if (TextUtils.isEmpty(result.content)) {
                        result.waitingForSMSOTP = true;
                        /* Register SMS provisioning receiver */
                        mSmsManager.registerSmsProvisioningReceiver(smsPortForOTP, primaryUri);
                    }
                    return result;

                case HttpURLConnection.HTTP_FORBIDDEN:
                    if (logActivated) {
                        sLogger.debug("Request to get OTP failed: Forbidden. MSISDN=" + msisdn);
                    }
                    return sendFirstRequestsToRequireOTP(null, primaryUri, secondaryUri);

                case HttpURLConnection.HTTP_UNAVAILABLE:
                    result.retryAfter = getRetryAfter(urlConnection);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                default:
                    if (logActivated) {
                        sLogger.debug("Request to get OTP failed: code=" + result.code);
                    }
                    return result;
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            /* If not waiting for the SMS with OTP */
            if (!result.waitingForSMSOTP && HttpURLConnection.HTTP_FORBIDDEN != result.code) {
                mSmsManager.unregisterSmsProvisioningReceiver();
            }
        }
    }

    /**
     * Update provisioning config with OTP
     * 
     * @param otp One time password
     * @param requestUri Request URI
     * @throws RcsAccountException thrown if RCS account failed to be created
     * @throws IOException
     */
    /* package private */void updateConfigWithOTP(String otp, String requestUri)
            throws RcsAccountException, IOException {
        // Cancel previous retry alarm
        HttpsProvisioningService.cancelRetryAlarm(mCtx, mRetryIntent);

        // Get config via HTTPS with OTP
        HttpsProvisioningResult result = sendSecondHttpsRequestWithOTP(otp, requestUri);
        //
        // // Process HTTPS provisioning result
        processProvisioningResult(result);
    }

    /**
     * Build the provisioning address with operator information
     * 
     * @return provisioning URI
     */
    private String buildProvisioningAddress() {
        String mnc = String.format("%03d", mRcsSettings.getMobileNetworkCode());
        String mcc = String.format("%03d", mRcsSettings.getMobileCountryCode());
        return "config.rcs.mnc" + mnc + ".mcc" + mcc + ".pub.3gppnetwork.org";
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, UTF8),
                    BUFFER_READER_SIZE);
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            CloseableUtils.tryToClose(in);
        }
    }

    /**
     * Get configuration
     * 
     * @return Result or null in case of internal exception
     * @throws IOException
     */
    private HttpsProvisioningResult getConfig() throws IOException {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Request config via HTTPS");
        }
        HttpURLConnection urlConnection = null;
        String primaryUri;
        String secondaryUri = null;
        try {
            /* Get provisioning address */
            String secondaryAddress = mRcsSettings.getSecondaryProvisioningAddress();
            if (secondaryAddress != null && mRcsSettings.isSecondaryProvisioningAddressOnly()) {
                primaryUri = secondaryAddress;
            } else {
                primaryUri = buildProvisioningAddress();
                secondaryUri = mRcsSettings.getSecondaryProvisioningAddress();
            }
            /*
             * Override primary URI if a file containing URI for HTTPS provisioning exists
             */
            File provFile = new File(mCtx.getFilesDir(), PROVISIONING_URI_FILENAME);
            if (provFile.exists()) {
                primaryUri = getPrimaryProvisionigServerUriFromFile(provFile);
                secondaryUri = null;
            }
            if (logActivated) {
                sLogger.debug("HCS/RCS Uri to connect: " + primaryUri + " or " + secondaryUri);
            }
            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);

            NetworkInfo networkInfo = mNetworkCnx.getConnectionMngr().getActiveNetworkInfo();
            /* If network is not mobile network, use request with OTP */
            if (networkInfo != null && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE) {
                // Proceed with non mobile network registration
                ContactId contactId = mRcsSettings.getUserProfileImsUserName();
                return sendFirstRequestsToRequireOTP(contactId, primaryUri, secondaryUri);
            }
            if (logActivated) {
                sLogger.debug("HTTP provisioning on mobile network");
            }
            /* Execute first HTTP request */
            String requestUri = primaryUri;
            urlConnection = executeHttpRequest(false, primaryUri);
            result.code = urlConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK != result.code && !StringUtils.isEmpty(secondaryUri)) {
                urlConnection.disconnect();
                urlConnection = null;
                /* First server not available, try the secondaryUri */
                requestUri = secondaryUri;
                urlConnection = executeHttpRequest(false, secondaryUri);
                result.code = urlConnection.getResponseCode();
            }
            switch (result.code) {
                case HttpURLConnection.HTTP_OK:
                    break;

                case HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED:
                    /*
                     * Blackbird guidelines ID_2_6 Configuration mechanism over PS without Header
                     * Enrichment Use SMS provisioning on PS data network if server reply 511
                     * NETWORK AUTHENTICATION REQUIRED
                     */
                    return sendFirstRequestsToRequireOTP(null, primaryUri, secondaryUri);

                case HttpURLConnection.HTTP_UNAVAILABLE:
                    result.retryAfter = getRetryAfter(urlConnection);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                default:
                    if (logActivated) {
                        sLogger.debug("First HTTPS request failed with code " + result.code);
                    }
                    return result;
            }
            urlConnection.disconnect();
            urlConnection = null;

            /* Format second HTTPS request */
            String request = requestUri + getHttpsRequestArguments(null, null, null);
            if (logActivated) {
                sLogger.info("Request provisioning: ".concat(request));
            }
            /* Execute second HTTPS request */
            urlConnection = executeHttpRequest(true, request);
            result.code = urlConnection.getResponseCode();
            switch (result.code) {
                case HttpURLConnection.HTTP_OK:
                    result.content = readStream(urlConnection.getInputStream());
                    return result;

                case HttpURLConnection.HTTP_UNAVAILABLE:
                    result.retryAfter = getRetryAfter(urlConnection);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                default:
                    if (logActivated) {
                        sLogger.debug("Second HTTPS request failed with code " + result.code);
                    }
                    return result;
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private String getPrimaryProvisionigServerUriFromFile(File provFile) throws IOException {
        DataInputStream dataInputStream = null;
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Provisioning URI file found !");
            }
            FileInputStream fis = new FileInputStream(provFile);
            dataInputStream = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dataInputStream));
            return br.readLine();

        } finally {
            CloseableUtils.tryToClose(dataInputStream);
        }
    }

    /**
     * Update provisioning config
     * 
     * @throws RcsAccountException thrown if RCS account failed to be created
     * @throws IOException
     */
    /* package private */void updateConfig() throws RcsAccountException, IOException {
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
     * @return Instance of {@link HttpsProvisioningResult} or null in case of internal exception
     * @throws IOException
     */
    /* package private */HttpsProvisioningResult sendSecondHttpsRequestWithOTP(String otp,
            String requestUri) throws IOException {
        HttpsProvisioningResult result = new HttpsProvisioningResult();
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Send second HTTPS with OTP");
        }
        HttpURLConnection urlConnection = null;
        try {
            /* Format second HTTPS request */
            String request = requestUri + "?OTP=" + otp;
            if (logActivated) {
                sLogger.info("Request provisioning with OTP: ".concat(request));
            }
            /* Execute second HTTPS request */
            urlConnection = executeHttpRequest(true, request);
            result.code = urlConnection.getResponseCode();
            switch (result.code) {
                case HttpURLConnection.HTTP_OK:
                    result.content = readStream(urlConnection.getInputStream());
                    return result;

                case HttpURLConnection.HTTP_UNAVAILABLE:
                    result.retryAfter = getRetryAfter(urlConnection);
                    /* Intentional fall through */
                    //$FALL-THROUGH$
                default:
                    if (logActivated) {
                        sLogger.debug("Request with OTP failed code=" + result.code);
                    }
                    return result;
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Get retry-after value
     * 
     * @return retry-after value in milliseconds
     */
    private long getRetryAfter(HttpURLConnection response) {
        String header = response.getHeaderField("Retry-After");
        if (header == null) {
            return 0;
        }
        return Integer.valueOf(header) * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
    }

    /**
     * Process provisioning result
     * 
     * @param result Instance of {@link HttpsProvisioningResult}
     * @throws RcsAccountException thrown if RCS account failed to be created
     */
    private void processProvisioningResult(HttpsProvisioningResult result)
            throws RcsAccountException {
        boolean logActivated = sLogger.isActivated();
        if (HttpURLConnection.HTTP_OK == result.code) {
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
             * release Blackbird and not present in previous one (i.e. Albatros). It is the absence
             * of this node in the configuration which allows us to determine that current release
             * is Albatros
             */
            GsmaRelease gsmaRelease = mRcsSettings.getGsmaRelease();
            /*
             * Save client Messaging Mode set into the provider. The message mode NONE value is not
             * defined in the standard. It is the absence of the messagingUx parameter which allows
             * us to determine that client Message Mode is set to NONE.
             */
            MessagingMode messagingMode = mRcsSettings.getMessagingMode();

            /* Before parsing the provisioning, the GSMA release is set to Albatros */
            mRcsSettings.setGsmaRelease(GsmaRelease.ALBATROS);
            /* Before parsing the provisioning, the client Messaging mode is set to NONE */
            mRcsSettings.setMessagingMode(MessagingMode.NONE);

            try {
                parser.parse(gsmaRelease, messagingMode, mFirstProvAfterBoot);
                // Successfully provisioned, 1st time reg finalized
                mFirstProvAfterBoot = false;
                ProvisioningInfo info = parser.getProvisioningInfo();

                // Save version
                int version = info.getVersion();
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

                mRcsSettings.setFileTransferHttpSupported(mRcsSettings.getFtHttpServer() != null
                        && mRcsSettings.getFtHttpLogin() != null
                        && mRcsSettings.getFtHttpPassword() != null);

                // Reset retry alarm counter
                mRetryCount = 0;
                if (Version.DISABLED_DORMANT.toInt() == version) {
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

                } else if (Version.DISABLED_NOQUERY.toInt() == version) {
                    // -2 : Disable RCS client and stop configuration query
                    if (logActivated) {
                        sLogger.debug("Provisioning: disable RCS client");
                    }
                    // We parsed successfully the configuration
                    mRcsSettings.setConfigurationValid(true);
                    // Disable and stop RCS service
                    mRcsSettings.setServiceActivationState(false);
                    LauncherUtils.stopRcsService(mCtx);

                } else if (Version.RESETED_NOQUERY.toInt() == version) {
                    // -1 Forbidden: reset account + version = 0-1 (doesn't restart)
                    if (logActivated) {
                        sLogger.debug("Provisioning forbidden: reset account");
                    }
                    // Reset config
                    LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver, mRcsSettings,
                            mMessagingLog, mContactManager);
                    // Force version to "-1" (resetRcs set version to "0")
                    mRcsSettings.setProvisioningVersion(version);
                    // Disable the RCS service
                    mRcsSettings.setServiceActivationState(false);

                } else if (Version.RESETED.toInt() == version) {
                    if (logActivated) {
                        sLogger.debug("Provisioning forbidden: no account");
                    }
                    // Reset config
                    LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver, mRcsSettings,
                            mMessagingLog, mContactManager);

                } else {
                    /* Start retry alarm */
                    if (validity > 0) {
                        HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent, validity);
                    }
                    boolean tcNotAnswered = TermsAndConditionsResponse.NO_ANSWER == mRcsSettings
                            .getTermsAndConditionsResponse();
                    boolean requestTermsAndConditions = mRcsSettings.isProvisioningAcceptButton()
                            || mRcsSettings.isProvisioningRejectButton();
                    /* Check if Terms and conditions should be requested. */
                    if (requestTermsAndConditions && tcNotAnswered) {
                        TermsAndConditionsRequest.showTermsAndConditions(mCtx);
                    } else {
                        if (tcNotAnswered) {
                            if (logActivated) {
                                sLogger.debug("Terms and conditions implicitly accepted");
                            }
                            mRcsAccountManager.createRcsAccount(
                                    mCtx.getString(R.string.rcs_core_account_username), true);
                            mRcsSettings
                                    .setTermsAndConditionsResponse(TermsAndConditionsResponse.ACCEPTED);
                        }
                        /* We parsed successfully the configuration */
                        mRcsSettings.setConfigurationValid(true);
                        LauncherUtils.launchRcsCoreService(mCtx, mRcsSettings);
                    }
                }

                IntentUtils.sendBroadcastEvent(mCtx,
                        RcsService.ACTION_SERVICE_PROVISIONING_DATA_CHANGED);
            } catch (SAXException e) {
                if (logActivated) {
                    sLogger.debug("Can't parse provisioning document");
                }
                // Restore GSMA release saved before parsing of the provisioning
                mRcsSettings.setGsmaRelease(gsmaRelease);

                // Restore the client messaging mode saved before parsing of the provisioning
                mRcsSettings.setMessagingMode(messagingMode);

                if (mFirstProvAfterBoot) {
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
        } else if (HttpURLConnection.HTTP_UNAVAILABLE == result.code) {
            // Server Unavailable
            if (logActivated) {
                sLogger.debug("Server Unavailable. Retry after: " + result.retryAfter + "ms");
            }
            if (mFirstProvAfterBoot) {
                // Reason: Unable to get configuration
                provisioningFails(ProvisioningFailureReasons.UNABLE_TO_GET_CONFIGURATION);
                if (result.retryAfter > 0) {
                    HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent, result.retryAfter);
                }
            } else {
                tryLaunchRcsCoreService(mCtx, result.retryAfter);
            }
        } else if (HttpURLConnection.HTTP_FORBIDDEN == result.code) {
            // Forbidden: reset account + version = 0
            if (logActivated) {
                sLogger.debug("Provisioning forbidden: reset account");
            }
            // Reset version to "0"
            mRcsSettings.setProvisioningVersion(Version.RESETED.toInt());
            // Reset config
            LauncherUtils.resetRcsConfig(mCtx, mLocalContentResolver, mRcsSettings, mMessagingLog,
                    mContactManager);
            // Reason: Provisioning forbidden
            provisioningFails(ProvisioningFailureReasons.PROVISIONING_FORBIDDEN);
        } else if (HTTP_STATUS_ERROR_NETWORK_AUTHENTICATION_REQUIRED == result.code) {
            // Provisioning authentication required
            if (logActivated) {
                sLogger.debug("Provisioning authentication required");
            }
            // Reset provisioning token
            mRcsSettings.setProvisioningToken(null);
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
            if (mFirstProvAfterBoot) {
                // Reason: No configuration present
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
     * @param context the context
     * @param timerRetry timer in milliseconds to trigger next provisioning request. Only applicable
     *            if greater than 0.
     */
    /* package private */void tryLaunchRcsCoreService(Context context, long timerRetry) {
        int version = mRcsSettings.getProvisioningVersion();
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
            if (Version.DISABLED_DORMANT.toInt() == version) {
                if (timerRetry > 0) {
                    HttpsProvisioningService.startRetryAlarm(context, mRetryIntent, timerRetry);
                } else
                    retry();
            }
        }
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
                        + ") provisioning after "
                        + HttpsProvisioningUtils.RETRY_AFTER_511_ERROR_TIMEOUT + "ms");
            }
            return true;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("No more retry after 511 error for provisioning");
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
    /* package private */void retry() {
        if (mRetryCount < HttpsProvisioningUtils.RETRY_MAX_COUNT) {
            mRetryCount++;
            int retryDelay = HttpsProvisioningUtils.RETRY_BASE_TIMEOUT + 2 * (mRetryCount - 1)
                    * HttpsProvisioningUtils.RETRY_BASE_TIMEOUT;
            HttpsProvisioningService.startRetryAlarm(mCtx, mRetryIntent, retryDelay);
            if (sLogger.isActivated()) {
                sLogger.debug("Retry provisioning count: " + mRetryCount);
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("No more retry for provisioning");
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

    /**
     * Schedule a background task on Handler for execution
     */
    /* package private */void scheduleProvisioningOperation(Runnable task) {
        mProvisioningOperationHandler.post(task);
    }

    /**
     * Causes the provisioning handler to quit without processing any more messages in the message
     * queue
     */
    /* package private */void quitProvisioningOperation() {
        mProvisioningOperationHandler.getLooper().quit();
    }

    /**
     * Checks if first provisioning after (re)boot.
     * 
     * @return true if first provisioning after (re)boot.
     */
    public boolean isFirstProvisioningAfterBoot() {
        return mFirstProvAfterBoot;
    }
}
