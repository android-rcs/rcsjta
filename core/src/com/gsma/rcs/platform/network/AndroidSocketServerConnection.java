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

package com.gsma.rcs.platform.network;

import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Android socket connection
 * 
 * @author jexa7410
 */
public class AndroidSocketServerConnection implements SocketServerConnection {
    /**
     * Socket server connection
     */
    private ServerSocket mAcceptSocket;

    private static final Logger sLogger = Logger.getLogger(AndroidSocketServerConnection.class
            .getSimpleName());

    /**
     * Constructor
     */
    public AndroidSocketServerConnection() {
    }

    /**
     * Open the socket
     * 
     * @param port Local port
     * @throws IOException
     */
    public void open(int port) throws IOException {
        mAcceptSocket = new ServerSocket(port);
    }

    /**
     * Close the socket
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (mAcceptSocket != null) {
            mAcceptSocket.close();
            mAcceptSocket = null;
        }
    }

    /**
     * Accept connection
     * 
     * @return Socket connection
     * @throws IOException
     */
    public SocketConnection acceptConnection() throws IOException {
        if (mAcceptSocket != null) {
            if (sLogger.isActivated()) {
                sLogger.debug("Socket serverSocket is waiting for incoming connection");
            }
            Socket socket = mAcceptSocket.accept();
            return new AndroidSocketConnection(socket);
        }
        throw new IOException("Connection not opened");
    }
}
