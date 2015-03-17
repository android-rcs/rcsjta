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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * SSL Factory created for file transfer
 * 
 * @author hhff3235
 */
public class FileTransSSLFactory {

    static private SSLContext sSslContext;

    static {
        try {
            sSslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            sSslContext = null;
        }
    }

    /**
     * Get a SSL context generated with a trust all manager
     * 
     * @return SSLContext result or null if fails
     */
    static public SSLContext getFileTransferSSLContext() {
        try {
            sSslContext.init(null, new TrustManager[] {
                new AllTrustManager()
            }, new SecureRandom());
        } catch (KeyManagementException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }

        return sSslContext;
    }

    /**
     * 
     *
     */
    public static class AllTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }

}
