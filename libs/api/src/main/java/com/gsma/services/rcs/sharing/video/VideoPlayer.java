/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.services.rcs.sharing.video;

/**
 * Video player offers an interface to manage the video player instance independently of the RCS
 * service. The video player is implemented in the application side.<br>
 * In the originating side, the video player captures the video from the device camera, encodes the
 * video into the selected format and streams the encoded video frames over the network in RTP.<br>
 * In the terminating side, the video renderer is implemented in the application side. The video
 * renderer receives the video streaming over the network in RTP, decodes the video frames and
 * displays the decoded picture on the device screen.
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoPlayer {
    /**
     * Constructor
     */
    public VideoPlayer() {
    }

    /**
     * Set the remote info
     * 
     * @param codec Video codec
     * @param remoteHost Remote RTP host
     * @param remotePort Remote RTP port
     * @param orientationHeaderId Orientation header extension ID. The extension ID is a value
     *            between 1 and 15 arbitrarily chosen by the sender, as defined in RFC5285
     */
    public abstract void setRemoteInfo(VideoCodec codec, String remoteHost, int remotePort,
            int orientationHeaderId);

    /**
     * Returns the local RTP port used to stream video
     * 
     * @return Port number
     */
    public abstract int getLocalRtpPort();

    /**
     * Returns the list of codecs supported by the player
     * 
     * @return List of codecs
     */
    public abstract VideoCodec[] getSupportedCodecs();

    /**
     * Returns the current codec
     * 
     * @return Codec
     */
    public abstract VideoCodec getCodec();
}
