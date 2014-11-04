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
package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Geoloc sharing event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class GeolocSharingListener extends IGeolocSharingListener.Stub {
	/**
	 * Callback called when the geoloc sharing state changes
	 *
	 * @param contact Contact ID
	 * @param sharingId ID of geoloc sharing
	 * @param state State of image sharing
	 * @param reasonCode Reason code of geoloc sharing state
	 */
	public abstract void onStateChanged(ContactId contact, String sharingId,
			int state, int reasonCode);

	/**
	 * Callback called during the sharing progress
	 *
	 * @param contact Contact ID
	 * @param sharingId ID of geoloc sharing
	 * @param currentSize Current transferred size in bytes
	 * @param totalSize Total size to transfer in bytes
	 */
	public abstract void onProgressUpdate(ContactId contact, String sharingId, long currentSize, long totalSize);
}
