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

package com.gsma.rcs.core.ims.protocol.rtp.media;

/**
 * Media sample
 * 
 * @author jexa7410
 */
public class MediaSample {

    /**
     * Data
     */
    private byte[] data;

    /**
     * Timestamp
     */
    private long mTimestamp;

    /**
     * RTP marker bit
     */
    private boolean marker = false;

    /**
     * Sequence number
     */
    private long sequenceNumber;

    // TODO: remove after implement receiver buffer

    /**
     * Constructor
     * 
     * @param data Data
     * @param timestamp Timestamp
     * @Param sequenceNumber Packet sequence number
     */
    public MediaSample(byte[] data, long timestamp, long sequenceNumber) {
        this.data = data;
        this.mTimestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Constructor
     * 
     * @param data Data
     * @param timestamp Timestamp
     */
    public MediaSample(byte[] data, long timestamp) {
        this.data = data;
        this.mTimestamp = timestamp;
    }

    /**
     * Constructor
     * 
     * @param data Data
     * @param timestamp Timestamp
     * @param marker Marker bit
     */
    public MediaSample(byte[] data, long timestamp, boolean marker) {
        this.data = data;
        this.mTimestamp = timestamp;
        this.marker = marker;
    }

    /**
     * Returns the data sample
     * 
     * @return Byte array
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Returns the length of the data sample
     * 
     * @return Data sample length
     */
    public int getLength() {
        if (data != null) {
            return data.length;
        }
        return 0;
    }

    /**
     * Returns the timestamp of the sample
     * 
     * @return Timestamp in microseconds
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Is RTP marker bit
     * 
     * @return Boolean
     */
    public boolean isMarker() {
        return marker;
    }

    /**
     * Get the sequence number
     * 
     * @return sequence number
     */
    public long getSequenceNumber() {
        return this.sequenceNumber;
    }

}
