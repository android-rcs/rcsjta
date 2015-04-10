/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core;

import com.gsma.rcs.addressbook.AddressBookManager;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.ipcall.IPCallService;
import com.gsma.rcs.core.ims.service.presence.PresenceService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.terms.TermsConditionsService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.DeviceUtils;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceException;

import android.content.Context;

/**
 * Core (singleton pattern)
 * 
 * @author JM. Auffret
 */
public class Core {
    /**
     * Singleton instance
     */
    private static volatile Core sInstance;

    /**
     * Core listener
     */
    private CoreListener mListener;

    /**
     * Core status
     */
    private boolean mStarted = false;

    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Address book manager
     */
    private AddressBookManager mAddressBookManager;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    /**
     * Boolean to check is the Core is stopping
     */
    private boolean mStopping = false;

    /**
     * Returns the singleton instance
     * 
     * @return Core instance
     */
    public static Core getInstance() {
        return sInstance;
    }

    /**
     * Instantiate the core
     * 
     * @param listener Listener
     * @param rcsSettings RcsSettings instance
     * @param messagingLog
     * @param contactsManager
     * @return Core instance
     * @throws CoreException
     */
    public static Core createCore(CoreListener listener, RcsSettings rcsSettings,
            ContactsManager contactsManager, MessagingLog messagingLog) throws CoreException {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (Core.class) {
            if (sInstance == null) {
                sInstance = new Core(listener, rcsSettings, contactsManager, messagingLog);
            }
        }
        return sInstance;
    }

    /**
     * Terminate the core
     */
    public synchronized static void terminateCore() {
        if (sInstance == null) {
            return;
        }
        sInstance.stopCore();
        sInstance = null;
    }

    /**
     * Constructor
     * 
     * @param listener Listener
     * @param messagingLog
     * @param contactsManager
     * @throws CoreException
     */
    private Core(CoreListener listener, RcsSettings rcsSettings, ContactsManager contactsManager,
            MessagingLog messagingLog) throws CoreException {
        boolean logActivated = logger.isActivated();
        if (logActivated) {
            logger.info("Terminal core initialization");
        }

        // Set core event listener
        mListener = listener;
        mRcsSettings = rcsSettings;

        Context context = AndroidFactory.getApplicationContext();
        // Get UUID
        if (logActivated) {
            try {
                logger.info("My device UUID is ".concat(String.valueOf(DeviceUtils
                        .getDeviceUUID(context))));
            } catch (RcsServiceException e) {
                logger.error(new StringBuilder(
                        "Exception caught while logging for device UUID; exception-msg=")
                        .append(e.getMessage()).append("!").toString());
            }
        }

        // Initialize the phone utils
        PhoneUtils.initialize(context, mRcsSettings);

        // Create the address book manager
        mAddressBookManager = new AddressBookManager(contactsManager);

        // Create the IMS module
        mImsModule = new ImsModule(this, context, mRcsSettings, contactsManager, messagingLog);

        if (logActivated) {
            logger.info("Terminal core is created with success");
        }
    }

    /**
     * Returns the event listener
     * 
     * @return Listener
     */
    public CoreListener getListener() {
        return mListener;
    }

    /**
     * Returns the IMS module
     * 
     * @return IMS module
     */
    public ImsModule getImsModule() {
        return mImsModule;
    }

    /**
     * Returns the address book manager
     * 
     * @return AddressBookManager
     */
    public AddressBookManager getAddressBookManager() {
        return mAddressBookManager;
    }

    /**
     * Is core started
     * 
     * @return Boolean
     */
    public boolean isStarted() {
        return mStarted;
    }

    /**
     * Start the terminal core
     * 
     * @throws CoreException
     */
    public synchronized void startCore() throws CoreException {
        if (mStarted) {
            // Already started
            return;
        }

        // Start the IMS module
        mImsModule.start();

        // Start the address book monitoring
        mAddressBookManager.startAddressBookMonitoring();

        // Notify event listener
        mListener.handleCoreLayerStarted();

        mStarted = true;
        if (logger.isActivated()) {
            logger.info("RCS core service has been started with success");
        }
    }

    /**
     * Stop the terminal core
     */
    private void stopCore() {
        if (!mStarted) {
            // Already stopped
            return;
        }

        mStopping = true;
        boolean logActivated = logger.isActivated();
        if (logActivated) {
            logger.info("Stop the RCS core service");
        }

        // Stop the address book monitoring
        mAddressBookManager.stopAddressBookMonitoring();

        try {
            // Stop the IMS module
            mImsModule.stop();
        } catch (Exception e) {
            if (logActivated) {
                logger.error("Error during core shutdown", e);
            }
        }

        mStopping = false;
        mStarted = false;
        if (logActivated) {
            logger.info("RCS core service has been stopped with success");
        }
        sInstance = null;
        // Notify event listener
        mListener.handleCoreLayerStopped();
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
     * Returns the capability service
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

    /**
     * Returns True if Core is stopping
     * 
     * @return True if Core is stopping
     */
    public boolean isStopping() {
        return mStopping;
    }

    /**
     * Sets the listener
     * 
     * @param listener
     */
    public void setListener(CoreListener listener) {
        mListener = listener;
    }
}
