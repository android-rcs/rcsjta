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

package com.orangelabs.rcs.core.ims.protocol.rtp;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.Codec;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.MediaRendererStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;

/**
 * Media RTP receiver
 */
public class MediaRtpReceiver {
    /**
     * Media processor
     */
    protected Processor processor = null;

	/**
	 * Local port number (RTP listening port)
	 */
    protected int localPort;

    /**
     * RTP Input Stream
     */
	protected RtpInputStream inputStream = null;

    /**
     * Constructor
     *
     * @param localPort Local port number
     */
	public MediaRtpReceiver(int localPort) {
		this.localPort = localPort;
	}

    /**
     * Prepare the RTP session
     *
     * @param remoteAddress Remote address 
     * @param remotePort Remote port
     * @param renderer Renderer
     * @param format format
     * @param rtpStreamListener RTP Stream listener
     * @throws RtpException When an error occurs
     */
    public void prepareSession(String remoteAddress, int remotePort, 
            MediaOutput renderer, Format format, RtpStreamListener rtpStreamListener)
            throws RtpException {
    	try {
			// Create the input stream
            inputStream = new RtpInputStream(remoteAddress, remotePort, localPort, format);
            inputStream.addRtpStreamListener(rtpStreamListener);
    		inputStream.open();

            // Create the output stream
        	MediaRendererStream outputStream = new MediaRendererStream(renderer);
    		outputStream.open();

        	// Create the codec chain
        	Codec[] codecChain = MediaRegistry.generateDecodingCodecChain(format.getCodec());

            // Create the media processor
    		processor = new Processor(inputStream, outputStream, codecChain);
        } catch(Exception e) {
        	throw new RtpException("Can't prepare resources");
        }
    }

    /**
	 * Start the RTP session
	 */
	public void startSession() {
		// Start the media processor
		if (processor != null) {
			processor.startProcessing();
		}
	}

	/**
	 * Stop the RTP session
	 */
	public void stopSession() {
		// Stop the media processor
		if (processor != null) {
			processor.stopProcessing();
		}
	}

    /**
     * Returns the RTP input stream
     *
     * @return RTP input stream
     */
    public RtpInputStream getInputStream() {
        return inputStream;
    }
}
