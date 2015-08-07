/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.core.ims.security;

import static com.gsma.rcs.utils.StringUtils.UTF8;

/**
 * HTTP Digest MD5 authentication (see RFC2617)
 * 
 * @author jexa7410
 */
public class HttpDigestMd5Authentication {

    private static final char COLON = ':';

    /**
     * Constant
     */
    public static final String NC_PARAM = "nc";

    /**
     * HTTP Digest scheme
     */
    public final static String HTTP_DIGEST_SCHEMA = "Digest";

    /**
     * HTTP Digest algorithm
     */
    public final static String HTTP_DIGEST_ALGO = "MD5";

    /**
     * Hex chars
     */
    private static final char[] HEX = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Domain name
     */
    private String mRealm;

    /**
     * Opaque parameter
     */
    private String mOpaque;

    /**
     * Nonce
     */
    private String mNonce;

    /**
     * Next nonce
     */
    private String mNextNonce;

    /**
     * Qop
     */
    private String mQop;

    /**
     * Cnonce
     */
    private String mCnonce = Long.toString(System.currentTimeMillis());

    /**
     * Cnonce counter
     */
    private int mCnonceCounter = 0;

    /**
     * MD5 algorithm
     */
    private MD5Digest mMd5Digest = new MD5Digest();

    /**
     * Constructor
     */
    public HttpDigestMd5Authentication() {
    }

    /**
     * Returns realm parameter
     * 
     * @return Realm
     */
    public String getRealm() {
        return mRealm;
    }

    /**
     * Set the realm parameter
     * 
     * @param realm Realm
     */
    public void setRealm(String realm) {
        mRealm = realm;
    }

    /**
     * Returns opaque parameter
     * 
     * @return Opaque
     */
    public String getOpaque() {
        return mOpaque;
    }

    /**
     * Set the opaque parameter
     * 
     * @param opaque Opaque
     */
    public void setOpaque(String opaque) {
        mOpaque = opaque;
    }

    /**
     * Get the client nonce parameter
     * 
     * @return Client nonce
     */
    public String getCnonce() {
        return mCnonce;
    }

    /**
     * Get the nonce parameter
     * 
     * @return Nonce
     */
    public String getNonce() {
        return mNonce;
    }

    /**
     * Set the nonce parameter
     * 
     * @param nonce Nonce
     */
    public void setNonce(String nonce) {
        mNonce = nonce;
    }

    /**
     * Returns the next nonce parameter
     * 
     * @return Next nonce
     */
    public String getNextnonce() {
        return mNextNonce;
    }

    /**
     * Set the next nonce parameter
     * 
     * @param nextnonce Next nonce
     */
    public void setNextnonce(String nextnonce) {
        mNextNonce = nextnonce;
    }

    /**
     * Returns the Qop parameter
     * 
     * @return Qop
     */
    public String getQop() {
        return mQop;
    }

    /**
     * Set the Qop parameter
     * 
     * @param qop Qop parameter
     */
    public void setQop(String qop) {
        if (qop != null) {
            qop = qop.split(",")[0];
        }
        mQop = qop;
    }

    /**
     * Update the nonce parameters
     */
    public void updateNonceParameters() {
        // Update nonce and nc
        if (mNextNonce.equals(mNonce)) {
            // Next nonce == nonce
            mCnonceCounter++;
        } else {
            // Next nonce != nonce
            mCnonceCounter = 1;
            mNonce = mNextNonce;
        }
    }

    /**
     * Build the cnonce counter
     * 
     * @return String (ie. "00000001")
     */
    public String buildNonceCounter() {
        String result = Integer.toHexString(mCnonceCounter);
        while (result.length() != 8) {
            result = "0" + result;
        }
        return result;
    }

    /**
     * Convert to hexa string
     * 
     * @param value Value to convert
     * @return String
     */
    private String toHexString(byte[] value) {
        int pos = 0;
        char[] c = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            c[pos++] = HEX[value[i] >> 4 & 0xf];
            c[pos++] = HEX[value[i] & 0xf];
        }
        return new String(c);
    }

    /**
     * Calculate HTTP Digest nonce response
     * 
     * @param user User
     * @param password Password
     * @param method Method
     * @param uri Request URI
     * @param nc Nonce counter
     * @param body Entity body
     * @return the HTTP Digest nonce response
     */
    public String calculateResponse(String user, String password, String method, String uri,
            String nc, String body) {
        String a1 = new StringBuilder(user).append(COLON).append(mRealm).append(COLON)
                .append(password).toString();
        StringBuilder a2 = new StringBuilder(method).append(COLON).append(uri);

        if (mQop != null) {
            if (!mQop.startsWith("auth")) {
                throw new IllegalArgumentException("Invalid qop: ".concat(mQop));
            }
            
            if (mQop.equals("auth-int")) {
                a2.append(COLON).append(H(body));
            }

            return H(new StringBuilder(H(a1)).append(COLON).append(mNonce).append(COLON).append(nc)
                    .append(COLON).append(mCnonce).append(COLON).append(mQop).append(COLON)
                    .append(H(a2.toString())).toString());
        } else {
            return H(new StringBuilder(H(a1)).append(COLON).append(mNonce).append(COLON)
                    .append(H(a2.toString())).toString());
        }
    }

    /**
     * HTTP Digest algo
     * 
     * @param data Input data
     * @return Hash key
     */
    private String H(String data) {
        if (data == null) {
            data = "";
        }
        byte[] bytes = data.getBytes(UTF8);
        mMd5Digest.update(bytes, 0, bytes.length);
        byte returnValue[] = new byte[mMd5Digest.getDigestSize()];
        mMd5Digest.doFinal(returnValue, 0);
        return toHexString(returnValue);
    }
}
