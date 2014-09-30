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

package com.orangelabs.rcs.core.ims.service.ipcall;

import com.orangelabs.rcs.core.ims.service.ImsSessionListener;

/**
 * IP call streaming session listener
 * 
 * @author opob7414
 */
public interface IPCallStreamingSessionListener extends ImsSessionListener  {
    
	/**
     * 486 Busy 
     */
    public void handle486Busy();

	/**
     * Call error
     * 
     * @param error Error
     */
    public void handleCallError(IPCallError error);
    
    /**
     * Video stream has been resized
     *
     * @param width Video width
     * @param height Video height
     */
    public void handleVideoResized(int width, int height);
    
    /**
     * Add Video Invitation
     *
     * @param videoEncoding
     * @param videoWidth
     * @param videoHeight
     */
    public void handleAddVideoInvitation(String videoEncoding, int videoWidth, int videoHeight);

    /**
     * Remove Video Invitation
     *
     */
    public void handleRemoveVideo();  
	
	/**
     * Add Video accepted by user
     *
     */
	public void handleAddVideoAccepted();

	 /**
     * Add Video aborted 
     *
     * @param errorCode
     */
	public void handleAddVideoAborted(int errorCode);
	
	/**
     * Remove Video accepted (200OK response)
     *
     */
	public void handleRemoveVideoAccepted();

	/**
     * Remove Video aborted 
     *
     */
	public void handleRemoveVideoAborted(int code);
	

	/**
     * Call hold
     *
     */
    public void handleCallHold();

    /**
     * Call resume
     *
     */
    public void handleCallResume();  
	
	/**
     * Call Hold accepted 
     *
     */
	public void handleCallHoldAccepted();

	 /**
     * Call Hold aborted 
     *
     * @param errorCode
     */
	public void handleCallHoldAborted(int errorCode);
	
	/**
     * Call resume accepted (200OK response)
     *
     */
	public void handleCallResumeAccepted();

	/**
     * Call resume aborted 
     *
     */
	public void handleCallResumeAborted();
}
