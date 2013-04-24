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

package com.orangelabs.rcs.service;

import java.util.Vector;

import org.gsma.joyn.capability.CapabilityServiceImpl;
import org.gsma.joyn.capability.ICapabilityService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.addressbook.AccountChangedReceiver;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.CoreListener;
import com.orangelabs.rcs.core.TerminalInfo;
import com.orangelabs.rcs.core.ims.ImsError;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.TerminatingOne2OneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardMsgSession;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.core.ims.service.presence.PresenceUtils;
import com.orangelabs.rcs.core.ims.service.presence.pidf.OverridingWillingness;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Person;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.Tuple;
import com.orangelabs.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.orangelabs.rcs.core.ims.service.sip.GenericSipSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.service.api.client.ClientApiIntents;
import com.orangelabs.rcs.service.api.client.IImsApi;
import com.orangelabs.rcs.service.api.client.ImsApiIntents;
import com.orangelabs.rcs.service.api.client.ImsDisconnectionReason;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.gsma.GsmaUiConnector;
import com.orangelabs.rcs.service.api.client.messaging.IMessagingApi;
import com.orangelabs.rcs.service.api.client.presence.FavoriteLink;
import com.orangelabs.rcs.service.api.client.presence.Geoloc;
import com.orangelabs.rcs.service.api.client.presence.IPresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PhotoIcon;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.service.api.client.richcall.IRichCallApi;
import com.orangelabs.rcs.service.api.client.sip.ISipApi;
import com.orangelabs.rcs.service.api.client.terms.ITermsApi;
import com.orangelabs.rcs.service.api.server.ImsApiService;
import com.orangelabs.rcs.service.api.server.gsma.GsmaUtils;
import com.orangelabs.rcs.service.api.server.messaging.MessagingApiService;
import com.orangelabs.rcs.service.api.server.presence.PresenceApiService;
import com.orangelabs.rcs.service.api.server.richcall.RichCallApiService;
import com.orangelabs.rcs.service.api.server.sip.SipApiService;
import com.orangelabs.rcs.service.api.server.terms.TermsApiService;
import com.orangelabs.rcs.utils.AppUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RCS core service. This service offers a flat API to any other process (activities)
 * to access to RCS features. This service is started automatically at device boot.
 * 
 * @author jexa7410
 */
public class RcsCoreService extends Service implements CoreListener {
	/**
	 * Notification ID
	 */
	private final static int SERVICE_NOTIFICATION = 1000;
	
	/**
	 * CPU manager
	 */
	private CpuManager cpuManager = new CpuManager();

	// --------------------- GSMA API -------------------------
	
	/**
	 * Capability API
	 */
    private CapabilityServiceImpl capabilityApi = new CapabilityServiceImpl(); 

	// --------------------- GSMA API -------------------------
    
	/**
	 * IMS API
	 */
	private ImsApiService imsApi = new ImsApiService(); 
	
	/**
	 * Terms API
	 */
    private TermsApiService termsApi = new TermsApiService(); 

    /**
	 * Presence API
	 */
    private PresenceApiService presenceApi = new PresenceApiService(); 
    
	/**
	 * Messaging API
	 */
	private MessagingApiService messagingApi = new MessagingApiService(); 

	/**
	 * Rich call API
	 */
	private RichCallApiService richcallApi = new RichCallApiService(); 
	
	/**
	 * SIP API
	 */
	private SipApiService sipApi = new SipApiService(); 
	
	/**
	 * IP call API
	 */
	// TODO : add here the VOIPApiService instanciation

    /**
     * Account changed broadcast receiver
     */
    private AccountChangedReceiver accountChangedReceiver = null;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Override
    public void onCreate() {
		// Set application context
		AndroidFactory.setApplicationContext(getApplicationContext());

		// Set the terminal version
		TerminalInfo.setProductVersion(AppUtils.getApplicationVersion(this));

    	// Start the core
    	startCore();
    }

