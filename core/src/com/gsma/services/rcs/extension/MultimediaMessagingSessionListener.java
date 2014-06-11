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
package com.gsma.services.rcs.extension;

/**
 * This class offers callback methods on multimedia messaging session events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class MultimediaMessagingSessionListener extends IMultimediaMessagingSessionListener.Stub {
	/**
	 * Callback called when the session is pending.
	 */
	public abstract void onSessionRinging();
	
	/**
	 * Callback called when the session is started
	 */
	public abstract void onSessionStarted();
	
	/**
	 * Callback called when the session has been aborted or terminated
	 */
	public abstract void onSessionAborted();
	
	/**
	 * Callback called when the session has failed
	 * 
	 * @param error Error
	 * @see MultimediaSession.Error
	 */
	public abstract void onSessionError(int error);
	
	/**
	 * Callback called when a new message has been received
	 * 
	 * @param content Message content
	 */
	public abstract void onNewMessage(byte[] content);
}
