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

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.core.ims.service.ImsServiceSession.TerminationReason;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSessionListener;
import com.gsma.rcs.provider.sharing.ImageSharingPersistedStorageAccessor;
import com.gsma.rcs.provider.sharing.ImageSharingStateAndReasonCode;
import com.gsma.rcs.service.broadcaster.IImageSharingEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.IImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;

import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;

/**
 * Image sharing implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingImpl extends IImageSharing.Stub implements ImageTransferSessionListener {

    private final String mSharingId;

    private final IImageSharingEventBroadcaster mBroadcaster;

    private final RichcallService mRichcallService;

    private final ImageSharingPersistedStorageAccessor mPersistentStorage;

    private final ImageSharingServiceImpl mImageSharingService;

    /**
     * Lock used for synchronization
     */
    private final Object lock = new Object();

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    private final ServerApiUtils mServerApiUtils;

    /**
     * Constructor
     * 
     * @param sharingId Unique Id of Image Sharing
     * @param richcallService RichcallService
     * @param broadcaster IImageSharingEventBroadcaster
     * @param persistentStorage ImageSharingPersistedStorageAccessor
     * @param imageSharingService ImageSharingServiceImpl
     * @param serverApiUtils
     */
    public ImageSharingImpl(String sharingId, RichcallService richcallService,
            IImageSharingEventBroadcaster broadcaster,
            ImageSharingPersistedStorageAccessor persistentStorage,
            ImageSharingServiceImpl imageSharingService, ServerApiUtils serverApiUtils) {
        mSharingId = sharingId;
        mRichcallService = richcallService;
        mBroadcaster = broadcaster;
        mPersistentStorage = persistentStorage;
        mImageSharingService = imageSharingService;
        mServerApiUtils = serverApiUtils;
    }

    /*
     * TODO: Fix reasoncode mapping in the switch.
     */
    private ImageSharingStateAndReasonCode toStateAndReasonCode(ContentSharingError error) {
        int contentSharingError = error.getErrorCode();
        switch (contentSharingError) {
            case ContentSharingError.SESSION_INITIATION_FAILED:
            case ContentSharingError.SEND_RESPONSE_FAILED:
                return new ImageSharingStateAndReasonCode(ImageSharing.State.FAILED,
                        ReasonCode.FAILED_INITIATION);
            case ContentSharingError.SESSION_INITIATION_CANCELLED:
            case ContentSharingError.SESSION_INITIATION_DECLINED:
                return new ImageSharingStateAndReasonCode(ImageSharing.State.REJECTED,
                        ReasonCode.REJECTED_BY_REMOTE);
            case ContentSharingError.MEDIA_SAVING_FAILED:
                return new ImageSharingStateAndReasonCode(ImageSharing.State.FAILED,
                        ReasonCode.FAILED_SAVING);
            case ContentSharingError.MEDIA_TRANSFER_FAILED:
            case ContentSharingError.MEDIA_STREAMING_FAILED:
            case ContentSharingError.UNSUPPORTED_MEDIA_TYPE:
                return new ImageSharingStateAndReasonCode(ImageSharing.State.FAILED,
                        ReasonCode.FAILED_SHARING);
            case ContentSharingError.NOT_ENOUGH_STORAGE_SPACE:
                return new ImageSharingStateAndReasonCode(ImageSharing.State.REJECTED,
                        ReasonCode.REJECTED_LOW_SPACE);
            case ContentSharingError.MEDIA_SIZE_TOO_BIG:
                return new ImageSharingStateAndReasonCode(ImageSharing.State.REJECTED,
                        ReasonCode.REJECTED_MAX_SIZE);
            default:
                throw new IllegalArgumentException(
                        new StringBuilder(
                                "Unknown reason in ImageSharingImpl.toStateAndReasonCode; contentSharingError=")
                                .append(contentSharingError).append("!").toString());
        }
    }

    private void setStateAndReasonCode(ContactId contact, State state, ReasonCode reasonCode) {
        if (mPersistentStorage.setStateAndReasonCode(state, reasonCode)) {
            mBroadcaster.broadcastStateChanged(contact, mSharingId, state, reasonCode);
        }
    }

    private void handleSessionRejected(ReasonCode reasonCode, ContactId contact) {
        if (mLogger.isActivated()) {
            mLogger.info("Session rejected; reasonCode=" + reasonCode + ".");
        }
        synchronized (lock) {
            mImageSharingService.removeImageSharing(mSharingId);
            setStateAndReasonCode(contact, ImageSharing.State.REJECTED, reasonCode);
        }
    }

    /**
     * Returns the sharing ID of the image sharing
     * 
     * @return Sharing ID
     * @throws RemoteException
     */
    public String getSharingId() throws RemoteException {
        try {
            return mSharingId;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws RemoteException
     */
    public ContactId getRemoteContact() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getRemoteContact();
            }
            return session.getRemoteContact();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the complete filename including the path of the file to be transferred
     * 
     * @return Filename
     * @throws RemoteException
     */
    public String getFileName() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getFileName();
            }
            return session.getContent().getName();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the Uri of the file to be transferred
     * 
     * @return Filename
     * @throws RemoteException
     */
    public Uri getFile() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getFile();
            }
            return session.getContent().getUri();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the size of the file to be transferred
     * 
     * @return Size in bytes
     * @throws RemoteException
     */
    public long getFileSize() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getFileSize();
            }
            return session.getContent().getSize();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
     * @throws RemoteException
     */
    public String getMimeType() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getMimeType();
            }
            return session.getContent().getEncoding();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the state of the image sharing
     * 
     * @return State
     * @throws RemoteException
     */
    public int getState() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getState().toInt();
            }
            if (session.isImageTransfered()) {
                return ImageSharing.State.TRANSFERRED.toInt();
            }
            SipDialogPath dialogPath = session.getDialogPath();
            if (dialogPath != null && dialogPath.isSessionEstablished()) {
                return ImageSharing.State.STARTED.toInt();

            } else if (session.isInitiatedByRemote()) {
                if (session.isSessionAccepted()) {
                    return ImageSharing.State.ACCEPTING.toInt();
                }
                return ImageSharing.State.INVITED.toInt();
            }
            return ImageSharing.State.INITIATING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the image sharing
     * 
     * @return ReasonCode
     * @throws RemoteException
     */
    public int getReasonCode() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getReasonCode().toInt();
            }
            return ReasonCode.UNSPECIFIED.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the direction of the sharing (incoming or outgoing)
     * 
     * @return Direction
     * @throws RemoteException
     * @see Direction
     */
    public int getDirection() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getDirection().toInt();
            }
            if (session.isInitiatedByRemote()) {
                return Direction.INCOMING.toInt();
            }
            return Direction.OUTGOING.toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the image sharing was initiated for outgoing image
     * sharing or the local timestamp of when the image sharing invitation was received for incoming
     * image sharings.
     * 
     * @return long
     * @throws RemoteException
     */
    public long getTimestamp() throws RemoteException {
        try {
            ImageTransferSession session = mRichcallService.getImageTransferSession(mSharingId);
            if (session == null) {
                return mPersistentStorage.getTimestamp();
            }
            return session.getTimestamp();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Accepts image sharing invitation
     * 
     * @throws RemoteException
     */
    public void acceptInvitation() throws RemoteException {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Accept session invitation");
            }
            final ImageTransferSession session = mRichcallService
                    .getImageTransferSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(new StringBuilder("Session with sharing ID '")
                        .append(mSharingId).append("' not available!").toString());
            }
            session.acceptSession(Binder.getCallingUid());
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Rejects image sharing invitation
     * 
     * @throws RemoteException
     */
    public void rejectInvitation() throws RemoteException {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Reject session invitation");
            }
            final ImageTransferSession session = mRichcallService
                    .getImageTransferSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(new StringBuilder("Session with sharing ID '")
                        .append(mSharingId).append("' not available!").toString());
            }
            session.rejectSession(InvitationStatus.INVITATION_REJECTED_DECLINE);
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Aborts the sharing
     * 
     * @throws RemoteException
     */
    public void abortSharing() throws RemoteException {
        try {
            if (mLogger.isActivated()) {
                mLogger.info("Cancel session");
            }
            final ImageTransferSession session = mRichcallService
                    .getImageTransferSession(mSharingId);
            if (session == null) {
                throw new ServerApiGenericException(new StringBuilder("Session with sharing ID '")
                        .append(mSharingId).append("' not available!").toString());
            }
            if (session.isImageTransfered()) {
                throw new ServerApiPermissionDeniedException(
                        "Cannot abort as image is already transferred!");
            }
            new Thread() {
                public void run() {
                    session.terminateSession(TerminationReason.TERMINATION_BY_USER);
                }
            }.start();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /**
     * Session is started
     */
    public void handleSessionStarted(ContactId contact) {
        if (mLogger.isActivated()) {
            mLogger.info("Session started");
        }
        synchronized (lock) {
            setStateAndReasonCode(contact, ImageSharing.State.STARTED, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * * Session has been aborted
     * 
     * @param reason Termination reason
     */
    public void handleSessionAborted(ContactId contact, TerminationReason reason) {
        if (mLogger.isActivated()) {
            mLogger.info(new StringBuilder("Session aborted (terminationReason ").append(reason)
                    .append(")").toString());
        }
        synchronized (lock) {
            mImageSharingService.removeImageSharing(mSharingId);
            switch (reason) {
                case TERMINATION_BY_SYSTEM:
                case TERMINATION_BY_TIMEOUT:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                    break;
                case TERMINATION_BY_CONNECTION_LOST:
                    setStateAndReasonCode(contact, State.FAILED, ReasonCode.FAILED_SHARING);
                    break;
                case TERMINATION_BY_USER:
                    setStateAndReasonCode(contact, State.ABORTED, ReasonCode.ABORTED_BY_USER);
                    break;
                case TERMINATION_BY_REMOTE:
                    /*
                     * TODO : Fix sending of SIP BYE by sender once transfer is completed and media
                     * session is closed. Then this check of state can be removed.
                     */
                    if (State.TRANSFERRED != mPersistentStorage.getState()) {
                        setStateAndReasonCode(contact, ImageSharing.State.ABORTED,
                                ReasonCode.ABORTED_BY_REMOTE);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(
                            new StringBuilder(
                                    "Unknown reason in ImageSharingImpl.handleSessionAborted; terminationReason=")
                                    .append(reason).append("!").toString());
            }
        }
    }

    /**
     * Content sharing error
     * 
     * @param error Error
     */
    public void handleSharingError(ContactId contact, ContentSharingError error) {
        if (mLogger.isActivated()) {
            mLogger.info("Sharing error " + error.getErrorCode());
        }
        ImageSharingStateAndReasonCode stateAndReasonCode = toStateAndReasonCode(error);
        State state = stateAndReasonCode.getState();
        ReasonCode reasonCode = stateAndReasonCode.getReasonCode();
        synchronized (lock) {
            mImageSharingService.removeImageSharing(mSharingId);
            setStateAndReasonCode(contact, state, reasonCode);
        }
    }

    /**
     * Content sharing progress
     * 
     * @param currentSize Data size transferred
     * @param totalSize Total size to be transferred
     */
    public void handleSharingProgress(ContactId contact, long currentSize, long totalSize) {
        synchronized (lock) {
            if (mPersistentStorage.setProgress(currentSize)) {
                mBroadcaster.broadcastProgressUpdate(contact, mSharingId, currentSize, totalSize);
            }
        }
    }

    /**
     * Content has been transferred
     * 
     * @param contact Remote contact
     * @param file File URI associated to the received content
     */
    public void handleContentTransfered(ContactId contact, Uri file) {
        if (mLogger.isActivated()) {
            mLogger.info("Image transferred");
        }
        synchronized (lock) {
            mImageSharingService.removeImageSharing(mSharingId);
            setStateAndReasonCode(contact, ImageSharing.State.TRANSFERRED, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionAccepted(ContactId contact) {
        if (mLogger.isActivated()) {
            mLogger.info("Accepting sharing");
        }
        synchronized (lock) {
            setStateAndReasonCode(contact, ImageSharing.State.ACCEPTING, ReasonCode.UNSPECIFIED);
        }
    }

    @Override
    public void handleSessionRejected(ContactId contact, TerminationReason reason) {
        switch (reason) {
            case TERMINATION_BY_USER:
                handleSessionRejected(ReasonCode.REJECTED_BY_USER, contact);
                break;
            case TERMINATION_BY_SYSTEM:
                /* Intentional fall through */
            case TERMINATION_BY_CONNECTION_LOST:
                handleSessionRejected(ReasonCode.REJECTED_BY_SYSTEM, contact);
                break;
            case TERMINATION_BY_TIMEOUT:
                handleSessionRejected(ReasonCode.REJECTED_BY_TIMEOUT, contact);
                break;
            case TERMINATION_BY_REMOTE:
                handleSessionRejected(ReasonCode.REJECTED_BY_REMOTE, contact);
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder(
                        "Unknown reason RejectedReason=").append(reason).append("!").toString());
        }
    }

    @Override
    public void handleSessionInvited(ContactId contact, MmContent content, long timestamp) {
        if (mLogger.isActivated()) {
            mLogger.info("Invited to image sharing session");
        }
        synchronized (lock) {
            mPersistentStorage.addImageSharing(contact, Direction.INCOMING, content,
                    ImageSharing.State.INVITED, ReasonCode.UNSPECIFIED, timestamp);
        }

        mBroadcaster.broadcastInvitation(mSharingId);
    }

    @Override
    public void handle180Ringing(ContactId contact) {
        synchronized (lock) {
            setStateAndReasonCode(contact, ImageSharing.State.RINGING, ReasonCode.UNSPECIFIED);
        }
    }

    /**
     * Override the onTransact Binder method. It is used to check authorization for an application
     * before calling API method. Control of authorization is made for third party applications (vs.
     * native application) by comparing the client application fingerprint with the RCS application
     * fingerprint
     */
    @Override
    public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags)
            throws android.os.RemoteException {
        mServerApiUtils.assertApiIsAuthorized(Binder.getCallingUid());
        return super.onTransact(code, data, reply, flags);
    }
}
