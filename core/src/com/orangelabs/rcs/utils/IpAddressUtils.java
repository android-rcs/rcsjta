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

package com.orangelabs.rcs.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP address utility functions
 *
 * @author B. JOGUET
 */
public class IpAddressUtils {

    /**
     * Extract host address (see RFC4007 and RFC2373)
     *
     * @param host Host address
     * @return Address
     */
    public static String extractHostAddress(String host) {
        if (host == null) {
            return null;
        }

        // Remove prefix from address
        int index = host.indexOf("/");
        if (index != -1) {
            host = host.substring(0, index);
        }

        // Remove zone id from address
        /*
         * RFC4007:
         * <address>%<zone_id> where: 
         * <address> is a literal IPv6 address, 
         * <zone_id> is a string identifying the zone of the address,
         * and `%' is a delimiter character to distinguish between <address> and <zone_id>.
         */
        index = host.indexOf("%");
        if (index != -1) {
            host = host.substring(0, index);
        }

        return host;
    }

    /**
     * is IPv6 Address
     *
     * @param ipAddress address
     * @return true if IPv6
     */
    public static boolean isIPv6(String ipAddress) {
        InetAddress addr;
        boolean isIPv6 = false;
        try {
            addr = InetAddress.getByName(ipAddress);
            isIPv6 = addr instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
        return isIPv6;
    }
}
