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

package com.gsma.rcs.core.ims.protocol.rtp;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.rtp.codec.Codec;
import com.gsma.rcs.core.ims.protocol.rtp.format.Format;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.gsma.rcs.core.ims.protocol.rtp.stream.VideoRendererStream;

import java.io.IOException;

/**
 * Video RTP receiver
 * 
 * @author hlxn7157
 */
public class VideoRtpReceiver extends MediaRtpReceiver {
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
     * @throws NetworkException
     */
    public void prepareSession(String remoteAddress, int remotePort, int orientationHeaderId,
            MediaOutput renderer, Format format, RtpStreamListener rtpStreamListener)
            throws NetworkException {
        try {
            // Create the input stream
            mInputStream = new RtpInputStream(remoteAddress, remotePort, mLocalPort, format);
            mInputStream.setExtensionHeaderId(orientationHeaderId);
            mInputStream.addRtpStreamListener(rtpStreamListener);
            mInputStream.open();
            if (sLogger.isActivated()) {
                sLogger.debug("Input stream: " + mInputStream.getClass().getName());
            }

            // Create the output stream
            VideoRendererStream outputStream = new VideoRendererStream(renderer);
            outputStream.open();
            if (sLogger.isActivated()) {
                sLogger.debug("Output stream: " + outputStream.getClass().getName());
            }

            // Create the codec chain
            Codec[] codecChain = MediaRegistry.generateDecodingCodecChain(format.getCodec());

            // Create the media processor
            mProcessor = new Processor(mInputStream, outputStream, codecChain);

            if (sLogger.isActivated()) {
                sLogger.debug("Session has been prepared with success");
            }
        } catch (IOException e) {
            throw new NetworkException(new StringBuilder(
                    "Can't prepare resources correctly for remoteAddress : ").append(remoteAddress)
                    .append(" with remotePort : ").append(remotePort)
                    .append(" orientationHeaderId : ").append(orientationHeaderId).append("!")
                    .toString(), e);
        }
    }
}
