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

package com.orangelabs.rcs.core.content;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileFactory;

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
	private Uri file;

	/**
	 * The filename
	 */
	private String fileName;

	/**
	 * Content size in bytes
	 */
	private long size;

	/**
	 * Encoding
	 */
	private String encoding;

	/**
	 * Data
	 */
	private byte[] data = null;

    /**
     * Stream to write received data direct to file.
     */
    private BufferedOutputStream out = null;

	private ParcelFileDescriptor pfd;

    /**
     * Constructor
     * 
     * @param encoding Encoding
     */
	public MmContent(String encoding) {
		this.encoding = encoding;
		this.size = -1;
	}

	/**
	 * Constructor
	 *
	 * @param fileName File name
	 * @param size Content size
	 * @param encoding Encoding
	 */
	public MmContent(String fileName, long size, String encoding) {
		this.fileName = fileName;
		this.size = size;
		this.encoding = encoding;
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
		this.file = file;
		this.encoding = encoding;
		this.size = size;
		this.fileName = fileName;
	}

    /**
	 * Returns the uri
	 *
	 * @return uri
	 */
	public Uri getUri() {
		return file;
	}

	/**
	 * Sets the uri
	 *
	 * @param file Uri
	 */
	public void setUri(Uri file) {
		this.file = file;
	}

    /**
     * Returns the content size in bytes
     * 
     * @return Size in bytes
     */
	public long getSize() {
		return size;
	}

    /**
     * Returns the content size in Kbytes
     * 
     * @return Size in Kbytes
     */
	public long getKbSize() {
		return size/1024;
	}

	/**
     * Returns the content size in Mbytes
     * 
     * @return Size in Mbytes
     */
	public long getMbSize(){
		return size/(1024*1024);
	}

    /**
     * Returns the encoding type
     * 
     * @return Encoding type
     */
	public String getEncoding() {
		return encoding;
	}

	/**
     * Set the encoding type
     * 
     * @param encoding Encoding type
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the codec from the encoding type
     * 
     * @return Codec name
     */
	public String getCodec() {
		int index = encoding.indexOf("/");
		if (index != -1) {
			return encoding.substring(index+1);
		} else {
			return encoding;
		}
    }

	/**
     * Get the name
     * 
     * @return Name
     */
	public String getName() {
		return fileName;
    }
	
	/**
     * Set the name
     * 
     * @return Name
     */
	public void setName(String fileName) {
		this.fileName = fileName;
    }

	/**
     * Returns the string representation of a content
     * 
     * @return String
     */
	public String toString() {
		return file + " (" + size + " bytes)";
	}

	/**
     * Returns the content data
     * 
     * @return Data
     */
	public byte[] getData() {
		return data;
	}

	/**
     * Sets the content data
     * 
     * @param Data
     */
	public void setData(byte[] data) {
		this.data = data;
	}

    /**
     * Write data chunk to file
     *
     * @param data Data to append to file
     * @throws IOException
     */
	public void writeData2File(byte[] data) throws IOException, IllegalArgumentException {
		if (out == null) {
			pfd = AndroidFactory.getApplicationContext().getContentResolver()
					.openFileDescriptor(file, "w");
			// To optimize I/O set buffer size to 8kBytes
			out = new BufferedOutputStream(new FileOutputStream(pfd.getFileDescriptor()), 8 * 1024);
		}
		out.write(data);
	}

    /**
     * Close written file and update media storage.
     *
     * @throws IOException
     */
    public void closeFile() throws IOException {
        try {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
                FileFactory.getFactory().updateMediaStorage(getUri().getEncodedPath());
            }
        } finally {
            if (pfd != null) {
                pfd.close();
            }
        }
    }

    /**
     * Delete File.
     *
     * @throws IOException
     */
	public void deleteFile() throws IOException {
		if (out != null) {
			try {
				out.close();
				out = null;
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
