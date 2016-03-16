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

import java.io.IOException;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.platform.network.SocketConnection;
import com.gsma.rcs.platform.network.SocketServerConnection;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.logger.Logger;

/**
 * MSRP server connection
 * 
 * @author jexa7410
 */
public class MsrpServerConnection extends MsrpConnection {
    /**
     * Local TCP port number
     */
    private int mLocalPort;

    /**
     * Socket server connection
     */
    private SocketServerConnection mSocketServer;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(MsrpServerConnection.class.getName());

    /**
     * Constructor
     * 
     * @param session MSRP session
     * @param localPort Local port number
     */
    public MsrpServerConnection(MsrpSession session, int localPort) {
        super(session);
        mLocalPort = localPort;
    }

    /**
     * Returns the socket connection
     * 
     * @return Socket
     * @throws NetworkException
     */
    public SocketConnection getSocketConnection() throws NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("Open server socket at " + mLocalPort);
            }
            mSocketServer = NetworkFactory.getFactory().createSocketServerConnection();
            mSocketServer.open(mLocalPort);

            if (sLogger.isActivated()) {
                sLogger.debug("Wait client connection");
            }

            SocketConnection socket = mSocketServer.acceptConnection();
            if (sLogger.isActivated()) {
                sLogger.debug("Socket connected to " + socket.getRemoteAddress() + ":"
                        + socket.getRemotePort());
            }
            return socket;

        } catch (IOException e) {
            throw new NetworkException("Failed to get socket connection!", e);
        }
    }

    /**
     * Close the connection
     */
    public void close() {
        super.close();
        CloseableUtils.tryToClose(mSocketServer);
    }
}
