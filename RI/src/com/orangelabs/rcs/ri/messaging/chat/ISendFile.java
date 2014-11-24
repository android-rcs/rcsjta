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
package com.orangelabs.rcs.ri.messaging.chat;

import android.net.Uri;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.ft.FileTransferService;


/**
 * @author YPLO6403
 *
 */
public interface ISendFile {

	/**
	 * Transfer file
	 * @param file
	 *            Uri of file to transfer
	 * @param fileicon
	 *            File icon option. If true, the stack tries to attach fileicon.
	 * @return True if file transfer is successful
	 */
	boolean transferFile(Uri file, boolean fileicon);
	
	/**
	 * Add file transfer event listener
	 * 
	 * @param fileTransferService
	 */
	void addFileTransferEventListener(FileTransferService fileTransferService) throws RcsServiceException;

	/**
	 * Remove file transfer event listener
	 * 
	 * @param fileTransferService
	 */
	void removeFileTransferEventListener(FileTransferService fileTransferService) throws RcsServiceException;

}
