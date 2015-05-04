/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap;

import java.io.Closeable;
import java.io.IOException;

/**
 * Low level service that manages the client-server connection.
 */
public interface IoService extends Closeable {

    /**
     * Returns true if the underlying connection is valid and open.
     * 
     * @return
     */
    public boolean isConnected();

    /**
     * Opens the connection to the remote server.
     * 
     * @throws IOException
     */
    public void connect() throws IOException;

    /**
     * Read the next line. This method is blocking.
     * 
     * @return
     * @throws IOException
     */
    public String readLine() throws IOException;

    /**
     * Sends a payload to the server.
     * 
     * @param payload
     * @throws IOException
     */
    public void writeln(String payload) throws IOException;

    /**
     * SSL handshake. If a socket connection is currently used it will be converted to a SSL
     * equivalent.
     * 
     * @throws IOException
     */
    public void startTLSHandshake() throws IOException;

    /**
     * Read the exact number of characters. Blocking method.
     * 
     * @param size
     * @return
     * @throws IOException
     */
    public String read(int size) throws IOException;

}
