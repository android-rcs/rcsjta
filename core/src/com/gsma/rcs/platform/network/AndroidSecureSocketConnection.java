/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.platform.network;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.security.cert.X509KeyManagerWrapper;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provisioning.https.EasyX509TrustManager;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.logger.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import javax2.sip.ListeningPoint;

/**
 * Android secure socket connection
 * 
 * @author jexa7410
 */
public class AndroidSecureSocketConnection extends AndroidSocketConnection {
    // Changed by Deutsche Telekom
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * SSL factory
     */
    // Changed by Deutsche Telekom
    // TODO: check why this was static before
    private SSLSocketFactory mSslSocketFactory;

    // Changed by Deutsche Telekom
    /**
     * usage of certificate checks
     */
    private boolean mCheckCertificate = true;

    // Changed by Deutsche Telekom
    /**
     * announced fingerprint
     */
    private String mFingerprint;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public AndroidSecureSocketConnection(RcsSettings rcsSettings) {
        super();
        mRcsSettings = rcsSettings;
    }

    // Changed by Deutsche Telekom
    /**
     * Constructor
     * 
     * @param fingerprint
     * @param rcsSettings
     */
    public AndroidSecureSocketConnection(String fingerprint, RcsSettings rcsSettings) {
        super();
        mCheckCertificate = false;
        mFingerprint = fingerprint;
        mRcsSettings = rcsSettings;
    }

    /**
     * Constructor
     * 
     * @param socket SSL socket
     * @param rcsSettings
     */
    public AndroidSecureSocketConnection(SSLSocket socket, RcsSettings rcsSettings) {
        super(socket);
        mRcsSettings = rcsSettings;
    }

    /**
     * Open the socket
     * 
     * @param remoteAddr Remote address
     * @param remotePort Remote port
     * @throws NetworkException
     * @throws PayloadException
     */
    public void open(String remoteAddr, int remotePort) throws NetworkException,
            PayloadException {
        SSLSocket socket = null;
        try {
        // Changed by Deutsche Telekom
            socket = (SSLSocket) getSslFactory().createSocket(remoteAddr, remotePort);
        // Changed by Deutsche Telekom
            socket.startHandshake();
            if (mFingerprint != null) {
                // example of a fingerprint:
                // SHA-1 4A:AD:B9:B1:3F:82:18:3B:54:02:12:DF:3E:5D:49:6B:19:E5:7C:AB
                String[] announcedFingerprintElements = mFingerprint.split(" ");
                if (announcedFingerprintElements != null && announcedFingerprintElements.length > 1) {
                    // use the same hashing algorithm as announced
                    String usedFingerprint = getFingerprint(announcedFingerprintElements[0], socket);
                    // compare fingerprints and stop if not matching
                    if (announcedFingerprintElements[1] != null
                            && !announcedFingerprintElements[1].equals(usedFingerprint)) {
                        if (logger.isActivated()) {
                            logger.debug("Wrong fingerprint! " + usedFingerprint
                                    + " is used while " + announcedFingerprintElements[1]
                                    + " is expected!");
                        }
                        /* Close the socket as an attack is assumed */
                        CloseableUtils.tryToClose(socket);
                        return;
                    }
                }
            }
            setSocket(socket);
        } catch (IOException e) {
            throw new NetworkException(new StringBuilder(
                    "Failed to open socket connection for address : ").append(remoteAddr)
                    .append("and port : ").append(remotePort).toString(), e);
        } finally {
            CloseableUtils.tryToClose(socket);
        }

    }

    /**
     * Get the certificate fingerprint
     * 
     * @param algorithm hash algorithm to be used
     * @param socket
     * @return String
     * @throws SSLPeerUnverifiedException
     * @throws PayloadException
     */
    private String getFingerprint(String algorithm, SSLSocket socket)
            throws SSLPeerUnverifiedException, PayloadException {
        try {
            final SSLSession session = socket.getSession();
            if (session == null) {
                throw new SSLPeerUnverifiedException("SSL session not available!");
            }
            Certificate[] certs = session.getPeerCertificates();
            if (logger.isActivated()) {
                logger.debug("Remote certificate chain length: " + certs.length);
            }
            if (certs.length == 0) {
                throw new SSLPeerUnverifiedException(
                        "No remote certificates available for SSL session!");
            }
            return KeyStoreManager.getCertFingerprint(certs[0], algorithm);

        } catch (CertificateEncodingException e) {
            throw new PayloadException("SSL session not available!", e);

        } catch (NoSuchAlgorithmException e) {
            throw new PayloadException("SSL session not available!", e);
        }
    }

