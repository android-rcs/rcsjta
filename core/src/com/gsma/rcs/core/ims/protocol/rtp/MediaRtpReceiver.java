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

package com.gsma.rcs.core.ims.protocol.rtp;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.rtp.codec.Codec;
import com.gsma.rcs.core.ims.protocol.rtp.format.Format;
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.gsma.rcs.core.ims.protocol.rtp.stream.MediaRendererStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;

/**
 * Media RTP receiver
 */
public class MediaRtpReceiver {
    /**
     * Media processor
     */
    protected Processor mProcessor;

    /**
     * Local port number (RTP listening port)
     */
    protected int mLocalPort;

    /**
     * RTP Input Stream
     */
    protected RtpInputStream mInputStream;

    /**
     * The logger
     */
    protected static final Logger sLogger = Logger.getLogger(MediaRtpReceiver.class.getName());

    /**
     * Constructor
     * 
     * @param localPort Local port number
     */
    public MediaRtpReceiver(int localPort) {
        this.mLocalPort = localPort;
    }

    /**
     * Prepare the RTP session
     * 
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param renderer Renderer
     * @param format format
     * @param rtpStreamListener RTP Stream listener
     * @throws NetworkException
     */
    public void prepareSession(String remoteAddress, int remotePort, MediaOutput renderer,
            Format format, RtpStreamListener rtpStreamListener) throws NetworkException {
        try {
            // Create the input stream
            mInputStream = new RtpInputStream(remoteAddress, remotePort, mLocalPort, format);
            mInputStream.addRtpStreamListener(rtpStreamListener);
            mInputStream.open();
            if (sLogger.isActivated()) {
                sLogger.debug("Input stream: " + mInputStream.getClass().getName());
            }

            // Create the output stream
            MediaRendererStream outputStream = new MediaRendererStream(renderer);
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
                    .append(" with remotePort : ").append(remotePort).append("!").toString(), e);
        }
    }

    /**
     * Start the RTP session
     */
    public void startSession() {
        if (sLogger.isActivated()) {
            sLogger.info("Start the session");
        }

        // Start the media processor
        if (mProcessor != null) {
            mProcessor.startProcessing();
        }
    }

    /**
     * Stop the RTP session
     */
    public void stopSession() {
        if (sLogger.isActivated()) {
            sLogger.info("Stop the session");
        }

        // Stop the media processor
        if (mProcessor != null) {
            mProcessor.stopProcessing();
        }
    }

    /**
     * Returns the RTP input stream
     * 
     * @return RTP input stream
     */
    public RtpInputStream getInputStream() {
        return mInputStream;
    }
}
