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
    private Uri serverAddr = Uri.parse(RcsSettings.getInstance().getFtHttpServer());

    /**
     * HTTP server login
     */
    private String serverLogin = RcsSettings.getInstance().getFtHttpLogin();

    /**
     * HTTP server password
     */
    private String serverPwd = RcsSettings.getInstance().getFtHttpPassword();

    /**
     * HTTP transfer event listener
     */
    private HttpTransferEventListener listener;

    /**
     * HTTP context
     */
    private HttpContext httpContext = null;

    /**
     * HTTP response
     */
    private HttpResponse response = null;

    /**
     * HTTP client
     */
    private DefaultHttpClient httpClient = null;

    /**
     * Cancellation flag
     */
    private boolean isCancelled = false;

    /**
     * Pause flag
     */
    private boolean isPaused = false;

    /**
     * The logger
     */
    private static final Logger logger = Logger
            .getLogger(HttpTransferManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param listener HTTP event listener
     */
    public HttpTransferManager(HttpTransferEventListener listener) {
        this.listener = listener;

        initServerAddress(serverAddr);
    }

    /**
     * Constructor
     * 
     * @param listener HTTP event listener
     * @param address HTTP server address
     */
    public HttpTransferManager(HttpTransferEventListener listener, Uri address) {
        this.listener = listener;
        this.serverAddr = address;
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
                String proxyHost = Proxy.getDefaultHost();
                if (proxyHost != null && proxyHost.length() > 1) {
                    int proxyPort = Proxy.getDefaultPort();
                    params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost,
                            proxyPort));
                }
            }
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
            httpClient = new DefaultHttpClient(cm, params);

            // Create local HTTP context
            CookieStore cookieStore = (CookieStore) new BasicCookieStore();
            httpContext = new BasicHttpContext();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
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
        return listener;
    }

    /**
     * Returns HTTP server address
     * 
     * @return Address
     */
    public Uri getHttpServerAddr() {
        return serverAddr;
    }

    /**
     * Returns HTTP server login
     * 
     * @return Login
     */
    public String getHttpServerLogin() {
        return serverLogin;
    }

    /**
     * Returns HTTP server password
     * 
     * @return Password
     */
    public String getHttpServerPwd() {
        return serverPwd;
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
        if (response != null) {
            response.getEntity().consumeContent();
        }
        if (httpClient != null) {
            response = httpClient.execute(request, httpContext);
            if (HTTP_TRACE_ENABLED) {
                String trace = "<<< Receive HTTP response:";
                trace += "\n" + response.getStatusLine().toString();
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    trace += "\n" + header.getName() + " " + header.getValue();
                }
                System.out.println(trace);
            }
            return response;
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
        return httpClient;
    }

    /**
     * Interrupts file transfer
     */
    public void interrupt() {
        if (logger.isActivated()) {
            logger.warn("interrupting transfer");
        }
        isCancelled = true;
    }

    /**
     * Interrupts file transfer
     */
    public void pauseTransferByUser() {
        if (logger.isActivated()) {
            logger.warn("User is pausing transfer");
        }
        isPaused = true;
        getListener().httpTransferPausedByUser();
    }

    /**
     * Interrupts file transfer
     */
    public void pauseTransferBySystem() {
        if (logger.isActivated()) {
            logger.warn("System is pausing transfer");
        }
        isPaused = true;
        getListener().httpTransferPausedBySystem();
    }

    /**
     * Resuming upload so resetting cancelled boolean
     */
    public void resetParamForResume() {
        isCancelled = false;
        isPaused = false;
    }

    /**
     * Return whether or not the file transfer has been cancelled
     * 
     * @return Boolean
     */
    public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * Return whether or not the file transfer has been cancelled
     * 
     * @return Boolean
     */
    public boolean isPaused() {
        return this.isPaused;
    }
}
