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
 * Class AndroidSocketConnection.
 */
public class AndroidSocketConnection implements SocketConnection {
    /**
     * Creates a new instance of AndroidSocketConnection.
     */
    public AndroidSocketConnection() {

    }

    /**
     * Creates a new instance of AndroidSocketConnection.
     *  
     * @param arg1 The arg1.
     */
    public AndroidSocketConnection(java.net.Socket arg1) {

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
     * @throws IOException if an i/o error occurs
     */
    public void open(String arg1, int arg2) throws java.io.IOException {

    }

    /**
     * Returns the input stream.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The input stream.
     */
    public java.io.InputStream getInputStream() throws java.io.IOException {
        return (java.io.InputStream) null;
    }

    /**
     * Returns the output stream.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The output stream.
     */
    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        return (java.io.OutputStream) null;
    }

    /**
     * Returns the so timeout.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The so timeout.
     */
    public int getSoTimeout() throws java.io.IOException {
        return 0;
    }

    /**
     * Sets the so timeout.
     *  
     * @param arg1 The so timeout.
     * @throws IOException if an i/o error occurs
     */
    public void setSoTimeout(int arg1) throws java.io.IOException {

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
     * Returns the remote address.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The remote address.
     */
    public String getRemoteAddress() throws java.io.IOException {
        return (java.lang.String) null;
    }

    /**
     * Returns the remote port.
     *  
     * @throws IOException if an i/o error occurs
     * @return  The remote port.
     */
    public int getRemotePort() throws java.io.IOException {
        return 0;
    }

    /**
     * Sets the socket.
     *  
     * @param arg1 The socket.
     */
    public void setSocket(java.net.Socket arg1) {

    }

    /**
     * Returns the socket.
     *  
     * @return  The socket.
     */
    public java.net.Socket getSocket() {
        return (java.net.Socket) null;
    }

} // end AndroidSocketConnection
