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
 * Visit Card content
 * 
 * @author vfml3370
 */
public class VisitCardContent extends MmContent {

	/**
	 * Constructor
	 *
	 * @param visitCardFile URI
	 * @param encoding the mime-type encoding
	 * @param size Content size
	 * @param fileName The filename
	 */
	public VisitCardContent(Uri visitCardFile, String encoding, long size, String fileName) {
		super(visitCardFile, encoding, size, fileName);
	}
}
