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

import com.gsma.contrib.javax.xml.crypto.AlgorithmMethod;
import com.gsma.contrib.javax.xml.crypto.KeySelector;
import com.gsma.contrib.javax.xml.crypto.KeySelectorException;
import com.gsma.contrib.javax.xml.crypto.KeySelectorResult;
import com.gsma.contrib.javax.xml.crypto.XMLCryptoContext;
import com.gsma.contrib.javax.xml.crypto.XMLStructure;
import com.gsma.contrib.javax.xml.crypto.dsig.SignatureMethod;
import com.gsma.contrib.javax.xml.crypto.dsig.keyinfo.KeyInfo;
import com.gsma.contrib.javax.xml.crypto.dsig.keyinfo.X509Data;
import com.gsma.contrib.javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;
import com.gsma.iariauth.validator.ProcessingResult;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PolicyNode;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

/**
 * A ValidationContext implementation for a PKIX certificate validation.
 */
class PKIXValidationContext extends KeySelector implements ValidationContext {

    PKIXValidationContext(PKIXCertificateManager certificateManager, AuthType authType) {
        this.certificateManager = certificateManager;
        this.authType = authType;
        this.trustAnchors = new HashSet<TrustAnchor>();
    }

    /***************************
     * IValidationContext methods
     ***************************/

    public PublicKey getSigningKey() {
        PublicKey result = null;
        if (selectorResult != null) {
            result = (PublicKey) selectorResult.getKey();
        }
        return result;
    }

    public int getVerificationResult() {
        if (selectorResult == null) {
            /* shouldn't happen, because core validation would have failed already */
            return ProcessingResult.STATUS_INTERNAL_ERROR;
        }

        if (selectorResult instanceof CertPathKeySelectorResult) {
            /* a valid path was constructed by the builder */
            CertPathKeySelectorResult certPathSelector = (CertPathKeySelectorResult) selectorResult;
            PKIXCertPathBuilderResult pathResult = ((CertPathKeySelectorResult) selectorResult)
                    .getPathResult();
            entityCert = CertificateInfo.create(certPathSelector.getEntityCertificate());
            rootCert = CertificateInfo.create(certPathSelector.getTrustedCertificate());
            policies = getValidPolicies(pathResult.getPolicyTree());

            List<? extends Certificate> certificates = certPathSelector.getCertPath()
                    .getCertificates();
            if (certificates != null && certificates.size() > 0) {
                certPath = new CertificateInfo[certificates.size() + 1];
                int idx = 0;
                for (Certificate c : certificates) {
                    certPath[idx++] = CertificateInfo.create((X509Certificate) c);
                }
                certPath[idx] = rootCert;
            }

            return ProcessingResult.STATUS_VALID_HAS_ANCHOR;
        }

        if (selectorResult instanceof NoPathKeySelectorResult) {
            /* no trust path */
            Throwable pathException = ((NoPathKeySelectorResult) selectorResult).getPathException();
            if (pathException instanceof CertPathBuilderException) {
                /* no better way to do this, see if revocation is mentioned in any cause message */
                String messages = "";
                while (pathException != null) {
                    String msg = pathException.getMessage();
                    if (msg != null)
                        messages += msg;
                    try {
                        pathException = ((CertPathBuilderException) pathException).getCause();
                        continue;
                    } catch (ClassCastException e) {
                        try {
                            pathException = ((CertPathValidatorException) pathException).getCause();
                            continue;
                        } catch (ClassCastException f) {
                            break;
                        }
                    }
                }
                if (messages.contains("revoke") || messages.contains("revocation")) {
                    return ProcessingResult.STATUS_REVOKED;
                }
            }
        }

        /* key validated ok, but missing or incomplete path */
        return ProcessingResult.STATUS_VALID_NO_ANCHOR;
    }

    public CertificateInfo getRootCert() {
        return rootCert;
    }

    public CertificateInfo getEntityCert() {
        return entityCert;
    }

    public String[] getPolicies() {
        return policies;
    }

    public KeySelector getKeySelector() {
        return this;
    }

    public CertificateInfo[] getCertificatePath() {
        return certPath;
    }

