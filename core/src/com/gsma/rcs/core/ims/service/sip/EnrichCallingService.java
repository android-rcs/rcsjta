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
