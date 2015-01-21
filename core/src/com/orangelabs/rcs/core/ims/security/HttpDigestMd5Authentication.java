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

package com.orangelabs.rcs.core.ims.security;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * HTTP Digest MD5 authentication (see RFC2617)
 *
 * @author jexa7410
 */
public class HttpDigestMd5Authentication {
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
	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/**
	 * Domain name
	 */
	private String realm = null;

	/**
	 * Opaque parameter
	 */
	private String opaque = null;

	/**
	 * Nonce
	 */
	private String nonce = null;

	/**
	 * Next nonce
	 */
	private String nextnonce = null;

	/**
	 * Qop
	 */
	private String qop = null;

	/**
	 * Cnonce
	 */
	private String cnonce = "" + System.currentTimeMillis();

	/**
	 * Cnonce counter
	 */
	private int nc = 0;

	/**
	 * MD5 algorithm
	 */
	private MD5Digest md5Digest = new MD5Digest();

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

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
		return realm;
	}

	/**
	 * Set the realm parameter
	 *
	 * @param realm Realm
	 */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/**
	 * Returns opaque parameter
	 *
	 * @return Opaque
	 */
	public String getOpaque() {
		return opaque;
	}

	/**
	 * Set the opaque parameter
	 *
	 * @param opaque Opaque
	 */
	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}

	/**
	 * Get the client nonce parameter
	 *
	 * @return Client nonce
	 */
	public String getCnonce() {
		return cnonce;
	}

	/**
	 * Get the nonce parameter
	 *
	 * @return Nonce
	 */
	public String getNonce() {
		return nonce;
	}

	/**
	 * Set the nonce parameter
	 *
	 * @param nonce Nonce
	 */
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	/**
	 * Returns the next nonce parameter
	 *
	 * @return Next nonce
	 */
	public String getNextnonce() {
		return nextnonce;
	}

	/**
	 * Set the next nonce parameter
	 *
	 * @param nextnonce Next nonce
	 */
	public void setNextnonce(String nextnonce) {
		this.nextnonce = nextnonce;
	}

	/**
	 * Returns the Qop parameter
	 *
	 * @return Qop
	 */
	public String getQop() {
		return qop;
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
		this.qop = qop;
	}

	/**
	 * Update the nonce parameters
	 */
	public void updateNonceParameters() {
		// Update nonce and nc
		if (nextnonce.equals(nonce)) {
	   		// Next nonce == nonce
			nc++;
		} else {
	   		// Next nonce != nonce
			nc = 1;
			nonce = nextnonce;
	   	}
	}

	/**
	 * Build the cnonce counter
	 *
	 * @return String (ie. "00000001")
	 */
	public String buildNonceCounter() {
		String result = Integer.toHexString(nc);
		while(result.length() != 8) {
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
	 * @throws Exception
	 */
	public String calculateResponse(String user, String password, String method, String uri, String nc, String body) throws Exception {
		if (user == null || realm == null || uri == null || nonce == null) {
			throw new Exception("Invalid Authorization header" +
					user + "/" + realm + "/" + uri + "/" + nonce);
		}

		String a1 = user + ":" + realm + ":" + password;
		String a2 = method + ":" + uri;

		if (qop != null) {
			if (!qop.startsWith("auth")) {
				throw new Exception("Invalid qop: " + qop);
			}

			if (nc == null || cnonce == null) {
				throw new Exception("Invalid Authorization header: " + nc + "/" + cnonce);
			}

			if (qop.equals("auth-int")) {
				a2 = a2 + ":" + H(body);
			}

			return H(H(a1) + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + H(a2));
		} else {
			return H(H(a1) + ":" + nonce + ":" + H(a2));
		}
	}

	/**
	 * HTTP Digest algo
	 *
	 *  @param data Input data
	 *  @return Hash key
	 */
	private String H(String data) {
		try {
			if (data == null) {
				data = "";
			}
			byte[] bytes = data.getBytes(UTF8);
			md5Digest.update(bytes, 0, bytes.length);
			byte returnValue[] = new byte[md5Digest.getDigestSize()];
			md5Digest.doFinal(returnValue, 0);
			return toHexString(returnValue);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("HTTP digest MD5 algo has failed", e);
			}
			return null;
		}
	}
}
