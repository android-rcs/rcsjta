/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.platform.network;

/**
 * Class AndroidNetworkFactory.
 */
public class AndroidNetworkFactory extends NetworkFactory {
    /**
     * Creates a new instance of AndroidNetworkFactory.
     */
    public AndroidNetworkFactory() {
        super();
    }

    /**
     * Returns the local ip address.
     *  
     * @param arg1 The arg1.
     * @return  The local ip address.
     */
    public String getLocalIpAddress(int arg1) {
        return (java.lang.String) null;
    }

    /**
     * Creates the datagram connection.
     *  
     * @return  The datagram connection.
     */
    public DatagramConnection createDatagramConnection() {
        return (DatagramConnection) null;
    }

    /**
     * Creates the socket client connection.
     *  
     * @return  The socket connection.
     */
    public SocketConnection createSocketClientConnection() {
        return (SocketConnection) null;
    }

    /**
     * Creates the secure socket client connection.
     *  
     * @return  The socket connection.
     */
    public SocketConnection createSecureSocketClientConnection() {
        return (SocketConnection) null;
    }

    /**
     * Creates the socket server connection.
     *  
     * @return  The socket server connection.
     */
    public SocketServerConnection createSocketServerConnection() {
        return (SocketServerConnection) null;
    }

    /**
     * Creates the http connection.
     *  
     * @return  The http connection.
     */
    public HttpConnection createHttpConnection() {
        return (HttpConnection) null;
    }

} // end AndroidNetworkFactory
