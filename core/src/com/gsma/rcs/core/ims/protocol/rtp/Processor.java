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
import com.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorInputStream;
import com.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorOutputStream;
import com.gsma.rcs.core.ims.protocol.rtp.util.Buffer;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Media processor. A processor receives an input stream, use a codec chain to filter the data
 * before to send it to the output stream.
 * 
 * @author jexa7410
 */
public class Processor extends Thread {
    /**
     * Processor input stream
     */
    private ProcessorInputStream inputStream;

    /**
     * Processor output stream
     */
    private ProcessorOutputStream outputStream;

    /**
     * Codec chain
     */
    private CodecChain codecChain;

    /**
     * Processor status flag
     */
    private boolean interrupted = false;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(Processor.class.getName());

    /**
     * Constructor
     * 
     * @param inputStream Input stream
     * @param outputStream Output stream
     * @param codecs List of codecs
     */
    public Processor(ProcessorInputStream inputStream, ProcessorOutputStream outputStream,
            Codec[] codecs) {
        super();

        this.inputStream = inputStream;
        this.outputStream = outputStream;

        // Create the codec chain
        codecChain = new CodecChain(codecs, outputStream);

        if (sLogger.isActivated()) {
            sLogger.debug("Media processor created");
        }
    }

    /**
     * Start processing
     */
    public void startProcessing() {
        if (sLogger.isActivated()) {
            sLogger.debug("Start media processor");
        }
        interrupted = false;
        start();
    }

    /**
     * Stop processing
     */
    public void stopProcessing() {
        if (sLogger.isActivated()) {
            sLogger.debug("Stop media processor");
        }
        interrupted = true;

        // Close streams
        outputStream.close();
        inputStream.close();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Processor processing is started");
            }

            // Start processing
            while (!interrupted) {
                // Read data from the input stream
                Buffer inBuffer = inputStream.read();
                if (inBuffer == null) {
                    interrupted = true;
                    if (sLogger.isActivated()) {
                        sLogger.debug("Processing terminated: null data received");
                    }
                    break;
                }

                // Codec chain processing
                int result = codecChain.process(inBuffer);
                if ((result != Codec.BUFFER_PROCESSED_OK)
                        && (result != Codec.OUTPUT_BUFFER_NOT_FILLED)) {
                    interrupted = true;
                    if (sLogger.isActivated()) {
                        sLogger.error("Codec chain processing error: " + result);
                    }
                    break;
                }
            }
        } catch (NetworkException e) {
            interrupted = true;
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            interrupted = true;
            sLogger.error("Unable to process codec chain!", e);
        }
    }

    /**
     * Returns the input stream
     * 
     * @return Stream
     */
    public ProcessorInputStream getInputStream() {
        return inputStream;
    }

    /**
     * Returns the output stream
     * 
     * @return Stream
     */
    public ProcessorOutputStream getOutputStream() {
        return outputStream;
    }
}
