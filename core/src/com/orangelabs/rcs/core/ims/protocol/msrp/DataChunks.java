/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Data chunks
 *
 * @author jexa7410
 */
public class DataChunks {
    /**
     * Current transfered size in bytes
     */
    private int currentSize = 0;

    /**
	 * Cache used to save data chunks
	 */
	private ByteArrayOutputStream cache = new ByteArrayOutputStream();

	/**
	 * Constructor
	 */
	public DataChunks() {
	}

	/**
	 * Add a new chunk
	 *
	 * @param data Data chunk
	 */
	public void addChunk(byte[] data) throws IOException, MsrpException {
        try {
		cache.write(data, 0, data.length);
        } catch (OutOfMemoryError e) {
            throw new MsrpException("Not enough memory to save data");
        }
		currentSize += data.length;
	}

	/**
     * Get received data
     *
     * @return Byte array
     */
    public byte[] getReceivedData() throws IOException, MsrpException {
    	byte[] result=null;
    	try {
			result = cache.toByteArray();
		} catch (OutOfMemoryError e) {
            throw new MsrpException("Not enough memory to copy data");
		}
        return result;
    }

	/**
     * Reset the cache
     */
    public void resetCache() {
    	cache.reset();
    }

    /**
	 * Returns the current size of the received chunks
	 *
	 * @return Size in bytes
	 */
	public int getCurrentSize() {
		return currentSize;
	}
}
