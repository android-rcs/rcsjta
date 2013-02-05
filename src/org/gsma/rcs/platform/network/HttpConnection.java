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
 * Class HttpConnection.
 */
public interface HttpConnection {
    /**
     * Constant GET_METHOD.
     */
    public static final String GET_METHOD = "GET";

    /**
     * Constant POST_METHOD.
     */
    public static final String POST_METHOD = "POST";

    /**
     *  
     * @throws IOException if an i/o error occurs
     * @return  The byte array output stream.
     */
    public java.io.ByteArrayOutputStream get() throws java.io.IOException;

    /**
     *  
     * @throws IOException if an i/o error occurs
     */
    public void close() throws java.io.IOException;

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     */
    public void open(String arg1) throws java.io.IOException;

    /**
     *  
     * @throws IOException if an i/o error occurs
     * @return  The byte array output stream.
     */
    public java.io.ByteArrayOutputStream post() throws java.io.IOException;

} // end HttpConnection
