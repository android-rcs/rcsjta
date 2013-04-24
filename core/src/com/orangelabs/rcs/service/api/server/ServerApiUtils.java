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

package com.orangelabs.rcs.service.api.server;

import android.content.pm.PackageManager;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.service.api.client.ClientApiPermission;
import com.orangelabs.rcs.service.api.client.SessionState;

/**
 * Server API utils
 * 
 * @author jexa7410
 */
public class ServerApiUtils {
	/**
	 * Test permission
	 * 
	 * @throws SecurityException
	 */
	public static void testPermission() throws SecurityException {
		if (AndroidFactory.getApplicationContext().checkCallingOrSelfPermission(ClientApiPermission.RCS_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException();
	    }
	}
	
	/**
	 * Test permission for extensions
	 * 
	 * @throws SecurityException
	 */
	public static void testPermissionForExtensions() throws SecurityException {
		if (AndroidFactory.getApplicationContext().checkCallingOrSelfPermission(ClientApiPermission.RCS_EXTENSION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
			throw new SecurityException();
	    }
	}

	/**
	 * Test core
	 * 
	 * @throws ServerApiException
	 */
	public static void testCore() throws ServerApiException {
		if (Core.getInstance() == null) {
			throw new ServerApiException("Core is not instanciated");
		}
	}
	
	/**
	 * Test IMS connection
	 * 
	 * @throws ServerApiException
	 */
	public static void testIms() throws ServerApiException {
		if (!isImsConnected()) { 
			throw new ServerApiException("Core is not connected to IMS"); 
		}
	}
	
	/**
	 * Is IMS connected
	 * 
	 * @return IMS connection state
	 */
	public static boolean isImsConnected(){
		return ((Core.getInstance() != null) &&
				(Core.getInstance().getImsModule().getCurrentNetworkInterface() != null) &&
				(Core.getInstance().getImsModule().getCurrentNetworkInterface().isRegistered()));
	}
	
	/**
	 * Get session state
	 * 
	 * @return State (see class SessionState) 
	 */
	public static int getSessionState(ImsServiceSession session) {
		int result = SessionState.UNKNOWN;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Canceled: CANCEL received
				result = SessionState.CANCELLED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Established: ACK exchanged
				result = SessionState.ESTABLISHED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Terminated: BYE received
				result = SessionState.TERMINATED;
			} else {
				// Pending
				result = SessionState.PENDING;
			}
		}
		return result;
	}	
}
