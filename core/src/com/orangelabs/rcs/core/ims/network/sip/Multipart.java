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

package com.orangelabs.rcs.core.ims.network.sip;

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

    /**
     * Parts
     */
    private Hashtable<String, String> parts = new Hashtable<String, String>();

    /**
     * Constructor
     * 
     * @param content Content parts
     * @param boundary Boundary delimiter
     */
    public Multipart(String content, String boundary) {
        if ((content != null) && (boundary != null)) {
            String[] fragments = content.split(BOUNDARY_DELIMITER + boundary);
            for (String fragment : fragments) {
                String trimmedFragment = fragment.trim();
                if ((trimmedFragment.length() > 0) && !BOUNDARY_DELIMITER.equals(trimmedFragment)) {
                    int begin = fragment.indexOf(SipUtils.CRLF + SipUtils.CRLF);
                    if (begin != -1) {
                        try {
                            // Extract content type
                            String type = fragment.substring(0, begin).trim();

                            // Extract content part
                            String part = fragment.substring(begin).trim();

                            // Extract MIME type from content type
                            int beginType = type.indexOf(ContentTypeHeader.NAME);
                            int endType = type.indexOf(SipUtils.CRLF, beginType);
                            String mime;
                            if (endType == -1) {
                                mime = type.substring(
                                        beginType + ContentTypeHeader.NAME.length() + 1).trim();
                            } else {
                                mime = type.substring(
                                        beginType + ContentTypeHeader.NAME.length() + 1, endType)
                                        .trim();
                            }

                            // Add part in lowercase
                            parts.put(mime.toLowerCase(), part);
                        } catch (Exception e) {
                            // Nothing to do
                        }
                    }
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

    /**
     * Get parts
     * 
     * @return List of parts
     */
    public Hashtable<String, String> getParts() {
        return parts;
    }
}
