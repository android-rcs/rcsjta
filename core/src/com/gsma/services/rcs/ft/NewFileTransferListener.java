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
package com.gsma.services.rcs.ft;


/**
 * New file transfer invitations event and delivery reports listener
 *  
 * @author Jean-Marc AUFFRET
 */
public abstract class NewFileTransferListener extends INewFileTransferListener.Stub {
	/**
	 * Callback method for new file transfer invitations
	 * 
	 * @param transferId Transfer ID
	 */
	public abstract void onNewFileTransfer(String transferId);
	
	/**
	 * Callback called when the file has been delivered
	 * 
	 * @param transferId Transfer ID
	 */
	public abstract void onReportFileDelivered(String transferId);
	
	/**
	 * Callback called when the file has been displayed
	 * 
	 * @param transferId Transfer ID
	 */
	public abstract void onReportFileDisplayed(String transferId);
	
}
