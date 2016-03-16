/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.telekom.bouncycastle.wrapper;

import local.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import local.org.bouncycastle.operator.ContentSigner;
import local.org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import local.org.bouncycastle.operator.OperatorCreationException;
import local.org.bouncycastle.operator.OperatorStreamException;
import local.org.bouncycastle.operator.RuntimeOperatorException;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Simplified class derived from org.bouncycastle.operator.jcajce.JcaContentSignerBuilder to avoid
 * implementing a complete security provider just for creating and signing certificates.
 */
public class SimpleContentSignerBuilder {

    private String mAlgorithm = "SHA1withRSA";
    private AlgorithmIdentifier sigAlgId;

    public SimpleContentSignerBuilder() {
        this.sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(mAlgorithm);
    }

    public ContentSigner build(PrivateKey privateKey) throws OperatorCreationException {
        try {
            final Signature sig = Signature.getInstance(mAlgorithm);

            sig.initSign(privateKey);

            return new ContentSigner() {
                private SignatureOutputStream stream = new SignatureOutputStream(sig);

                public AlgorithmIdentifier getAlgorithmIdentifier() {
                    return sigAlgId;
                }

                public OutputStream getOutputStream() {
                    return stream;
                }

                public byte[] getSignature() {
                    try {
                        return stream.getSignature();
                    } catch (SignatureException e) {
                        throw new RuntimeOperatorException("exception obtaining signature: "
                                + e.getMessage(), e);
                    }
                }
            };
        } catch (GeneralSecurityException e) {
            throw new OperatorCreationException("cannot create signer: " + e.getMessage(), e);
        }
    }

    private class SignatureOutputStream extends OutputStream {
        private Signature sig;

        SignatureOutputStream(Signature sig) {
            this.sig = sig;
        }

        public void write(byte[] bytes, int off, int len) throws IOException {
            try {
                sig.update(bytes, off, len);
            } catch (SignatureException e) {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(),
                        e);
            }
        }

        public void write(byte[] bytes) throws IOException {
            try {
                sig.update(bytes);
            } catch (SignatureException e) {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(),
                        e);
            }
        }

        public void write(int b) throws IOException {
            try {
                sig.update((byte) b);
            } catch (SignatureException e) {
                throw new OperatorStreamException("exception in content signer: " + e.getMessage(),
                        e);
            }
        }

        byte[] getSignature() throws SignatureException {
            return sig.sign();
        }
    }
}
