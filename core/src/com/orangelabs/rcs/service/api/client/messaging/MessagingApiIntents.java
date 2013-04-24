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

package com.orangelabs.rcs.service.api.client.messaging;

/**
 * Messaging API intents 
 * 
 * @author jexa7410
 */
public interface MessagingApiIntents {
    /**
     * Intent broadcasted when a new file transfer invitation has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     *   <li><em>sessionId</em> - Session ID of the file transfer session.</li>
     *   <li><em>chatSessionId</em> - Session ID of the chat session associated to the file
     *    transfer (may be null if the file transfer is outside of a chat).</li>
     *   <li><em>filename</em> - Name of the file.</li>
     *   <li><em>filesize</em> - Size of the file in bytes.</li>
     *   <li><em>filetype</em> - Type of file encoding.</li>
     *   <li><em>thumbnail</em> - Path for the file thumbnail.</li>
     * </ul>
     * </ul>
     */
	public final static String FILE_TRANSFER_INVITATION = "com.orangelabs.rcs.messaging.FILE_TRANSFER_INVITATION";
	
    /**
     * Intent broadcasted when a new chat invitation has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     *   <li><em>sessionId</em> - Session ID of the file transfer session.</li>
     *   <li><em>isGroupChat</em> - Boolean indicating if it's a group chat.</li>
     *   <li><em>replacedSessionId</em> - Session ID of the session which has been replaced by
     *    this group chat session (may be null).</li>
     *   <li><em>firstMessage</em> - First message of the session (only for 1-1 chat).</li>
     *   <li><em>subject</em> - Subject of the session (only for group chat).</li>
     * </ul>
     * </ul>
     */
	public final static String CHAT_INVITATION = "com.orangelabs.rcs.messaging.CHAT_INVITATION";
	
    /**
     * Intent broadcasted when a 1-1 chat session has been replaced by a group chat session
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>sessionId</em> - Session ID of the file transfer session.</li>
     *   <li><em>replacedSessionId</em> - Session ID of the session which has been replaced by this group chat session.</li>
     * </ul>
     * </ul>
     */
	public final static String CHAT_SESSION_REPLACED = "com.orangelabs.rcs.messaging.CHAT_SESSION_REPLACED";
}
