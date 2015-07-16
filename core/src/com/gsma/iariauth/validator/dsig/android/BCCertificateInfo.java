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

package com.gsma.iariauth.validator.dsig.android;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

import local.org.bouncycastle.asn1.ASN1OctetString;
import local.org.bouncycastle.asn1.ASN1Primitive;
import local.org.bouncycastle.asn1.ASN1String;
import local.org.bouncycastle.asn1.DERSequence;
import local.org.bouncycastle.asn1.x509.GeneralName;

import com.gsma.iariauth.validator.dsig.CertificateInfo;

/**
 * A CertificateInfo implementation based on a certificate obtained from the BouncyCastle provider.
 */
public class BCCertificateInfo extends CertificateInfo {

    private static final String FINGERPRINT_DIGEST_ALG = "SHA-1";
    private static final String SAN_OID = "2.5.29.17";

    private X509Certificate cert;
    private String cn;
    private String organisation;
    private String country;
    private String fingerprint;
    private String[] uriIdentities;
    private String publicKey;
    private String subject;
    private String serialNumber;

    /**************************
     * public constructor
     **************************/

    public BCCertificateInfo() {
    }

    public void initFromX509(X509Certificate x509Cert) {
        this.cert = x509Cert;
        try {
            getFingerprint(x509Cert);
            getSANData(x509Cert);

            publicKey = bytes2hexstring(x509Cert.getPublicKey().getEncoded());
            subject = bytes2hexstring(x509Cert.getSubjectX500Principal().getEncoded());
            serialNumber = x509Cert.getSerialNumber().toString();
            return;
        } catch (CertificateEncodingException e) {
        } catch (IOException e) {
        }
    }

    /**************************
     * CertificateInfo methods
     **************************/

    @Override
    public String getCN() {
        return cn;
    }

    @Override
    public String getOrganisation() {
        return organisation;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public String[] getURIIdentities() {
        return uriIdentities;
    }

    @Override
    public String getEncodedPublicKey() {
        return publicKey;
    }

    @Override
    public String getEncodedSubject() {
        return subject;
    }

    @Override
    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public X509Certificate getX509Certificate() {
        return cert;
    }

    /*************************************
     * stringify for error/warning dialogs
     *************************************/

    public String toString() {
        StringBuffer details = new StringBuffer();
        if (cn != null && cn.length() != 0) {
            details.append("CN=");
            details.append(cn);
            details.append('\n');
        }

        if (organisation != null && organisation.length() != 0) {
            details.append("O=");
            details.append(organisation);
            details.append('\n');
        }

        if (country != null && country.length() != 0) {
            details.append("C=");
            details.append(country);
            details.append('\n');
        }

        if (uriIdentities != null && uriIdentities.length != 0) {
            for (String uri : uriIdentities) {
                details.append("URI=");
                details.append(uri);
                details.append('\n');
            }
        }

        if (fingerprint != null && fingerprint.length() != 0) {
            details.append(fingerprint);
            details.append('\n');
        }

        return details.toString();
    }

    /**************************
     * Helpers
     **************************/

    private void getFingerprint(X509Certificate x509Cert) throws CertificateEncodingException {
        try {
            byte[] digest = MessageDigest.getInstance(FINGERPRINT_DIGEST_ALG).digest(
                    x509Cert.getEncoded());
            StringBuffer result = new StringBuffer();
            int len = digest.length - 1;
            for (int i = 0; i < len; i++) {
                result.append(byte2hex(digest[i]));
                result.append(':');
            }
            result.append(byte2hex(digest[len]));
            fingerprint = result.toString();
        } catch (NoSuchAlgorithmException e) {/* should not be possible */
        }
    }

    private void getSANData(X509Certificate x509Cert) throws IOException {
        byte[] bytes = x509Cert.getExtensionValue(SAN_OID);
        if (bytes != null) {
            ArrayList<String> cUriIdentities = new ArrayList<String>();
            Enumeration<?> it = DERSequence.getInstance(fromExtensionValue(bytes)).getObjects();
            while (it.hasMoreElements()) {
                GeneralName genName = GeneralName.getInstance(it.nextElement());
                if (genName.getTagNo() == GeneralName.uniformResourceIdentifier) {
                    cUriIdentities.add(((ASN1String) genName.getName()).getString());
                }
            }
            if (cUriIdentities.size() > 0) {
                uriIdentities = cUriIdentities.toArray(new String[cUriIdentities.size()]);
            }
        }
    }

    private static ASN1Primitive fromExtensionValue(byte[] encodedValue) throws IOException {
        ASN1OctetString octs = (ASN1OctetString) ASN1Primitive.fromByteArray(encodedValue);
        return ASN1Primitive.fromByteArray(octs.getOctets());
    }

    private static char[] byte2hex(byte b) {
        String lookup = "0123456789ABCDEF";
        int c1 = (b & 0xf0) >> 4;
        int c2 = b & 0xf;
        return new char[] {
                lookup.charAt(c1), lookup.charAt(c2)
        };
    }

    private static String bytes2hexstring(byte b[]) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            result.append(byte2hex(b[i]));
        }
        return result.toString();
    }

}
