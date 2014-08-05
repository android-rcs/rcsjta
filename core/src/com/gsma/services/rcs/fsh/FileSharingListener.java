/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.gsma.services.rcs.fsh;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * File sharing event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class FileSharingListener extends IFileSharingListener.Stub {
	/**
	 * Callback called when the sharing state changes
	 *
	 * @param contactId Contact ID
	 * @param sharingId ID of file sharing
	 * @param state State of file sharing 
	 */
	public abstract void onFileSharingStateChanged(ContactId contact, String sharingId, int state);

	/**
	 * Callback called during the sharing progress
	 *
	 * @param contactId Contact ID
	 * @param sharingId ID of file sharing
	 * @param currentSize Current transferred size in bytes
	 * @param totalSize Total size to transfer in bytes
	 */
	public abstract void onFileSharingProgress(ContactId contact, String sharingId, long currentSize, long totalSize);
}
