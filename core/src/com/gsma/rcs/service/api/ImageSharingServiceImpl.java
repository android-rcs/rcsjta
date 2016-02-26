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

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.SessionIdGenerator;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.platform.file.FileDescription;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.ImageSharingPersistedStorageAccessor;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.service.broadcaster.ImageSharingEventBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.FileUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.IImageSharing;
import com.gsma.services.rcs.sharing.image.IImageSharingListener;
import com.gsma.services.rcs.sharing.image.IImageSharingService;
import com.gsma.services.rcs.sharing.image.IImageSharingServiceConfiguration;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;

import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Image sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingServiceImpl extends IImageSharingService.Stub {

    private final ImageSharingEventBroadcaster mBroadcaster = new ImageSharingEventBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final RichcallService mRichcallService;

    private final RichCallHistory mRichCallLog;

    private final RcsSettings mRcsSettings;

    private final Map<String, IImageSharing> mImageSharingCache = new HashMap<String, IImageSharing>();

    private static final Logger sLogger = Logger.getLogger(ImageSharingServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    /**
     * Constructor
     * 
     * @param richcallService Richcall Service
     * @param richCallLog RichCallHistory accessor
     * @param rcsSettings RcsSettings accessor
     */
    public ImageSharingServiceImpl(RichcallService richcallService, RichCallHistory richCallLog,
            RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.info("Image sharing service API is loaded");
        }
        mRichcallService = richcallService;
        mRichcallService.register(this);
        mRichCallLog = richCallLog;
        mRcsSettings = rcsSettings;
    }

    /**
     * Close API
     */
    public void close() {
        mImageSharingCache.clear();
        if (sLogger.isActivated()) {
            sLogger.info("Image sharing service API is closed");
        }
    }

    /**
     * Add an image sharing in the list
     * 
     * @param imageSharing Image sharing
     * @param sharingId String
     */
    private void addImageSharing(ImageSharingImpl imageSharing, String sharingId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add an image sharing in the list (size=")
                    .append(mImageSharingCache.size()).append(")").toString());
        }

        mImageSharingCache.put(sharingId, imageSharing);
    }

    /**
     * Remove an image sharing from the list
     * 
     * @param sharingId Sharing ID
     */
    public void removeImageSharing(String sharingId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove an image sharing from the list (size="
                    + mImageSharingCache.size() + ")");
        }

        mImageSharingCache.remove(sharingId);
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

    public void setImageSharingStateAndReasonCode(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode) {
        mRichCallLog.setImageSharingStateAndReasonCode(sharingId, state, reasonCode);
        mBroadcaster.broadcastStateChanged(contact, sharingId, state, reasonCode);
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
        // Notify listeners
        synchronized (mLock) {
            mRcsServiceRegistrationEventBroadcaster.broadcastServiceUnRegistered(reasonCode);
        }
    }

    /**
     * Receive a new image sharing invitation
     * 
     * @param session Image sharing session
     */
    public void receiveImageSharingInvitation(ImageTransferSession session) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive image sharing invitation from " + session.getRemoteContact()
                    + " displayName=" + session.getRemoteDisplayName());
        }
        String sharingId = session.getSessionID();
        ImageSharingPersistedStorageAccessor persistedStorage = new ImageSharingPersistedStorageAccessor(
                sharingId, mRichCallLog);
        ImageSharingImpl imageSharing = new ImageSharingImpl(sharingId, mRichcallService,
                mBroadcaster, persistedStorage, this);
        addImageSharing(imageSharing, sharingId);
        session.addListener(imageSharing);
    }

    /**
     * Returns the configuration of image sharing service
     * 
     * @return Configuration
     * @throws RemoteException
     */
    public IImageSharingServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new ImageSharingServiceConfigurationImpl(mRcsSettings);

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
     * Shares an image with a contact. The parameter file contains the URI of the image to be
     * shared(for a local or a remote image). An exception if thrown if there is no ongoing CS call.
     * The parameter contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an
     * exception is thrown.
     * 
     * @param contact Contact ID
     * @param file Uri of file to share
     * @return Image sharing
     * @throws RemoteException
     */
    public IImageSharing shareImage(ContactId contact, Uri file) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an image sharing session with ".concat(contact.toString()));
        }
        ServerApiUtils.testIms();
        try {
            Uri localFile = FileUtils.createCopyOfSentFile(file, mRcsSettings);
            FileDescription desc = FileFactory.getFactory().getFileDescription(localFile);
            MmContent content = ContentManager.createMmContent(localFile, desc.getSize(),
                    desc.getName());
            long timestamp = System.currentTimeMillis();
            final ImageTransferSession session = mRichcallService.createImageSharingSession(
                    contact, content, null, timestamp);
            final String sharingId = session.getSessionID();
            mRichCallLog.addImageSharing(sharingId, contact, Direction.OUTGOING,
                    session.getContent(), ImageSharing.State.INITIATING, ReasonCode.UNSPECIFIED,
                    timestamp);

            ImageSharingPersistedStorageAccessor persistedStorage = new ImageSharingPersistedStorageAccessor(
                    sharingId, contact, Direction.OUTGOING, localFile, content.getName(),
                    content.getEncoding(), content.getSize(), mRichCallLog, timestamp);
            final ImageSharingImpl imageSharing = new ImageSharingImpl(sharingId, mRichcallService,
                    mBroadcaster, persistedStorage, this);

            final ImageSharingServiceImpl imageSharingService = this;
            mRichcallService.scheduleImageShareOperation(new Runnable() {
                @Override
                public void run() {
                    try {
                        session.addListener(imageSharing);
                        imageSharingService.addImageSharing(imageSharing, sharingId);
                        session.startSession();

                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Failed initiate image sharing right now!", e);
                    }
                }
            });
            return imageSharing;

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
     * Returns a current image sharing from its unique ID
     * 
     * @param sharingId Sharing ID
     * @return Image sharing
     * @throws RemoteException
     */
    public IImageSharing getImageSharing(String sharingId) throws RemoteException {
        if (TextUtils.isEmpty(sharingId)) {
            throw new ServerApiIllegalArgumentException("sharingId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get image sharing session ".concat(sharingId));
        }
        try {
            IImageSharing imageSharing = mImageSharingCache.get(sharingId);
            if (imageSharing != null) {
                return imageSharing;
            }
            ImageSharingPersistedStorageAccessor persistedStorage = new ImageSharingPersistedStorageAccessor(
                    sharingId, mRichCallLog);
            return new ImageSharingImpl(sharingId, mRichcallService, mBroadcaster,
                    persistedStorage, this);

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
     * Add and broadcast image sharing invitation rejection invitation.
     * 
     * @param contact Contact
     * @param content Image content
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     */
    public void addImageSharingInvitationRejected(ContactId contact, MmContent content,
            ReasonCode reasonCode, long timestamp) {
        String sessionId = SessionIdGenerator.getNewId();
        mRichCallLog.addImageSharing(sessionId, contact, Direction.INCOMING, content,
                ImageSharing.State.REJECTED, reasonCode, timestamp);
        mBroadcaster.broadcastInvitation(sessionId);
    }

    /**
     * Adds a listener on image sharing events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    public void addEventListener2(IImageSharingListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add an Image sharing event listener");
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
     * Removes a listener on image sharing events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    public void removeEventListener2(IImageSharingListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove an Image sharing event listener");
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
     * Deletes all image sharing from history and abort/reject any associated ongoing session if
     * such exists.
     * 
     * @throws RemoteException
     */
    public void deleteImageSharings() throws RemoteException {
        mRichcallService.tryToDeleteImageSharings();
    }

    /**
     * Deletes image sharing with a given contact from history and abort/reject any associated
     * ongoing session if such exists
     * 
     * @param contact Remote contact
     * @throws RemoteException
     */
    public void deleteImageSharings2(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        mRichcallService.tryToDeleteImageSharings(contact);
    }

    /**
     * deletes an image sharing by its sharing id from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param sharingId Sharing ID
     * @throws RemoteException
     */
    public void deleteImageSharing(String sharingId) throws RemoteException {
        if (TextUtils.isEmpty(sharingId)) {
            throw new ServerApiIllegalArgumentException("sharingId must not be null or empty!");
        }
        mRichcallService.tryToDeleteImageSharing(sharingId);
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

    public void broadcastDeleted(ContactId contact, Set<String> sharingIds) {
        mBroadcaster.broadcastDeleted(contact, sharingIds);
    }

    /**
     * Ensure copy of file if existing is deleted
     *
     * @param sharingId Unique Id of file transfer
     */
    public void ensureFileCopyIsDeletedIfExisting(String sharingId) {
        if (Direction.INCOMING == mRichCallLog.getImageSharingDirection(sharingId)) {
            return;
        }
        Uri file = mRichCallLog.getFile(sharingId);
        if (file != null) {
            new File(file.getPath()).delete();
        }
    }
}
