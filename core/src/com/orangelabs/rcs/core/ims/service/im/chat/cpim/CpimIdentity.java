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

package com.orangelabs.rcs.core.ims.service.im.chat.cpim;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable class to decode the content of the CPIM identity header according to RFC3862
 * 
 * @author YPLO6403
 */
public class CpimIdentity {
    /**
     * Regular expression of the CPIM 'From' header
     * <p>
     * Extract of RFC3862 <b>Common Presence and Instant Messaging (CPIM): Message Format</b> <br>
     * From-header = "From" ": " [ Formal-name ] "<" URI ">" ; "From" is case-sensitive
     */
    private final static String REGEXP_FROM_DISPLAY_NAME = "^\\s*?\"?([^\"<]*)\"?\\s*?<([^>]*)>$";

    /**
     * Pattern to extract display name and Uri from CPIM 'From' header
     */
    private final static Pattern PATTERN_FROM_DISPLAY_NAME = Pattern
            .compile(REGEXP_FROM_DISPLAY_NAME);

    /**
     * The optional display name (may be null)
     */
    private final String displayName;

    /**
     * The URI
     */
    private final String uri;

    /**
     * Constructor
     * 
     * @param identity the CPIM identity
     */
    public CpimIdentity(final String identity) {
        if (identity == null)
            throw new IllegalArgumentException("Null argument");
        Matcher matcher = PATTERN_FROM_DISPLAY_NAME.matcher(identity);
        if (matcher.find()) {
            if (matcher.groupCount() == 2) {
                String result = matcher.group(1).trim();
                displayName = (result.length() == 0) ? null : result;
                uri = matcher.group(2);
                return;
            }
        }
        throw new IllegalArgumentException("Invalid argument: " + identity);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "CpimIdentity [displayName=" + displayName + ", uri=" + uri + "]";
    }

}
