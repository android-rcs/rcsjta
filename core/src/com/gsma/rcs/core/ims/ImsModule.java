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

package com.gsma.rcs.core.ims;

import com.gsma.rcs.addressbook.AddressBookManager;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreListener;
import com.gsma.rcs.core.ims.network.ImsConnectionManager;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.gsm.CallManager;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConnection;
import com.gsma.rcs.core.ims.protocol.sip.SipEventListener;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsService.ImsServiceType;
import com.gsma.rcs.core.ims.service.ImsServiceDispatcher;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpTransferManager;
import com.gsma.rcs.core.ims.service.presence.PresenceService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.terms.TermsConditionsService;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * IMS module
 * 
 * @author JM. Auffret
 */
public class ImsModule implements SipEventListener {

    private Core mCore;

    private static UserProfile sImsUserProfile;

    private ImsConnectionManager mCnxManager;

    private Map<ImsServiceType, ImsService> mServices;

    private ImsServiceDispatcher mServiceDispatcher;

    private final CallManager mCallManager;

    private final ServiceExtensionManager mExtensionManager;

    /**
     * flag to indicate whether instantiation is finished
     */
    private boolean mInitializationFinished = false;

    private final RcsSettings mRcsSettings;

    private static final Logger sLogger = Logger.getLogger(ImsModule.class.getName());

    /**
     * Constructor
     * 
     * @param core Core
     * @param ctx The context this module is part of
     * @param localContentResolver The local content resolver
     * @param rcsSettings RCSsettings instance
     * @param contactManager Contact manager accessor
     * @param messagingLog Messaging log accessor
     * @param historyLog The history log accessor
     * @param richCallHistory The rich call accessor
     * @param addressBookManager The address book manager instance
     */
    public ImsModule(Core core, Context ctx, LocalContentResolver localContentResolver,
            RcsSettings rcsSettings, ContactManager contactManager, MessagingLog messagingLog,
            HistoryLog historyLog, RichCallHistory richCallHistory,
            AddressBookManager addressBookManager) {
        mCore = core;
        mRcsSettings = rcsSettings;

        mExtensionManager = new ServiceExtensionManager(this, ctx, mCore, rcsSettings);
        mCnxManager = new ImsConnectionManager(this, ctx, mCore, rcsSettings);
        mCallManager = new CallManager(this, ctx);

        mServices = new HashMap<>();
        mServices.put(ImsServiceType.TERMS_CONDITIONS,
                new TermsConditionsService(this, rcsSettings));
        CapabilityService capabilityService = new CapabilityService(this, rcsSettings,
                contactManager, addressBookManager);
        mServices.put(ImsServiceType.CAPABILITY, capabilityService);
        mServices.put(ImsServiceType.INSTANT_MESSAGING, new InstantMessagingService(this,
                rcsSettings, contactManager, messagingLog, historyLog, localContentResolver, ctx,
                core));
        mServices
                .put(ImsServiceType.RICHCALL, new RichcallService(this, richCallHistory,
                        contactManager, rcsSettings, mCallManager, localContentResolver,
                        capabilityService));
        mServices.put(ImsServiceType.PRESENCE, new PresenceService(this, ctx, rcsSettings,
                contactManager, addressBookManager));
        mServices.put(ImsServiceType.SIP, new SipService(this, contactManager, rcsSettings));

        mServiceDispatcher = new ImsServiceDispatcher(this, rcsSettings);

        if (sLogger.isActivated()) {
            sLogger.info("IMS module creation");
        }
    }

    /**
     * Initializes IMS module
     */
    public void initialize() {
        SipManager.setTimeout(mRcsSettings.getSipTransactionTimeout());
        MsrpConnection.setMsrpTraceEnabled(mRcsSettings.isMediaTraceActivated());
        HttpTransferManager.setHttpTraceEnabled(mRcsSettings.isMediaTraceActivated());

        mCnxManager.initialize();
        getInstantMessagingService().initialize();
        getRichcallService().initialize();
        getPresenceService().initialize();

        mInitializationFinished = true;
        if (sLogger.isActivated()) {
            sLogger.info("IMS module initialization");
        }
    }

    /**
     * Returns the SIP manager
     * 
     * @return SIP manager
     */
    public SipManager getSipManager() {
        return getCurrentNetworkInterface().getSipManager();
    }

    /**
     * Returns the current network interface
     * 
     * @return Network interface
     */
    public ImsNetworkInterface getCurrentNetworkInterface() {
        return mCnxManager.getCurrentNetworkInterface();
    }

    /**
     * Is connected to a Wi-Fi access
     * 
     * @return Boolean
     */
    public boolean isConnectedToWifiAccess() {
        return mCnxManager.isConnectedToWifi();
    }

    /**
     * Is connected to a mobile access
     * 
     * @return Boolean
     */
    public boolean isConnectedToMobileAccess() {
        return mCnxManager.isConnectedToMobile();
    }

    /**
     * Returns the ImsConnectionManager
     * 
     * @return ImsConnectionManager
     */
    public ImsConnectionManager getImsConnectionManager() {
        return mCnxManager;
    }

    /**
     * Start the IMS module
     */
    public void start() {
        if (sLogger.isActivated()) {
            sLogger.info("Start the IMS module");
        }
        mExtensionManager.start();
        mServiceDispatcher.start();
        mCallManager.start();
        if (sLogger.isActivated()) {
            sLogger.info("IMS module is started");
        }
    }

