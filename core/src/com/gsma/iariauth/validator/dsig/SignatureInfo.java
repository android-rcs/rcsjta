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

/**
 * A class encapsulating the relevant details of a processed signature.
 */
public class SignatureInfo {

    /***********************
     * Public API
     ***********************/

    static SignatureInfo create(AuthType authType, String id, CertificateInfo rootCert,
            CertificateInfo entityCert, CertificateInfo[] certPath, String[] policies) {
        SignatureInfo result = new SignatureInfo();
        result.authType = authType;
        result.id = id;
        result.rootCert = rootCert;
        result.entityCert = entityCert;
        result.certPath = certPath;
        return result;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public String getId() {
        return id;
    }

    public CertificateInfo getRootCertificate() {
        return rootCert;
    }

    public CertificateInfo getEntityCertificate() {
        return entityCert;
    }

    public String[] getValidPolicies() {
        return policies;
    }

    public CertificateInfo[] getCertificatePath() {
        return certPath;
    }

    /*************************************
     * stringify for error/warning dialogs
     *************************************/

    public String toString() {
        StringBuffer details = new StringBuffer();
        details.append('\n');
        details.append(entityCert.toString());
        details.append(rootCert.toString());

        return details.toString();
    }

    /***********************
     * Internal
     ***********************/

    private SignatureInfo() {
    }

    private AuthType authType;
    private String id;
    private CertificateInfo rootCert;
    private CertificateInfo entityCert;
    private String[] policies;
    private CertificateInfo[] certPath;

}
