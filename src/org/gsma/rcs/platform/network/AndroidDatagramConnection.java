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
 * Class AndroidDatagramConnection.
 */
public class AndroidDatagramConnection implements DatagramConnection {
    /**
     * Creates a new instance of AndroidDatagramConnection.
     */
    public AndroidDatagramConnection() {

    }

    /**
     *  
     * @throws IOException if an i/o error occurs
     */
    public void close() throws java.io.IOException {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3 array.
     * @throws IOException if an i/o error occurs
     */
    public void send(String arg1, int arg2, byte[] arg3) throws java.io.IOException {

    }

    /**
     *  
     * @throws IOException if an i/o error occurs
     */
    public void open() throws java.io.IOException {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     */
    public void open(int arg1) throws java.io.IOException {

    }

    /**
     * Returns the local port.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The local port.
     */
    public int getLocalPort() throws java.io.IOException {
        return 0;
    }

    /**
     * Returns the local address.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The local address.
     */
    public String getLocalAddress() throws java.io.IOException {
        return (java.lang.String) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The byte array.
     */
    public byte[] receive(int arg1) throws java.io.IOException {
        return (byte []) null;
    }

    /**
     *  
     * @throws IOException if an i/o error occurs
     * @return  The byte array.
     */
    public byte[] receive() throws java.io.IOException {
        return (byte []) null;
    }

} // end AndroidDatagramConnection
