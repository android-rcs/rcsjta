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

package org.gsma.rcs.core.ims.protocol.rtp.media;

/**
 * Class MediaSample.
 */
public class MediaSample {
    /**
     * Creates a new instance of MediaSample.
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2.
     */
    public MediaSample(byte[] arg1, long arg2) {

    }

    /**
     * Creates a new instance of MediaSample.
     *  
     * @param arg1 The arg1 array.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     */
    public MediaSample(byte[] arg1, long arg2, boolean arg3) {

    }

    /**
     * Returns the length.
     *  
     * @return  The length.
     */
    public int getLength() {
        return 0;
    }

    /**
     * Returns the data.
     *  
     * @return  The data array.
     */
    public byte[] getData() {
        return (byte []) null;
    }

    /**
     * Returns the time stamp.
     *  
     * @return  The time stamp.
     */
    public long getTimeStamp() {
        return 0l;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isMarker() {
        return false;
    }

} // end MediaSample
