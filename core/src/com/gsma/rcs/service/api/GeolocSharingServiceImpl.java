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
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.GeolocSharingPersistedStorageAccessor;
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

import android.os.RemoteException;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Geoloc sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocSharingServiceImpl extends IGeolocSharingService.Stub {

    private final GeolocSharingEventBroadcaster mBroadcaster = new GeolocSharingEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final RichcallService mRichcallService;

    private final RichCallHistory mRichcallLog;

    private final Map<String, IGeolocSharing> mGeolocSharingCache = new HashMap<>();

    private static final Logger sLogger = Logger.getLogger(GeolocSharingServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param richcallService RichcallService
     * @param rcsSettings Rcs settings accessor
     * @param richCallHistory Richcall history log accessor
     */
    public GeolocSharingServiceImpl(RichcallService richcallService,
            RichCallHistory richCallHistory, RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.info("Geoloc sharing service API is loaded.");
        }
        mRichcallService = richcallService;
        mRichcallService.register(this);
        mRichcallLog = richCallHistory;
        mRcsSettings = rcsSettings;
    }

    /**
     * Close API
     */
    public void close() {
        // Clear list of sessions
        mGeolocSharingCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("Geoloc sharing service API is closed");
        }
    }

    /**
     * Add an geoloc sharing in the list
     * 
     * @param geolocSharing Geoloc sharing
     * @param sharingId String
     */
    private void addGeolocSharing(GeolocSharingImpl geolocSharing, String sharingId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add an geoloc sharing in the list (size=")
                    .append(mGeolocSharingCache.size()).append(")").toString());
        }
        mGeolocSharingCache.put(sharingId, geolocSharing);
    }

    /**
     * Remove an geoloc sharing from the list
     * 
     * @param sharingId Sharing ID
     */
    public void removeGeolocSharing(String sharingId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a geoloc sharing from the list (size="
                    + mGeolocSharingCache.size() + ")");
        }
        mGeolocSharingCache.remove(sharingId);
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     */
    @Override
    public boolean isServiceRegistered() {
        return ServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    @Override
    public int getServiceRegistrationReasonCode() {
        return ServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    @Override
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Add a service listener");
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
    @Override
    public void removeEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Remove a service listener");
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
     * Receive a new geoloc sharing invitation
     * 
     * @param session Geoloc sharing session
     */
    public void receiveGeolocSharingInvitation(GeolocTransferSession session) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Receive geoloc sharing invitation from ")
                    .append(session.getRemoteContact()).append("; displayName=")
                    .append(session.getRemoteDisplayName()).append(".").toString());
        }
        ContactId contact = session.getRemoteContact();
        String sharingId = session.getSessionID();
        GeolocSharingPersistedStorageAccessor persistedStorage = new GeolocSharingPersistedStorageAccessor(
                sharingId, contact, session.getGeoloc(), Direction.INCOMING, mRichcallLog,
                session.getTimestamp());
        GeolocSharingImpl geolocSharing = new GeolocSharingImpl(sharingId, mBroadcaster,
                mRichcallService, this, persistedStorage);
        addGeolocSharing(geolocSharing, sharingId);
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
     * @throws RemoteException
     */
    public IGeolocSharing shareGeoloc(ContactId contact, Geoloc geoloc) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (geoloc == null) {
            throw new ServerApiIllegalArgumentException("geoloc must not be null!");
        }
        String label = geoloc.getLabel();
        if (label != null) {
            int labelLength = label.length();
            int labelMaxLength = mRcsSettings.getMaxGeolocLabelLength();
            if (labelLength > labelMaxLength) {
                throw new ServerApiIllegalArgumentException(new StringBuilder()
                        .append("geoloc message label length: ").append(labelLength)
                        .append(" exeeds max length: ").append(labelMaxLength).append("!")
                        .toString());
            }
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a geoloc sharing session with ".concat(contact.toString()));
        }
        ServerApiUtils.testIms();
        try {
            String msgId = IdGenerator.generateMessageID();
            long timestamp = System.currentTimeMillis();
            String geolocDoc = ChatUtils.buildGeolocDocument(geoloc, ImsModule.getImsUserProfile()
                    .getPublicUri(), msgId, timestamp);
            byte[] data = geolocDoc.getBytes(UTF8);
            MmContent content = new GeolocContent("geoloc.xml", data.length, data);

            final GeolocTransferSession session = mRichcallService.createGeolocSharingSession(
                    contact, content, geoloc, timestamp);
            final String sharingId = session.getSessionID();
            final GeolocSharingPersistedStorageAccessor persistedStorage = new GeolocSharingPersistedStorageAccessor(
                    sharingId, contact, geoloc, Direction.OUTGOING, mRichcallLog, timestamp);
            final GeolocSharingImpl geolocSharing = new GeolocSharingImpl(sharingId, mBroadcaster,
                    mRichcallService, this, persistedStorage);

            mRichcallLog.addOutgoingGeolocSharing(contact, sharingId, geoloc, State.INITIATING,
                    ReasonCode.UNSPECIFIED, timestamp);

            final GeolocSharingServiceImpl geolocSharingService = this;
            mRichcallService.scheduleGeolocShareOperation(new Runnable() {
                @Override
                public void run() {
                    try {
                        session.addListener(geolocSharing);
                        geolocSharingService.addGeolocSharing(geolocSharing, sharingId);
                        session.startSession();

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Failed initiate geoloc sharing right now!", e);
                    }
                }
            });
            return geolocSharing;

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
     * Returns a current geoloc sharing from its unique ID
     * 
     * @param sharingId sharing ID
     * @return Geoloc sharing
     * @throws RemoteException
     */
    public IGeolocSharing getGeolocSharing(String sharingId) throws RemoteException {
        if (TextUtils.isEmpty(sharingId)) {
            throw new ServerApiIllegalArgumentException("sharingId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get geoloc sharing session ".concat(sharingId));
        }
        try {
            IGeolocSharing geolocSharing = mGeolocSharingCache.get(sharingId);
            if (geolocSharing != null) {
                return geolocSharing;
            }
            GeolocSharingPersistedStorageAccessor persistedStorage = new GeolocSharingPersistedStorageAccessor(
                    sharingId, mRichcallLog);
            return new GeolocSharingImpl(sharingId, mBroadcaster, mRichcallService, this,
                    persistedStorage);

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
     * Adds a listener on geoloc sharing events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    @Override
    public void addEventListener2(IGeolocSharingListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a Geoloc sharing event listener");
        }
        try {
            synchronized (mLock) {
                mBroadcaster.addEventListener(listener);
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
     * Removes a listener on geoloc sharing events
     * 
     * @param listener Listener
     */
    @Override
    public void removeEventListener2(IGeolocSharingListener listener) {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a Geoloc sharing event listener");
        }
        try {
            synchronized (mLock) {
                mBroadcaster.removeEventListener(listener);
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
    @Override
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Adds and broadcast that a geoloc sharing invitation was rejected
     * 
     * @param contact Contact ID
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     */
    public void addGeolocSharingInvitationRejected(ContactId contact, ReasonCode reasonCode,
            long timestamp) {
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
     * 
     * @throws RemoteException
     */
    public void deleteGeolocSharings() throws RemoteException {
        mRichcallService.tryToDeleteGeolocSharings();
    }

    /**
     * Deletes geoloc sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact remote contact
     * @throws RemoteException
     */
    public void deleteGeolocSharings2(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        mRichcallService.tryToDeleteGeolocSharings(contact);
    }

    /**
     * Deletes a geoloc sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId sharing ID
     * @throws RemoteException
     */
    public void deleteGeolocSharing(String sharingId) throws RemoteException {
        if (TextUtils.isEmpty(sharingId)) {
            throw new ServerApiIllegalArgumentException("sharingId must not be null or empty!");
        }
        mRichcallService.tryToDeleteGeolocSharing(sharingId);
    }

    public void broadcastDeleted(ContactId contact, Set<String> sharingIds) {
        mBroadcaster.broadcastDeleted(contact, sharingIds);
    }
}
