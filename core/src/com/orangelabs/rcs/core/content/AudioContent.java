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
 * Audio content
 * 
 * @author opob7414
 */
public class AudioContent extends MmContent {

	/**
	 * Constructor
	 * 
	 * @param url URL
	 * @aparam encoding Encoding
	 * @param size Content size
	 */
	public AudioContent(String url, String encoding, long size) {
		super(url, encoding, size);
	}

	/**
	 * Constructor
	 * 
	 * @param url URL
	 * @aparam encoding Encoding
	 */
	public AudioContent(String url, String encoding) {
		super(url, encoding);
	}

}
