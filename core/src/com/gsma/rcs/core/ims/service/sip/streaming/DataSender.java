/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.sip.streaming;

import com.gsma.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.gsma.rcs.utils.FifoBuffer;

/**
 * Data player in charge of sending data payload to the network via the RTP protocol
 * 
 * @author Jean-Marc AUFFRET
 */
public class DataSender implements MediaInput {
    /**
     * Received frames
     */
    private FifoBuffer mFifo;

    /**
     * Constructor
     */
    public DataSender() {
    }

    /**
     * Add a new video frame
     * 
     * @param data Data
     * @param timestamp Timestamp
     */
    public void addFrame(byte[] data, long timestamp) {
        if (mFifo != null) {
            MediaSample sample = new MediaSample(data, timestamp);
            mFifo.addObject(sample);
        }
    }

    /**
     * Open the player
     */
    public void open() {
        mFifo = new FifoBuffer();
    }

    /**
     * Close the player
     */
    public void close() {
        if (mFifo != null) {
            mFifo.close();
            mFifo = null;
        }
    }

    /**
     * Read a media sample (blocking method)
     * 
     * @return Media sample
     */
    public MediaSample readSample() {
        return (MediaSample) mFifo.getObject();
    }
}
