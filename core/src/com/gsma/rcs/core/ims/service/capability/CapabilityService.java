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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.addressbook.AddressBookEventListener;
import com.gsma.rcs.addressbook.AddressBookManager;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.core.ims.service.capability.SyncContactTask.ISyncContactTaskListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.CapabilityServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capability discovery service
 * 
 * @author jexa7410
 */
public class CapabilityService extends ImsService implements AddressBookEventListener {

    private static final String CAPABILITIES_OPERATION_THREAD_NAME = "CapabiltiesOps";

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private OptionsManager mOptionsManager;

    private AnonymousFetchManager mAnonymousFetchManager;

    private PollingManager mPollingManager;

    private ExecutorService mSyncExecutor;

    private final AddressBookManager mAddressBookManager;

    private final static Logger sLogger = Logger.getLogger(CapabilityService.class.getSimpleName());

    private final ISyncContactTaskListener mISyncContactTaskListener;

    private static final int MAX_CONTACTS_TO_DISPLAY = 10;

    /**
     * This purpose of this handler is to make the request asynchronous with the mechanisms provider
     * by android by placing the request in the main thread message queue.
     */
    private final Handler mCapabilityOperationsHandler;

    private CapabilityServiceImpl mCapabilityService;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RCS settings accessor
     * @param contactsManager Contact manager accessor
     * @param addressBookManager The address book manager instance
     */
    public CapabilityService(ImsModule parent, RcsSettings rcsSettings,
            ContactManager contactsManager, AddressBookManager addressBookManager) {
        super(parent, true);
        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
        mAddressBookManager = addressBookManager;

        mCapabilityOperationsHandler = allocateBgHandler(CAPABILITIES_OPERATION_THREAD_NAME);
        mISyncContactTaskListener = new ISyncContactTaskListener() {
            @Override
            public void endOfSyncContactTask() {
                if (!isServiceStarted()) {
                    return;
                }
                /* Listen to address book changes */
                mAddressBookManager.addAddressBookListener(CapabilityService.this);
                mPollingManager.start();
                if (sLogger.isActivated()) {
                    sLogger.debug("Capability service initialize");
                }
            }
        };

        mPollingManager = new PollingManager(this, mRcsSettings, mContactManager);
        mOptionsManager = new OptionsManager(parent, mRcsSettings, mContactManager);
        mAnonymousFetchManager = new AnonymousFetchManager(parent, mRcsSettings, mContactManager);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public void register(CapabilityServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mCapabilityService = service;
    }

    public void scheduleCapabilityOperation(Runnable runnable) {
        mCapabilityOperationsHandler.post(runnable);
    }

    @Override
    public synchronized void start() {
        if (isServiceStarted()) {
            /* Already started */
            return;
        }
        setServiceStarted(true);
        mOptionsManager.start();

        /* Force a first capability check */
        mSyncExecutor = Executors.newSingleThreadExecutor();
        synchronizeContacts();

        if (sLogger.isActivated()) {
            sLogger.debug("Capability service start");
        }
    }

    @Override
    public synchronized void stop(ImsServiceSession.TerminationReason reasonCode) {
        if (!isServiceStarted()) {
            /* Already stopped */
            return;
        }
        setServiceStarted(false);

        mSyncExecutor.shutdownNow();

        mPollingManager.stop();
        /* Stop listening to address book changes */
        mAddressBookManager.removeAddressBookListener(this);
        mOptionsManager.stop();

        if (ImsServiceSession.TerminationReason.TERMINATION_BY_SYSTEM == reasonCode) {
            mCapabilityOperationsHandler.getLooper().quit();
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Capability service stop");
        }
    }

    /**
     * Check the IMS service
     */
    public void check() {
    }

    /**
     * Get the options manager
     * 
     * @return Options manager
     */
    public OptionsManager getOptionsManager() {
        return mOptionsManager;
    }

    /**
     * Get the options manager
     * 
     * @return Options manager
     */
    public AnonymousFetchManager getAnonymousFetchManager() {
        return mAnonymousFetchManager;
    }

    /**
     * Request contact capabilities
     * 
     * @param contact Contact identifier
     */
    public synchronized void requestContactCapabilities(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Request capabilities for ".concat(contact.toString()));
        }
        mOptionsManager.requestCapabilities(contact);
    }

