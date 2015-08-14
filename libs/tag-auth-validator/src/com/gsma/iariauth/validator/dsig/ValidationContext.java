/*
 * Copyright (C) 2014 GSM Association
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.gsma.iariauth.validator.dsig;

import java.security.PublicKey;
import com.gsma.contrib.javax.xml.crypto.KeySelector;

/**
 * An interface encapsulating the state of an ongoing signature and certificate path validation
 */
public interface ValidationContext {
    /**
     * Get KeySelector
     */
    public KeySelector getKeySelector();

    /**
     * Get signing key
     */
    public PublicKey getSigningKey();

    /**
     * Get the result of key verification
     */
    public int getVerificationResult();

    /**
     * Get the root certificate if valid
     */
    public CertificateInfo getRootCert();

    /**
     * Get the entity certificate if valid
     */
    public CertificateInfo getEntityCert();

    /**
     * Get the valid policies for the certificate path
     */
    public String[] getPolicies();

    /**
     * Get the whole certificate path
     */
    public CertificateInfo[] getCertificatePath();
}
