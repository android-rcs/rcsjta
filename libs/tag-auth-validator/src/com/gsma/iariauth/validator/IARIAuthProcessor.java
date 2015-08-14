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

package com.gsma.iariauth.validator;

import com.gsma.iariauth.validator.dsig.CertificateInfo;
import com.gsma.iariauth.validator.dsig.SignatureValidator;
import com.gsma.iariauth.validator.util.Base64;

import org.w3c.dom.Element;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that processes an IARI Authorization document
 */
public class IARIAuthProcessor implements ProcessingResult {

    /*******************************
     * Public API
     *******************************/

    /**
     * Constructs an IARIAuth processor
     */
    public IARIAuthProcessor() {
    }

    /**
     * Process an IARI Authorization document presented as an InputStream. It is the caller's
     * responsibility to close the stream after this method has returned.
     * 
     * @param is
     * @return status, indicating whether or not processing was successful. See ProcessingResult for
     *         result values.
     */
    public int process(InputStream is) {
        /* read document */
        authDocument = new IARIAuthDocument();
        error = authDocument.read(is);
        if (error != null) {
            return (status = error.status);
        }

        /* get document refs */
        Set<String> expectedRefs = new HashSet<String>();
        expectedRefs.add(authDocument.getIariNode().getAttribute(Constants.ID));
        expectedRefs.add(authDocument.getPackageSignerNode().getAttribute(Constants.ID));
        Element packageNameNode = authDocument.getPackageNameNode();
        if (packageNameNode != null)
            expectedRefs.add(packageNameNode.getAttribute(Constants.ID));

        /* decode and validate the signature */
        SignatureValidator signatureValidator = new SignatureValidator(authDocument.getDocument(),
                authDocument.getSignatureNode(), expectedRefs, authDocument.authType);
        int sigStatus = signatureValidator.validate();
        if ((sigStatus != ProcessingResult.STATUS_VALID_NO_ANCHOR)
                && (sigStatus != ProcessingResult.STATUS_VALID_HAS_ANCHOR)) {
            error = signatureValidator.getError();
            return (status = error.status);
        }
        authDocument.signature = signatureValidator.getSignatureInfo();
        int checkStatus = checkSignatureProperties(sigStatus);
        if (checkStatus != ProcessingResult.STATUS_OK) {
            return (status = error.status);
        }

        return (status = ProcessingResult.STATUS_OK);
    }

    /*******************************
     * ProcessingResult API
     *******************************/

    /**
     * Get the status value after processing has completed.
     */
    @Override
    public int getStatus() {
        return status;
    }

    /**
     * Get detailed error information if processing resulted in an error.
     */
    @Override
    public Artifact getError() {
        return error;
    }

    /**
     * Get the IARI Authorization document following processing. If the initial processing of the
     * document is unsuccessful, (eg if the document is not well-formed) then this method will
     * return null. Returning a non-null value does not by itself signify that processing was
     * successful or that the document is valid; use the status value (or the result from process())
     * to determine whether or not processing was successful.
     */
    @Override
    public IARIAuthDocument getAuthDocument() {
        return authDocument;
    }

    /*******************************
     * Internal
     *******************************/

    private int checkSignatureProperties(int sigStatus) {
        /* check that the IARI is in the end-entity certificate */
        CertificateInfo entityCert = authDocument.signature.getEntityCertificate();
        String[] entityURIIdentities = entityCert.getURIIdentities();
        if (!contains(entityURIIdentities, authDocument.iari)) {
            error = new Artifact("IARI Authorization signature does not match requested IARI");
            return error.status;
        }
        /* check that the IARI matches the entity cert public key */
        if (!authDocument.iari.startsWith(Constants.SELF_SIGNED_IARI_PREFIX)) {
            error = new Artifact("Requested IARI does not belong to self-signed ext range");
            return error.status;
        }
        byte[] entityPublicKey = entityCert.getX509Certificate().getPublicKey().getEncoded();
        String keyHash = hash(entityPublicKey);
        if (!authDocument.iari.substring(Constants.SELF_SIGNED_IARI_PREFIX.length())
                .equals(keyHash)) {
            error = new Artifact("Requested IARI key-specific part does not match signing key");
            return error.status;
        }
        return ProcessingResult.STATUS_OK;
    }

    private static boolean contains(String[] arr, String item) {
        if (arr != null && item != null)
            for (String member : arr)
                if (item.equals(member))
                    return true;
        return false;
    }

    private static String hash(byte[] key) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-224", sha224Provider);
            byte[] hash = sha.digest(key);
            String b64 = new String(Base64.encode(hash));
            /* make URL-safe, remove trailing = */
            return b64.replace('+', '-').replace('/', '_').replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    static {
        /* find a provider that can do SHA-224 */
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-224");
        } catch (NoSuchAlgorithmException e) {
            try {
                Provider provider = (Provider) (Class
                        .forName("com.gsma.iariauth.validator.crypto.android.SHA224").newInstance());
                Security.addProvider(provider);
                sha = MessageDigest.getInstance("SHA-224", provider);
            } catch (InstantiationException e1) {
            } catch (IllegalAccessException e1) {
            } catch (ClassNotFoundException e1) {
            } catch (NoSuchAlgorithmException e1) {
            }
        }
        sha224Provider = sha.getProvider();
    }

    private static Provider sha224Provider;
    private IARIAuthDocument authDocument;
    private int status = ProcessingResult.STATUS_NOT_PROCESSED;
    private Artifact error;
}
