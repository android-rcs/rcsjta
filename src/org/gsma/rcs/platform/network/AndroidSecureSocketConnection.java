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
 * Class AndroidSecureSocketConnection.
 */
public class AndroidSecureSocketConnection extends AndroidSocketConnection {
    /**
     * Creates a new instance of AndroidSecureSocketConnection.
     */
    public AndroidSecureSocketConnection() {
        super();
    }

    /**
     * Creates a new instance of AndroidSecureSocketConnection.
     *  
     * @param arg1 The arg1.
     */
    public AndroidSecureSocketConnection(javax.net.ssl.SSLSocket arg1) {
        super();
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
     * Returns the fingerprint.
     *  
     * @return  The fingerprint.
     */
    public String getFingerprint() {
        return (java.lang.String) null;
    }

} // end AndroidSecureSocketConnection
