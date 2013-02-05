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
 * Class NetworkFactory.
 */
public abstract class NetworkFactory {
    /**
     * Creates a new instance of NetworkFactory.
     */
    public NetworkFactory() {

    }

    /**
     * Returns the local ip address.
     *  
     * @param arg1 The arg1.
     * @return  The local ip address.
     */
    public abstract String getLocalIpAddress(int arg1);

    /**
     * Creates the datagram connection.
     *  
     * @return  The datagram connection.
     */
    public abstract DatagramConnection createDatagramConnection();

    /**
     * Creates the socket client connection.
     *  
     * @return  The socket connection.
     */
    public abstract SocketConnection createSocketClientConnection();

    /**
     * Creates the secure socket client connection.
     *  
     * @return  The socket connection.
     */
    public abstract SocketConnection createSecureSocketClientConnection();

    /**
     * Creates the socket server connection.
     *  
     * @return  The socket server connection.
     */
    public abstract SocketServerConnection createSocketServerConnection();

    /**
     * Creates the http connection.
     *  
     * @return  The http connection.
     */
    public abstract HttpConnection createHttpConnection();

    /**
     * Returns the factory.
     *  
     * @return  The factory.
     */
    public static NetworkFactory getFactory() {
        return (NetworkFactory) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public static void loadFactory(String arg1) throws org.gsma.rcs.platform.FactoryException {

    }

} // end NetworkFactory
