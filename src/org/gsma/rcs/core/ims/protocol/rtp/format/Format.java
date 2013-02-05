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

package org.gsma.rcs.core.ims.protocol.rtp.format;

/**
 * Class Format.
 */
public abstract class Format {
    /**
     * Constant UNKNOWN_PAYLOAD.
     */
    public static final int UNKNOWN_PAYLOAD = -1;

    /**
     * Creates a new instance of Format.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public Format(String arg1, int arg2) {

    }

    /**
     * Returns the codec.
     *  
     * @return  The codec.
     */
    public String getCodec() {
        return (java.lang.String) null;
    }

    /**
     * Returns the payload.
     *  
     * @return  The payload.
     */
    public int getPayload() {
        return 0;
    }

} // end Format
