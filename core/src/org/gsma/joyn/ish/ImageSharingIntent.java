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
package org.gsma.joyn.ish;

/**
 * Intent for image sharing invitations
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingIntent {
    /**
     * Broadcast action: a new image sharing invitation has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_SHARING_ID} containing the unique ID of the image sharing.
     * <li> {@link #EXTRA_FILENAME} containing the filename of image to be shared.
     * <li> {@link #EXTRA_FILESIZE} containing the size of the image to be shared.
     * <li> {@link #EXTRA_FILETYPE} containing the MIME type of the image to be shared.
     * </ul>
     */
	public final static String ACTION_NEW_INVITATION = "org.gsma.joyn.ish.action.NEW_IMAGE_SHARING";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Unique ID of the image sharing
	 */
	public final static String EXTRA_SHARING_ID = "sharingId";

	/**
	 * Name of the file
	 */
	public final static String EXTRA_FILENAME = "filename";
	
	/**
	 * Size of the file in byte
	 */
	public final static String EXTRA_FILESIZE = "filesize";
	
	/**
	 * MIME type of the file
	 */
	public final static String EXTRA_FILETYPE = "filetype";
}
