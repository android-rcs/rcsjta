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

import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.service.SessionIdGenerator;
import com.gsma.rcs.core.ims.service.ipcall.IPCallService;
import com.gsma.rcs.core.ims.service.ipcall.IPCallSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.ipcall.IPCallHistory;
import com.gsma.rcs.provider.ipcall.IPCallPersistedStorageAccessor;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.IPCallEventBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.service.ipcalldraft.IIPCall;
import com.gsma.rcs.service.ipcalldraft.IIPCallListener;
import com.gsma.rcs.service.ipcalldraft.IIPCallPlayer;
import com.gsma.rcs.service.ipcalldraft.IIPCallRenderer;
import com.gsma.rcs.service.ipcalldraft.IIPCallService;
import com.gsma.rcs.service.ipcalldraft.IIPCallServiceConfiguration;
import com.gsma.rcs.service.ipcalldraft.IPCall;
import com.gsma.rcs.service.ipcalldraft.IPCall.ReasonCode;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.contact.ContactId;

import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IP call service API implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallServiceImpl extends IIPCallService.Stub {

    private final IPCallEventBroadcaster mBroadcaster = new IPCallEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final IPCallService mIPCallService;

    private final IPCallHistory mIPCallLog;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final Map<String, IIPCall> mIPCallCache = new HashMap<String, IIPCall>();

    private static final Logger sLogger = Logger.getLogger(IPCallServiceImpl.class.getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();
    
    private final ServerApiUtils mServerApiUtils;

    /**
     * Constructor
     * 
     * @param ipCallService IPCallService
     * @param ipCallLog IPCallHistory
     * @param contactManager ContactManager
     * @param rcsSettings RcsSettings
     * @param serverApiUtils
     */
    public IPCallServiceImpl(IPCallService ipCallService, IPCallHistory ipCallLog,
            ContactManager contactManager, RcsSettings rcsSettings, ServerApiUtils serverApiUtils) {
        if (sLogger.isActivated()) {
            sLogger.info("IP call service API is loaded");
        }
        mIPCallService = ipCallService;
        mIPCallLog = ipCallLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mServerApiUtils = serverApiUtils;
    }

    /**
     * Close API
     */
    public void close() {
        // Clear lists of sessions
        mIPCallCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("IP call service API is closed");
        }
    }

    /**
     * Add an IP Call session in the list
     * 
     * @param ipCall IP call session
     */

    protected void addIPCall(IPCallImpl ipCall) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add an IP Call session in the list (size=" + mIPCallCache.size() + ")");
        }

        mIPCallCache.put(ipCall.getCallId(), ipCall);
    }

    /**
     * Remove an IP Call session from the list
     * 
     * @param sessionId Session ID
     */
    protected void removeIPCall(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove an IP Call session from the list (size=" + mIPCallCache.size()
                    + ")");
        }

        mIPCallCache.remove(sessionId);
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     */
    public boolean isServiceRegistered() {
        return mServerApiUtils.isImsConnected();
    }

    /**
     * Return the reason code for IMS service registration
     * 
     * @return the reason code for IMS service registration
     */
    public int getServiceRegistrationReasonCode() {
        return mServerApiUtils.getServiceRegistrationReasonCode().toInt();
    }

    /**
     * Registers a listener on service registration events
     * 
     * @param listener Service registration listener
     */
    public void addEventListener(IRcsServiceRegistrationListener listener) {
        if (sLogger.isActivated()) {
            sLogger.info("Add a service listener");
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
        if (sLogger.isActivated()) {
            sLogger.info("Remove a service listener");
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
     * Receive a new IP call invitation
     * 
     * @param session IP call session
     */
    public void receiveIPCallInvitation(IPCallSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.info("Receive IP call invitation from " + contact);
        }

        // Update displayName of remote contact
        mContactManager.setContactDisplayName(contact, session.getRemoteDisplayName());

        String callId = session.getSessionID();
        IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(callId,
                mIPCallLog);
        IPCallImpl ipCall = new IPCallImpl(callId, mBroadcaster, mIPCallService, storageAccessor,
                this);
        addIPCall(ipCall);
        session.addListener(ipCall);
    }

    /**
     * Initiates an IP call with a contact. The parameter contact supports the following formats:
     * MSISDN in national or international format, SIP address, SIP-URI or el-URI. If the format of
     * the contact is not supported an exception is thrown.
     * 
     * @param contact Contact ID
     * @param player IP call player
     * @param renderer IP call renderer
     * @return IP call
     * @throws RemoteException
     */
    public IIPCall initiateCall(ContactId contact, IIPCallPlayer player, IIPCallRenderer renderer)
            throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (player == null) {
            throw new ServerApiIllegalArgumentException("player must not be null!");
        }
        if (renderer == null) {
            throw new ServerApiIllegalArgumentException("renderer must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an IP call audio session with " + contact);
        }

        mServerApiUtils.testIms();

        long timestamp = System.currentTimeMillis();
        try {
            final IPCallSession session = mIPCallService.initiateIPCallSession(contact, false,
                    player, renderer, timestamp);

            String callId = session.getSessionID();
            mIPCallLog.addCall(callId, contact, Direction.OUTGOING, session.getAudioContent(),
                    session.getVideoContent(), IPCall.State.INITIATED, ReasonCode.UNSPECIFIED,
                    timestamp);
            mBroadcaster.broadcastIPCallStateChanged(contact, callId, IPCall.State.INITIATED,
                    ReasonCode.UNSPECIFIED);

            IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(
                    callId, contact, Direction.OUTGOING, mIPCallLog, timestamp);
            IPCallImpl ipCall = new IPCallImpl(callId, mBroadcaster, mIPCallService,
                    storageAccessor, this);

            addIPCall(ipCall);
            session.addListener(ipCall);
            session.startSession();
            return ipCall;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            mIPCallLog.addCall(SessionIdGenerator.getNewId(), contact, Direction.OUTGOING, null,
                    null, IPCall.State.FAILED, ReasonCode.FAILED_INITIATION, timestamp);
            ;
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            mIPCallLog.addCall(SessionIdGenerator.getNewId(), contact, Direction.OUTGOING, null,
                    null, IPCall.State.FAILED, ReasonCode.FAILED_INITIATION, timestamp);
            ;
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Initiates an IP call visio with a contact (audio and video). The parameter contact supports
     * the following formats: MSISDN in national or international format, SIP address, SIP-URI or
     * Tel-URI. If the format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact ID
     * @param player IP call player
     * @param renderer IP call renderer
     * @return IP call
     * @throws RemoteException
     */
    public IIPCall initiateVisioCall(ContactId contact, IIPCallPlayer player,
            IIPCallRenderer renderer) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (player == null) {
            throw new ServerApiIllegalArgumentException("player must not be null!");
        }
        if (renderer == null) {
            throw new ServerApiIllegalArgumentException("renderer must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an IP call visio session with " + contact);
        }

        mServerApiUtils.testIms();

        long timestamp = System.currentTimeMillis();
        try {
            final IPCallSession session = mIPCallService.initiateIPCallSession(contact, true,
                    player, renderer, timestamp);

            String callId = session.getSessionID();
            mIPCallLog.addCall(callId, contact, Direction.OUTGOING, session.getAudioContent(),
                    session.getVideoContent(), IPCall.State.INITIATED, ReasonCode.UNSPECIFIED,
                    session.getTimestamp());
            mBroadcaster.broadcastIPCallStateChanged(contact, callId, IPCall.State.INITIATED,
                    ReasonCode.UNSPECIFIED);

            IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(
                    callId, contact, Direction.OUTGOING, mIPCallLog, session.getTimestamp());
            IPCallImpl ipCall = new IPCallImpl(callId, mBroadcaster, mIPCallService,
                    storageAccessor, this);

            addIPCall(ipCall);
            session.addListener(ipCall);
            session.startSession();
            return ipCall;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            mIPCallLog.addCall(SessionIdGenerator.getNewId(), contact, Direction.OUTGOING, null,
                    null, IPCall.State.FAILED, ReasonCode.FAILED_INITIATION, timestamp);
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            mIPCallLog.addCall(SessionIdGenerator.getNewId(), contact, Direction.OUTGOING, null,
                    null, IPCall.State.FAILED, ReasonCode.FAILED_INITIATION, timestamp);
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns a current IP call from its unique ID
     * 
     * @param callId Call ID
     * @return IP call
     * @throws RemoteException
     */
    public IIPCall getIPCall(String callId) throws RemoteException {
        if (TextUtils.isEmpty(callId)) {
            throw new ServerApiIllegalArgumentException("callId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get IP call " + callId);
        }
        try {
            IIPCall ipCall = mIPCallCache.get(callId);
            if (ipCall != null) {
                return ipCall;
            }
            IPCallPersistedStorageAccessor storageAccessor = new IPCallPersistedStorageAccessor(
                    callId, mIPCallLog);
            return new IPCallImpl(callId, mBroadcaster, mIPCallService, storageAccessor, this);

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
     * Returns the list of IP calls in progress
     * 
     * @return List of IP calls
     * @throws RemoteException
     */
    public List<IBinder> getIPCalls() throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.info("Get IP call sessions");
        }

        try {
            List<IBinder> ipCalls = new ArrayList<IBinder>(mIPCallCache.size());
            for (IIPCall ipCall : mIPCallCache.values()) {
                ipCalls.add(ipCall.asBinder());
            }
            return ipCalls;

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
     * Returns the configuration of IP call service
     * 
     * @return Configuration
     * @throws RemoteException
     */
    public IIPCallServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new IPCallServiceConfigurationImpl(mRcsSettings);

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
     * Add and broadcast IP call invitation rejections
     * 
     * @param contact Contact ID
     * @param audioContent Audio content
     * @param videoContent Video content
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     */
    public void addIPCallInvitationRejected(ContactId contact, AudioContent audioContent,
            VideoContent videoContent, ReasonCode reasonCode, long timestamp) {
        String sessionId = SessionIdGenerator.getNewId();
        mIPCallLog.addCall(sessionId, contact, Direction.INCOMING, audioContent, videoContent,
                IPCall.State.REJECTED, reasonCode, timestamp);
        mBroadcaster.broadcastIPCallInvitation(sessionId);
    }

    /**
     * Adds an event listener on IP call events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    public void addEventListener2(IIPCallListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add an IP call event listener");
        }

        try {
            synchronized (lock) {
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
     * Removes an event listener from IP call events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    public void removeEventListener2(IIPCallListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove an IP call event listener");
        }

        try {
            synchronized (lock) {
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
