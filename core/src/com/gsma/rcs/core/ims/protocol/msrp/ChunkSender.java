/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.protocol.msrp;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Chunks sender
 * 
 * @author jexa7410
 */
public class ChunkSender extends Thread {
    /**
     * MSRP connection
     */
    private MsrpConnection mConnection;

    /**
     * MSRP output stream
     */
    private OutputStream mStream;

    /**
     * Buffer of chunks
     */
    private FifoBuffer mBuffer = new FifoBuffer();

    /**
     * Termination flag
     */
    private boolean mTerminated;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(ChunkSender.class.getName());

    /**
     * Constructor
     * 
     * @param connection MSRP connection
     * @param stream TCP output stream
     */
    public ChunkSender(MsrpConnection connection, OutputStream stream) {
        mConnection = connection;
        mStream = stream;
    }

    /**
     * Returns the MSRP connection
     * 
     * @return MSRP connection
     */
    public MsrpConnection getConnection() {
        return mConnection;
    }

    /**
     * Terminate the sender
     */
    public void terminate() {
        mTerminated = true;
        mBuffer.unblockRead();
        interrupt();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            // Read chunk to be sent
            byte chunk[] = null;
            while ((chunk = (byte[]) mBuffer.getMessage()) != null) {
                // Write chunk to the output stream
                if (MsrpConnection.isMsrpTraceEnabled()) {
                    System.out.println(">>> Send MSRP message:\n" + new String(chunk, UTF8));
                }
                writeData(chunk);
            }
        } catch (NetworkException e) {
            if (!mTerminated) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
                /* Notify the msrp session listener that an error has occured */
                /* Changed by Deutsche Telekom */
                mConnection.getSession().getMsrpEventListener()
                        .msrpTransferError(null, e.getMessage(), TypeMsrpChunk.Unknown);
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Unable to send chunks!", e);
            if (!mTerminated) {
                /* Notify the msrp session listener that an error has occured */
                /* Changed by Deutsche Telekom */
                mConnection.getSession().getMsrpEventListener()
                        .msrpTransferError(null, e.getMessage(), TypeMsrpChunk.Unknown);
            }
        }
    }

    /**
     * Send a chunk
     * 
     * @param chunk New chunk
     * @throws NetworkException
     */
    public void sendChunk(byte chunk[]) throws NetworkException {
        if (mConnection.getSession().isFailureReportRequested()) {
            mBuffer.putMessage(chunk);
        } else {
            sendChunkImmediately(chunk);
        }
    }

    /**
     * Send a chunk immediately
     * 
     * @param chunk New chunk
     * @throws NetworkException
     */
    public void sendChunkImmediately(byte chunk[]) throws NetworkException {
        if (MsrpConnection.isMsrpTraceEnabled()) {
            System.out.println(">>> Send MSRP message:\n" + new String(chunk, UTF8));
        }
        writeData(chunk);
    }

    /**
     * Write data to the stream
     * 
     * @param chunk Data chunk
     * @throws NetworkException
     */
    private synchronized void writeData(byte chunk[]) throws NetworkException {
        try {
            mStream.write(chunk);
            mStream.flush();
        } catch (IOException e) {
            throw new NetworkException("Failed to write data!", e);
        }
    }
}
