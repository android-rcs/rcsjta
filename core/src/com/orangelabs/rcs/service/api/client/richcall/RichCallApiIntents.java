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

package com.orangelabs.rcs.service.api.client.richcall;

/**
 * Rich call API intents
 * 
 * @author jexa7410
 */
public class RichCallApiIntents {
	/**
     * Intent broadcasted when a new image sharing invitation has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     *   <li><em>sessionId</em> - Session ID of the file transfer session.</li>
     *   <li><em>filename</em> - Name of the file.</li>
     *   <li><em>filesize</em> - Size of the file in bytes.</li>
     *   <li><em>filetype</em> - Type of file encoding.</li>
     * </ul>
     * </ul>
     */
	public final static String IMAGE_SHARING_INVITATION = "com.orangelabs.rcs.richcall.IMAGE_SHARING_INVITATION";

    /**
     * Intent broadcasted when a new video sharing invitation has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     *   <li><em>sessionId</em> - Session ID of the file transfer session.</li>
     *   <li><em>videotype</em> - Type of video encoding.</li>
     *   <li><em>videowidth</em> - Width of video.</li>
     *   <li><em>videoheight</em> - Height of video.</li>
     * </ul>
     * </ul>
     */
	public final static String VIDEO_SHARING_INVITATION = "com.orangelabs.rcs.richcall.VIDEO_SHARING_INVITATION";
	
	/**
     * Intent broadcasted when a new geoloc sharing invitation has been received
     * 
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>contact</em> - Contact phone number.</li>
     *   <li><em>contactDisplayname</em> - Display name associated to the contact.</li>
     * </ul>
     * </ul>
     */
	public final static String GEOLOC_SHARING_INVITATION = "com.orangelabs.rcs.richcall.GEOLOC_SHARING_INVITATION";	
}
