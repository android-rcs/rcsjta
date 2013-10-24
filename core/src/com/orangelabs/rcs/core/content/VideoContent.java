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

package com.orangelabs.rcs.core.content;


/**
 * Video content
 * 
 * @author jexa7410
 */
public class VideoContent extends MmContent {
	/**
	 * Encoding type
	 */
	public static final String ENCODING = "video/*";
	
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
	 * @param url URL
	 * @aparam encoding Encoding
	 * @param size Content size
	 */
	public VideoContent(String url, String encoding, long size) {
		super(url, encoding, size);
	}

	/**
	 * Constructor
	 * 
	 * @param url URL
	 * @aparam encoding Encoding
	 */
	public VideoContent(String url, String encoding) {
		super(url, encoding);
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
