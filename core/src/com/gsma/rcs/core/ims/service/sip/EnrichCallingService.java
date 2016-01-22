package com.gsma.rcs.core.ims.service.sip;

import com.gsma.rcs.core.ims.network.sip.FeatureTags;

/**
 * Enrich calling service
 *
 * @author jmauffret
 */
public class EnrichCallingService {
    public final static String CALL_COMPOSER_SERVICE_ID = "gsma.callcomposer";

    public final static String SHARED_MAP_SERVICE_ID = "gsma.sharedmap";

    public final static String SHARED_SKETCH_SERVICE_ID = "gsma.sharedsketch";

    public final static String POST_CALL_SERVICE_ID = "gsma.callunanswered";

    public final static String CALL_COMPOSER_FEATURE_TAG = FeatureTags.FEATURE_RCSE + "=\""
            + FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + CALL_COMPOSER_SERVICE_ID + "\"";
}
