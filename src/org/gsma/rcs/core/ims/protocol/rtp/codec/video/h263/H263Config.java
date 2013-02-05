/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h263;

/**
 * Class H263Config.
 */
public class H263Config {
    /**
     * Constant CODEC_NAME.
     */
    public static final String CODEC_NAME = "H263-2000";

    /**
     * Constant CLOCK_RATE.
     */
    public static final int CLOCK_RATE = 90000;

    /**
     * Constant CODEC_PARAMS.
     */
    public static final String CODEC_PARAMS = "profile=0;level=45";

    /**
     * Constant VIDEO_WIDTH.
     */
    public static final int VIDEO_WIDTH = 176;

    /**
     * Constant VIDEO_HEIGHT.
     */
    public static final int VIDEO_HEIGHT = 144;

    /**
     * Constant FRAME_RATE.
     */
    public static final int FRAME_RATE = 15;

    /**
     * Constant BIT_RATE.
     */
    public static final int BIT_RATE = 128000;

    /**
     * Creates a new instance of H263Config.
     */
    public H263Config() {

    }

} // end H263Config
