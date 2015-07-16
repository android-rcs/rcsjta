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

package com.gsma.rcs.core.ims.service.extension;

import com.gsma.iariauth.validator.Constants;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.provider.security.AuthorizationData.Type;

/**
 * IARI utility
 * 
 * @author LEMORDANT Philippe
 */
public class IariUtil {

    public static final String IARI_DOC_NAME_FOR_APP_ID = "iari_authorization.xml";

    public static final String IARI_DOC_NAME_FOR_EXT_PREFIX = "iari_authorization.";

    public static final String IARI_DOC_NAME_FOR_EXT_POSTFIX = ".xml";

    /**
     * Common prefix
     */
    public static final String COMMON_PREFIX = "urn:urn-7:3gpp-application.ims.iari.rcs.";

    public static final String EXTENSION_ID_PREFIX = "ext.ss.";

    /**
     * Is a valid IARI
     * 
     * @param iari
     * @return True if IARI is valid
     */
    public static boolean isValidIARI(String iari) {
        return iari.startsWith(Constants.SELF_SIGNED_IARI_PREFIX);
    }

    /**
     * Return the extension ID from IARI
     * 
     * @param iari
     * @return extension ID
     */
    public static String getExtensionId(String iari) {
        if (iari.startsWith(COMMON_PREFIX)) {
            return iari.substring(COMMON_PREFIX.length());
        } else if (iari.startsWith(FeatureTags.FEATURE_RCSE_EXTENSION)) {
            return iari.substring(FeatureTags.FEATURE_RCSE_EXTENSION.length() + 1);
        }
        return null;
    }

    /**
     * Gets the authorization type
     * 
     * @param filename the IARI filename
     * @return the authorization type
     */
    public static Type getType(String filename) {
        if (IARI_DOC_NAME_FOR_APP_ID.equals(filename)) {
            return Type.APPLICATION_ID;
        }
        return Type.SERVICE_ID;
    }
}
