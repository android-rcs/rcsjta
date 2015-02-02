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

package com.orangelabs.rcs.core.ims.protocol.rtp.stream;

import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;

/**
 * Media capture stream
 * 
 * @author jexa7410
 */
public class MediaCaptureStream implements ProcessorInputStream {
	/**
     * Media player
     */
	private MediaInput player;

	/**
	 * Media format
	 */
	private Format format;
	
    /**
     * Sequence number
     */
    protected long seqNo = 0;

    /**
     * Input buffer
     */
	protected Buffer buffer = new Buffer();

    /**
	 * Constructor
	 * 
	 * @param format Input format
     * @param player Media player
	 */
    public MediaCaptureStream(Format format, MediaInput player) {
    	this.format = format;
		this.player = player;
	}
    
    
    /**
	 * Open the input stream
	 * 
     * @throws Exception
	 */	
    public void open() throws Exception {
    	try {
	    	player.open();
    	} catch(Exception e) {
			throw e;
    	}
	}    	
	
    /**
     * Close the input stream
     */
    public void close() {
		player.close();
    }

    /**
     * Format of the data provided by the source stream
     * 
     * @return Format
     */
    public Format getFormat() {
    	return format;
    }

    /**
     * returns the MediaInput
     *
     * @return MediaInput
     */
    public MediaInput getPlayer() {
        return player;
    }

    /**
     * Read from the stream
     * 
     * @return Buffer
     * @throws Exception
     */
    public Buffer read() throws Exception {
    	// Read a new sample from the media player
    	MediaSample sample = player.readSample();
    	if (sample == null) {
    		return null;
    	}

    	// Create a buffer
	    buffer.setData(sample.getData());
	    buffer.setLength(sample.getLength());
    	buffer.setFormat(format);
    	buffer.setSequenceNumber(seqNo++);
    	if (sample.isMarker()) {
    		buffer.setFlags(Buffer.FLAG_RTP_MARKER);
    	}
    	buffer.setTimeStamp(sample.getTimeStamp());
    	return buffer;
    }
}
