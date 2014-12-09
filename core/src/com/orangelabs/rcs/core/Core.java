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

package com.orangelabs.rcs.core;

import com.orangelabs.rcs.addressbook.AddressBookManager;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.capability.CapabilityService;
import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;
import com.orangelabs.rcs.core.ims.service.ipcall.IPCallService;
import com.orangelabs.rcs.core.ims.service.presence.PresenceService;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.core.ims.service.sip.SipService;
import com.orangelabs.rcs.core.ims.service.terms.TermsConditionsService;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.DeviceUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Core (singleton pattern)
 *  
 * @author JM. Auffret
 */
public class Core {
	/**
	 * Singleton instance
	 */
	private static Core instance = null;
	
    /**
     * Core listener
     */
    private CoreListener listener;
    
    /**
     * Core status
     */
	private boolean started = false;

    /**
	 * IMS module
	 */
	private ImsModule imsModule;

	/**
	 * Address book manager
	 */
	private AddressBookManager addressBookManager;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    
    /**
     * Returns the singleton instance
     * 
     * @return Core instance
     */
    public static Core getInstance() {
    	return instance;
    }
    
    /**
     * Instanciate the core
     * 
	 * @param listener Listener
     * @return Core instance
     * @throws CoreException
     */
    public synchronized static Core createCore(CoreListener listener) throws CoreException {
    	if (instance == null) {
    		instance = new Core(listener);
    	}
    	return instance;
    }
    
    /**
     * Terminate the core
     */
    public synchronized static void terminateCore() {
    	if (instance != null) {
    		instance.stopCore();
    	}
   		instance = null;
    }

    /**
     * Constructor
     * 
	 * @param listener Listener
     * @throws CoreException
     */
    private Core(CoreListener listener) throws CoreException {
		if (logger.isActivated()) {
        	logger.info("Terminal core initialization");
    	}

		// Set core event listener
		this.listener = listener;
       
        // Get UUID
		if (logger.isActivated()) {
			logger.info("My device UUID is " + DeviceUtils.getDeviceUUID(AndroidFactory.getApplicationContext()));
		}

        // Initialize the phone utils
    	PhoneUtils.initialize(AndroidFactory.getApplicationContext());

        // Create the address book manager
        addressBookManager = new AddressBookManager();

        // Create the IMS module
        imsModule = new ImsModule(this);
        
        if (logger.isActivated()) {
    		logger.info("Terminal core is created with success");
    	}
    }

	/**
	 * Returns the event listener
	 * 
	 * @return Listener
	 */
	public CoreListener getListener() {
		return listener;
	}

	/**
     * Returns the IMS module
     * 
     * @return IMS module
     */
	public ImsModule getImsModule() {
		return imsModule;
	}

	/**
	 * Returns the address book manager
	 */
	public AddressBookManager getAddressBookManager(){
		return addressBookManager;
	}
	
	/**
     * Is core started
     * 
     * @return Boolean
     */
    public boolean isCoreStarted() {
    	return started;
    }

    /**
     * Start the terminal core
     * 
     * @throws CoreException
     */
    public synchronized void startCore() throws CoreException {
    	if (started) {
    		// Already started
    		return;
    	}

    	// Start the IMS module 
    	imsModule.start();

    	// Start the address book monitoring
    	addressBookManager.startAddressBookMonitoring();
    	
    	// Notify event listener
		listener.handleCoreLayerStarted();
		
		started = true;
    	if (logger.isActivated()) {
    		logger.info("RCS core service has been started with success");
    	}
    }
    	
    /**
     * Stop the terminal core
     */
    public synchronized void stopCore() {
    	if (!started) {
    		// Already stopped
    		return;
    	}    	
    	
    	if (logger.isActivated()) {
    		logger.info("Stop the RCS core service");
    	}
    	
    	// Stop the address book monitoring
    	addressBookManager.stopAddressBookMonitoring();

    	try {
	    	// Stop the IMS module 
	    	imsModule.stop();	    	
    	} catch(Exception e) {
    		if (logger.isActivated()) {
    			logger.error("Error during core shutdown", e);
    		}
    	}
    	
    	// Notify event listener
		listener.handleCoreLayerStopped();

    	started = false;
    	if (logger.isActivated()) {
    		logger.info("RCS core service has been stopped with success");
    	}
    }

	/**
	 * Returns the terms service
	 * 
	 * @return Terms service
	 */
	public TermsConditionsService getTermsConditionsService() {
		return getImsModule().getTermsConditionsService();
	}

	/**
	 * Returns the presence service
	 * 
	 * @return Presence service
	 */
	public PresenceService getPresenceService() {
		return getImsModule().getPresenceService();
	}
	
	/**
	 * Returns the capabity service
	 * 
	 * @return Capability service
	 */
	public CapabilityService getCapabilityService() {
		return getImsModule().getCapabilityService();
	}

	/**
	 * Returns the IP call service
	 * 
	 * @return Rich call service
	 */
	public IPCallService getIPCallService() {
		return getImsModule().getIPCallService();
	}
	
	/**
	 * Returns the richcall service
	 * 
	 * @return Rich call service
	 */
	public RichcallService getRichcallService() {
		return getImsModule().getRichcallService();
	}
	
	/**
	 * Returns the IM service
	 * 
	 * @return IM service
	 */
	public InstantMessagingService getImService() {
		return getImsModule().getInstantMessagingService();
	}
	
	/**
	 * Returns the SIP service
	 * 
	 * @return SIP service
	 */
	public SipService getSipService() {
		return getImsModule().getSipService();
	}
}
