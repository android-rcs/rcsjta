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
    private DummyFormat mFormat = new DummyFormat();

    /**
     * Time base
     */
    private SystemTimeBase mSystemTimeBase = new SystemTimeBase();

    /**
     * Sequence number
     */
    private long mSeqNo;

    /**
     * Message buffer
     */
    private FifoBuffer mBuffer = new FifoBuffer();

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(DummyPacketSourceStream.class.getName());

    /**
     * Interruption flag
     */
    private boolean mInterrupted;

    /**
     * Incoming stream is started ?
     */
    private boolean mIncomingStarted;

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
        if (sLogger.isActivated()) {
            sLogger.debug("Dummy source stream opened");
        }
    }

    /**
     * Close the input stream
     */
    public void close() {
        mInterrupted = true;
        mBuffer.close();
        if (sLogger.isActivated()) {
            sLogger.debug("Dummy source stream closed");
        }
    }

    /**
     * Format of the data provided by the source stream
     * 
     * @return Format
     */
    public Format getFormat() {
        return mFormat;
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            while (!mInterrupted) {
                try {
                    // Build a new dummy packet
                    Buffer packet = new Buffer();
                    packet.setData(new byte[0]);
                    packet.setLength(0);
                    packet.setFormat(mFormat);
                    packet.setSequenceNumber(mSeqNo++);
                    packet.setTimestamp(mSystemTimeBase.getTimestamp());

                    // Post the packet in the FIFO
                    mBuffer.addObject(packet);

                    // Make a pause
                    if (!mIncomingStarted) {
                        Thread.sleep(DUMMY_SOURCE_OPENING_PERIOD);
                    } else {
                        Thread.sleep(DUMMY_SOURCE_PERIOD);
                    }
                } catch (InterruptedException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }
                }
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Dummy packet source has failed!", e);
        }
    }

    /**
     * Read from the stream
     * 
     * @return Buffer
     */
    public Buffer read() {
        // Read the FIFO the buffer
        Buffer buffer = (Buffer) mBuffer.getObject();
        return buffer;
    }

    /**
     * Set incomingStarted.
     */
    public void incomingStarted() {
        mIncomingStarted = true;
    }
}
