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
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.VideoRendererStream;

/**
 * Video RTP receiver
 *
 * @author hlxn7157
 */
public class VideoRtpReceiver  extends MediaRtpReceiver {
    /**
     * Constructor
     *
     * @param localPort Local port number
     */
	public VideoRtpReceiver(int localPort) {
		super(localPort);
	}

    /**
     * Prepare the RTP session
     *
     * @param remoteAddress Remote address 
     * @param remotePort Remote port
     * @param orientationHeaderId RTP orientation extension header id 
     * @param renderer Renderer
     * @param format Video format
     * @param rtpStreamListener RTP Stream listener
     * @throws RtpException When an error occurs
     */
    public void prepareSession(String remoteAddress, int remotePort, int orientationHeaderId, 
            MediaOutput renderer, Format format, RtpStreamListener rtpStreamListener)
            throws RtpException {
    	try {
			// Create the input stream
            inputStream = new RtpInputStream(remoteAddress, remotePort, localPort, format);
            inputStream.setExtensionHeaderId(orientationHeaderId);
            inputStream.addRtpStreamListener(rtpStreamListener);
    		inputStream.open();

            // Create the output stream
        	VideoRendererStream outputStream = new VideoRendererStream(renderer);
    		outputStream.open();

        	// Create the codec chain
        	Codec[] codecChain = MediaRegistry.generateDecodingCodecChain(format.getCodec());

            // Create the media processor
    		processor = new Processor(inputStream, outputStream, codecChain);
        } catch(Exception e) {
        	throw new RtpException("Can't prepare resources");
        }
    }
}
