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
package com.orangelabs.rcs.core.ims.service.sip.streaming;

import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.utils.FifoBuffer;

/**
 * Data player in charge of sending data payload to the network via
 * the RTP protocol
 * 
 * @author Jean-Marc AUFFRET
 */
public class DataSender implements MediaInput {
    /**
     * Received frames
     */
    private FifoBuffer fifo = null;

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
     * @param marker Marker bit 
     */
    public void addFrame(byte[] data, long timestamp) {
        if (fifo != null) {
        	MediaSample sample = new MediaSample(data, timestamp);
            fifo.addObject(sample);
        }
    }

    /**
     * Open the player
     */
    public void open() {
        fifo = new FifoBuffer();
    }

    /**
     * Close the player
     */
    public void close() {
        if (fifo != null) {
            fifo.close();
            fifo = null;
        }
    }

    /**
     * Read a media sample (blocking method)
     *
     * @return Media sample
     * @throws MediaException
     */
    public MediaSample readSample() throws MediaException {
        try {
            if (fifo != null) {
                return (MediaSample)fifo.getObject();
            } else {
                throw new MediaException("Media input not opened");
            }
        } catch (Exception e) {
            throw new MediaException("Can't read media sample");
        }
    }
}