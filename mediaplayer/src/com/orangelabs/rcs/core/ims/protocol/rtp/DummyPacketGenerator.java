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
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.DummyPacketSourceStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpOutputStream;

/**
 * Dummy packet generator for maintaining alive the network address in NAT
 *
 * @author jexa7410
 */
public class DummyPacketGenerator {
    /**
     * Media processor
     */
    private Processor processor = null;

    /**
     * RTP output stream
     */
    private RtpOutputStream outputStream = null;

    /**
     * DummyPacketSourceStream
     */
    private DummyPacketSourceStream inputStream = null;

    /**
     * Constructor
     */
    public DummyPacketGenerator() {
    }

    /**
     * Prepare the RTP session
     *
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param rtpStream already existing RTP input stream
     * @throws RtpException
     */
    public void prepareSession(String remoteAddress, int remotePort, RtpInputStream rtpStream)
            throws RtpException {
    	try {
    		// Create the input stream
            inputStream = new DummyPacketSourceStream();
    		inputStream.open();

            // Create the output stream
            outputStream = new RtpOutputStream(remoteAddress, remotePort, rtpStream);
    		outputStream.open();

            // Create the media processor
    		processor = new Processor(inputStream, outputStream, new Codec[0]);

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

        if (outputStream != null)
            outputStream.close();
    }

    /**
     * Set incomingStarted.
     */
    public void incomingStarted() {
        inputStream.incomingStarted();
    }
}
