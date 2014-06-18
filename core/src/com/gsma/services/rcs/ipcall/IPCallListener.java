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
package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * IP call event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class IPCallListener extends IIPCallListener.Stub {
	/**
	 * Callback called when the call state is changed
	 *
	 * @param contact Contact ID
	 * @param callId ID of the call
	 * @param state State of the call
	 * @param reasonCode Reason code for the IP call state
	 */
	public abstract void onIPCallStateChanged(ContactId contact, String callId, int state,
			int reasonCode);
}
