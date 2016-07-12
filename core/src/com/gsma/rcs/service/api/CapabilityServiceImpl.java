/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.CapabilitiesBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.ICapabilitiesListener;
import com.gsma.services.rcs.capability.ICapabilityService;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Capability service API implementation
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class CapabilityServiceImpl extends ICapabilityService.Stub {

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final CapabilitiesBroadcaster mCapabilitiesBroadcaster = new CapabilitiesBroadcaster();

    private final RcsSettings mRcsSettings;

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private static final Logger sLogger = Logger.getLogger(CapabilityServiceImpl.class.getName());

    private final ContactManager mContactManager;

    private final CapabilityService mCapabilityService;

    private class CapabilitiesRequester implements Runnable {

        private final ContactId mContact;

        public CapabilitiesRequester(ContactId contact) {
            mContact = contact;
        }

        public void run() {
            try {
                mCapabilityService.requestContactCapabilities(mContact);

            } catch (RuntimeException e) {
                /*
                 * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
                 * which should be handled/fixed within the code. However the cases when we are
                 * executing operations on a thread unhandling such exceptions will eventually lead
                 * to exit the system and thus can bring the whole system down, which is not
                 * intended.
                 */
                sLogger.error("Failed to handle capabilities request for contact: " + mContact, e);
            }
        }
    }

    private class AllCapabilitiesRequester implements Runnable {

        public void run() {
            try {
                mCapabilityService.requestContactCapabilities(mContactManager
                        .getAllContactsFromRcsContactProvider());
            } catch (RuntimeException e) {
                /*
                 * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
                 * which should be handled/fixed within the code. However the cases when we are
                 * executing operations on a thread unhandling such exceptions will eventually lead
                 * to exit the system and thus can bring the whole system down, which is not
                 * intended.
                 */
                sLogger.error("Failed to handle capabilities request for contacts!", e);
            }
        }
    }

    /**
     * Constructor
     * 
     * @param contactManager Contacts manager
     * @param rcsSettings RCS settings accessor
     * @param capabilityService Capacility service
     */
    public CapabilityServiceImpl(CapabilityService capabilityService,
            ContactManager contactManager, RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.info("Capability service API is loaded");
        }
        mCapabilityService = capabilityService;
        mCapabilityService.register(this);
        mContactManager = contactManager;
        mRcsSettings = rcsSettings;
    }

    /**
     * Close API
     */
    public void close() {
        if (sLogger.isActivated()) {
            sLogger.info("Capability service API is closed");
        }
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     */
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.addEventListener(listener);
        }
    }

    /**
     * Unregisters a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    public void removeEventListener(IRcsServiceRegistrationListener listener) {
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Notifies registration event
     */
    public void notifyRegistration() {
        // Notify listeners
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceRegistered();
        }
    }

    /**
     * Notifies unregistration event
     * 
     * @param reasonCode for unregistration
     */
    public void notifyUnRegistration(RcsServiceRegistration.ReasonCode reasonCode) {
        // Notify listeners
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Returns the capabilities supported by the local end user. The supported capabilities are
     * fixed by the MNO and read during the provisioning.
     * 
     * @return Capabilities
     * @throws RemoteException
     */
    public Capabilities getMyCapabilities() throws RemoteException {
        try {
            return ContactServiceImpl.getCapabilities(mRcsSettings.getMyCapabilities());

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the capabilities of a given contact from the local database. This method does not
     * request any network update to the remote contact. The parameter contact supports the
     * following formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact ContactId
     * @return Capabilities
     * @throws RemoteException
     */
    public Capabilities getContactCapabilities(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            com.gsma.rcs.core.ims.service.capability.Capabilities caps = mContactManager
                    .getContactCapabilities(contact);
            // TODO update code so not to insert default capabilities in provider
            if (caps == null
                    || caps.getTimestampOfLastResponse() == com.gsma.rcs.core.ims.service.capability.Capabilities.INVALID_TIMESTAMP) {
                // no capabilities are known, returns null as per 1.5.1 specification
                return null;
            }
            // Read capabilities in the local database
            return ContactServiceImpl.getCapabilities(mContactManager
                    .getContactCapabilities(contact));

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Requests capabilities to a remote contact. This method initiates in background a new
     * capability request to the remote contact by sending a SIP OPTIONS. The result of the
     * capability request is sent asynchronously via callback method of the capabilities listener. A
     * capability refresh is only sent if the timestamp associated to the capability has expired
     * (the expiration value is fixed via MNO provisioning). The parameter contact supports the
     * following formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown. The result of
     * the capability refresh request is provided to all the clients that have registered the
     * listener for this event.
     * 
     * @param contact ContactId
     * @throws RemoteException
     */
    public void requestContactCapabilities(final ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        ServerApiUtils.testIms();
        mCapabilityService.scheduleCapabilityOperation(new CapabilitiesRequester(contact));
    }

    /**
     * Receive capabilities from a contact
     * 
     * @param contact ContactId
     * @param capabilities Capabilities
     */
    public void receiveCapabilities(ContactId contact,
            com.gsma.rcs.core.ims.service.capability.Capabilities capabilities) {
        synchronized (mLock) {
            // Create capabilities instance
            Capabilities c = ContactServiceImpl.getCapabilities(capabilities);
            // Notify capabilities listeners
            notifyListeners(contact, c);
        }
    }

    /**
     * Notify listeners
     * 
     * @param contact ContactId
     * @param capabilities Capabilities
     */
    private void notifyListeners(ContactId contact, Capabilities capabilities) {
        mCapabilitiesBroadcaster.broadcastCapabilitiesReceived(contact, capabilities);
    }

    /**
     * Requests capabilities for all contacts existing in the local address book. This method
     * initiates in background new capability requests for each contact of the address book by
     * sending SIP OPTIONS. The result of a capability request is sent asynchronously via callback
     * method of the capabilities listener. A capability refresh is only sent if the timestamp
     * associated to the capability has expired (the expiration value is fixed via MNO
     * provisioning). The result of the capability refresh request is provided to all the clients
     * that have registered the listener for this event.
     * 
     * @throws RemoteException
     */
    public void requestAllContactsCapabilities() throws RemoteException {
        ServerApiUtils.testIms();
        mCapabilityService.scheduleCapabilityOperation(new AllCapabilitiesRequester());
    }

    @Override
    public void requestContactCapabilities2(List<ContactId> contacts) throws RemoteException {
        ServerApiUtils.testIms();
        if (contacts == null) {
            throw new ServerApiIllegalArgumentException("contacts must not be null!");
        }
        final Set<ContactId> setOfContacts = new HashSet<>(contacts);
        mCapabilityService.scheduleCapabilityOperation(new Runnable() {

            @Override
            public void run() {
                mCapabilityService.requestContactCapabilities(setOfContacts);
            }
        });
    }

    /**
     * Registers a capabilities listener on any contact
     * 
     * @param listener Capabilities listener
     * @throws RemoteException
     */
    public void addCapabilitiesListener(ICapabilitiesListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mCapabilitiesBroadcaster.addCapabilitiesListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Unregisters a capabilities listener
     * 
     * @param listener Capabilities listener
     * @throws RemoteException
     */
    public void removeCapabilitiesListener(ICapabilitiesListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mCapabilitiesBroadcaster.removeCapabilitiesListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Registers a listener for receiving capabilities of a given contact
     * 
     * @param contact ContactId
     * @param listener Capabilities listener
     * @throws RemoteException
     */
    public void addCapabilitiesListener2(ContactId contact, ICapabilitiesListener listener)
            throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mCapabilitiesBroadcaster.addContactCapabilitiesListener(contact, listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Unregisters a listener of capabilities for a given contact
     * 
     * @param contact ContactId
     * @param listener Capabilities listener
     * @throws RemoteException
     */
    public void removeCapabilitiesListener2(ContactId contact, ICapabilitiesListener listener)
            throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        try {
            synchronized (mLock) {
                mCapabilitiesBroadcaster.removeContactCapabilitiesListener(contact, listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     */
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

}
