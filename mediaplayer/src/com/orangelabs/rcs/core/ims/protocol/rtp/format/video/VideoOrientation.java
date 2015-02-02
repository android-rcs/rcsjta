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

package com.orangelabs.rcs.core.ims.protocol.rtp.format.video;

import com.orangelabs.rcs.core.ims.protocol.rtp.RtpUtils;

/**
 * RCS Video orientation
 *
 * @author Deutsche Telekom
 */
public class VideoOrientation {

    /**
     * Header Id
     */
    private int headerId = RtpUtils.RTP_DEFAULT_EXTENSION_ID;

    /**
     * Camera
     */
    private CameraOptions camera;

    /**
     * Camera orientation
     */
    private Orientation orientation;

    /**
     * Constructor
     *
     * @param headerId Orientation header id
     * @param camera Camera
     * @param orientation Orientation
     */
    public VideoOrientation(int headerId, CameraOptions camera, Orientation orientation) {
        this.headerId = headerId;
        this.camera = camera;
        this.orientation = orientation;
    }

    /**
     * Constructor
     *
     * @param camera Camera
     * @param orientation Orientation
     */
    public VideoOrientation(CameraOptions camera, Orientation orientation) {
        this.camera = camera;
        this.orientation = orientation;
    }

    /**
     * Gets the VideoOrientation camera
     *
     * @return Camera
     */
    public CameraOptions getCamera() {
        return this.camera;
    }

    /**
     * Gets the VideoOrientation orientation
     *
     * @return Orientation
     */
    public Orientation getOrientation() {
        return this.orientation;
    }

    /**
     * Converts the video orientation into the byte used to transmit int RTP
     * header
     *
     * @return Byte representing the video orientation
     */
    public byte getVideoOrientation() {
        return (byte) ((camera.getValue() << 3) | orientation.getValue());
    }

    /**
     * Gets the negotiated header ID.
     *
     * @return Header id
     */
    public int getHeaderId() {
        return headerId;
    }

    /**
     * Parses the byte into a VideoOrientation object
     *
     * @param videoOrientation Byte representing the video Orientation
     * @return VideoOrientation object
     */
    public static VideoOrientation parse(byte videoOrientation) {
        return new VideoOrientation(CameraOptions.convert((videoOrientation & 0x08) >>> 3),
                Orientation.convert(videoOrientation & 0x07));
    }
}
