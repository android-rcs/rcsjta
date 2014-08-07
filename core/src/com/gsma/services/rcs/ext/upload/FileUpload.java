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
package com.gsma.services.rcs.ext.upload;

import android.net.Uri;

import com.gsma.services.rcs.JoynServiceException;

/**
 * File upload
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUpload {

    /**
     * File upload state
     */
    public static class State {
    	/**
    	 * Inactive state
    	 */
    	public final static int INACTIVE = 0;

    	/**
    	 * Upload is started
    	 */
    	public final static int STARTED = 3;
    	
    	/**
    	 * File has been transferred with success 
    	 */
    	public final static int TRANSFERRED = 4;
    	
    	/**
    	 * Upload has been aborted 
    	 */
    	public final static int ABORTED = 5;
    	
    	/**
    	 * Upload has failed 
    	 */
    	public final static int FAILED = 6;

        private State() {
        }    	
    }
    
    /**
     * File upload error
     */
    public static class Error {
    	/**
    	 * Transfer has failed
    	 */
    	public final static int TRANSFER_FAILED = 0;
    	
        private Error() {
        }    	
    }

    /**
     * File upload interface
     */
    private IFileUpload uploadInf;
    
    /**
     * Constructor
     * 
     * @param uploadInf Upload interface
     */
    FileUpload(IFileUpload uploadInf) {
    	this.uploadInf = uploadInf;
    }
    	
    /**
	 * Returns the upload ID of the file
	 * 
	 * @return Upload ID
	 * @throws JoynServiceException
	 */
	public String getUploadId() throws JoynServiceException {
		try {
			return uploadInf.getUploadId();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the URI of the uploaded file
	 *
	 * @return Uri
	 * @throws JoynServiceException
	 */
	public Uri getFile() throws JoynServiceException {
		try {
			return uploadInf.getFile();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns info related to upload file
	 *
	 * @return Upload info or null if not yet upload or in case of error
	 * @see FileUploadInfo
	 * @throws JoynServiceException
	 */
	public FileUploadInfo getUploadInfo() throws JoynServiceException {
		try {
			return uploadInf.getUploadInfo();
		} catch (Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}

	/**
	 * Returns the state of the upload
	 * 
	 * @return State
	 * @see FileUpload.State
	 * @throws JoynServiceException
	 */
	public int getState() throws JoynServiceException {
		try {
			return uploadInf.getState();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}		

	/**
	 * Aborts the upload
	 * 
	 * @throws JoynServiceException
	 */
	public void abortUpload() throws JoynServiceException {
		try {
			uploadInf.abortUpload();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