    /**********************
     * Private
     **********************/
    @SuppressWarnings("unchecked")
    @Override
    public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method,
            XMLCryptoContext context) throws KeySelectorException {

        if (keyInfo == null) {
            return nullResult;
        }

        /* process key references */
        SignatureMethod sigMethod = (SignatureMethod) method;
        Iterator<XMLStructure> keyInfoItems = keyInfo.getContent().iterator();
        while (keyInfoItems.hasNext()) {
            XMLStructure item = keyInfoItems.next();
            if (item instanceof X509Data) {
                X509Data x509Item = (X509Data) item;
                KeySelectorResult result = selectByX509(keyInfo, x509Item, sigMethod);
                if (result != nullResult) {
                    selectorResult = result;
                    return result;
                }
            }
        }
        /* if there was x509 then don't try other types */
        return nullResult;
    }

    @SuppressWarnings("unchecked")
    public KeySelectorResult selectByX509(KeyInfo keyInfo, X509Data x509Item,
            SignatureMethod sigMethod) throws KeySelectorException {
        /*
         * Here we may be told the certificate explicitly (ie by IssuerSerial, SubjectName, or SKI)
         * or where there is a sole X509Certificate referenced. Alternatively, we might only know
         * implicitly if there are multiple X50Certificates referenced We process the first explicit
         * indication that we find in document order This algorithm assumes that if there is no
         * explicit indication, then the first referenced certificate is the end-entity cert. We
         * might be able to improve this later based on Extended Key Usage or other certificate
         * attributes
         */
        X509Certificate includedCert = null;
        int includedCertCount = 0;
        X509CertSelector certSelector = null;

        Iterator<Object> items = x509Item.getContent().iterator();
        while (items.hasNext()) {
            Object item = items.next();
            if (item instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) item;
                /* count certs, and remember the first */
                if (includedCertCount++ == 0) {
                    includedCert = cert;
                }
                /* add cert as untrusted path cert, or trust anchor, depending on authType */
                if (authType == AuthType.SELF_SIGNED && isSelfSigned(cert))
                    trustAnchors.add(new TrustAnchor(cert, null));

                /* unconditionally add this cert to the CertificateManager */
                certificateManager.addCert(cert);
            } else if (item instanceof byte[]) {
                /* X509 SKI */
                byte[] ski = (byte[]) item;
                /* DER-encode ski */
                byte[] encodedSki = new byte[ski.length + 2];
                encodedSki[0] = 0x04; // OCTET STRING tag value
                encodedSki[1] = (byte) ski.length; // length
                System.arraycopy(ski, 0, encodedSki, 2, ski.length);
                certSelector = new X509CertSelector();
                certSelector.setSubjectKeyIdentifier(encodedSki);
            } else if (item instanceof X509IssuerSerial) {
                /* issuer serial */
                X509IssuerSerial serial = (X509IssuerSerial) item;
                certSelector = new X509CertSelector();
                try {
                    certSelector.setSerialNumber(serial.getSerialNumber());
                    certSelector.setIssuer(new X500Principal(serial.getIssuerName()).getName());
                } catch (IOException ioe) {
                    throw new KeySelectorException(ioe);
                }
            } else if (item instanceof String) {
                /* Subject Name */
                String subjectName = (String) item;
                certSelector = new X509CertSelector();
                try {
                    certSelector.setSubject(new X500Principal(subjectName).getEncoded());
                } catch (IOException ioe) {
                    throw new KeySelectorException(ioe);
                }
            } else if (item instanceof X509CRL) {
                /* CRL - add to our local collection */
                X509CRL crl = (X509CRL) item;
                certificateManager.addCRL(crl);
            } else {
                /* there are no other types */
                continue;
            }
        }

        /* use the implicit included cert if we do not have explicit identification */
        if (certSelector == null) {
            if (includedCertCount == 0) {
                /* no useful data here */
                return nullResult;
            }
            certSelector = new X509CertSelector();
            certSelector.setCertificate(includedCert);
        }
        try {
            certSelector.setSubjectPublicKeyAlgID(getOIDforAlgorithm(sigMethod.getAlgorithm()));
        } catch (IOException ioe) {
            throw new KeySelectorException(ioe);
        }

        /* now we can build the certification path */
        Exception pathException = null;
        CollectionCertStoreParameters ccsParams = new CollectionCertStoreParameters(
                certificateManager.getCertificates());
        CertStore certStore = null;
        try {
            certStore = CertStore.getInstance("Collection", ccsParams);
            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            PKIXBuilderParameters builderParams = new PKIXBuilderParameters(trustAnchors,
                    certSelector);

            builderParams.addCertStore(certStore);
            builderParams.setRevocationEnabled(false);

            PKIXCertPathBuilderResult buildResult = (PKIXCertPathBuilderResult) builder
                    .build(builderParams);
            return new CertPathKeySelectorResult(buildResult);
        } catch (NoSuchAlgorithmException nsae) {
            pathException = nsae;
        } catch (InvalidAlgorithmParameterException iape) {
            pathException = iape;
        } catch (CertPathBuilderException cpbe) {
            pathException = cpbe;
        } catch (NullPointerException npe) {/* null keystore, pass through as if no path */
        }

        /* so full construction of the path failed, but we may still return a useful key */
        if (includedCert == null) {
            if (certStore != null) {
                try {
                    Collection<? extends Certificate> certs = certStore
                            .getCertificates(certSelector);
                    if (certs != null && certs.size() > 0) {
                        includedCert = certs.toArray(new X509Certificate[] {}).clone()[0];
                    }
                } catch (CertStoreException e) {
                }
            }
        }
        if (includedCert != null) {
            return new NoPathKeySelectorResult(includedCert, pathException);
        }

        /* nothing we can do now */
        return nullResult;
    }

    /*
     * Checks whether given X509 certificate is self-signed.
     */
    private static boolean isSelfSigned(X509Certificate cert) {
        try {
            /* Try to verify certificate signature with its own public key */
            PublicKey key = cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (CertificateException ce) {
            /* Encoding error --> signature is invalid */
        } catch (NoSuchAlgorithmException nsae) {
            /* Unsupported algorithm --> signature is unverifiable */
        } catch (NoSuchProviderException nspe) {
            /* We can't get here ... */
        } catch (SignatureException se) {
            /* Invalid signature --> not self-signed */
        } catch (InvalidKeyException ke) {
            /* Invalid key --> not self-signed */
        }
        return false;
    }

    /*
     * Returns an OID of a public-key algorithm compatible with the specified signature algorithm
     * URI.
     */
    private String getOIDforAlgorithm(String algURI) {
        if (algURI.equalsIgnoreCase(SignatureConstants.SIG_ALGORITHM_DSAwithSHA1)) {
            return "1.2.840.10040.4.1";
        } else if (algURI.equalsIgnoreCase(SignatureConstants.SIG_ALGORITHM_RSAwithSHA256)) {
            return "1.2.840.113549.1.1.1";
        } else if (algURI.equalsIgnoreCase(SignatureConstants.SIG_ALGORITHM_ECDSAwithSHA256)) {
            return "1.2.840.10045.4.1";
        } else {
            return null;
        }
    }

    private String[] getValidPolicies(PolicyNode tree) {
        ArrayList<String> policies = new ArrayList<String>();
        enumeratePolicyNode(tree, policies);
        int policyCount = policies.size();
        return policyCount == 0 ? null : policies.toArray(new String[policyCount]);
    }

    private void enumeratePolicyNode(PolicyNode tree, ArrayList<String> policies) {
        if (tree != null) {
            Iterator<? extends PolicyNode> children = tree.getChildren();
            if (children == null || !children.hasNext()) {
                policies.add(tree.getValidPolicy());
                return;
            }
            while (children.hasNext()) {
                PolicyNode child = children.next();
                enumeratePolicyNode(child, policies);
            }
        }
    }

    /*
     * KeySelectorResult class signifying no key available
     */
    KeySelectorResult nullResult = new KeySelectorResult() {
        @Override
        public Key getKey() {
            return null;
        }
    };

    /*
     * KeySelectorResult class wrapping a complete certificate path
     */
    class CertPathKeySelectorResult implements KeySelectorResult {
        private final PKIXCertPathBuilderResult pathResult;
        private final Key key;

        CertPathKeySelectorResult(PKIXCertPathBuilderResult pathResult) {
            this.pathResult = pathResult;
            this.key = pathResult.getPublicKey();
        }

        @Override
        public Key getKey() {
            return key;
        }

        public CertPath getCertPath() {
            return pathResult.getCertPath();
        }

        public X509Certificate getEntityCertificate() {
            List<? extends Certificate> pathCerts = getCertPath().getCertificates();
            if (pathCerts.size() == 0)
                return getTrustedCertificate();
            else
                return (X509Certificate) pathCerts.get(0);
        }

        public X509Certificate getTrustedCertificate() {
            return pathResult.getTrustAnchor().getTrustedCert();
        }

        public PKIXCertPathBuilderResult getPathResult() {
            return pathResult;
        }
    }

    /*
     * KeySelectorResult class wrapping a certificate for which a complete path could not be
     * constructed, but with relevant key/certificate data and error information
     */
    class NoPathKeySelectorResult implements KeySelectorResult {
        private final X509Certificate certificate;
        private final Exception pathException;
        private final Key key;

        NoPathKeySelectorResult(X509Certificate certificate, Exception pathException) {
            this.certificate = certificate;
            this.pathException = pathException;
            this.key = certificate.getPublicKey();
        }

        @Override
        public Key getKey() {
            return key;
        }

        public X509Certificate getEntityCertificate() {
            return certificate;
        }

        public Exception getPathException() {
            return pathException;
        }
    }

    private final CertificateManager certificateManager;
    private final AuthType authType;
    private final Set<TrustAnchor> trustAnchors;
    private KeySelectorResult selectorResult;
    private CertificateInfo rootCert, entityCert;
    private String[] policies;
    private CertificateInfo[] certPath;
}
