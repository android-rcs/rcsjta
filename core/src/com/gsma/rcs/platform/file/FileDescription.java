/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.platform.file;

/**
 * File description
 * 
 * @author jexa7410
 */
public class FileDescription {

    private String mName;

    private long mSize = -1;

    private boolean mDirectory = false;

    /**
     * Constructor
     */
    public FileDescription(String name, long size) {
        mName = name;
        mSize = size;
    }

    /**
     * Constructor
     */
    public FileDescription(String name, long size, boolean directory) {
        mName = name;
        mSize = size;
        mDirectory = directory;
    }

    /**
     * Returns the size of the file
     * 
     * @return File size
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Returns the name of the file
     * 
     * @return File name
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the size of the file
     * 
     * @return File size
     */
    public boolean isDirectory() {
        return mDirectory;
    }

}
