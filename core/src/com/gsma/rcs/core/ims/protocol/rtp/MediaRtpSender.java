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
import com.gsma.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.gsma.rcs.core.ims.protocol.rtp.stream.MediaCaptureStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpOutputStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;

/**
 * Media RTP sender
 */
public class MediaRtpSender {
    /**
     * Format
     */
    protected Format format;

    /**
     * Media processor
     */
    protected Processor processor = null;

    /**
     * MediaCaptureStream
     */
    protected MediaCaptureStream inputStream = null;

    /**
     * RTP output stream
     */
    protected RtpOutputStream outputStream = null;

    /**
     * Local RTP port
     */
    protected int localPort;

    /**
     * The logger
     */
    protected Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param format Media format
     */
    public MediaRtpSender(Format format, int localRtpPort) {
        this.format = format;
        this.localPort = localRtpPort;
    }

    /**
     * Prepare the RTP session for a sender associated to a receiver
     * 
     * @param player Media player
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @throws NetworkException
     */
    public void prepareSession(MediaInput player, String remoteAddress, int remotePort,
            RtpInputStream rtpStream, RtpStreamListener rtpStreamListener)
            throws NetworkException {
        try {
            if (logger.isActivated()) {
                logger.debug("Prepare session");
            }

            // Create the input stream
            inputStream = new MediaCaptureStream(format, player);
            inputStream.open();
            if (logger.isActivated()) {
                logger.debug("Input stream: " + inputStream.getClass().getName());
            }

            // Create the output stream
            outputStream = new RtpOutputStream(remoteAddress, remotePort, rtpStream);
            outputStream.addRtpStreamListener(rtpStreamListener);
            outputStream.open();
            if (logger.isActivated()) {
                logger.debug("Output stream: " + outputStream.getClass().getName());
            }

            // Create the codec chain
            Codec[] codecChain = MediaRegistry.generateEncodingCodecChain(format.getCodec());

            // Create the media processor
            if (logger.isActivated()) {
                logger.debug("New processor");
            }
            processor = new Processor(inputStream, outputStream, codecChain);

            if (logger.isActivated()) {
                logger.debug("Session has been prepared with success");
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
        if (logger.isActivated()) {
            logger.debug("Start the session");
        }

        // Start the media processor
        if (processor != null) {
            processor.startProcessing();
        }
    }

    /**
     * Stop the RTP session
     */
    public void stopSession() {
        if (logger.isActivated()) {
            logger.debug("Stop the session");
        }

        // Stop the media processor
        if (processor != null) {
            processor.stopProcessing();
        }

        if (outputStream != null) {
            outputStream.close();
        }
    }
}
