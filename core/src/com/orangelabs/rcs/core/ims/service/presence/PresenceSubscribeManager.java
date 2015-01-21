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

package com.orangelabs.rcs.core.ims.service.presence;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import javax2.sip.header.AcceptHeader;
import javax2.sip.header.EventHeader;
import javax2.sip.header.SubscriptionStateHeader;
import javax2.sip.header.SupportedHeader;

import org.xml.sax.InputSource;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfParser;
import com.orangelabs.rcs.core.ims.service.presence.rlmi.ResourceInstance;
import com.orangelabs.rcs.core.ims.service.presence.rlmi.RlmiDocument;
import com.orangelabs.rcs.core.ims.service.presence.rlmi.RlmiParser;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Subscribe manager for presence event
 *
 * @author jexa7410
 */
public class PresenceSubscribeManager extends SubscribeManager {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param parent IMS module
     */
    public PresenceSubscribeManager(ImsModule parent) {
    	super(parent);
    }

    /**
     * Returns the presentity
     *
     * @return Presentity
     */
    public String getPresentity() {
    	return ImsModule.IMS_USER_PROFILE.getPublicUri()+";pres-list=rcs";
    }

    /**
     * Create a SUBSCRIBE request
     *
	 * @param dialog SIP dialog path
	 * @param expirePeriod Expiration period
	 * @return SIP request
	 * @throws SipException
     */
    public SipRequest createSubscribe(SipDialogPath dialog, int expirePeriod) throws SipException {
    	// Create SUBSCRIBE message
    	SipRequest subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod);

    	// Set the Event header
    	subscribe.addHeader(EventHeader.NAME, "presence");

    	// Set the Accept header
    	subscribe.addHeader(AcceptHeader.NAME, "application/pidf+xml, application/rlmi+xml, multipart/related");

    	// Set the Supported header
    	subscribe.addHeader(SupportedHeader.NAME, "eventlist");

    	return subscribe;
    }

    /**
     * Receive a notification
     *
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
    	// Check notification
    	if (!isNotifyForThisSubscriber(notify)) {
    		return;
    	}

		if (logger.isActivated()) {
			logger.debug("New presence notification received");
		}

		// Parse XML part
	    String content = notify.getContent();
		if (content != null) {
	    	try {
	    		String boundary = notify.getBoundaryContentType();
				Multipart multi = new Multipart(content, boundary);
			    if (multi.isMultipart()) {
			    	// RLMI
			    	String rlmiPart = multi.getPart("application/rlmi+xml");
			    	if (rlmiPart != null) {
    					try {
	    	    			// Parse RLMI part
	    					InputSource rlmiInput = new InputSource(new ByteArrayInputStream(
	    							rlmiPart.getBytes(UTF8)));
	    					RlmiParser rlmiParser = new RlmiParser(rlmiInput);
	    					RlmiDocument rlmiInfo = rlmiParser.getResourceInfo();
	    					Vector<ResourceInstance> list = rlmiInfo.getResourceList();
	    					for(int i=0; i < list.size(); i++) {
	    						ResourceInstance res = (ResourceInstance)list.elementAt(i);
	    						ContactId contact = ContactUtils.createContactId(res.getUri());
	    						String state = res.getState();
	    						String reason = res.getReason();

	    						if ((state != null) && (reason != null)) {
	    							if (state.equalsIgnoreCase("terminated") && reason.equalsIgnoreCase("rejected")) {
	    								// It's a "terminated" event with status "rejected" the contact
	    								// should be removed from the "rcs" list
	    								getImsModule().getPresenceService().getXdmManager().removeContactFromGrantedList(contact);
	    							}

	    							// Notify listener
	    					    	getImsModule().getCore().getListener().handlePresenceSharingNotification(
	    					    			contact, state, reason);
	    						}
	    					}
    			    	} catch(Exception e) {
    			    		if (logger.isActivated()) {
    			    			logger.error("Can't parse RLMI notification", e);
    			    		}
    			    	}
			    	}

			    	// PIDF
			    	String pidfPart = multi.getPart("application/pidf+xml");
					try {
    	    			// Parse PIDF part
						InputSource pidfInput = new InputSource(new ByteArrayInputStream(
								pidfPart.getBytes(UTF8)));
    					PidfParser pidfParser = new PidfParser(pidfInput);
    					PidfDocument presenceInfo = pidfParser.getPresence();

						ContactId contact = ContactUtils.createContactId(presenceInfo.getEntity());
						// Notify listener
						getImsModule().getCore().getListener().handlePresenceInfoNotification(contact, presenceInfo);
			    	} catch(Exception e) {
			    		if (logger.isActivated()) {
			    			logger.error("Can't parse PIDF notification", e);
			    		}
			    	}
			    }
	    	} catch(Exception e) {
	    		if (logger.isActivated()) {
	    			logger.error("Can't parse presence notification", e);
	    		}
	    	}

			// Check subscription state
	    	SubscriptionStateHeader stateHeader = (SubscriptionStateHeader)notify.getHeader(SubscriptionStateHeader.NAME);
	    	if ((stateHeader != null) && stateHeader.getState().equalsIgnoreCase("terminated")) {
				if (logger.isActivated()) {
					logger.info("Presence subscription has been terminated by server");
				}
				terminatedByServer();
			}
		}
    }
}
