/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.platform.network;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Android socket connection
 * 
 * @author jexa7410
 */
public class AndroidSocketConnection implements SocketConnection {
    /**
     * Socket connection
     */
    private Socket mSocket;

    /**
     * Constructor
     */
    public AndroidSocketConnection() {
    }

    /**
     * Constructor
     * 
     * @param socket Socket
     */
    public AndroidSocketConnection(Socket socket) {
        this.mSocket = socket;
    }

    /**
     * Open the socket
     * 
     * @param remoteAddr Remote address
     * @param remotePort Remote port
     * @throws NetworkException
     * @throws PayloadException
     */
    public void open(String remoteAddr, int remotePort) throws NetworkException, PayloadException {
        try {
            mSocket = new Socket(remoteAddr, remotePort);
        } catch (IOException e) {
            throw new NetworkException(new StringBuilder("Failed to open socket for address : ")
                    .append(remoteAddr).append(" and port : ").append(remotePort).toString(), e);
        }
    }

    /**
     * Set the socket
     * 
     * @param socket Socket
     */
    public void setSocket(Socket socket) {
        this.mSocket = socket;
    }

    /**
     * Get the socket
     * 
     * @return Socket
     */
    public Socket getSocket() {
        return this.mSocket;
    }

    /**
     * Close the socket
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    /**
     * Returns the socket input stream
     * 
     * @return Input stream
     * @throws NetworkException
     */
    public InputStream getInputStream() throws NetworkException {
        try {
            return mSocket.getInputStream();

        } catch (IOException e) {
            throw new NetworkException("Failed to get input stream from connection!", e);
        }
    }

    /**
     * Returns the socket output stream
     * 
     * @return Output stream
     * @throws NetworkException
     */
    public OutputStream getOutputStream() throws NetworkException {
        try {
            return mSocket.getOutputStream();

        } catch (IOException e) {
            throw new NetworkException("Failed to get output stream from connection!", e);
        }
    }

    /**
     * Returns the remote address of the connection
     * 
     * @return Address
     */
    public String getRemoteAddress() {
        return mSocket.getInetAddress().getHostAddress();
    }

    /**
     * Returns the remote port of the connection
     * 
     * @return Port
     */
    public int getRemotePort() {
        return mSocket.getPort();
    }

    /**
     * Returns the local address of the connection
     * 
     * @return Address
     */
    public String getLocalAddress() {
        return mSocket.getLocalAddress().getHostAddress();
    }

    /**
     * Returns the local port of the connection
     * 
     * @return Port
     */
    public int getLocalPort() {
        return mSocket.getLocalPort();
    }

    /**
     * Get the timeout for this socket during which a reading operation shall block while waiting
     * for data
     * 
     * @return Timeout in milliseconds
     * @throws NetworkException
     */
    public int getSoTimeout() throws NetworkException {
        try {
            return mSocket.getSoTimeout();

        } catch (IOException e) {
            throw new NetworkException("Failed to get socket timeout!", e);
        }
    }

    /**
     * Set the timeout for this socket during which a reading operation shall block while waiting
     * for data
     * 
     * @param timeout Timeout in milliseconds
     * @throws NetworkException
     */
    public void setSoTimeout(long timeout) throws NetworkException {
        try {
            /* NOTE: External API limiting timeout that should be in type 'long' to 'int'. */
            mSocket.setSoTimeout((int) timeout);
        } catch (IOException e) {
            throw new NetworkException(new StringBuilder("Failed to set socket timeout : ").append(
                    timeout).toString(), e);
        }
    }
}
