/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SocketIoService implements IoService {

    private Socket mSocket = null;

    private BufferedReader mIn;

    private PrintWriter mOut;

    private final String mHost;

    private final int mPort;

    private boolean mUseSsl = true;

    private Logger mLogger = Logger.getLogger(getClass().getName());

    public SocketIoService(String url) {
        super();
        int defport = 993; // default imap SSL port
        if (url.startsWith("imap://")) {
            mUseSsl = false;
            defport = 143;
            url = url.substring(7);
        } else if (url.startsWith("imaps://")) { // default
            url = url.substring(8);
        }
        int i = url.lastIndexOf(':');
        if (i != -1) {
            mPort = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        } else {
            mPort = defport;
        }
        this.mHost = url;
    }

    @Override
    public void close() throws IOException {
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    @Override
    public void connect() throws IOException {
        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.fine("Connecting to " + mHost + ":" + mPort);
        }

        if (mUseSsl) {
            SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket c = (SSLSocket) f.createSocket(mHost, mPort);
            c.startHandshake();
            mSocket = c;
        } else {
            mSocket = new Socket(mHost, mPort);
        }

        mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mOut = new PrintWriter(mSocket.getOutputStream(), true);
    }

    @Override
    public void startTLSHandshake() throws IOException {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                            String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                            String authType) {
                    }
                }
            }, new java.security.SecureRandom());
        } catch (Exception e) {
            throw new IOException(e);
        }
        SSLSocketFactory sslSocketFactory = ((SSLSocketFactory) sc.getSocketFactory());
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(mSocket, mSocket
                .getInetAddress().getHostAddress(), mSocket.getPort(), true);
        sslSocket.startHandshake();
        mSocket = sslSocket;
        mUseSsl = true;
    }

    public String readLine() throws IOException {
        String line = mIn.readLine();
        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.fine("S: " + line);
        }
        return line;
    }

    public String read(int size) throws IOException {
        char[] c = new char[size];
        mIn.read(c);
        String s = new String(c);
        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.fine("S: " + s);
        }

        return s;
    }

    public synchronized void write(String s) throws IOException {
        mOut.print(s);
        mOut.flush();
        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.fine("C: " + s);
        }
    }

    public synchronized void writeln(String s) throws IOException {
        mOut.println(s);
        if (mLogger.isLoggable(Level.FINE)) {
            mLogger.fine("C: " + s);
        }
    }

    @Override
    public boolean isConnected() {
        return (mSocket != null && mSocket.isConnected() && !mSocket.isClosed());
    }

}
