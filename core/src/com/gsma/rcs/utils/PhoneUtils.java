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

package com.gsma.rcs.utils;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phone utility functions
 * 
 * @author jexa7410
 */
public class PhoneUtils {
    /**
     * Tel-URI format
     */
    private static boolean TEL_URI_SUPPORTED = true;

    /**
     * Regular expression of the SIP header
     */
    private final static String REGEXP_EXTRACT_URI = "<(.*)>";

    /**
     * Pattern to extract Uri from SIP header
     */
    private final static Pattern PATTERN_EXTRACT_URI = Pattern.compile(REGEXP_EXTRACT_URI);

    /**
     * Header for TEL URI
     */
    public static final String TEL_URI_HEADER = "tel:";

    /**
     * header for SIP URI
     */
    public static final String SIP_URI_HEADER = "sip:";

    /**
     * Start delimiter for URI
     */
    public static final String URI_START_DELIMITER = "<";

    /**
     * End delimiter for URI
     */
    public static final String URI_END_DELIMITER = ">";

    /**
     * Set the country code
     * 
     * @param context Context
     * @param mRcsSettings
     */
    public static synchronized void initialize(Context context, RcsSettings mRcsSettings) {
        TEL_URI_SUPPORTED = mRcsSettings.isTelUriFormatUsed();
    }

    /**
     * Format ContactId to tel or sip Uri
     * 
     * @param contactId the contact identifier
     * @return the Uri
     */
    public static String formatContactIdToUri(ContactId contactId) {
        if (contactId == null) {
            throw new IllegalArgumentException("ContactId is null");
        }
        if (TEL_URI_SUPPORTED) {
            /* Tel-URI format */
            return TEL_URI_HEADER.concat(contactId.toString());

        }
        /* SIP-URI format */
        return new StringBuilder(SIP_URI_HEADER).append(contactId).append("@")
                .append(ImsModule.IMS_USER_PROFILE.getHomeDomain()).append(";user=phone")
                .toString();
    }

    /**
     * Extract user part phone number from a SIP-URI or Tel-URI or SIP address
     * 
     * @param uri SIP or Tel URI
     * @return Unformatted Number or null in case of error
     */
    /* package private */static String extractNumberFromUriWithoutFormatting(String uri) {
        if (uri == null) {
            return null;
        }
        /* Extract URI from address */
        int index0 = uri.indexOf(URI_START_DELIMITER);
        if (index0 != -1) {
            uri = uri.substring(index0 + URI_START_DELIMITER.length(),
                    uri.indexOf(URI_END_DELIMITER, index0));
        }

        /* Extract a Tel-URI */
        int index1 = uri.indexOf(TEL_URI_HEADER);
        if (index1 != -1) {
            uri = uri.substring(index1 + TEL_URI_HEADER.length());
        }

        /* Extract a SIP-URI */
        index1 = uri.indexOf(SIP_URI_HEADER);
        if (index1 != -1) {
            int index2 = uri.indexOf("@", index1);
            uri = uri.substring(index1 + SIP_URI_HEADER.length(), index2);
        }

        /* Remove URI parameters */
        int index2 = uri.indexOf(";");
        if (index2 != -1) {
            uri = uri.substring(0, index2);
        }

        /* Returns the extracted number (username part of the URI) */
        return uri;
    }

    /**
     * get URI from SIP identity header
     * 
     * @param header the SIP header
     * @return the Uri
     */
    public static String extractUriFromSipHeader(String header) {
        if (header != null) {
            Matcher matcher = PATTERN_EXTRACT_URI.matcher(header);
            if (matcher.find()) {
                return matcher.group(1);

            }
        }
        return header;
    }

}
