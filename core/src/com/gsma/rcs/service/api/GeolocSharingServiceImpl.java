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

package com.gsma.rcs.service.api;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.content.GeolocContent;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.SessionIdGenerator;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocSharingPersistedStorageAccessor;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.GeolocSharingDeleteTask;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.service.broadcaster.GeolocSharingEventBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.ReasonCode;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing.State;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharingListener;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharingService;

import android.os.IBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Geoloc sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingServiceImpl extends IGeolocSharingService.Stub {

    private final GeolocSharingEventBroadcaster mBroadcaster = new GeolocSharingEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final RichcallService mRichcallService;

    private final ContactsManager mContactsManager;

    private final RichCallHistory mRichcallLog;

    private final LocalContentResolver mLocalContentResolver;

    private final ExecutorService mImOperationExecutor;

    private final Map<String, IGeolocSharing> mGeolocSharingCache = new HashMap<String, IGeolocSharing>();

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(GeolocSharingServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();

    private final Object mImsLock;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param richcallService RichcallService
     * @param richCallLog RichCallHistory
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactsManager
     * @param core Core
     * @param localContentResolver LocalContentResolver
     * @param imOperationExecutor IM ExecutorService
     * @param imsLock ims lock object
     */
    public GeolocSharingServiceImpl(RichcallService richcallService,
            ContactsManager contactsManager, RichCallHistory richCallHistory,
            RcsSettings rcsSettings, LocalContentResolver localContentResolver,
            ExecutorService imOperationExecutor, Object imsLock) {
        if (logger.isActivated()) {
            logger.info("Geoloc sharing service API is loaded.");
        }
        mRichcallService = richcallService;
        mContactsManager = contactsManager;
        mRichcallLog = richCallHistory;
        mRcsSettings = rcsSettings;
        mLocalContentResolver = localContentResolver;
        mImOperationExecutor = imOperationExecutor;
        mImsLock = imsLock;
    }

    /**
     * Close API
     */
    public void close() {
        // Clear list of sessions
        mGeolocSharingCache.clear();

        if (logger.isActivated()) {
            logger.info("Geoloc sharing service API is closed");
        }
    }

    /**
     * Add an geoloc sharing in the list
     * 
     * @param geolocSharing Geoloc sharing
     */
    private void addGeolocSharing(GeolocSharingImpl geolocSharing) {
        if (logger.isActivated()) {
            logger.debug("Add a geoloc sharing in the list (size=" + mGeolocSharingCache.size()
                    + ")");
        }

        mGeolocSharingCache.put(geolocSharing.getSharingId(), geolocSharing);
    }

    /**
     * Remove an geoloc sharing from the list
     * 
     * @param sharingId Sharing ID
     */
    public void removeGeolocSharing(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Remove a geoloc sharing from the list (size="
                    + mGeolocSharingCache.size() + ")");
        }

        mGeolocSharingCache.remove(sharingId);
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
        if (logger.isActivated()) {
            logger.info("Add a service listener");
        }
        synchronized (lock) {
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
        synchronized (lock) {
            mRcsServiceRegistrationEventBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Notifies registration event
     */
    public void notifyRegistration() {
        // Notify listeners
        synchronized (lock) {
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
        synchronized (lock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Receive a new geoloc sharing invitation
     * 
     * @param session Geoloc sharing session
     */
    public void receiveGeolocSharingInvitation(GeolocTransferSession session) {
        if (logger.isActivated()) {
            logger.info(new StringBuilder("Receive geoloc sharing invitation from ")
                    .append(session.getRemoteContact()).append("; displayName=")
                    .append(session.getRemoteDisplayName()).append(".").toString());
        }
        // TODO : Add entry into GeolocSharing provider (to be implemented as part of CR025)

        ContactId contact = session.getRemoteContact();
        String remoteDisplayName = session.getRemoteDisplayName();
        // Update displayName of remote contact
        mContactsManager.setContactDisplayName(contact, remoteDisplayName);

        String sharingId = session.getSessionID();
        GeolocSharingPersistedStorageAccessor persistedStorage = new GeolocSharingPersistedStorageAccessor(
                sharingId, contact, session.getGeoloc(), Direction.INCOMING, mRichcallLog,
                session.getTimestamp());
        GeolocSharingImpl geolocSharing = new GeolocSharingImpl(sharingId, mBroadcaster,
                mRichcallService, this, persistedStorage);
        addGeolocSharing(geolocSharing);
        session.addListener(geolocSharing);
    }

    /**
     * Shares a geolocation with a contact. An exception if thrown if there is no ongoing CS call.
     * The parameter contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an
     * exception is thrown.
     * 
     * @param contact Contact
     * @param geoloc Geolocation info
     * @return Geoloc sharing
     * @throws ServerApiException
     */
    public IGeolocSharing shareGeoloc(ContactId contact, Geoloc geoloc) throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("Initiate a geoloc sharing session with " + contact);
        }

        // Test IMS connection
        ServerApiUtils.testIms();

        try {
            // Create a geoloc content
            String msgId = IdGenerator.generateMessageID();
            long timestamp = System.currentTimeMillis();
            String geolocDoc = ChatUtils.buildGeolocDocument(geoloc,
                    ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId, timestamp);
            byte[] data = geolocDoc.getBytes(UTF8);
            MmContent content = new GeolocContent("geoloc.xml", data.length, data);

            // Initiate a sharing session
            final GeolocTransferSession session = mRichcallService.initiateGeolocSharingSession(
                    contact, content, geoloc, timestamp);
            String sharingId = session.getSessionID();
            mRichcallLog.addOutgoingGeolocSharing(contact, sharingId, geoloc, State.INITIATING,
                    ReasonCode.UNSPECIFIED, timestamp);
            mBroadcaster.broadcastStateChanged(contact, sharingId, State.INITIATING,
                    ReasonCode.UNSPECIFIED);

            GeolocSharingPersistedStorageAccessor persistedStorage = new GeolocSharingPersistedStorageAccessor(
                    sharingId, contact, geoloc, Direction.OUTGOING, mRichcallLog, timestamp);
            GeolocSharingImpl geolocSharing = new GeolocSharingImpl(sharingId, mBroadcaster,
                    mRichcallService, this, persistedStorage);

            // Start the session
            new Thread() {
                public void run() {
                    session.startSession();
                }
            }.start();

            // Add session in the list
            addGeolocSharing(geolocSharing);
            session.addListener(geolocSharing);
            return geolocSharing;

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Unexpected error", e);
            }
            throw new ServerApiException(e.getMessage());
        }
    }

    /**
     * Returns the list of geoloc sharings in progress
     * 
     * @return List of geoloc sharings
     * @throws ServerApiException
     */
    public List<IBinder> getGeolocSharings() throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("Get geoloc sharing sessions");
        }

        try {
            List<IBinder> geolocSharings = new ArrayList<IBinder>(mGeolocSharingCache.size());
            for (IGeolocSharing geolocSharing : mGeolocSharingCache.values()) {
                geolocSharings.add(geolocSharing.asBinder());
            }
            return geolocSharings;

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Unexpected error", e);
            }
            throw new ServerApiException(e.getMessage());
        }
    }

    /**
     * Returns a current geoloc sharing from its unique ID
     * 
     * @param sharingId
     * @return Geoloc sharing
     * @throws ServerApiException
     */
    public IGeolocSharing getGeolocSharing(String sharingId) throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("Get geoloc sharing session " + sharingId);
        }

        IGeolocSharing geolocSharing = mGeolocSharingCache.get(sharingId);
        if (geolocSharing != null) {
            return geolocSharing;
        }
        GeolocSharingPersistedStorageAccessor persistedStorage = new GeolocSharingPersistedStorageAccessor(
                sharingId, mRichcallLog);
        return new GeolocSharingImpl(sharingId, mBroadcaster, mRichcallService, this,
                persistedStorage);
    }

    /**
     * Adds a listener on geoloc sharing events
     * 
     * @param listener Listener
     */
    public void addEventListener2(IGeolocSharingListener listener) {
        if (logger.isActivated()) {
            logger.info("Add a Geoloc sharing event listener");
        }
        synchronized (lock) {
            mBroadcaster.addEventListener(listener);
        }
    }

    /**
     * Removes a listener on geoloc sharing events
     * 
     * @param listener Listener
     */
    public void removeEventListener2(IGeolocSharingListener listener) {
        if (logger.isActivated()) {
            logger.info("Remove a Geoloc sharing event listener");
        }
        synchronized (lock) {
            mBroadcaster.removeEventListener(listener);
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     * @throws ServerApiException
     */
    public int getServiceVersion() throws ServerApiException {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Adds and broadcast that a geoloc sharing invitation was rejected
     * 
     * @param contact Contact ID
     * @param content Geolocation content
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     */
    public void addAndBroadcastGeolocSharingInvitationRejected(ContactId contact,
            GeolocContent content, ReasonCode reasonCode, long timestamp) {
        String sharingId = SessionIdGenerator.getNewId();
        mRichcallLog.addIncomingGeolocSharing(contact, sharingId, GeolocSharing.State.REJECTED,
                reasonCode, timestamp);
        mBroadcaster.broadcastInvitation(sharingId);
    }

    public void setGeolocSharingStateAndReasonCode(ContactId contact, String sharingId,
            State state, ReasonCode reasonCode) {
        mRichcallLog.setGeolocSharingStateAndReasonCode(sharingId, state, reasonCode);
        mBroadcaster.broadcastStateChanged(contact, sharingId, state, reasonCode);
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Deletes all geoloc sharing from history and abort/reject any associated ongoing session if
     * such exists.
     */
    public void deleteGeolocSharings() {
        mImOperationExecutor.execute(new GeolocSharingDeleteTask(this, mRichcallService,
                mLocalContentResolver, mImsLock));
    }

    /**
     * Deletes geoloc sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact
     */
    public void deleteGeolocSharings2(ContactId contact) {
        mImOperationExecutor.execute(new GeolocSharingDeleteTask(this, mRichcallService,
                mLocalContentResolver, mImsLock, contact));
    }

    /**
     * Deletes a geoloc sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId
     */
    public void deleteGeolocSharing(String sharingId) {
        mImOperationExecutor.execute(new GeolocSharingDeleteTask(this, mRichcallService,
                mLocalContentResolver, mImsLock, sharingId));
    }

    public void broadcastDeleted(ContactId contact, List<String> sharingIds) {
        mBroadcaster.broadcastDeleted(contact, sharingIds);
    }
}