    /**
     * Request capabilities for a set of contacts
     * 
     * @param contacts Set of contact identifiers
     */
    public void requestContactCapabilities(Set<ContactId> contacts) {
        if (sLogger.isActivated()) {
            int nbOfContactsToQuery = contacts.size();
            if (nbOfContactsToQuery > MAX_CONTACTS_TO_DISPLAY) {
                sLogger.debug("Request capabilities for " + nbOfContactsToQuery + " contacts");
            } else {
                sLogger.debug("Request capabilities for ".concat(Arrays.toString(contacts.toArray())));
            }
        }
        mOptionsManager.requestCapabilities(contacts);
    }

    /**
     * Receive a capability request (options procedure)
     * 
     * @param options Received options message
     */
    public void onCapabilityRequestReceived(final SipRequest options) {
        scheduleCapabilityOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mOptionsManager.onCapabilityRequestReceived(options);
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive capability request! (" + e.getMessage()
                                + ")");
                    }
                } catch (PayloadException e) {
                    sLogger.error("Failed to receive capability request!", e);
                } catch (ContactManagerException e) {
                    sLogger.error("Failed to receive capability request!", e);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive capability request!", e);
                }
            }
        });
    }

    /**
     * Receive a notification (anonymous fetch procedure)
     * 
     * @param notify Received notify
     */
    public void onNotificationReceived(final SipRequest notify) {
        scheduleCapabilityOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    mAnonymousFetchManager.onNotificationReceived(notify);
                } catch (ContactManagerException e) {
                    sLogger.error("Failed to receive capability notification!", e);
                } catch (PayloadException e) {
                    sLogger.error("Failed to receive capability notification!", e);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive capability notification!", e);
                }
            }
        });
    }

    private void synchronizeContacts() {
        mSyncExecutor.execute(new SyncContactTask(mISyncContactTaskListener, this, mContactManager,
                mAddressBookManager, mPollingManager, mOptionsManager));
    }

    /**
     * Address book content has changed.<br>
     * This method requests update of capabilities for non RCS contacts (which capabilities are
     * unknown).<br>
     * This method set contact information for RCS contacts not yet aggregated.
     * 
     * @throws ContactManagerException thrown if RCS contact aggregation fails
     */
    @Override
    public void handleAddressBookHasChanged() throws ContactManagerException {
        if (sLogger.isActivated()) {
            sLogger.debug("handle address book changes");
        }
        synchronizeContacts();
    }

    /**
     * Reset the content sharing capabilities for a given contact identifier
     * 
     * @param contact Contact identifier
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    public void resetContactCapabilitiesForContentSharing(ContactId contact)
            throws PayloadException, NetworkException, ContactManagerException {
        try {
            Capabilities capabilities = mContactManager.getContactCapabilities(contact);
            if (capabilities == null
                    || (!capabilities.isImageSharingSupported() && !capabilities
                            .isVideoSharingSupported())) {
                return;
            }
            CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder(capabilities);
            /* Force a reset of content sharing capabilities */
            capaBuilder.setImageSharing(false);
            capaBuilder.setVideoSharing(false);
            capabilities = capaBuilder.build();
            mContactManager.setContactCapabilities(contact, capabilities);

            onReceivedCapabilities(contact, capabilities);

        } catch (FileAccessException e) {
            throw new PayloadException("Failed to reset content share capabilities for contact : "
                    + contact, e);
        }
    }

    /**
     * Capabilities update notification has been received
     * 
     * @param contact Contact identifier
     * @param capabilities Capabilities
     */
    public void onReceivedCapabilities(ContactId contact, Capabilities capabilities) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle capabilities update notification for " + contact + " ("
                    + capabilities.toString() + ")");
        }
        mCapabilityService.receiveCapabilities(contact, capabilities);
    }

}
