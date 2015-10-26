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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.net.Uri;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Abstract HTTP transfer manager
 * 
 * @author vfml3370
 */
public abstract class HttpTransferManager {
    /**
     * Max chunk size
     */
    public static final int CHUNK_MAX_SIZE = 10 * 1024;

    private static boolean sHttpTraceEnabled = false;

    private final Uri mServerAddr;

    private final String mServerLogin;

    private final String mServerPwd;

    /**
     * HTTP transfer event listener
     */
    private HttpTransferEventListener mListener;

    /**
     * Cancellation flag
     */
    private boolean mIsCancelled = false;

    /**
     * Pause flag
     */
    private boolean mIsPaused = false;

    protected final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(HttpTransferManager.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param listener HTTP event listener
     * @param rcsSettings
     */
    public HttpTransferManager(HttpTransferEventListener listener, RcsSettings rcsSettings) {
        this(listener, rcsSettings.getFtHttpServer(), rcsSettings);
    }

    /**
     * Constructor
     * 
     * @param listener HTTP event listener
     * @param address HTTP server address
     * @param rcsSettings
     */
    public HttpTransferManager(HttpTransferEventListener listener, Uri address,
            RcsSettings rcsSettings) {
        mListener = listener;
        mServerAddr = address;
        mServerLogin = rcsSettings.getFtHttpLogin();
        mServerPwd = rcsSettings.getFtHttpPassword();
        mRcsSettings = rcsSettings;
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    /**
     * Returns the transfer event listener
     * 
     * @return Listener
     */
    public HttpTransferEventListener getListener() {
        return mListener;
    }

    /**
     * Returns HTTP server address
     * 
     * @return Address
     */
    public Uri getHttpServerAddr() {
        return mServerAddr;
    }

    /**
     * Returns HTTP server login
     * 
     * @return Login
     */
    public String getHttpServerLogin() {
        return mServerLogin;
    }

    /**
     * Returns HTTP server password
     * 
     * @return Password
     */
    public String getHttpServerPwd() {
        return mServerPwd;
    }

    /**
     * Interrupts file transfer
     */
    public void interrupt() {
        if (sLogger.isActivated()) {
            sLogger.warn("interrupting transfer");
        }
        mIsCancelled = true;
    }

    /**
     * Interrupts file transfer
     */
    public void pauseTransferByUser() {
        if (sLogger.isActivated()) {
            sLogger.warn("User is pausing transfer");
        }
        mIsPaused = true;
        getListener().onHttpTransferPausedByUser();
    }

    /**
     * Interrupts file transfer
     */
    public void pauseTransferBySystem() {
        if (sLogger.isActivated()) {
            sLogger.warn("System is pausing transfer");
        }
        mIsPaused = true;
        getListener().onHttpTransferPausedBySystem();
    }

    /**
     * Resuming upload so resetting cancelled boolean
     */
    public void resumeTransfer() {
        if (sLogger.isActivated()) {
            sLogger.warn("Transfer is resuming");
        }
        mIsCancelled = false;
        mIsPaused = false;
        getListener().onHttpTransferResumed();
    }

    /**
     * Return whether or not the file transfer has been cancelled
     * 
     * @return Boolean
     */
    public boolean isCancelled() {
        return mIsCancelled;
    }

    /**
     * Return whether or not the file transfer has been cancelled
     * 
     * @return Boolean
     */
    public boolean isPaused() {
        return mIsPaused;
    }

    /**
     * Open HTTP connection
     * 
     * @param url the URL to connect
     * @param properties HTTP properties to set
     * @return HttpURLConnection
     * @throws NetworkException
     */
    protected HttpURLConnection openHttpConnection(URL url, Map<String, String> properties)
            throws NetworkException {
        try {
            HttpURLConnection cnx = (HttpURLConnection) url.openConnection();
            for (Entry<String, String> header : properties.entrySet()) {
                cnx.setRequestProperty(header.getKey(), header.getValue());
            }
            cnx.setRequestProperty("User-Agent", SipUtils.userAgentString());
            return cnx;

        } catch (IOException e) {
            throw new NetworkException(new StringBuilder(
                    "Failed to open http connection with url : ").append(url).toString(), e);
        }
    }

    /**
     * Checks if HTTP trace is enabled
     * 
     * @return True if HTTP trace is enabled
     */
    public static boolean isHttpTraceEnabled() {
        return sHttpTraceEnabled;
    }

    /**
     * Sets HTTP trace enabled
     * 
     * @param enabled True if HTTP trace is enabled
     */
    public static void setHttpTraceEnabled(boolean enabled) {
        sHttpTraceEnabled = enabled;
    }
}
