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

package com.gsma.rcs.core.ims.protocol.msrp;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.platform.network.SocketConnection;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.logger.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract MSRP connection between two end points
 * 
 * @author jexa7410
 */
public abstract class MsrpConnection {

    private static boolean sMsrpTraceEnabled = false;

    private MsrpSession mSession;

    private SocketConnection mSocket;

    private OutputStream mOutputStream;

    private InputStream mInputStream;

    /**
     * Chunk receiver
     */
    private ChunkReceiver mReceiver;

    /**
     * Chunk sender
     */
    private ChunkSender mSender;

    private static final Logger sLogger = Logger.getLogger(MsrpConnection.class.getName());

    /**
     * Constructor
     * 
     * @param session MSRP session
     */
    public MsrpConnection(MsrpSession session) {
        mSession = session;
    }

    /**
     * Returns the MSRP session associated to the MSRP connection
     * 
     * @return MSRP session
     */
    public MsrpSession getSession() {
        return mSession;
    }

    /**
     * Open the connection
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void open() throws NetworkException, PayloadException {
        // Open socket connection
        mSocket = getSocketConnection();
        // Open I/O stream
        mInputStream = mSocket.getInputStream();
        mOutputStream = mSocket.getOutputStream();
        // Create the chunk receiver
        mReceiver = new ChunkReceiver(this, mInputStream);
        mReceiver.start();
        // Create the chunk sender
        mSender = new ChunkSender(this, mOutputStream);
        mSender.start();
        if (sLogger.isActivated()) {
            sLogger.debug("Connection has been opened");
        }
    }

    /**
     * Open the connection with SO_TIMEOUT on the socket
     * 
     * @param timeout Timeout value (in milliseconds)
     * @throws NetworkException
     * @throws PayloadException
     */
    public void open(long timeout) throws NetworkException, PayloadException {
        // Open socket connection
        mSocket = getSocketConnection();
        // Set SoTimeout
        mSocket.setSoTimeout(timeout);
        // Open I/O stream
        mInputStream = mSocket.getInputStream();
        mOutputStream = mSocket.getOutputStream();
        // Create the chunk receiver
        mReceiver = new ChunkReceiver(this, mInputStream);
        mReceiver.start();
        // Create the chunk sender
        mSender = new ChunkSender(this, mOutputStream);
        mSender.start();
        if (sLogger.isActivated()) {
            sLogger.debug("Connection has been opened");
        }
    }

    /**
     * Close the connection
     */
    public void close() {
        if (mSender != null) {
            mSender.terminate();
        }
        if (mReceiver != null) {
            mReceiver.terminate();
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Close the socket connection");
        }
        CloseableUtils.tryToClose(mInputStream);
        CloseableUtils.tryToClose(mOutputStream);
        CloseableUtils.tryToClose(mSocket);
        if (sLogger.isActivated()) {
            sLogger.debug("Connection has been closed");
        }
    }

    /**
     * Send a new data chunk
     * 
     * @param chunk Data chunk
     * @throws NetworkException
     */
    public void sendChunk(byte chunk[]) throws NetworkException {
        if (mSender == null) {
            throw new NetworkException("ChunkSender is already closed!");
        }
        mSender.sendChunk(chunk);
    }

    /**
     * Send a new data chunk immediately
     * 
     * @param chunk Data chunk
     * @throws NetworkException
     */
    public void sendChunkImmediately(byte chunk[]) throws NetworkException {
        mSender.sendChunkImmediately(chunk);
    }

    /**
     * Returns the socket connection
     * 
     * @return Socket
     * @throws PayloadException
     * @throws NetworkException
     */
    public abstract SocketConnection getSocketConnection() throws PayloadException,
            NetworkException;

    /**
     * Checks if MSRP trace is enabled
     * 
     * @return True if MSRP trace is enabled
     */
    public static boolean isMsrpTraceEnabled() {
        return sMsrpTraceEnabled;
    }

    /**
     * Sets MSRP trace enabled
     * 
     * @param enable True if MSRP trace is enabled
     */
    public static void setMsrpTraceEnabled(boolean enable) {
        sMsrpTraceEnabled = enable;
    }
}
