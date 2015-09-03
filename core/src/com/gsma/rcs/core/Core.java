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
import com.gsma.rcs.addressbook.LocaleManager;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.ipcall.IPCallService;
import com.gsma.rcs.core.ims.service.presence.PresenceService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.terms.TermsConditionsService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.DeviceUtils;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceException;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.security.KeyStoreException;

/**
 * Core (singleton pattern)
 *
 * @author JM. Auffret
 */
public class Core {

    private static final String BACKGROUND_THREAD_NAME = Core.class.getSimpleName();

    private static volatile Core sInstance;

    private CoreListener mListener;

    private boolean mStarted = false;

    private ImsModule mImsModule;

    private AddressBookManager mAddressBookManager;

    private static final Logger sLogger = Logger.getLogger(Core.class.getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * Handler to process messages & runnable associated with background thread.
     */
    private final Handler mBackgroundHandler;

    /**
     * Boolean to check is the Core is stopping
     */
    private boolean mStopping = false;

    private final LocaleManager mLocaleManager;

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
     * @param ctx The application context
     * @param listener Listener
     * @param rcsSettings RcsSettings instance
     * @param contentResolver
     * @param contactsManager
     * @param messagingLog
     * @return Core instance
     * @throws IOException
     * @throws KeyStoreException
     */
    public static Core createCore(Context ctx, CoreListener listener, RcsSettings rcsSettings,
            ContentResolver contentResolver, ContactManager contactsManager,
            MessagingLog messagingLog) throws IOException, KeyStoreException {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (Core.class) {
            if (sInstance == null) {
                KeyStoreManager.loadKeyStore(rcsSettings);
                sInstance = new Core(ctx, listener, rcsSettings, contentResolver, contactsManager,
                        messagingLog);
            }
        }
        return sInstance;
    }

    /**
     * Terminate the core
     * 
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public synchronized static void terminateCore() throws SipPayloadException, SipNetworkException {
        if (sInstance == null) {
            return;
        }
        sInstance.stopCore();
        sInstance = null;
    }

    /**
     * Constructor
     * 
     * @param ctx The application context
     * @param listener Listener
     * @param rcsSettings The RCS settings accessor
     * @param contentResolver The content resolver
     * @param contactManager The contact manager
     * @param messagingLog The messaging log accessor
     */
    private Core(Context ctx, CoreListener listener, RcsSettings rcsSettings,
            ContentResolver contentResolver, ContactManager contactManager,
            MessagingLog messagingLog) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Terminal core initialization");
        }
        mListener = listener;
        mRcsSettings = rcsSettings;
        // Get UUID
        if (logActivated) {
            try {
                sLogger.info("My device UUID is ".concat(String.valueOf(DeviceUtils
                        .getDeviceUUID(ctx))));
            } catch (RcsServiceException e) {
                sLogger.error(new StringBuilder(
                        "Exception caught while logging for device UUID; exception-msg=")
                        .append(e.getMessage()).append("!").toString());
            }
        }

        // Initialize the phone utils
        PhoneUtils.initialize(ctx, mRcsSettings);

        // Create the address book manager
        mAddressBookManager = new AddressBookManager(contentResolver, contactManager);
        mLocaleManager = new LocaleManager(ctx, this, rcsSettings, contactManager);

        final HandlerThread backgroundThread = new HandlerThread(BACKGROUND_THREAD_NAME);
        backgroundThread.start();

        mBackgroundHandler = new Handler(backgroundThread.getLooper());

        /* Create the IMS module */
        mImsModule = new ImsModule(this, ctx, mRcsSettings, contactManager, messagingLog,
                mAddressBookManager);

        if (logActivated) {
            sLogger.info("Terminal core is created with success");
        }
    }

    /**
     * Initializes Core
     */
    public void initialize() {
        mImsModule.initialize();
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
     * Schedule a background task on Handler for execution
     */
    public void scheduleForBackgroundExecution(Runnable task) {
        mBackgroundHandler.post(task);
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
     */
    public synchronized void startCore() {
        if (mStarted) {
            return;
        }
        mImsModule.start();
        mAddressBookManager.start();
        mLocaleManager.start();
        /* Notify event listener */
        mListener.handleCoreLayerStarted();

        mStarted = true;
        if (sLogger.isActivated()) {
            sLogger.info("RCS core service has been started with success");
        }
    }

    /**
     * Stop the terminal core
     * 
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void stopCore() throws SipPayloadException, SipNetworkException {
        if (!mStarted) {
            return;
        }
        mStopping = true;
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Stop the RCS core service");
        }
        mLocaleManager.stop();
        mAddressBookManager.stop();
        mImsModule.stop();

        mStopping = false;
        mStarted = false;
        if (logActivated) {
            sLogger.info("RCS core service has been stopped with success");
        }
        sInstance = null;
        /* Notify event listener */
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
