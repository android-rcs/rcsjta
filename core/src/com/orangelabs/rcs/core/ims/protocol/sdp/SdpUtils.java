/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.protocol.sdp;

import com.orangelabs.rcs.utils.IpAddressUtils;

/**
 * SDP utility functions
 * 
 * @author jexa7410
 */
public class SdpUtils {
	/**
	 * Extract the remote host address from the connection info
	 * 
	 * @param connectionInfo Connection info
	 * @return Address
	 */
	public static String extractRemoteHost(String connectionInfo) {
		// c=IN IP4 172.20.138.145
		String[] tokens = connectionInfo.split(" ");
		if (tokens.length > 2) {
			return tokens[2];
		} else {
			return null;
		}
	}

    /**
     * Format "IN IP" attribute (4 or 6)
     *
     * @param address IP address
     * @return "IN IP4 address" or "IN IP6 address"
     */
    public static String formatAddressType(String address) {
        if (IpAddressUtils.isIPv6(address)) {
            return "IN IP6 " + address;
        } else {
            return "IN IP4 " + address;
        }
    }
}
