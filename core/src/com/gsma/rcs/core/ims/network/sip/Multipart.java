/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.network.sip;

import java.util.Hashtable;

import javax2.sip.header.ContentTypeHeader;

/**
 * Multipart content for SIP message
 * 
 * @author jexa7410
 */
public class Multipart {
    /**
     * Boundary delimiter
     */
    public final static String BOUNDARY_DELIMITER = "--";

    private static final String DOUBLE_CRLF = "\r\n\r\n";

    /**
     * Parts
     */
    private Hashtable<String, String> parts = new Hashtable<>();

    /**
     * Constructor
     * 
     * @param content Content parts
     * @param boundary Boundary delimiter
     */
    public Multipart(String content, String boundary) {
        String[] fragments = content.split(BOUNDARY_DELIMITER + boundary);
        for (String fragment : fragments) {
            if (fragment.length() > 0 && !BOUNDARY_DELIMITER.equals(fragment)) {
                int begin = fragment.indexOf(DOUBLE_CRLF);
                if (begin != -1) {
                    begin += DOUBLE_CRLF.length();
                    /* Extract content type */
                    String type = fragment.substring(0, begin);
                    /* Extract MIME type from content type */
                    int beginType = type.indexOf(ContentTypeHeader.NAME);
                    int endType = type.indexOf(SipUtils.CRLF, beginType);
                    String mime;
                    if (endType == -1) {
                        mime = type.substring(beginType + ContentTypeHeader.NAME.length() + 1)
                                .trim();
                    } else {
                        mime = type.substring(beginType + ContentTypeHeader.NAME.length() + 1,
                                endType).trim();
                    }
                    /* Extract content part */
                    String part = fragment.substring(begin);
                    int endPart = part.lastIndexOf(SipUtils.CRLF);
                    if (endPart != -1) {
                        part = part.substring(0, endPart);
                    }
                    parts.put(mime.toLowerCase(), part);
                }
            }
        }
    }

    /**
     * Is a multipart
     * 
     * @return Boolean
     */
    public boolean isMultipart() {
        return (parts.size() > 0);
    }

    /**
     * Get part from its MIME-type
     * 
     * @param type MIME-type
     * @return Part as string
     */
    public String getPart(String type) {
        return parts.get(type.toLowerCase());
    }

}
