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

package org.gsma.rcs.platform.file;

/**
 * Class AndroidFileFactory.
 */
public class AndroidFileFactory extends FileFactory {
    /**
     * Creates a new instance of AndroidFileFactory.
     */
    public AndroidFileFactory() {
        super();
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The input stream.
     */
    public java.io.InputStream openFileInputStream(String arg1) throws java.io.IOException {
        return (java.io.InputStream) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The output stream.
     */
    public java.io.OutputStream openFileOutputStream(String arg1) throws java.io.IOException {
        return (java.io.OutputStream) null;
    }

    /**
     * Returns the file description.
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The file description.
     */
    public FileDescription getFileDescription(String arg1) throws java.io.IOException {
        return (FileDescription) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void updateMediaStorage(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public boolean fileExists(String arg1) {
        return false;
    }

} // end AndroidFileFactory
