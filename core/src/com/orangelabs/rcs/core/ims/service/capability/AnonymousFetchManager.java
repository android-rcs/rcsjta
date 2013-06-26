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

package com.orangelabs.rcs.core.ims.service.capability;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ContactInfo;
import com.orangelabs.rcs.core.ims.service.presence.PresenceUtils;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfParser;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Tuple;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Capability discovery manager using anonymous fetch procedure
 * 
 * @author Jean-Marc AUFFRET
 */
public class AnonymousFetchManager implements DiscoveryManager {
	   /**
     * IMS module
     */
    private ImsModule imsModule;
    
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public AnonymousFetchManager(ImsModule parent) {
        this.imsModule = parent;
    }
	
	/**
     * Request contact capabilities
     * 
     * @param contact Remote contact
     * @return Returns true if success
     */
    public boolean requestCapabilities(String contact) {
    	if (logger.isActivated()) {
    		logger.debug("Request capabilities in background for " + contact);
    	}
		AnonymousFetchRequestTask task = new AnonymousFetchRequestTask(imsModule, contact);
		task.start();
		return true;
	}
	
	/**
     * Receive a notification
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
    	if (logger.isActivated()) {
			logger.debug("Anonymous fetch notification received");
		}
    	
		// Parse XML part
	    byte[] content = notify.getContentBytes();
	    if (content != null) {
	    	if (logger.isActivated()) {
	    		logger.debug("Anonymous fetch notification with PIDF document");
	    	}
	    	try {
		    	InputSource pidfInput = new InputSource(new ByteArrayInputStream(content));
		    	PidfParser pidfParser = new PidfParser(pidfInput);
		    	PidfDocument presence = pidfParser.getPresence();
		    	if (presence != null) {
		    		// Extract capabilities
			    	Capabilities capabilities =  new Capabilities();
			    	
			    	// We queried via anonymous fetch procedure, so set presence discovery to true
			    	capabilities.setPresenceDiscoverySupport(true);
			    	
		    		String contact = presence.getEntity();
		    		Vector<Tuple> tuples = presence.getTuplesList();
		    		for(int i=0; i < tuples.size(); i++) {
		    			Tuple tuple = (Tuple)tuples.elementAt(i);
		    			boolean state = false; 
		    			if (tuple.getStatus().getBasic().getValue().equals("open")) {
		    				state = true;
		    			}
		    			String id = tuple.getService().getId();
		    			if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_VIDEO_SHARE)) {
		    				capabilities.setVideoSharingSupport(state);
		    			} else
	    				if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_IMAGE_SHARE)) {
	    					capabilities.setImageSharingSupport(state);
	    				} else
    					if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_FT)) {
    						capabilities.setFileTransferSupport(state);
    					} else
						if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_CS_VIDEO)) {
							capabilities.setCsVideoSupport(state);
						} else
						if (id.equalsIgnoreCase(PresenceUtils.FEATURE_RCS2_CHAT)) {
							capabilities.setImSessionSupport(state);
						}
		    		}

		    		// Update capabilities in database
		    		ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.RCS_CAPABLE, ContactInfo.REGISTRATION_STATUS_UNKNOWN);

		    		// Notify listener
		    		imsModule.getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
		    	}
	    	} catch(Exception e) {
	    		if (logger.isActivated()) {
	    			logger.error("Can't parse XML notification", e);
	    		}
	    	}    	
	    } else {
	    	if (logger.isActivated()) {
	    		logger.debug("Anonymous fetch notification is empty");
	    	}
	    	String contact = PhoneUtils.extractNumberFromUri(SipUtils.getAssertedIdentity(notify));

	    	// Notify content was empty 
	    	Capabilities capabilities = new Capabilities();

	    	// Update capabilities in database
	    	ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.NO_INFO, ContactInfo.REGISTRATION_STATUS_UNKNOWN);

	    	// Notify listener
	    	imsModule.getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
	    }
    }	
}
