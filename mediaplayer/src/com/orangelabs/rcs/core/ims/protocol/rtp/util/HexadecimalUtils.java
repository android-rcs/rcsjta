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

package com.orangelabs.rcs.core.ims.protocol.rtp.util;

/**
 * Hexadecimal utils
 *
 * @author Deutsche Telekom AG
 */
public class HexadecimalUtils {

    /**
     * Decode hex string to a byte array
     *
     * @param s hexadecimal encoded string
     * @return array of bytes
     */
    public static byte[] hexStringToByteArray(String s) {

        if (s == null || s.length() == 0) {
            return null;
        }

        int len = s.length();

        // '111' is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException();
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Convert byte array into Hexadecimal string
     *
     * @param bytes
     * @return {@link String} if valid byte array, otherwise <code>null</code>
     */
    public static String byteArrayToHexString(byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            return null;
        }

        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        char[] hexChars = new char[bytes.length * 2];
        int value;
        for ( int j = 0; j < bytes.length; j++) {
            value = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[value >>> 4];
            hexChars[j * 2 + 1] = hexArray[value & 0x0F];
        }
        return new String(hexChars);
    }
}