    /**
     * Returns the SSL factory instance
     * 
     * @param checkCertificate flag to indicate whether the remote certificate will be checked
     * @return SSL factory
     * @throws IOException
     */
    // Changed by Deutsche Telekom
    // TODO: check why this was static before
    private synchronized SSLSocketFactory getSslFactory() throws IOException {
        // Changed by Deutsche Telekom
        FileInputStream ksFileInputStream = null;
        FileInputStream tsFileInputStream = null;
        if (mSslSocketFactory == null) {
            try {
                // Changed by Deutsche Telekom
                if (logger.isActivated()) {
                    logger.debug("Create SSLSocketFactory");
                }
                TrustManager[] tms = null;
                KeyManager[] kms = null;

                String keyStoreType = KeyStoreManager.getKeystoreType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);

                String keyStoreFile = KeyStoreManager.getKeystore().getPath();
                ksFileInputStream = new FileInputStream(keyStoreFile);

                char[] keyStorePassword = KeyStoreManager.getKeystorePassword().toCharArray();
                keyStore.load(ksFileInputStream, keyStorePassword);

                String algorithm = KeyManagerFactory.getDefaultAlgorithm();

                if (mCheckCertificate) {
                    // only use trusted certificates terminating on a well-known root CA
                    TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(algorithm);

                    if (KeyStoreManager.isOwnCertificateUsed(mRcsSettings)) {
                        KeyStore trustStore = KeyStore.getInstance(keyStoreType);
                        // Changed by Deutsche Telekom
                        tsFileInputStream = new FileInputStream(keyStoreFile);
                        trustStore.load(tsFileInputStream, keyStorePassword);
                        tmFactory.init(trustStore);

                        KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);
                        kmFactory.init(keyStore, keyStorePassword);
                        kms = kmFactory.getKeyManagers();
                    } else {
                        tmFactory.init((KeyStore) null);
                    }
                    tms = tmFactory.getTrustManagers();
                } else {
                    // use an own TrustManager to allow the usage of self-signed certificates
                    // Changed by Deutsche Telekom
                    if (logger.isActivated()) {
                        logger.debug("Use self-signed certificates");
                    }
                    KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(algorithm);
                    kmFactory.init(keyStore, keyStorePassword);
                    kms = kmFactory.getKeyManagers();

                    // overwrite 1st key manager with own wrapper to work around
                    // certificate request for unknown issuers
                    KeyManager km = new X509KeyManagerWrapper(kms);
                    kms[0] = km;

                    tms = new TrustManager[] {
                        new EasyX509TrustManager(null)
                    };
                }
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextInt();
                SSLContext sslContext = SSLContext.getInstance(ListeningPoint.TLS);
                sslContext.init(kms, tms, secureRandom);
                mSslSocketFactory = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(
                        "Unable to create SSL instance for service type :  "
                                .concat(ListeningPoint.TLS),
                        e);

            } catch (KeyStoreException e) {
                throw new IOException(
                        "Unable to create SSL instance for service type :  "
                                .concat(ListeningPoint.TLS),
                        e);

            } catch (CertificateException e) {
                throw new IOException(
                        "Unable to create SSL instance for service type :  "
                                .concat(ListeningPoint.TLS),
                        e);

            } catch (UnrecoverableKeyException e) {
                throw new IOException(
                        "Unable to create SSL instance for service type :  "
                                .concat(ListeningPoint.TLS),
                        e);

            } catch (KeyManagementException e) {
                throw new IOException(
                        "Unable to create SSL instance for service type :  "
                                .concat(ListeningPoint.TLS),
                        e);

            } finally {
                CloseableUtils.tryToClose(ksFileInputStream);
                CloseableUtils.tryToClose(tsFileInputStream);
            }
        }
        return mSslSocketFactory;
    }

}