    /**
     * Stop the IMS module
     * 
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    public void stop() throws PayloadException, NetworkException, ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.info("Stop the IMS module");
        }
        mCallManager.stop();
        mCnxManager.terminate();
        mServiceDispatcher.terminate();
        mExtensionManager.stop();
        if (sLogger.isActivated()) {
            sLogger.info("IMS module has been stopped");
        }
    }

    /**
     * Start IMS services
     * 
     * @throws NetworkException
     * @throws PayloadException
     * @throws ContactManagerException
     */
    public void startImsServices() throws PayloadException, NetworkException,
            ContactManagerException {
        for (ImsService imsService : mServices.values()) {
            if (imsService.isActivated()) {
                if (sLogger.isActivated()) {
                    sLogger.info("Start IMS service: ".concat(imsService.getClass().getName()));
                }
                imsService.start();
            }
        }
        getCallManager().connectionEvent(true);
    }

    /**
     * Stop IMS services
     * 
     * @param reasonCode The reason code
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    public void stopImsServices(TerminationReason reasonCode) throws PayloadException,
            NetworkException, ContactManagerException {
        // Terminate all pending sessions
        terminateAllSessions(reasonCode);

        // Stop each services
        for (ImsService imsService : mServices.values()) {
            try {
                if (imsService.isActivated()) {
                    if (sLogger.isActivated()) {
                        sLogger.info("Stop IMS service: ".concat(imsService.getClass().getName()));
                    }
                    imsService.stop();
                }
            } catch (PayloadException e) {
                sLogger.error(
                        "Unable to stop IMS service: ".concat(imsService.getClass().getName()), e);
            } catch (NetworkException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
            }
        }
        // Send call manager event
        getCallManager().connectionEvent(false);
    }

    /**
     * Check IMS services
     */
    public void checkImsServices() {
        for (ImsService imsService : mServices.values()) {
            if (imsService.isActivated()) {
                imsService.check();
            }
        }
    }

    /**
     * Returns the call manager
     * 
     * @return Call manager
     */
    public CallManager getCallManager() {
        return mCallManager;
    }

    /**
     * Returns the IMS services
     * 
     * @return Collection of IMS service
     */
    public Collection<ImsService> getImsServices() {
        return mServices.values();
    }

    /**
     * Returns the terms & conditions service
     * 
     * @return Terms & conditions service
     */
    public TermsConditionsService getTermsConditionsService() {
        return (TermsConditionsService) mServices.get(ImsServiceType.TERMS_CONDITIONS);
    }

    /**
     * Returns the capability service
     * 
     * @return Capability service
     */
    public CapabilityService getCapabilityService() {
        return (CapabilityService) mServices.get(ImsServiceType.CAPABILITY);
    }

    /**
     * Returns the rich call service
     * 
     * @return Richcall service
     */
    public RichcallService getRichcallService() {
        return (RichcallService) mServices.get(ImsServiceType.RICHCALL);
    }

    /**
     * Returns the presence service
     * 
     * @return Presence service
     */
    public PresenceService getPresenceService() {
        return (PresenceService) mServices.get(ImsServiceType.PRESENCE);
    }

    /**
     * Returns the Instant Messaging service
     * 
     * @return Instant Messaging service
     */
    public InstantMessagingService getInstantMessagingService() {
        return (InstantMessagingService) mServices.get(ImsServiceType.INSTANT_MESSAGING);
    }

    /**
     * Returns the SIP service
     * 
     * @return SIP service
     */
    public SipService getSipService() {
        return (SipService) mServices.get(ImsServiceType.SIP);
    }

    /**
     * Return the core instance
     * 
     * @return Core instance
     */
    public Core getCore() {
        return mCore;
    }

    /**
     * Return the core listener
     * 
     * @return Core listener
     */
    public CoreListener getCoreListener() {
        return mCore.getListener();
    }

    /**
     * Receive SIP request
     * 
     * @param request SIP request
     */
    public void receiveSipRequest(SipRequest request) {
        // Post the incoming request to the service dispatcher
        mServiceDispatcher.postSipRequest(request);
    }

    /**
     * This function is used when all session needs to terminated in both invitation pending and
     * started state.
     * 
     * @param reasonCode The reason code
     * @throws NetworkException
     * @throws PayloadException
     */
    public void terminateAllSessions(TerminationReason reasonCode) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Terminate all sessions");
        }
        for (ImsService service : getImsServices()) {
            service.terminateAllSessions(reasonCode);
        }
    }

    /**
     * Check whether ImsModule instantiation has finished
     * 
     * @return true if ImsModule is completely initialized
     */
    public boolean isInitializationFinished() {
        return mInitializationFinished;
    }

    /**
     * @return true is device is in roaming
     */
    public boolean isInRoaming() {
        return mCnxManager.isInRoaming();
    }

    /**
     * Gets IMS user profile
     * 
     * @return IMS user profile
     */
    public static UserProfile getImsUserProfile() {
        return sImsUserProfile;
    }

    /**
     * Sets IMS user profile
     * 
     * @param profile The user profile
     */
    public static void setImsUserProfile(UserProfile profile) {
        sImsUserProfile = profile;
    }
}
