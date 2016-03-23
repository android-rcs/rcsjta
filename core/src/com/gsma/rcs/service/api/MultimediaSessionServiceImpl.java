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

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.protocol.rtp.format.data.DataFormat;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.MultimediaMessagingSessionEventBroadcaster;
import com.gsma.rcs.service.broadcaster.MultimediaStreamingSessionEventBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.IMultimediaMessagingSession;
import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.IMultimediaSessionService;
import com.gsma.services.rcs.extension.IMultimediaSessionServiceConfiguration;
import com.gsma.services.rcs.extension.IMultimediaStreamingSession;
import com.gsma.services.rcs.extension.IMultimediaStreamingSessionListener;
import com.gsma.services.rcs.extension.InstantMultimediaMessageIntent;
import com.gsma.services.rcs.extension.MultimediaSession.State;

import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multimedia session API service
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionServiceImpl extends IMultimediaSessionService.Stub {

    private final MultimediaMessagingSessionEventBroadcaster mMultimediaMessagingSessionEventBroadcaster = new MultimediaMessagingSessionEventBroadcaster();

    private final MultimediaStreamingSessionEventBroadcaster mMultimediaStreamingSessionEventBroadcaster = new MultimediaStreamingSessionEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final SipService mSipService;

    private final RcsSettings mRcsSettings;

    private final Map<String, IMultimediaMessagingSession> mMultimediaMessagingCache = new HashMap<>();

    private final Map<String, IMultimediaStreamingSession> mMultimediaStreamingCache = new HashMap<>();

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private static final Logger sLogger = Logger.getLogger(MultimediaSessionServiceImpl.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param sipService SipService
     * @param rcsSettings RcsSettings
     */
    public MultimediaSessionServiceImpl(SipService sipService, RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.info("Multimedia session API is loaded");
        }
        mSipService = sipService;
        mSipService.register(this);
        mRcsSettings = rcsSettings;
    }

    /**
     * Close API
     */
    public void close() {
        // Clear list of sessions
        mMultimediaMessagingCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("Multimedia session service API is closed");
        }
    }

    /**
     * Add a multimedia messaging in the list
     * 
     * @param multimediaMessaging Multimedia messaging implementation
     */
    private void addMultimediaMessaging(MultimediaMessagingSessionImpl multimediaMessaging) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a MultimediaMessaging in the list (size="
                    + mMultimediaMessagingCache.size() + ")");
        }

        mMultimediaMessagingCache.put(multimediaMessaging.getSessionId(), multimediaMessaging);
    }

    /**
     * Remove a multimedia messaging from the list
     * 
     * @param sessionId Session ID
     */
    /* package private */void removeMultimediaMessaging(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a MultimediaMessaging from the list (size="
                    + mMultimediaMessagingCache.size() + ")");
        }

        mMultimediaMessagingCache.remove(sessionId);
    }

    /**
     * Add a multimedia streaming in the list
     * 
     * @param multimediaStreaming Multimedia streaming implementation
     */
    private void addMultimediaStreaming(MultimediaStreamingSessionImpl multimediaStreaming) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a MultimediaStreaming in the list (size="
                    + mMultimediaMessagingCache.size() + ")");
        }

        mMultimediaStreamingCache.put(multimediaStreaming.getSessionId(), multimediaStreaming);
    }

    /**
     * Remove a multimedia streaming from the list
     * 
     * @param sessionId Session ID
     */
    /* package private */void removeMultimediaStreaming(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a MultimediaStreaming from the list (size="
                    + mMultimediaMessagingCache.size() + ")");
        }

        mMultimediaStreamingCache.remove(sessionId);
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
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Receive a new SIP session invitation with MRSP media
     * 
     * @param session SIP session
     */
    public void receiveSipMsrpSessionInvitation(GenericSipMsrpSession session) {
        ContactId remote = session.getRemoteContact();
        MultimediaMessagingSessionImpl multimediaMessaging = new MultimediaMessagingSessionImpl(
                session.getSessionID(), mMultimediaMessagingSessionEventBroadcaster, mSipService,
                this, Direction.INCOMING, remote, session.getServiceId(), State.INVITED);
        session.addListener(multimediaMessaging);
        addMultimediaMessaging(multimediaMessaging);
    }

    /**
     * Receive a new SIP session invitation with RTP media
     * 
     * @param session SIP session
     */
    public void receiveSipRtpSessionInvitation(GenericSipRtpSession session) {
        ContactId remote = session.getRemoteContact();
        MultimediaStreamingSessionImpl multimediaStreaming = new MultimediaStreamingSessionImpl(
                session.getSessionID(), mMultimediaStreamingSessionEventBroadcaster, mSipService,
                this, Direction.INCOMING, remote, session.getServiceId(), State.INVITED);
        session.addListener(multimediaStreaming);
        addMultimediaStreaming(multimediaStreaming);
    }

    /**
     * Returns the configuration of the multimedia session service
     * 
     * @return Configuration
     * @throws RemoteException
     */
    public IMultimediaSessionServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new MultimediaSessionServiceConfigurationImpl(mRcsSettings);

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
     * Initiates a new session for real time messaging with a remote contact and for a given service
     * extension. The messages are exchanged in real time during the session and may be from any
     * type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not
     * supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact ID
     * @return Multimedia messaging session
     * @throws RemoteException
     */
    public IMultimediaMessagingSession initiateMessagingSession(String serviceId, ContactId contact)
            throws RemoteException {
        return initiateMessagingSession2(serviceId, contact, null, null);
    }

    /**
     * Initiates a new session for real time messaging with a remote contact and for a given service
     * extension. The messages are exchanged in real time during the session are specified by the
     * parameter accept-types and accept-wrapped-types. The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the
     * format of the contact is not supported an exception is thrown.
     *
     * @param serviceId Service ID
     * @param contact Contact ID
     * @param acceptTypes Accept-types related to exchanged messages (may be null or empty)
     * @param acceptWrappedTypes Accept-wrapped-types related to exchanged messages (may be null or
     *            empty)
     * @return Multimedia messaging session
     * @throws RemoteException
     */
    public IMultimediaMessagingSession initiateMessagingSession2(String serviceId,
            ContactId contact, String[] acceptTypes, String[] acceptWrappedTypes)
            throws RemoteException {
        if (TextUtils.isEmpty(serviceId)) {
            throw new ServerApiIllegalArgumentException("serviceId must not be null or empty!");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a multimedia messaging session with " + contact);
        }
        ServerApiUtils.testImsExtension(serviceId);
        try {
            String featureTag = FeatureTags.FEATURE_RCSE + "=\""
                    + FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + serviceId + "\"";
            final GenericSipMsrpSession session = mSipService.createMsrpSession(contact,
                    featureTag, acceptTypes, acceptWrappedTypes);
            MultimediaMessagingSessionImpl multiMediaMessaging = new MultimediaMessagingSessionImpl(
                    session.getSessionID(), mMultimediaMessagingSessionEventBroadcaster,
                    mSipService, this, Direction.OUTGOING, contact, serviceId, State.INITIATING);

            session.addListener(multiMediaMessaging);
            addMultimediaMessaging(multiMediaMessaging);
            session.startSession();
            return multiMediaMessaging;

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
     * Returns a current messaging session from its unique session ID
     * 
     * @param sessionId Session ID
     * @return Multimedia messaging session
     * @throws RemoteException
     */
    public IMultimediaMessagingSession getMessagingSession(String sessionId) throws RemoteException {
        if (TextUtils.isEmpty(sessionId)) {
            throw new ServerApiIllegalArgumentException("sessionId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get multimedia messaging ".concat(sessionId));
        }
        try {
            return mMultimediaMessagingCache.get(sessionId);

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
     * Returns the list of messaging sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return List of sessions
     * @throws RemoteException
     */
    public List<IBinder> getMessagingSessions(String serviceId) throws RemoteException {
        if (TextUtils.isEmpty(serviceId)) {
            throw new ServerApiIllegalArgumentException("serviceId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get multimedia messaging sessions for service ".concat(serviceId));
        }
        try {
            List<IBinder> multimediaMessagingSessions = new ArrayList<>();
            for (IMultimediaMessagingSession multimediaMessagingSession : mMultimediaMessagingCache
                    .values()) {
                if (multimediaMessagingSession.getServiceId().contains(serviceId)) {
                    multimediaMessagingSessions.add(multimediaMessagingSession.asBinder());
                }
            }
            return multimediaMessagingSessions;

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
     * Initiates a new session for real time streaming with a remote contact and for a given service
     * extension. The payload are exchanged in real time during the session and may be from any
     * type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not
     * supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact ID
     * @return Multimedia streaming session
     * @throws RemoteException
     */
    public IMultimediaStreamingSession initiateStreamingSession(String serviceId, ContactId contact)
            throws RemoteException {
        return initiateStreamingSession2(serviceId, contact, DataFormat.ENCODING);
    }

    /**
     * Initiates a new session for real time streaming with a remote contact for a given service
     * extension and encoding (ie. rtpmap format containing <encoding name>/<clock rate> and
     * optional parameters if needed. The payload are exchanged in real time during the session and
     * may be from any type. The parameter contact supports the following formats: MSISDN in
     * national or international format, SIP address, SIP-URI or Tel-URI. If the format of the
     * contact is not supported an exception is thrown.
     *
     * @param serviceId Service ID
     * @param contact Contact ID
     ** @param encoding Encoding payload format
     * @return Multimedia streaming session
     * @throws RemoteException
     */
    public IMultimediaStreamingSession initiateStreamingSession2(String serviceId,
            ContactId contact, String encoding) throws RemoteException {
        if (TextUtils.isEmpty(serviceId)) {
            throw new ServerApiIllegalArgumentException("serviceId must not be null or empty!");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a multimedia streaming session with " + contact);
        }
        ServerApiUtils.testImsExtension(serviceId);
        try {
            String featureTag = FeatureTags.FEATURE_RCSE + "=\""
                    + FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + serviceId + "\"";
            final GenericSipRtpSession session = mSipService.createRtpSession(contact, featureTag,
                    encoding);

            MultimediaStreamingSessionImpl multimediaStreaming = new MultimediaStreamingSessionImpl(
                    session.getSessionID(), mMultimediaStreamingSessionEventBroadcaster,
                    mSipService, this, Direction.OUTGOING, contact, serviceId, State.INITIATING);

            session.addListener(multimediaStreaming);
            addMultimediaStreaming(multimediaStreaming);
            session.startSession();
            return multimediaStreaming;

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
     * Returns a current streaming session from its unique session ID
     * 
     * @param sessionId Session ID
     * @return Multimedia streaming session or null if not found
     * @throws RemoteException
     */
    public IMultimediaStreamingSession getStreamingSession(String sessionId) throws RemoteException {
        if (TextUtils.isEmpty(sessionId)) {
            throw new ServerApiIllegalArgumentException("sessionId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get multimedia streaming ".concat(sessionId));
        }
        try {
            return mMultimediaStreamingCache.get(sessionId);

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
     * Returns the list of streaming sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return List of sessions
     * @throws RemoteException
     */
    public List<IBinder> getStreamingSessions(String serviceId) throws RemoteException {
        if (TextUtils.isEmpty(serviceId)) {
            throw new ServerApiIllegalArgumentException("serviceId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get multimedia streaming sessions for service " + serviceId);
        }
        try {
            List<IBinder> multimediaStreamingSessions = new ArrayList<>();
            for (IMultimediaStreamingSession multimediaStreamingSession : mMultimediaStreamingCache
                    .values()) {
                if (multimediaStreamingSession.getServiceId().contains(serviceId)) {
                    multimediaStreamingSessions.add(multimediaStreamingSession.asBinder());
                }
            }
            return multimediaStreamingSessions;

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
     * Adds a listener on multimedia messaging session events
     * 
     * @param listener Session event listener
     * @throws RemoteException
     */
    public void addEventListener2(IMultimediaMessagingSessionListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add an event listener");
        }
        try {
            synchronized (mLock) {
                mMultimediaMessagingSessionEventBroadcaster
                        .addMultimediaMessagingEventListener(listener);
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
     * Removes a listener on multimedia messaging session events
     * 
     * @param listener Session event listener
     * @throws RemoteException
     */
    public void removeEventListener2(IMultimediaMessagingSessionListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove an event listener");
        }
        try {
            synchronized (mLock) {
                mMultimediaMessagingSessionEventBroadcaster
                        .removeMultimediaMessagingEventListener(listener);
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
     * Adds a listener on multimedia streaming session events
     * 
     * @param listener Session event listener
     * @throws RemoteException
     */
    public void addEventListener3(IMultimediaStreamingSessionListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add an event listener");
        }
        try {
            synchronized (mLock) {
                mMultimediaStreamingSessionEventBroadcaster
                        .addMultimediaStreamingEventListener(listener);
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
     * Removes a listener on multimedia streaming session events
     * 
     * @param listener Session event listener
     * @throws RemoteException
     */
    public void removeEventListener3(IMultimediaStreamingSessionListener listener)
            throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove an event listener");
        }
        try {
            synchronized (mLock) {
                mMultimediaStreamingSessionEventBroadcaster
                        .removeMultimediaStreamingEventListener(listener);
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
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }

    /**
     * Sends an instant multimedia message to a remote contact and for a given service extension.
     * The content takes part of the message, so any multimedia session is needed to exchange
     * content here. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not
     * supported an exception is thrown.
     *
     * @param serviceId Service ID
     * @param contact Contact identifier
     * @param content Content of the message
     * @param contentType Content type of the the message
     * @throws RemoteException
     */
    public void sendInstantMultimediaMessage(final String serviceId, final ContactId contact,
            final byte[] content, final String contentType) throws RemoteException {
        if (TextUtils.isEmpty(serviceId)) {
            throw new ServerApiIllegalArgumentException("serviceId must not be null or empty!");
        }
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (content == null) {
            throw new ServerApiIllegalArgumentException("content must not be null!");
        }
        if (contentType == null) {
            throw new ServerApiIllegalArgumentException("content type must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Send an instant multimedia message to " + contact);
        }
        ServerApiUtils.testImsExtension(serviceId);
        mSipService.scheduleMultimediaMessageOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    String featureTag = FeatureTags.FEATURE_RCSE + "=\""
                            + FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + serviceId + "\"";
                    mSipService.sendInstantMultimediaMessage(contact, featureTag, content,
                            contentType);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to send message for service ID:".concat(serviceId), e);
                }
            }
        });
    }

    /**
     * Receive a new SIP instant message
     *
     * @param intent Received intent
     * @param contact Remote contact
     * @param content Message content
     * @param contentType Message content type
     * @param serviceId Service ID
     */
    public void receiveSipInstantMessage(Intent intent, ContactId contact, byte[] content,
            String contentType, String serviceId) {
        intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(intent);
        intent.putExtra(InstantMultimediaMessageIntent.EXTRA_CONTACT, (Parcelable) contact);
        intent.putExtra(InstantMultimediaMessageIntent.EXTRA_SERVICE_ID, serviceId);
        intent.putExtra(InstantMultimediaMessageIntent.EXTRA_CONTENT, content);
        intent.putExtra(InstantMultimediaMessageIntent.EXTRA_CONTENT_TYPE, contentType);
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }
}
