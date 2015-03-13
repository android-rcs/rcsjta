/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ImapUtil {

    protected static final String HEADER_CONTENT_TYPE = "Content-Type";

    protected static final String CRLF = "\r\n";

    protected static final String CRLFCRLF = "\r\n\r\n";

    private static final int ANDROID_UTIL = 0; // android
    private static final int APACHE_COMMON_UTIL = 1; // apache common in lib

    private static int sBase64Impl = -1;

    private static Method sMethodEncode = null;

    private static Method sMethodDecode = null;

    static {
        Class<?> base64Class = null;
        try {
            base64Class = Class.forName("android.util.Base64");
            sBase64Impl = ANDROID_UTIL;
            // encode(byte[] input, int flags)
            sMethodEncode = base64Class.getMethod("encode", new Class[] {
                    byte[].class, int.class
            });
            sMethodDecode = base64Class.getMethod("decode", new Class[] {
                    byte[].class, int.class
            });
        } catch (Exception e) {
        }
        try {
            if (base64Class == null) {
                base64Class = Class.forName("org.apache.commons.codec.binary.Base64");
                // encodeBase64(source, true)
                sMethodEncode = base64Class.getMethod("encodeBase64", new Class[] {
                        byte[].class, boolean.class
                });
                sMethodDecode = base64Class.getMethod("decodeBase64", new Class[] {
                    byte[].class
                });
                sBase64Impl = APACHE_COMMON_UTIL;
            }
        } catch (Exception e) {
        }

    }

    protected static String encodeBase64(byte[] source) {
        try {
            if (sBase64Impl == APACHE_COMMON_UTIL) {
                byte[] data = (byte[]) sMethodEncode.invoke(null, source, true);
                return new String(data);
            } else if (sBase64Impl == ANDROID_UTIL) {
                byte[] data = (byte[]) sMethodEncode.invoke(null, source, 4); // 4 = CRLF instead of
                                                                              // LF !!
                return new String(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("BASE64 encoding not defined");
    }

    public static byte[] decodeBase64(byte[] source) {
        try {
            if (sBase64Impl == APACHE_COMMON_UTIL) {
                byte[] data = (byte[]) sMethodDecode.invoke(null, source);
                return data;
            } else if (sBase64Impl == ANDROID_UTIL) {
                byte[] data = (byte[]) sMethodDecode.invoke(null, source, 4);
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("BASE64 encoding not defined");
    }

    protected static String getFlagsAsString(Flag... flags) {
        return getFlagsAsString(Arrays.asList(flags));
    }

    protected static String getFlagsAsString(List<Flag> flags) {
        if (flags == null || flags.size() == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        boolean b = false;
        for (Flag f : flags) {
            if (b) {
                sb.append(' ');
            }
            sb.append('\\');
            sb.append(f);
            b = true;
        }
        sb.append(')');
        return sb.toString();
    }

}
