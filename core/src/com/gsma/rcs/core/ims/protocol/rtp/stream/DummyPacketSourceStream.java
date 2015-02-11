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

package com.gsma.rcs.core.ims.protocol.rtp.stream;

import com.gsma.rcs.core.ims.protocol.rtp.format.DummyFormat;
import com.gsma.rcs.core.ims.protocol.rtp.format.Format;
import com.gsma.rcs.core.ims.protocol.rtp.util.Buffer;
import com.gsma.rcs.core.ims.protocol.rtp.util.SystemTimeBase;
import com.gsma.rcs.utils.FifoBuffer;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Dummy packet source stream (used to pass NAT)
 * 
 * @author jexa7410
 */
public class DummyPacketSourceStream extends Thread implements ProcessorInputStream {

    /**
     * Source period for the opening phase (in milliseconds)
     */
    public final static int DUMMY_SOURCE_OPENING_PERIOD = 100;

    /**
     * Source period (in milliseconds)
     */
    public final static int DUMMY_SOURCE_PERIOD = 15000;

    /**
     * Input format
     */
    private DummyFormat format = new DummyFormat();

    /**
     * Time base
     */
    private SystemTimeBase systemTimeBase = new SystemTimeBase();

    /**
     * Sequence number
     */
    private long seqNo = 0;

    /**
     * Message buffer
     */
    private FifoBuffer fifo = new FifoBuffer();

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Interruption flag
     */
    private boolean interrupted = false;

    /**
     * Incoming stream is started ?
     */
    private boolean incomingStarted = false;

    /**
     * Constructor
     */
    public DummyPacketSourceStream() {
    }

    /**
     * Open the input stream
     */
    public void open() {
        start();
        if (logger.isActivated()) {
            logger.debug("Dummy source stream opened");
        }
    }

    /**
     * Close the input stream
     */
    public void close() {
        interrupted = true;
        try {
            fifo.close();
        } catch (Exception e) {
            // Intentionally blank
        }
        if (logger.isActivated()) {
            logger.debug("Dummy source stream closed");
        }
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
     * Background processing
     */
    public void run() {
        while (!interrupted) {
            try {
                // Build a new dummy packet
                Buffer packet = new Buffer();
                packet.setData(new byte[0]);
                packet.setLength(0);
                packet.setFormat(format);
                packet.setSequenceNumber(seqNo++);
                packet.setTimeStamp(systemTimeBase.getTime());

                // Post the packet in the FIFO
                fifo.addObject(packet);

                // Make a pause
                if (!incomingStarted) {
                    Thread.sleep(DUMMY_SOURCE_OPENING_PERIOD);
                } else {
                    Thread.sleep(DUMMY_SOURCE_PERIOD);
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Dummy packet source has failed", e);
                }
            }
        }
    }

    /**
     * Read from the stream
     * 
     * @return Buffer
     * @throws Exception
     */
    public Buffer read() throws Exception {
        // Read the FIFO the buffer
        Buffer buffer = (Buffer) fifo.getObject();
        return buffer;
    }

    /**
     * Set incomingStarted.
     */
    public void incomingStarted() {
        incomingStarted = true;
    }
}
