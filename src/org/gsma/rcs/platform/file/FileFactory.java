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
 * Class FileFactory.
 */
public abstract class FileFactory {
    /**
     * Creates a new instance of FileFactory.
     */
    public FileFactory() {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The input stream.
     */
    public abstract java.io.InputStream openFileInputStream(String arg1) throws java.io.IOException;

    /**
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The output stream.
     */
    public abstract java.io.OutputStream openFileOutputStream(String arg1) throws java.io.IOException;

    /**
     * Returns the file description.
     *  
     * @param arg1 The arg1.
     * @throws IOException if an i/o error occurs
     * @return  The file description.
     */
    public abstract FileDescription getFileDescription(String arg1) throws java.io.IOException;

    /**
     *  
     * @param arg1 The arg1.
     */
    public abstract void updateMediaStorage(String arg1);

    /**
     *  
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public abstract boolean fileExists(String arg1);

    /**
     * Returns the factory.
     *  
     * @return  The factory.
     */
    public static FileFactory getFactory() {
        return (FileFactory) null;
    }

    /**
     * Creates the directory.
     *  
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public static boolean createDirectory(String arg1) {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public static void loadFactory(String arg1) throws org.gsma.rcs.platform.FactoryException {

    }

} // end FileFactory
