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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpRequestBase;
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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.net.Uri;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

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

    /**
     * HTTP traces enabled
     */
    public static boolean HTTP_TRACE_ENABLED = false;

    /**
     * HTTP server address
     */
    private final Uri mServerAddr;

    /**
     * HTTP server login
     */
    private final String mServerLogin;

    /**
     * HTTP server password
     */
    private final String mServerPwd;

    /**
     * HTTP transfer event listener
     */
    private HttpTransferEventListener mListener;

    /**
     * HTTP context
     */
    private HttpContext mHttpContext;

    /**
     * HTTP response
     */
    private HttpResponse mResponse;

    /**
     * HTTP client
     */
    private DefaultHttpClient mHttpClient;

    /**
     * Cancellation flag
     */
    private boolean mIsCancelled = false;

    /**
     * Pause flag
     */
    private boolean mIsPaused = false;

    protected final RcsSettings mRcsSettings;

    /**
     * The logger
     */
    private static final Logger LOGGER = Logger
            .getLogger(HttpTransferManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param listener HTTP event listener
     * @param rcsSettings
     */
    public HttpTransferManager(HttpTransferEventListener listener, RcsSettings rcsSettings) {
        mListener = listener;
        mServerAddr = Uri.parse(rcsSettings.getFtHttpServer());
        mServerLogin = rcsSettings.getFtHttpLogin();
        mServerPwd = rcsSettings.getFtHttpPassword();
        mRcsSettings = rcsSettings;
        initServerAddress(mServerAddr);
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
        initServerAddress(address);
    }

    /**
     * Initialize with server address
     * 
     * @param address server address
     */
    private void initServerAddress(Uri address) {
        try {
            // Extract protocol and port
            URL url = new URL(address.toString());
            String protocol = url.getProtocol();
            int port = url.getPort();
            if (port == -1) {
                // Set default port
                if (protocol.equals("https")) {
                    port = 443;
                } else {
                    port = 80;
                }
            }

            // Format HTTP request
            ConnectivityManager connMgr = (ConnectivityManager) AndroidFactory
                    .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            if (protocol.equals("https")) {
                schemeRegistry.register(new Scheme("https",
                        new com.gsma.rcs.provisioning.https.EasySSLSocketFactory(), port));
            } else {
                schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(),
                        port));
            }
            HttpParams params = new BasicHttpParams();
            params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(
                    30));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                @SuppressWarnings("deprecation")
                String proxyHost = Proxy.getDefaultHost();
                if (proxyHost != null && proxyHost.length() > 1) {
                    @SuppressWarnings("deprecation")
                    int proxyPort = Proxy.getDefaultPort();
                    params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost,
                            proxyPort));
                }
            }
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
            mHttpClient = new DefaultHttpClient(cm, params);

            // Create local HTTP context
            CookieStore cookieStore = (CookieStore) new BasicCookieStore();
            mHttpContext = new BasicHttpContext();
            mHttpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
        } catch (MalformedURLException e) {
            // Nothing to do
        }
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
     * Execute HTTP request
     * 
     * @param request HTTP request
     * @return HTTP response
     * @throws IOException
     * @throws ClientProtocolException
     */
    public HttpResponse executeRequest(HttpRequestBase request) throws ClientProtocolException,
            IOException {
        if (mResponse != null) {
            mResponse.getEntity().consumeContent();
        }
        if (mHttpClient != null) {
            mResponse = mHttpClient.execute(request, mHttpContext);
            if (HTTP_TRACE_ENABLED) {
                String trace = "<<< Receive HTTP response:";
                trace += "\n" + mResponse.getStatusLine().toString();
                Header[] headers = mResponse.getAllHeaders();
                for (Header header : headers) {
                    trace += "\n" + header.getName() + " " + header.getValue();
                }
                System.out.println(trace);
            }
            return mResponse;
        } else {
            throw new IOException("HTTP client not found");
        }
    }

    /**
     * Get HTTP client
     * 
     * @return HTTP client
     */
    public DefaultHttpClient getHttpClient() {
        return mHttpClient;
    }

    /**
     * Interrupts file transfer
     */
    public void interrupt() {
        if (LOGGER.isActivated()) {
            LOGGER.warn("interrupting transfer");
        }
        mIsCancelled = true;
    }

    /**
     * Interrupts file transfer
     */
    public void pauseTransferByUser() {
        if (LOGGER.isActivated()) {
            LOGGER.warn("User is pausing transfer");
        }
        mIsPaused = true;
        getListener().httpTransferPausedByUser();
    }

    /**
     * Interrupts file transfer
     */
    public void pauseTransferBySystem() {
        if (LOGGER.isActivated()) {
            LOGGER.warn("System is pausing transfer");
        }
        mIsPaused = true;
        getListener().httpTransferPausedBySystem();
    }

    /**
     * Resuming upload so resetting cancelled boolean
     */
    public void resetParamForResume() {
        mIsCancelled = false;
        mIsPaused = false;
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
}
