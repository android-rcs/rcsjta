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

package org.gsma.rcs.core.ims.protocol.rtp.format.audio;

/**
 * Class PcmuAudioFormat.
 */
public class PcmuAudioFormat extends AudioFormat {
    /**
     * Constant ENCODING.
     */
    public static final String ENCODING = "pcmu";

    /**
     * Constant PAYLOAD.
     */
    public static final int PAYLOAD = 0;

    /**
     * Creates a new instance of PcmuAudioFormat.
     */
    public PcmuAudioFormat() {
        super((java.lang.String) null, 0);
    }

} // end PcmuAudioFormat
