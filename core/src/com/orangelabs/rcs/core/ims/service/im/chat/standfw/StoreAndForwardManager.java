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
package com.orangelabs.rcs.core.ims.service.im.chat.standfw;

import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Store & forward manager
 */
public class StoreAndForwardManager {
	/**
	 * Store & forward service URI
	 */
	public final static String SERVICE_URI = "rcse-standfw@";
	
    /**
     * IMS service
     */
    private ImsService imsService;
	
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Constructor
     * 
     * @param imsService IMS service
     */    
    public StoreAndForwardManager(ImsService imsService) {
    	this.imsService = imsService;
    }
    
    /**
     * Receive stored messages
     * 
     * @param invite Received invite
     */
    public void receiveStoredMessages(SipRequest invite) {
    	if (logger.isActivated()) {
			logger.debug("Receive stored messages");
		}    	
    	
		// Create a new session
    	TerminatingStoreAndForwardMsgSession session = new TerminatingStoreAndForwardMsgSession(imsService, invite);
    	
		// Start the session
		session.startSession();
		
		// Notify listener
		imsService.getImsModule().getCore().getListener().handleStoreAndForwardMsgSessionInvitation(session);
    }    

    /**
     * Receive stored notifications
     * 
     * @param invite Received invite
     */
    public void receiveStoredNotifications(SipRequest invite) {
    	if (logger.isActivated()) {
			logger.debug("Receive stored notifications");
		}    	
    	
		// Create a new session
    	TerminatingStoreAndForwardNotifSession session = new TerminatingStoreAndForwardNotifSession(
    			imsService,
				invite);
		
		// Start the session
		session.startSession();
    }    
}
