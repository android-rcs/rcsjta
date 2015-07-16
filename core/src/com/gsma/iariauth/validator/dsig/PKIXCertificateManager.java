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

import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * KeySelector class that supports all of the required use cases for certificate location, trust and
 * certificate path creation Platform has designed trust anchors plus additional known certificates
 * Supports identification of end-entity certs by KeyName plus any of the standard X509Data
 * identification means Supports construction of certificate paths using trust anchors, known certs,
 * plus certs provided in X509Data.
 */
public class PKIXCertificateManager implements CertificateManager {

    /**
     * public API
     */
    public PKIXCertificateManager() {
    }

    /**
     * Add a known certificate, untrusted, to the set of known certificates This certificate may be
     * used in constructing and validating certificate paths NOTE there is no way to add new trusted
     * certs - this is only possible through the trustAnchor KeyStores
     */
    @Override
    public synchronized void addCert(X509Certificate cert) {
        certificates.add(cert);
    }

    /**
     * Retrieve certificates
     * 
     * @return
     */
    @Override
    public synchronized List<X509Certificate> getCertificates() {
        return certificates;
    }

    /**
     * Add a CRL to the set of known CRLs This CRL may be used in constructing and validating
     * certificate paths
     */
    @Override
    public synchronized void addCRL(X509CRL crl) {
        crls.add(crl);
    }

    /**
     * Retrieve certificates
     * 
     * @return
     */
    @Override
    public synchronized List<X509CRL> getCRLs() {
        return crls;
    }

    /**
     * Get Validation Context
     */
    @Override
    public ValidationContext getValidationContext(AuthType authType) {
        PKIXValidationContext ctx = new PKIXValidationContext(this, authType);
        validationContexts.add(ctx);
        return ctx;
    }

    /**
     * Release resources associated with a context
     */
    @Override
    public void releaseContext(ValidationContext ctx) {
        if (ctx != null) {
            validationContexts.remove(ctx);
        }
    }

    private final List<X509Certificate> certificates = new ArrayList<X509Certificate>();
    private final List<X509CRL> crls = new ArrayList<X509CRL>();
    private final List<ValidationContext> validationContexts = new ArrayList<ValidationContext>();
}
