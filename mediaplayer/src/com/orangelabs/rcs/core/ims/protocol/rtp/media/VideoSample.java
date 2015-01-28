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

package com.orangelabs.rcs.core.ims.protocol.rtp.media;

import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;

/**
 * Video sample
 *
 * @author hlxn7157
 */
public class VideoSample extends MediaSample {

    /**
     * Video Orientation
     */
    private VideoOrientation videoOrientation;

    /**
     * Constructor
     *
     * @param data Data
     * @param time Time stamp
     * @param videoOrientation Video orientation
     */
    public VideoSample(byte[] data, long time, VideoOrientation videoOrientation) {
        super(data, time);
        this.videoOrientation = videoOrientation;
    }

    /**
     * Constructor
     *
     * @param data Data
     * @param time Time stamp
     * @Param sequenceNumber Packet sequence number
     * @param videoOrientation Video orientation
     */
    public VideoSample(byte[] data, long time, long sequenceNumber, VideoOrientation videoOrientation) {
        super(data, time, sequenceNumber);
        this.videoOrientation = videoOrientation;
    }

    /**
     * Gets the video orientation
     *
     * @return VideoOrientation
     */
    public VideoOrientation getVideoOrientation() {
        return videoOrientation;
    }

    /**
     * Sets the video orientation
     *
     * @param videoOrientation VideoOrientation
     */
    public void setVideoOrientation(VideoOrientation orientation) {
        this.videoOrientation = orientation;
    }
}
