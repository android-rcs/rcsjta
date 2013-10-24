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
package org.gsma.joyn.ft;

/**
 * Intent for file transfer invitation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferIntent {
    /**
     * Broadcast action: a new file transfer has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_TRANSFER_ID} containing the unique ID of the file transfer.
     * <li> {@link #EXTRA_FILENAME} containing the filename of file to be transferred.
     * <li> {@link #EXTRA_FILESIZE} containing the size of the file to be transferred.
     * <li> {@link #EXTRA_FILETYPE} containing the MIME type of the file to be transferred.
     * <li> {@link #EXTRA_FILEICON} containing the filename of the file icon associated to the file to be transferred. 
     * </ul>
     */
	public final static String ACTION_NEW_INVITATION = "org.gsma.joyn.ft.action.NEW_FILE_TRANSFER";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation (extracted from the SIP address)
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Unique ID of the file transfer
	 */
	public final static String EXTRA_TRANSFER_ID = "transferId";

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

	/**
	 * Name of the file icon
	 */
	public final static String EXTRA_FILEICON = "fileicon";
}
