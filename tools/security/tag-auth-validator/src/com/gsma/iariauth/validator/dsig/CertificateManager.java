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
import java.util.List;

/**
 * An interface representing a container of certificates obtained from signature processing.
 */
public interface CertificateManager {

    /**
     * Add a known certificate, untrusted, to the set of known certificates. This certificate may be
     * used in constructing and validating certificate paths NOTE there is no way to add new trusted
     * certs - this is only possible through the trustAnchor KeyStores
     */
    public void addCert(X509Certificate cert);

    /**
     * Retrieve certificates
     * 
     * @return
     */
    public List<X509Certificate> getCertificates();

    /**
     * Add a CRL to the set of known CRLs This CRL may be used in constructing and validating
     * certificate paths
     */
    public void addCRL(X509CRL crl);

    /**
     * Retrieve CRLs
     * 
     * @return
     */
    public List<X509CRL> getCRLs();

    /**
     * Open a validation context
     */
    public ValidationContext getValidationContext(AuthType authType);

    /**
     * Release resources associated with a context
     */
    public void releaseContext(ValidationContext ctx);
}
