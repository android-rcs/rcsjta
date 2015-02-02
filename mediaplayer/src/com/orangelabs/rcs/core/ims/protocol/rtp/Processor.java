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
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.ProcessorInputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.ProcessorOutputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;

/**
 * Media processor. A processor receives an input stream, use a codec chain
 * to filter the data before to send it to the output stream.
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
     * Constructor
     *
     * @param inputStream Input stream
     * @param outputStream Output stream
     * @param codecs List of codecs
     */
	public Processor(ProcessorInputStream inputStream, ProcessorOutputStream outputStream, Codec[] codecs) {
        super();

		this.inputStream = inputStream;
        this.outputStream = outputStream;

		// Create the codec chain
		codecChain = new CodecChain(codecs, outputStream);
	}

	/**
	 * Start processing
	 */
	public void startProcessing() {
		interrupted = false;
        start();
	}

	/**
	 * Stop processing
	 */
	public void stopProcessing() {
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
			// Start processing
			while (!interrupted) {
				// Read data from the input stream
				Buffer inBuffer = inputStream.read();
				if (inBuffer == null) {
					interrupted = true;
					break;
				}
				
                // Codec chain processing
                int result = codecChain.process(inBuffer);
                if ((result != Codec.BUFFER_PROCESSED_OK)
                        && (result != Codec.OUTPUT_BUFFER_NOT_FILLED)) {
                    interrupted = true;
                    break;
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
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
