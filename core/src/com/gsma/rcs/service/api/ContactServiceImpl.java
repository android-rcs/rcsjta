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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.ims.service.ContactInfo;
import com.gsma.rcs.core.ims.service.ContactInfo.BlockingState;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.IContactService;
import com.gsma.services.rcs.contact.RcsContact;

import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contact service API implementation
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class ContactServiceImpl extends IContactService.Stub {
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ContactServiceImpl.class.getSimpleName());

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private final RcsSettings mRcsSettings;

    /**
     * Contacts manager
     */
    private final ContactManager mContactManager;

    /**
     * Constructor
     * 
     * @param contactManager Contacts manager
     * @param rcsSettings
     */
    public ContactServiceImpl(ContactManager contactManager, RcsSettings rcsSettings) {
        if (logger.isActivated()) {
            logger.info("Contacts service API is loaded");
        }

        mContactManager = contactManager;
        mRcsSettings = rcsSettings;
    }

    /**
     * Close API
     */
    public void close() {
        if (logger.isActivated()) {
            logger.info("Contacts service API is closed");
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
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        if (logger.isActivated()) {
            logger.info("Add a service listener");
        }
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
        if (logger.isActivated()) {
            logger.info("Remove a service listener");
        }
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
     * Returns the RCS contact infos from its contact ID (i.e. MSISDN)
     * 
     * @param contact Contact ID
     * @return Contact
     * @throws RemoteException
     */
    public RcsContact getRcsContact(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (logger.isActivated()) {
            logger.info("Get RCS contact " + contact);
        }
        try {
            // Read capabilities in the local database
            return getRcsContact(mContactManager.getContactInfo(contact));

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Convert the com.gsma.rcs.core.ims.service.capability.Capabilities instance into a
     * Capabilities instance
     * 
     * @param capabilities com.gsma.rcs.core.ims.service.capability.Capabilities instance
     * @return Capabilities instance
     */
    /* package private */static Capabilities getCapabilities(
            com.gsma.rcs.core.ims.service.capability.Capabilities capabilities) {
        if (capabilities == null) {
            return null;
        }
        return new Capabilities(capabilities.isImageSharingSupported(),
                capabilities.isVideoSharingSupported(), capabilities.isImSessionSupported(),
                capabilities.isFileTransferSupported()
                        || capabilities.isFileTransferHttpSupported(),
                capabilities.isGeolocationPushSupported(), capabilities.getSupportedExtensions(),
                capabilities.isSipAutomata(), capabilities.getTimestampOfLastResponse());
    }

    /**
     * Convert the ContactInfo instance into a RcsContact instance
     * 
     * @param contactInfo the ContactInfo instance
     * @return RcsContact instance
     */
    private RcsContact getRcsContact(ContactInfo contactInfo) {
        // Discard if argument is null
        if (contactInfo == null) {
            return null;
        }
        Capabilities capaApi = getCapabilities(contactInfo.getCapabilities());
        boolean registered = RegistrationState.ONLINE.equals(contactInfo.getRegistrationState());
        boolean blocked = (contactInfo.getBlockingState() == BlockingState.BLOCKED);
        return new RcsContact(contactInfo.getContact(), registered, capaApi,
                contactInfo.getDisplayName(), blocked, contactInfo.getBlockingTimestamp());
    }

    /**
     * Interface to filter ContactInfo
     * 
     * @author YPLO6403
     */
    private interface FilterContactInfo {
        /**
         * The filtering method
         * 
         * @param contactInfo
         * @return true if contactInfo is in the scope
         */
        boolean inScope(ContactInfo contactInfo);
    }

    /**
     * Get a filtered list of RcsContact
     * 
     * @param filterContactInfo the filter (or null if not applicable)
     * @return the filtered list of RcsContact
     */
    private List<RcsContact> getRcsContacts(FilterContactInfo filterContactInfo) {
        List<RcsContact> rcsContacts = new ArrayList<RcsContact>();
        // Read capabilities in the local database
        Set<ContactId> contacts = mContactManager.getRcsContactsFromRcsContactProvider();
        for (ContactId contact : contacts) {
            ContactInfo contactInfo = mContactManager.getContactInfo(contact);
            if (contactInfo != null) {
                if (filterContactInfo == null || filterContactInfo.inScope(contactInfo)) {
                    RcsContact contact2add = getRcsContact(contactInfo);
                    if (contact2add != null) {
                        rcsContacts.add(getRcsContact(contactInfo));
                    }
                }
            }
        }
        return rcsContacts;
    }

    /**
     * Returns the list of rcs contacts
     * 
     * @return List of contacts
     * @throws RemoteException
     */
    public List<RcsContact> getRcsContacts() throws RemoteException {
        if (logger.isActivated()) {
            logger.info("Get rcs contacts");
        }
        try {
            return getRcsContacts(null);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the list of online contacts (i.e. registered)
     * 
     * @return List of contacts
     * @throws RemoteException
     */
    public List<RcsContact> getRcsContactsOnline() throws RemoteException {
        if (logger.isActivated()) {
            logger.info("Get registered rcs contacts");
        }
        try {
            return getRcsContacts(new FilterContactInfo() {

                @Override
                public boolean inScope(ContactInfo contactInfo) {
                    return RegistrationState.ONLINE.equals(contactInfo.getRegistrationState());
                }
            });

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the list of contacts supporting a given extension (i.e. feature tag)
     * 
     * @param serviceId Service ID
     * @return List of contacts
     * @throws RemoteException
     */
    public List<RcsContact> getRcsContactsSupporting(final String serviceId) throws RemoteException {
        if (TextUtils.isEmpty(serviceId)) {
            throw new ServerApiIllegalArgumentException("serviceId must not be null or empty!");
        }
        if (logger.isActivated()) {
            logger.info("Get rcs contacts supporting " + serviceId);
        }
        try {
            return getRcsContacts(new FilterContactInfo() {

                @Override
                public boolean inScope(ContactInfo contactInfo) {
                    com.gsma.rcs.core.ims.service.capability.Capabilities capabilities = contactInfo
                            .getCapabilities();
                    if (capabilities != null) {
                        Set<String> supportedExtensions = capabilities.getSupportedExtensions();
                        if (supportedExtensions != null) {
                            for (String supportedExtension : supportedExtensions) {
                                if (supportedExtension.equals(serviceId)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            });

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
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
        if (logger.isActivated()) {
            logger.debug("getCommonConfiguration");
        }
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Block a contact. Any communication from the given contact will be blocked and redirected to
     * the corresponding spambox.
     * 
     * @param contact Contact ID
     * @throws RemoteException
     */
    public void blockContact(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (logger.isActivated()) {
            logger.info("Block contact " + contact);
        }
        try {
            mContactManager.setBlockingState(contact, BlockingState.BLOCKED);
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Unblock a contact
     * 
     * @param contact Contact ID
     * @throws RemoteException
     */
    public void unblockContact(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (logger.isActivated()) {
            logger.info("Unblock contact " + contact);
        }
        try {
            mContactManager.setBlockingState(contact, BlockingState.NOT_BLOCKED);
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                logger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            logger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }
}
