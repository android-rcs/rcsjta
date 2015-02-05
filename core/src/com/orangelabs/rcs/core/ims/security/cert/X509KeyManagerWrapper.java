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

package com.orangelabs.rcs.core.ims.security.cert;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import com.orangelabs.rcs.utils.logger.Logger;

// Changed by Deutsche Telekom
/**
 * This is a wrapper class to allow overwriting of requested DN in certificate request
 * 
 * @author Deutsche Telekom AG
 */
public class X509KeyManagerWrapper implements X509KeyManager {
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private X509KeyManager defaultKeyManager;

    public X509KeyManagerWrapper(KeyManager[] keyManagers) {
        for (KeyManager keyManager : keyManagers) {
            if (keyManager instanceof X509KeyManager) {
                defaultKeyManager = (X509KeyManager) keyManager;
                if (logger.isActivated()) {
                    logger.debug("Choosen key manager: " + defaultKeyManager.toString()
                            + " of class " + defaultKeyManager.getClass().getName());
                }
            }
        }

    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        String alias = null;
        if (logger.isActivated()) {
            if ((keyType != null) && (keyType.length > 0)) {
                for (String kt : keyType) {
                    logger.debug("chooseClientAlias;  key: " + kt);
                }
            }
            if ((issuers != null) && (issuers.length > 0)) {
                for (Principal issuer : issuers) {
                    logger.debug("chooseClientAlias;  issuer: " + issuer.getName());
                }
            }
        }

        alias = defaultKeyManager.chooseClientAlias(keyType, issuers, socket);
        // patch to work around TLS handshake which request a client certificate
        // from a specific issuer
        if (alias == null) {
            if (logger.isActivated()) {
                logger.debug("No client certificate alias found! Fall back to default certificate.");
            }
            alias = KeyStoreManager.CLIENT_CERT_ALIAS;
        }
        return alias;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return defaultKeyManager.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return defaultKeyManager.getCertificateChain(alias);
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return defaultKeyManager.getClientAliases(keyType, issuers);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return defaultKeyManager.getPrivateKey(alias);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return defaultKeyManager.getServerAliases(keyType, issuers);
    }
}
