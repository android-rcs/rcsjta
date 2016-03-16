/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2016 Orange.
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

package com.gsma.rcs.core.ims.service.sip;

import com.gsma.rcs.core.ims.network.sip.FeatureTags;

/**
 * Enrich calling service implemented on top of the MM session service
 *
 * @author jmauffret
 */
public class EnrichCallingService {
    /**
     * Call composer service ID
     */
    public final static String CALL_COMPOSER_SERVICE_ID = "gsma.callcomposer";

    /**
     * Shared map service ID
     */
    public final static String SHARED_MAP_SERVICE_ID = "gsma.sharedmap";

    /**
     * Shared sketch service ID
     */
    public final static String SHARED_SKETCH_SERVICE_ID = "gsma.sharedsketch";

    /**
     * Post call service ID
     */
    public final static String POST_CALL_SERVICE_ID = "gsma.callunanswered";

    /**
     * Call composer feature tag
     */
    public final static String CALL_COMPOSER_FEATURE_TAG = FeatureTags.FEATURE_RCSE + "=\""
            + FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + CALL_COMPOSER_SERVICE_ID + "\"";
}
