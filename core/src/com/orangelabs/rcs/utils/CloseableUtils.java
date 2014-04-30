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
package com.orangelabs.rcs.utils;

import java.io.Closeable;
import java.io.IOException;

public class CloseableUtils {
	
	/**
	 * Closes properly objects implementing Closeable (input stream, output stream...)
	 * 
	 * @param c
	 *            object to close or null
	 * @return IOException or null
	 * 
	 *         <p>
	 *         <b>Be Careful:</b><br />
	 *         In Android SDK 15 and earlier Cursor does not implement Closeable. So do not use with cursor.
	 *         </p>
	 */
	public static IOException close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException e) {
				return e;
			}
		}
		return null;
	}
}
