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

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.ImsSessionListener;

/**
 * IP call streaming session listener
 * 
 * @author opob7414
 */
public interface IPCallStreamingSessionListener extends ImsSessionListener  {
    
	/**
     * 486 Busy 
	 * @param contact Remote contact
     */
    public void handle486Busy(ContactId contact);

	/**
     * Call error
	 * @param contact Remote contact
	 * @param error Error
     */
    public void handleCallError(ContactId contact, IPCallError error);
    
    /**
     * Video stream has been resized
     *
     * @param width Video width
     * @param height Video height
     */
    public void handleVideoResized(int width, int height);
    
    /**
     * Add Video Invitation
     * @param contact Remote contact
     * @param videoEncoding
     * @param videoWidth
     * @param videoHeight
     */
    public void handleAddVideoInvitation(ContactId contact, String videoEncoding, int videoWidth, int videoHeight);

    /**
     * Remove Video Invitation
     * @param contact Remote contact
     *
     */
    public void handleRemoveVideo(ContactId contact);
	
	/**
     * Add Video accepted by user
	 * @param contact Remote contact
     *
     */
	public void handleAddVideoAccepted(ContactId contact);

	 /**
     * Add Video aborted 
	 * @param contact Remote contact
	 * @param errorCode
     */
	public void handleAddVideoAborted(ContactId contact, int errorCode);
	
	/**
     * Remove Video accepted (200OK response)
	 * @param contact Remote contact
     *
     */
	public void handleRemoveVideoAccepted(ContactId contact);

	/**
     * Remove Video aborted 
	 * @param contact Remote contact
     *
     */
	public void handleRemoveVideoAborted(ContactId contact, int code);
	

	/**
     * Call hold
	 * @param contact Remote contact
     *
     */
    public void handleCallHold(ContactId contact);

    /**
     * Call resume
     * @param contact Remote contact
     *
     */
    public void handleCallResume(ContactId contact);
	
	/**
     * Call Hold accepted 
	 * @param contact Remote contact
     *
     */
	public void handleCallHoldAccepted(ContactId contact);

	 /**
     * Call Hold aborted 
	 * @param contact Remote contact
	 * @param errorCode
     */
	public void handleCallHoldAborted(ContactId contact, int errorCode);
	
	/**
     * Call resume accepted (200OK response)
	 * @param contact Remote contact
     *
     */
	public void handleCallResumeAccepted(ContactId contact);

	/**
     * Call resume aborted 
	 * @param contact Remote contact
     */
	public void handleCallResumeAborted(ContactId contact);

	/**
	 * A session invitation has been received
	 * @param contact Remote contact
	 * @param audio Audio content
	 * @param video Video content
	 */
	public void handleSessionInvited(ContactId contact, AudioContent audio, VideoContent video);
}