    @Override
    public void onDestroy() {
        // Unregister account changed broadcast receiver
	    if (accountChangedReceiver != null) {
	        try {
	        	unregisterReceiver(accountChangedReceiver);
	        } catch (IllegalArgumentException e) {
	        	// Nothing to do
	        }
	    }

    	// Close APIs
    	imsApi.close();
    	termsApi.close();
		presenceApi.close();
		capabilityApi.close();
		richcallApi.close();
		messagingApi.close();
		sipApi.close();

        // Stop the core
        Thread t = new Thread() {
            /**
             * Processing
             */
            public void run() {
                stopCore();
            }
        };
        t.start();
    }

    /**
     * Start core
     */
    public synchronized void startCore() {
		if (Core.getInstance() != null) {
			// Already started
			return;
		}

        try {
    		if (logger.isActivated()) {
    			logger.debug("Start RCS core service");
    		}
    		
    		// Send service intent 
			Intent intent = new Intent(ClientApiIntents.SERVICE_STATUS);
			intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STARTING);
			getApplicationContext().sendBroadcast(intent);
            
			// Instantiate the settings manager
            RcsSettings.createInstance(getApplicationContext());
            
            // Set the logger properties
    		Logger.activationFlag = RcsSettings.getInstance().isTraceActivated();
    		String traceLevel = RcsSettings.getInstance().getTraceLevel();
    		if (traceLevel.equalsIgnoreCase("DEBUG")) {
        		Logger.traceLevel = Logger.DEBUG_LEVEL;    			
    		} else if (traceLevel.equalsIgnoreCase("INFO")) {
        		Logger.traceLevel = Logger.INFO_LEVEL;
    		} else if (traceLevel.equalsIgnoreCase("WARN")) {
        		Logger.traceLevel = Logger.WARN_LEVEL;
    		} else if (traceLevel.equalsIgnoreCase("ERROR")) {
        		Logger.traceLevel = Logger.ERROR_LEVEL;
    		} else if (traceLevel.equalsIgnoreCase("FATAL")) {
        		Logger.traceLevel = Logger.FATAL_LEVEL;
    		}

    		// Terminal version
            if (logger.isActivated()) {
                logger.info("RCS stack release is " + TerminalInfo.getProductVersion());
            }
    		
    		// Instantiate the contacts manager
            ContactsManager.createInstance(getApplicationContext());

            // Instantiate the rich messaging history 
            RichMessaging.createInstance(getApplicationContext());
            
            // Instantiate the rich call history 
            RichCall.createInstance(getApplicationContext());

            // Create the core
			Core.createCore(this);

			// Start the core
			Core.getInstance().startCore();		

			// Create multimedia directory on sdcard
			FileFactory.createDirectory(RcsSettings.getInstance().getPhotoRootDirectory());
			FileFactory.createDirectory(RcsSettings.getInstance().getVideoRootDirectory());
			FileFactory.createDirectory(RcsSettings.getInstance().getFileRootDirectory());
			
			// Init CPU manager
			cpuManager.init();

            // Register account changed event receiver
            if (accountChangedReceiver == null) {
                accountChangedReceiver = new AccountChangedReceiver();

                // Register account changed broadcast receiver after a timeout of 2s (This is not done immediately, as we do not want to catch
                // the removal of the account (creating and removing accounts is done asynchronously). We can reasonably assume that no
                // RCS account deletion will be done by user during this amount of time, as he just started his service.
                Handler handler = new Handler();
                handler.postDelayed(
                        new Runnable() {
                            public void run() {
                                registerReceiver(accountChangedReceiver, new IntentFilter(
                                        "android.accounts.LOGIN_ACCOUNTS_CHANGED"));
                            }},
                        2000);
            }

	        // Show a first notification
	    	addRcsServiceNotification(false, getString(R.string.rcs_core_loaded));

	    	// Update GSMA client API
	    	GsmaUtils.setClientActivationState(getApplicationContext(), true);
	    	
			// Send service intent 
			intent = new Intent(ClientApiIntents.SERVICE_STATUS);
			intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STARTED);
			getApplicationContext().sendBroadcast(intent);

