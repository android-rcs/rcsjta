/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.core.content;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.file.FileFactory;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Multimedia content
 * 
 * @author jexa7410
 */
public abstract class MmContent {
    /**
     * Content uri
     */
    private Uri mFile;

    /**
     * The filename
     */
    private String mFileName;

    /**
     * Content size in bytes
     */
    private long mSize;

    /**
     * Encoding
     */
    private String mEncoding;

    /**
     * Data
     */
    private byte[] mData;

    /**
     * Stream to write received data direct to file.
     */
    private BufferedOutputStream mOut;

    private ParcelFileDescriptor mPfd;

    /**
     * Constructor
     * 
     * @param encoding Encoding
     */
    public MmContent(String encoding) {
        mEncoding = encoding;
        mSize = -1;
    }

    /**
     * Constructor
     * 
     * @param fileName File name
     * @param size Content size
     * @param encoding Encoding
     */
    public MmContent(String fileName, long size, String encoding) {
        mFileName = fileName;
        mSize = size;
        mEncoding = encoding;
    }

    /**
     * Constructor
     * 
     * @param file Uri
     * @param encoding Encoding
     * @param size Content size
     * @param fileName File name
     */
    public MmContent(Uri file, String encoding, long size, String fileName) {
        mFile = file;
        mEncoding = encoding;
        mSize = size;
        mFileName = fileName;
    }

    /**
     * Returns the uri
     * 
     * @return uri
     */
    public Uri getUri() {
        return mFile;
    }

    /**
     * Sets the uri
     * 
     * @param file Uri
     */
    public void setUri(Uri file) {
        mFile = file;
    }

    /**
     * Returns the content size in bytes
     * 
     * @return Size in bytes
     */
    public long getSize() {
        return mSize;
    }

    /**
     * Returns the content size in Kbytes
     * 
     * @return Size in Kbytes
     */
    public long getKbSize() {
        return mSize / 1024;
    }

    /**
     * Returns the content size in Mbytes
     * 
     * @return Size in Mbytes
     */
    public long getMbSize() {
        return mSize / (1024 * 1024);
    }

    /**
     * Returns the encoding type
     * 
     * @return Encoding type
     */
    public String getEncoding() {
        return mEncoding;
    }

    /**
     * Set the encoding type
     * 
     * @param encoding Encoding type
     */
    public void setEncoding(String encoding) {
        mEncoding = encoding;
    }

    /**
     * Returns the codec from the encoding type
     * 
     * @return Codec name
     */
    public String getCodec() {
        int index = mEncoding.indexOf("/");
        if (index != -1) {
            return mEncoding.substring(index + 1);
        } else {
            return mEncoding;
        }
    }

    /**
     * Get the name
     * 
     * @return Name
     */
    public String getName() {
        return mFileName;
    }

    /**
     * Set the name
     * 
     * @param fileName
     */
    public void setName(String fileName) {
        mFileName = fileName;
    }

    /**
     * Returns the string representation of a content
     * 
     * @return String
     */
    public String toString() {
        return mFile + " (" + mSize + " bytes)";
    }

    /**
     * Returns the content data
     * 
     * @return Data
     */
    public byte[] getData() {
        return mData;
    }

    /**
     * Sets the content data
     * 
     * @param data
     */
    public void setData(byte[] data) {
        mData = data;
    }

    /**
     * Write data chunk to file
     * 
     * @param data Data to append to file
     * @throws IOException
     */
    public void writeData2File(byte[] data) throws IOException {
        if (mOut == null) {
            mPfd = AndroidFactory.getApplicationContext().getContentResolver()
                    .openFileDescriptor(mFile, "w");
            // To optimize I/O set buffer size to 8kBytes
            mOut = new BufferedOutputStream(new FileOutputStream(mPfd.getFileDescriptor()),
                    8 * 1024);
        }
        mOut.write(data);
    }

    /**
     * Close written file and update media storage.
     * 
     * @throws IOException
     */
    public void closeFile() throws IOException {
        try {
            if (mOut != null) {
                mOut.flush();
                mOut.close();
                mOut = null;
                FileFactory.getFactory().updateMediaStorage(getUri().getEncodedPath());
            }
        } finally {
            if (mPfd != null) {
                mPfd.close();
            }
        }
    }

    /**
     * Delete File.
     * 
     * @throws IOException
     */
    public void deleteFile() throws IOException {
        if (mOut != null) {
            try {
                mOut.close();
                mOut = null;
            } finally {
                Uri fileToDelete = getUri();
                if (ContentResolver.SCHEME_FILE.equals(fileToDelete.getScheme())) {
                    File file = new File(fileToDelete.getPath());
                    if (file != null) {
                        if (!file.delete()) {
                            throw new IOException("Unable to delete file: "
                                    + file.getAbsolutePath());
                        }
                    }
                } else {
                    throw new IOException("Not possible to delete file: " + fileToDelete);
                }
            }
        }
    }
}
