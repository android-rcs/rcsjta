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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Represents the SDP orientation extension
 *
 * @author Deutsche Telekom
 */
public class SdpOrientationExtension {

    /**
     * GSMA VideoOrientation extension URI
     */
    public static final String GSMA_ORIENTATION_URI = "urn:3gpp:video-orientation";

    /**
     * Minimum ID value for RTP Extension header. RFC5285
     */
    private static final int MIN_ID_VALUE = 1;

    /**
     * Maximum ID value for RTP Extension header. RFC5285
     */
    private static final int MAX_ID_VALUE = 14;

    /**
     * Extension header ID
     */
    private int extensionId;

    /**
     * ExtensionHeader URI
     */
    private String uri;

    /**
     * Logger
     */
    private static Logger logger = Logger.getLogger(SdpOrientationExtension.class.getName());

    /**
     * Constructor
     *
     * @param extensionId Extension header ID
     * @param uri ExtensionHeader URI
     */
    private SdpOrientationExtension(int extensionId, String uri) {
        this.extensionId = extensionId;
        this.uri = uri;
    }

    /**
     * Gets the extension Header id.
     *
     * @return Extension header id
     */
    public int getExtensionId() {
        return this.extensionId;
    }

    /**
     * Verifies if is a valid SDP Orientation header
     *
     * @return <code>True</code> if is a valid header, <code>false</code>
     *         otherwise.
     */
    public boolean isValid() {
        return (extensionId >= MIN_ID_VALUE && extensionId <= MAX_ID_VALUE)
                && GSMA_ORIENTATION_URI.equalsIgnoreCase(uri);
    }

    /**
     * Creates a {@link SdpOrientationExtension} from the "extmap" media
     * attribute
     *
     * @param mediaAttribute Extmap media attribute
     * @return A SdpOrientationExtension.
     * @throws RuntimeException if attribute is invalid.
     */
    public static SdpOrientationExtension create(MediaAttribute mediaAttribute) {
        if (mediaAttribute == null || mediaAttribute.getValue() == null) {
            throw new RuntimeException("Invalid media attribute");
        }

        try {
            String[] values = mediaAttribute.getValue().split(" ");
            return new SdpOrientationExtension(Integer.parseInt(values[0].trim()), values[1].trim());
        } catch (Exception ex) {
            throw new RuntimeException("Invalid media attribute value", ex);
        }
    }

    /**
     * Creates a {@link SdpOrientationExtension} from the Video MediaDescription
     * 
     * @param videoMediaDescriptionVideo media description
     * @return A new SdpOrientationExtension or null if invalid media description
     * @throws RuntimeException if videoMediaDescription is invalid.
     */
    public static SdpOrientationExtension create(MediaDescription videoMediaDescription) {
        if (videoMediaDescription == null) {
            throw new RuntimeException("Invalid videoMediaDescription");
        }

        MediaAttribute attribute = videoMediaDescription
                .getMediaAttribute(VideoSdpBuilder.ATTRIBUTE_EXTENSION);
        if (attribute != null) {
            try {
                return SdpOrientationExtension.create(attribute);
            } catch (Exception ex) {
                logger.error("Invalid SDP video orientation extension", ex);
            }
        }

        return null;
    }
}
