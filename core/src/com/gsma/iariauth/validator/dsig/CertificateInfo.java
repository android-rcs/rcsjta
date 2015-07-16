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

import java.security.cert.X509Certificate;

public abstract class CertificateInfo {
    public abstract String getCN();

    public abstract String getOrganisation();

    public abstract String getCountry();

    public abstract String getFingerprint();

    public abstract String[] getURIIdentities();

    public abstract String getEncodedPublicKey();

    public abstract String getEncodedSubject();

    public abstract String getSerialNumber();

    public abstract X509Certificate getX509Certificate();

    public abstract void initFromX509(X509Certificate x509Cert);

    public static CertificateInfo create(X509Certificate x509Cert) {
        CertificateInfo result = null;
        try {
            try {
                result = (CertificateInfo) Class.forName(
                        "com.gsma.iariauth.validator.dsig.android.BCCertificateInfo").newInstance();
            } catch (ClassNotFoundException cnfe) {
                result = (CertificateInfo) Class.forName(
                        "com.gsma.iariauth.validator.dsig.jre.BCCertificateInfo").newInstance();
            }
        } catch (ClassNotFoundException cnfe) { /* not expected */
        } catch (IllegalAccessException e) { /* not expected */
        } catch (InstantiationException ie) { /* not expected */
        }

        result.initFromX509(x509Cert);
        return result;
    }
}
