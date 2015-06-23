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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.CoreListener;
import com.gsma.rcs.core.ims.network.ImsConnectionManager;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.gsm.CallManager;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConnection;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpSource;
import com.gsma.rcs.core.ims.protocol.sip.SipEventListener;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManagerException;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsService.ImsServiceType;
import com.gsma.rcs.core.ims.service.ImsServiceDispatcher;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpTransferManager;
import com.gsma.rcs.core.ims.service.ipcall.IPCallService;
import com.gsma.rcs.core.ims.service.presence.PresenceService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.terms.TermsConditionsService;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
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
    /**
     * Core
     */
    private Core mCore;

    /**
     * IMS user profile
     */
    public static UserProfile IMS_USER_PROFILE;

    /**
     * IMS connection manager
     */
    private ImsConnectionManager mCnxManager;

    /**
     * Map of IMS services
     */
    private Map<ImsServiceType, ImsService> mServices;

    /**
     * Service dispatcher
     */
    private ImsServiceDispatcher mServiceDispatcher;

    /**
     * Call manager
     */
    private CallManager mCallManager;

    /**
     * flag to indicate whether instantiation is finished
     */
    private boolean mInitializationFinished = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param core Core
     * @param context The context this module is part of
     * @param rcsSettings RCSsettings instance
     * @param contactManager Contact manager accessor
     * @param messagingLog Messaging log accessor
     * @throws CoreException Exception thrown if IMS module failed to be initialized
     */
    public ImsModule(Core core, Context context, RcsSettings rcsSettings,
            ContactManager contactManager, MessagingLog messagingLog) throws CoreException {
        mCore = core;

        if (logger.isActivated()) {
            logger.info("IMS module initialization");
        }

        // Get capability extensions
        ServiceExtensionManager.getInstance(rcsSettings).updateSupportedExtensions(context);

        // Create the IMS connection manager
        try {
            mCnxManager = new ImsConnectionManager(this, rcsSettings);
        } catch (Exception e) {
            throw new CoreException("IMS connection manager initialization has failed", e);
        }

        // Set general parameters
        SipManager.TIMEOUT = rcsSettings.getSipTransactionTimeout();
        RtpSource.CNAME = ImsModule.IMS_USER_PROFILE.getPublicUri();
        MsrpConnection.MSRP_TRACE_ENABLED = rcsSettings.isMediaTraceActivated();
        HttpTransferManager.HTTP_TRACE_ENABLED = rcsSettings.isMediaTraceActivated();

        // Load keystore for certificates
        try {
            KeyStoreManager.loadKeyStore(rcsSettings);
        } catch (KeyStoreManagerException e) {
            throw new CoreException("Can't load keystore manager", e);
        }

        // Instantiates the IMS services
        mServices = new HashMap<ImsServiceType, ImsService>();

        // Create terms & conditions service
        mServices.put(ImsServiceType.TERMS_CONDITIONS,
                new TermsConditionsService(this, rcsSettings));

        // Create capability discovery service
        mServices.put(ImsServiceType.CAPABILITY, new CapabilityService(this, rcsSettings,
                contactManager));

        // Create IM service (mandatory)
        mServices.put(ImsServiceType.INSTANT_MESSAGING, new InstantMessagingService(this, core,
                rcsSettings, contactManager, messagingLog));

        // Create IP call service (optional)
        mServices.put(ImsServiceType.IPCALL, new IPCallService(this, rcsSettings, contactManager));

        // Create richcall service (optional)
        mServices.put(ImsServiceType.RICHCALL, new RichcallService(this, core, contactManager,
                rcsSettings));

        // Create presence service (optional)
        mServices.put(ImsServiceType.PRESENCE, new PresenceService(this, rcsSettings,
                contactManager));

        // Create generic SIP service
        mServices.put(ImsServiceType.SIP, new SipService(this, contactManager, rcsSettings));

        // Create the service dispatcher
        mServiceDispatcher = new ImsServiceDispatcher(this, rcsSettings);

        // Create the call manager
        mCallManager = new CallManager(this, context);

        mInitializationFinished = true;

        if (logger.isActivated()) {
            logger.info("IMS module has been created");
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
        if (logger.isActivated()) {
            logger.info("Start the IMS module");
        }

        // Start the service dispatcher
        mServiceDispatcher.start();

        // Start call monitoring
        mCallManager.startCallMonitoring();

        if (logger.isActivated()) {
            logger.info("IMS module is started");
        }
    }

    /**
     * Stop the IMS module
     * 
     * @throws SipNetworkException
     * @throws SipPayloadException
     */
    public void stop() throws SipPayloadException, SipNetworkException {
        if (logger.isActivated()) {
            logger.info("Stop the IMS module");
        }

        // Stop call monitoring
        mCallManager.stopCallMonitoring();

        // Terminate the connection manager
        mCnxManager.terminate();

        // Terminate the service dispatcher
        mServiceDispatcher.terminate();

        if (logger.isActivated()) {
            logger.info("IMS module has been stopped");
        }
    }

    /**
     * Start IMS services
     */
    public void startImsServices() {
        // Start each services
        for (ImsService imsService : mServices.values()) {
            if (imsService.isActivated()) {
                if (logger.isActivated()) {
                    logger.info("Start IMS service: ".concat(imsService.getClass().getName()));
                }
                imsService.start();
            }
        }
        // Send call manager event
        getCallManager().connectionEvent(true);
    }

    /**
     * Stop IMS services
     * @param reasonCode The reason code
     */
    public void stopImsServices(TerminationReason reasonCode) {
        // Terminate all pending sessions
        terminateAllSessions(reasonCode);

        // Stop each services
        for (ImsService imsService : mServices.values()) {
            if (imsService.isActivated()) {
                if (logger.isActivated()) {
                    logger.info("Stop IMS service: ".concat(imsService.getClass().getName()));
                }
                imsService.stop();
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
                if (logger.isActivated()) {
                    logger.info("Check IMS service: ".concat(imsService.getClass().getName()));
                }
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
     * Returns the IP call service
     * 
     * @return IP call service
     */
    public IPCallService getIPCallService() {
        return (IPCallService) mServices.get(ImsServiceType.IPCALL);
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
     * @param reasonCode The reason code
     */
    public void terminateAllSessions(TerminationReason reasonCode) {
        if (logger.isActivated()) {
            logger.debug("Terminate all sessions");
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
}
