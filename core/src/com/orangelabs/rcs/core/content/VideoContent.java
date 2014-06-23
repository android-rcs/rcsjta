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

import android.net.Uri;


/**
 * Video content
 * 
 * @author jexa7410
 */
public class VideoContent extends MmContent {
	
    /**
     * Height
     */
    private int height = 0;

    /**
     * Width
     */
    private int width = 0;

	/**
	 * Constructor
	 * 
	 * @aparam encoding Encoding
	 */
	public VideoContent(String encoding) {
		super(encoding);
	}

	/**
	 * Constructor
	 *
	 * @param videoFile URI
	 * @param encoding Encoding
	 * @param size Content size
	 * @param fileName Filename
	 */
	public VideoContent(Uri videoFile, String encoding, long size, String fileName) {
		super(videoFile, encoding, size, fileName);
	}

    /**
     * Set the width
     *
     * @param width width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Get the width
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Set the height
     *
     * @param height height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Get the height
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }
}