			if (logger.isActivated()) {
				logger.info("RCS core service started with success");
			}
		} catch(Exception e) {
			// Unexpected error
			if (logger.isActivated()) {
				logger.error("Can't instanciate the RCS core service", e);
			}
			
			// Send service intent 
			Intent intent = new Intent(ClientApiIntents.SERVICE_STATUS);
			intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_FAILED);
			getApplicationContext().sendBroadcast(intent);

			// Show error in notification bar
	    	addRcsServiceNotification(false, getString(R.string.rcs_core_failed));
	    	
			// Exit service
	    	stopSelf();
		}
    }
    
    /**
     * Stop core
     */
    public synchronized void stopCore() {
		if (Core.getInstance() == null) {
			// Already stopped
			return;
		}
		
		if (logger.isActivated()) {
			logger.debug("Stop RCS core service");
		}

    	// Update GSMA client API
    	GsmaUtils.setClientActivationState(getApplicationContext(), false);
		
		// Send service intent 
		Intent intent = new Intent(ClientApiIntents.SERVICE_STATUS);
		intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STOPPING);
		getApplicationContext().sendBroadcast(intent);
		
		// Terminate the core in background
		Core.terminateCore();

		// Close CPU manager
		cpuManager.close();

		// Send service intent 
		intent = new Intent(ClientApiIntents.SERVICE_STATUS);
		intent.putExtra("status", ClientApiIntents.SERVICE_STATUS_STOPPED);
		getApplicationContext().sendBroadcast(intent);

		if (logger.isActivated()) {
			logger.info("RCS core service stopped with success");
		}
    }

    @Override
    public IBinder onBind(Intent intent) {    	
        if (IImsApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("IMS API binding");
    		}
            return imsApi;
        } else
        if (ITermsApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Terms API binding");
    		}
            return termsApi;
        } else
        if (IPresenceApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Presence API binding");
    		}
            return presenceApi;
        } else
        if (ICapabilityService.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Capability service API binding");
    		}
            return capabilityApi;
        } else
        if (IMessagingApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Messaging API binding");
    		}
            return messagingApi;
        } else
        if (IRichCallApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("Rich call API binding");
    		}
            return richcallApi;
        } else
        if (ISipApi.class.getName().equals(intent.getAction())) {
    		if (logger.isActivated()) {
    			logger.debug("SIP API binding");
    		}
            return sipApi;
        } else {
        	return null;
        }
    }
    
    /**
     * Add RCS service notification
     * 
     * @param state Service state (ON|OFF)
     * @param label Label
     */
    public static void addRcsServiceNotification(boolean state, String label) {
    	// Create notification
    	Intent intent = new Intent(ClientApiIntents.RCS_SETTINGS);
    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(AndroidFactory.getApplicationContext(), 0, intent, 0);
		int iconId; 
		if (state) {
			iconId  = R.drawable.rcs_core_notif_on_icon;
		} else {
			iconId  = R.drawable.rcs_core_notif_off_icon; 
		}
        Notification notif = new Notification(iconId, "", System.currentTimeMillis());
        notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_FOREGROUND_SERVICE;
        notif.setLatestEventInfo(AndroidFactory.getApplicationContext(),
        		AndroidFactory.getApplicationContext().getString(R.string.rcs_core_rcs_notification_title),
        		label, contentIntent);
        
        // Send notification
		NotificationManager notificationManager = (NotificationManager)AndroidFactory.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(SERVICE_NOTIFICATION, notif);
    }
    
    /*---------------------------- CORE EVENTS ---------------------------*/
    
    /**
     * Core layer has been started
     */
    public void handleCoreLayerStarted() {
		if (logger.isActivated()) {
			logger.debug("Handle event core started");
		}

		// Display a notification
		addRcsServiceNotification(false, getString(R.string.rcs_core_started));
    }

    /**
     * Core layer has been terminated
     */
    public void handleCoreLayerStopped() {
        // Display a notification
        if (logger.isActivated()) {
            logger.debug("Handle event core terminated");
        }
        addRcsServiceNotification(false, getString(R.string.rcs_core_stopped));
    }

    /**
     * Send IMS intent when registered
     */
    private void sendImsIntentRegistered() {
		// TODO keep only one intent here

		// Send registration intent
		Intent intent = new Intent(ImsApiIntents.IMS_STATUS);
		intent.putExtra("status", true);
		getApplicationContext().sendBroadcast(intent);

		// Send GSMA UI Connector intent
		Intent intentGsma = new Intent(GsmaUiConnector.ACTION_REGISTRATION_CHANGED);
		intentGsma.putExtra(GsmaUiConnector.EXTRA_REGISTRATION_STATUS, true);
		getApplicationContext().sendBroadcast(intentGsma);
    }

    /**
     * Send IMS intent when not registered
     *
     * @param reason Disconnection reason
     */
    private void sendImsIntentNotRegistered(int reason) {
		// TODO keep only one intent here

		// Send registration intent
		Intent intent = new Intent(ImsApiIntents.IMS_STATUS);
		intent.putExtra("status", false);
		intent.putExtra("reason", reason);
		getApplicationContext().sendBroadcast(intent);

		// Send GSMA UI Connector intent
		Intent intentGsma = new Intent(GsmaUiConnector.ACTION_REGISTRATION_CHANGED);
		intentGsma.putExtra(GsmaUiConnector.EXTRA_REGISTRATION_STATUS, false);
		getApplicationContext().sendBroadcast(intentGsma);
    }
    
    /**
	 * Handle "registration successful" event
	 * 
	 * @param registered Registration flag
	 */
	public void handleRegistrationSuccessful() {
		if (logger.isActivated()) {
			logger.debug("Handle event registration ok");
		}
		
		// Send registration intent
		sendImsIntentRegistered();
		
		// Display a notification
		addRcsServiceNotification(true, getString(R.string.rcs_core_ims_connected));
	}

	/**
	 * Handle "registration failed" event
	 * 
     * @param error IMS error
   	 */
	public void handleRegistrationFailed(ImsError error) {
		if (logger.isActivated()) {
			logger.debug("Handle event registration failed");
		}

		// Send registration intent
		sendImsIntentNotRegistered(ImsDisconnectionReason.REGISTRATION_FAILED);
		
		// Display a notification
		addRcsServiceNotification(false, getString(R.string.rcs_core_ims_connection_failed));
	}

	/**
	 * Handle "registration terminated" event
	 */
	public void handleRegistrationTerminated() {
        if (logger.isActivated()) {
            logger.debug("Handle event registration terminated");
        }

        if (Core.getInstance().getImsModule().getImsConnectionManager().isDisconnectedByBattery()) {
            // Display a notification
            addRcsServiceNotification(false, getString(R.string.rcs_core_ims_battery_disconnected));

            // Send registration intent
            sendImsIntentNotRegistered(ImsDisconnectionReason.BATTERY_LOW);
        } else {
            // Display a notification
        	addRcsServiceNotification(false, getString(R.string.rcs_core_ims_disconnected));

        	// Send registration intent
        	sendImsIntentNotRegistered(ImsDisconnectionReason.SERVICE_TERMINATED);
        }
	}

    /**
     * A new presence sharing notification has been received
     * 
     * @param contact Contact
     * @param status Status
     * @param reason Reason
     */
    public void handlePresenceSharingNotification(String contact, String status, String reason) {
		if (logger.isActivated()) {
			logger.debug("Handle event presence sharing notification for " + contact + " (" + status + ":" + reason + ")");
		}

		try {
			// Check if its a notification for a contact or for the end user
			String me = ImsModule.IMS_USER_PROFILE.getPublicUri();
			if (PhoneUtils.compareNumbers(me, contact)) {
				// End user notification
				if (logger.isActivated()) {
					logger.debug("Presence sharing notification for me: by-pass it");
				}
	    	} else { 
		    	// Update contacts database
				ContactsManager.getInstance().setContactSharingStatus(contact, status, reason);
	
				// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		    	intent.putExtra("contact", contact);
		    	intent.putExtra("status", status);
		    	intent.putExtra("reason", reason);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
	    	}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
    	}
    }

    /**
     * A new presence info notification has been received
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void handlePresenceInfoNotification(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Handle event presence info notification for " + contact);
		}

		try {
			// Test if person item is not null
			Person person = presence.getPerson();
			if (person == null) {
				if (logger.isActivated()) {
					logger.debug("Presence info is empty (i.e. no item person found) for contact " + contact);
				}
				return;
			}

			// Check if its a notification for a contact or for me
			String me = ImsModule.IMS_USER_PROFILE.getPublicUri();
			if (PhoneUtils.compareNumbers(me, contact)) {
				// Notification for me
				presenceInfoNotificationForMe(presence);
			} else {
				// Check that the contact exist in database
				int rcsStatus = ContactsManager.getInstance().getContactSharingStatus(contact);
				if (rcsStatus == -1) {
					if (logger.isActivated()) {
						logger.debug("Contact " + contact + " is not a RCS contact, by-pass the notification");
					}
					return;
				}

				// Notification for a contact
				presenceInfoNotificationForContact(contact, presence);
			}
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
	}

    /**
     * A new presence info notification has been received for me
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void presenceInfoNotificationForMe(PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Presence info notification for me");
		}

    	try {
			// Get the current presence info for me
    		PresenceInfo currentPresenceInfo = ContactsManager.getInstance().getMyPresenceInfo();
    		if (currentPresenceInfo == null) {
    			currentPresenceInfo = new PresenceInfo();
    		}

			// Update presence status
			String presenceStatus = PresenceInfo.UNKNOWN;
			Person person = presence.getPerson();
			OverridingWillingness willingness = person.getOverridingWillingness();
			if (willingness != null) {
				if ((willingness.getBasic() != null) && (willingness.getBasic().getValue() != null)) {
					presenceStatus = willingness.getBasic().getValue();
				}
			}				
			currentPresenceInfo.setPresenceStatus(presenceStatus);
    		
    		// Update the presence info
			currentPresenceInfo.setTimestamp(person.getTimestamp());
			if (person.getNote() != null) {
				currentPresenceInfo.setFreetext(person.getNote().getValue());
			}
			if (person.getHomePage() != null) {
				currentPresenceInfo.setFavoriteLink(new FavoriteLink(person.getHomePage()));
			}
			
    		// Get photo Etag values
			String lastEtag = null;
			String newEtag = null; 
			if (person.getStatusIcon() != null) {
				newEtag = person.getStatusIcon().getEtag();
			}
			if (currentPresenceInfo.getPhotoIcon() != null) {
				lastEtag = currentPresenceInfo.getPhotoIcon().getEtag();
			}
    		
    		// Test if the photo has been removed
			if ((lastEtag != null) && (person.getStatusIcon() == null)) {
	    		if (logger.isActivated()) {
	    			logger.debug("Photo has been removed for me");
	    		}
	    		
    			// Update the presence info
				currentPresenceInfo.setPhotoIcon(null);

				// Update EAB provider
				ContactsManager.getInstance().removeMyPhotoIcon();
			} else		
	    	// Test if the photo has been changed
	    	if ((person.getStatusIcon() != null) &&	(newEtag != null)) {
	    		if ((lastEtag == null) || (!lastEtag.equals(newEtag))) {
		    		if (logger.isActivated()) {
		    			logger.debug("Photo has changed for me, download it in background");
		    		}
		
		    		// Download the photo in background
		    		downloadPhotoForMe(presence.getPerson().getStatusIcon().getUrl(), newEtag);
	    		}
	    	}
	    	   		    		
	    	// Update EAB provider
			ContactsManager.getInstance().setMyInfo(currentPresenceInfo);

    		// Broadcast intent
	    	Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }

    /**
     * A new presence info notification has been received for a given contact
     * 
     * @param contact Contact
     * @param presense Presence info document
     */
    public void presenceInfoNotificationForContact(String contact, PidfDocument presence) {
    	if (logger.isActivated()) {
			logger.debug("Presence info notification for contact " + contact);
		}

    	try {
    		// Extract number from contact 
    		String number = PhoneUtils.extractNumberFromUri(contact);

    		// Get the current presence info
    		ContactInfo currentContactInfo = ContactsManager.getInstance().getContactInfo(contact);
    		ContactInfo newContactInfo = currentContactInfo;
    		if (currentContactInfo == null) {
    			if (logger.isActivated()) {
    				logger.warn("Contact " + contact + " not found in EAB: by-pass the notification");
    			}
    			return;
    		}
    		PresenceInfo newPresenceInfo = currentContactInfo.getPresenceInfo();
    		if (newPresenceInfo == null) {
    			newPresenceInfo = new PresenceInfo();
    			newContactInfo.setPresenceInfo(newPresenceInfo);
    		}

			// Update the current capabilities
			Capabilities capabilities =  new Capabilities(); 
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
			newContactInfo.setCapabilities(capabilities);

			// Update presence status
			String presenceStatus = PresenceInfo.UNKNOWN;
			Person person = presence.getPerson();
			OverridingWillingness willingness = person.getOverridingWillingness();
			if (willingness != null) {
				if ((willingness.getBasic() != null) && (willingness.getBasic().getValue() != null)) {
					presenceStatus = willingness.getBasic().getValue();
				}
			}				
			newPresenceInfo.setPresenceStatus(presenceStatus);

			// Update the presence info
			newPresenceInfo.setTimestamp(person.getTimestamp());
			if (person.getNote() != null) {
				newPresenceInfo.setFreetext(person.getNote().getValue());
			}
			if (person.getHomePage() != null) {
				newPresenceInfo.setFavoriteLink(new FavoriteLink(person.getHomePage()));
			}
			
			// Update geoloc info
			if (presence.getGeopriv() != null) {
				Geoloc geoloc = new Geoloc(presence.getGeopriv().getLatitude(),
						presence.getGeopriv().getLongitude(),
						presence.getGeopriv().getAltitude());
				newPresenceInfo.setGeoloc(geoloc);
			}
			newContactInfo.setPresenceInfo(newPresenceInfo);
			
	    	// Update contacts database
			ContactsManager.getInstance().setContactInfo(newContactInfo, currentContactInfo);

    		// Get photo Etag values
			String lastEtag = ContactsManager.getInstance().getContactPhotoEtag(contact);
			String newEtag = null; 
			if (person.getStatusIcon() != null) {
				newEtag = person.getStatusIcon().getEtag();
			}

    		// Test if the photo has been removed
			if ((lastEtag != null) && (person.getStatusIcon() == null)) {
	    		if (logger.isActivated()) {
	    			logger.debug("Photo has been removed for " + contact);
	    		}

	    		// Update contacts database
	    		ContactsManager.getInstance().setContactPhotoIcon(contact, null);
				
	    		// Broadcast intent
				Intent intent = new Intent(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		    	intent.putExtra("contact", number);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
			} else		
	    	// Test if the photo has been changed
	    	if ((person.getStatusIcon() != null) &&	(newEtag != null)) {
	    		if ((lastEtag == null) || (!lastEtag.equals(newEtag))) {
		    		if (logger.isActivated()) {
		    			logger.debug("Photo has changed for " + contact + ", download it in background");
		    		}
		
		    		// Download the photo in background
		    		downloadPhotoForContact(contact, presence.getPerson().getStatusIcon().getUrl(), newEtag);
	    		}
	    	}    	
	    	   		    		
	    	// Broadcast intent
	    	Intent intent = new Intent(PresenceApiIntents.CONTACT_INFO_CHANGED);
	    	intent.putExtra("contact", number);
	    	getApplicationContext().sendBroadcast(intent);
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Internal exception", e);
    		}
		}
    }
    
    /**
     * Capabilities update notification has been received
     * 
     * @param contact Contact
     * @param capabilities Capabilities
     */
    public void handleCapabilitiesNotification(String contact, Capabilities capabilities) {
    	if (logger.isActivated()) {
			logger.debug("Handle capabilities update notification for " + contact + " (" + capabilities.toString() + ")");
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(contact);

		// Notify API
		capabilityApi.receiveCapabilities(number, capabilities);
    }
    
    /**
     * Download photo for me
     * 
     * @param url Photo URL
     * @param etag New Etag associated to the photo
     */
    private void downloadPhotoForMe(final String url, final String etag) {
		Thread t = new Thread() {
			public void run() {
		    	try {
		    		// Download from XDMS
		    		PhotoIcon icon = Core.getInstance().getPresenceService().getXdmManager().downloadContactPhoto(url, etag);    		
		    		if (icon != null) {
		    			// Update the presence info
		    			Core.getInstance().getPresenceService().getPresenceInfo().setPhotoIcon(icon);
		    			
						// Update contacts database
		    			ContactsManager.getInstance().setMyPhotoIcon(icon);
						
			    		// Broadcast intent
		    			// TODO : use a specific intent for the end user photo
				    	Intent intent = new Intent(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
				    	getApplicationContext().sendBroadcast(intent);
			    	}
		    	} catch(Exception e) {
		    		if (logger.isActivated()) {
		    			logger.error("Internal exception", e);
		    		}
	    		}
			}
		};
		t.start();
    }
    
    /**
     * Download photo for a given contact
     * 
     * @param contact Contact
     * @param url Photo URL 
     * @param etag New Etag associated to the photo
     */
    private void downloadPhotoForContact(final String contact, final String url, final String etag) {
		Thread t = new Thread() {
			public void run() {
		    	try {
		    		// Download from XDMS
		    		PhotoIcon icon = Core.getInstance().getPresenceService().getXdmManager().downloadContactPhoto(url, etag);    		
		    		if (icon != null) {
		    			// Update contacts database
		    			ContactsManager.getInstance().setContactPhotoIcon(contact, icon);

		    			// Extract number from contact 
		    			String number = PhoneUtils.extractNumberFromUri(contact);

		    			// Broadcast intent
		    			Intent intent = new Intent(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		    			intent.putExtra("contact", number);
		    			getApplicationContext().sendBroadcast(intent);
			    	}
		    	} catch(Exception e) {
		    		if (logger.isActivated()) {
		    			logger.error("Internal exception", e);
		    		}
	    		}
			}
		};
		t.start();
    }
    
    /**
     * A new presence sharing invitation has been received
     * 
     * @param contact Contact
     */
    public void handlePresenceSharingInvitation(String contact) {
		if (logger.isActivated()) {
			logger.debug("Handle event presence sharing invitation");
		}
		
		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(contact);
		
    	// Broadcast intent related to the received invitation
    	Intent intent = new Intent(PresenceApiIntents.PRESENCE_INVITATION);
    	intent.putExtra("contact", number);
    	getApplicationContext().sendBroadcast(intent);
    }
    
    /**
     * New content sharing transfer invitation
     * 
     * @param session Content sharing transfer invitation
     */
    public void handleContentSharingTransferInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing transfer invitation");
		}

		// Broadcast the invitation
		richcallApi.receiveImageSharingInvitation(session);
    }
    
    /**
     * New content sharing transfer invitation
     * 
     * @param session Content sharing transfer invitation
     */
    public void handleContentSharingTransferInvitation(GeolocTransferSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing transfer invitation");
		}

		// Broadcast the invitation
		richcallApi.receiveGeolocSharingInvitation(session);
    }
    
    /**
     * New content sharing streaming invitation
     * 
     * @param session CSh session
     */
    public void handleContentSharingStreamingInvitation(VideoStreamingSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event content sharing streaming invitation");
		}

		// Broadcast the invitation
		richcallApi.receiveVideoSharingInvitation(session);
    }

	/**
	 * A new file transfer invitation has been received
	 * 
	 * @param session File transfer session
	 */
	public void handleFileTransferInvitation(FileSharingSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event file transfer invitation");
		}
		
    	// Broadcast the invitation
    	messagingApi.receiveFileTransferInvitation(session);
	}
    
	/**
     * New one-to-one chat session invitation
     * 
     * @param session Chat session
     */
	public void handleOneOneChatSessionInvitation(TerminatingOne2OneChatSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive 1-1 chat session invitation");
		}
		
    	// Broadcast the invitation
		messagingApi.receiveOneOneChatInvitation(session);
    }

    /**
     * New ad-hoc group chat session invitation
     * 
     * @param session Chat session
     */
	public void handleAdhocGroupChatSessionInvitation(TerminatingAdhocGroupChatSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive ad-hoc group chat session invitation");
		}

    	// Broadcast the invitation
		messagingApi.receiveGroupChatInvitation(session);
	}
    
    /**
     * One-to-one chat session extended to a group chat session
     * 
     * @param groupSession Group chat session
     * @param oneoneSession 1-1 chat session
     */
    public void handleOneOneChatSessionExtended(GroupChatSession groupSession, OneOneChatSession oneoneSession) {
		if (logger.isActivated()) {
			logger.debug("Handle event 1-1 chat session extended");
		}

    	// Broadcast the event
		messagingApi.extendOneOneChatSession(groupSession, oneoneSession);
    }
	
    /**
     * Store and Forward messages session invitation
     * 
     * @param session Chat session
     */
    public void handleStoreAndForwardMsgSessionInvitation(TerminatingStoreAndForwardMsgSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event S&F messages session invitation");
		}
		
    	// Broadcast the invitation
		messagingApi.receiveOneOneChatInvitation(session);
    }
    
    /**
     * New message delivery status
     * 
     * @param contact Contact
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String contact, String msgId, String status) {
		if (logger.isActivated()) {
			logger.debug("Handle message delivery status");
		}
    	
		// Notify listeners
		messagingApi.handleMessageDeliveryStatus(contact, msgId, status);
    }
    
    /**
     * New SIP session invitation
     * 
	 * @param intent Resolved intent
     * @param session SIP session
     */
    public void handleSipSessionInvitation(Intent intent, GenericSipSession session) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive SIP session invitation");
		}
		
		// Broadcast the invitation
		sipApi.receiveSipSessionInvitation(intent, session);
    }    

	/**
	 * New SIP instant message received
	 * 
	 * @param intent Resolved intent
	 */
    public void handleSipInstantMessageReceived(Intent intent) {
		if (logger.isActivated()) {
			logger.debug("Handle event receive SIP instant message");
		}
		
		// Broadcast the message
		sipApi.receiveSipInstantMessage(intent);
    }

	/**
     * User terms confirmation request
     *
     * @param remote Remote server
     * @param id Request ID
     * @param type Type of request
     * @param pin PIN number requested
     * @param subject Subject
     * @param text Text
     * @param btnLabelAccept Label of Accept button
     * @param btnLabelReject Label of Reject button
     * @param timeout Timeout request
     */
    public void handleUserConfirmationRequest(String remote, String id,
    		String type, boolean pin, String subject, String text,
    		String acceptButtonLabel, String rejectButtonLabel, int timeout) {
        if (logger.isActivated()) {
			logger.debug("Handle event user terms confirmation request");
		}

		// Notify listeners
        termsApi.receiveTermsRequest(remote, id, type, pin, subject, text, acceptButtonLabel, rejectButtonLabel, timeout);
    }

    /**
     * User terms confirmation acknowledge
     * 
     * @param remote Remote server
     * @param id Request ID
     * @param status Status
     * @param subject Subject
     * @param text Text
     */
    public void handleUserConfirmationAck(String remote, String id, String status, String subject, String text) {
		if (logger.isActivated()) {
			logger.debug("Handle event user terms confirmation ack");
		}

		// Notify listeners
		termsApi.receiveTermsAck(remote, id, status, subject, text);
    }

    /**
     * User terms notification
     *
     * @param remote Remote server
     * @param id Request ID
     * @param subject Subject
     * @param text Text
     * @param btnLabel Label of OK button
     */
    public void handleUserNotification(String remote, String id, String subject, String text, String okButtonLabel) {
        if (logger.isActivated()) {
            logger.debug("Handle event user terms notification");
        }

        // Notify listeners
        termsApi.receiveUserNotification(remote, id, subject, text, okButtonLabel);
    }

    /**
	 * SIM has changed
	 */
    public void handleSimHasChanged() {
        if (logger.isActivated()) {
            logger.debug("Handle SIM has changed");
        }

		// Restart the RCS service
        LauncherUtils.stopRcsService(getApplicationContext());
        LauncherUtils.launchRcsService(getApplicationContext(), true);
    }
}
