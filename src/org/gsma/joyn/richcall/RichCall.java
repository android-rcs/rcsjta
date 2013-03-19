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

package org.gsma.joyn.richcall;

import android.content.Context;
import android.net.Uri;
import java.lang.String;

/**
 * Rich call history. This content provider removes old messages if there is no enough space.
 *
 * @author mhsm6403 (Orange)
 * @version 1.0
 * @since 1.0
 */
public class RichCall {

	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
    public static synchronized void createInstance(Context ctx) { };

	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
    public static RichCall getInstance() {
        return (RichCall) null;
    };

	/**
	 * Add a new entry in the call history 
	 * <p>
   * FIXME: java.lang.object should be a MmContent object - missed import from Orange RCS stack
   * 
	 * @param contact Remote contact
	 * @param sessionId Session ID
	 * @param direction Call event direction
	 * @param content Shared content
	 * @param status Call status   
	 */
    public Uri addCall(String contact, String sessionId, int direction, java.lang.Object content, int status) {
        return (Uri) null;
    }

	/**
	 * Delete entry from its date in the call history
	 * 
	 * @param contact Contact id
	 * @param date Date
	 */
    public void removeCall(String contact, long date) { };

	/**
	 * Update the status of an entry in the call history
	 * 
	 * @param sessionId Session ID of the entry
	 * @param status New status
	 */

    public void setStatus(String sessionId, int status) { };

}
