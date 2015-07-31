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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpException;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.platform.file.FileDescription;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.FileTransferPersistedStorageAccessor;
import com.gsma.rcs.provider.messaging.GroupFileTransferDeleteTask;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneFileTransferDeleteTask;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.service.broadcaster.GroupFileTransferBroadcaster;
import com.gsma.rcs.service.broadcaster.OneToOneFileTransferBroadcaster;
import com.gsma.rcs.service.broadcaster.RcsServiceRegistrationEventBroadcaster;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.IFileTransfer;
import com.gsma.services.rcs.filetransfer.IFileTransferService;
import com.gsma.services.rcs.filetransfer.IFileTransferServiceConfiguration;
import com.gsma.services.rcs.filetransfer.IGroupFileTransferListener;
import com.gsma.services.rcs.filetransfer.IOneToOneFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * File transfer service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferServiceImpl extends IFileTransferService.Stub {

    private final OneToOneFileTransferBroadcaster mOneToOneFileTransferBroadcaster = new OneToOneFileTransferBroadcaster();

    private final GroupFileTransferBroadcaster mGroupFileTransferBroadcaster = new GroupFileTransferBroadcaster();

    private final RcsServiceRegistrationEventBroadcaster mRcsServiceRegistrationEventBroadcaster = new RcsServiceRegistrationEventBroadcaster();

    private final InstantMessagingService mImService;

    private final ChatServiceImpl mChatService;

    private final MessagingLog mMessagingLog;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final Core mCore;

    private final LocalContentResolver mLocalContentResolver;

    private final ExecutorService mImOperationExecutor;

    private final OneToOneUndeliveredImManager mOneToOneUndeliveredImManager;

    private final Map<String, OneToOneFileTransferImpl> mOneToOneFileTransferCache = new HashMap<String, OneToOneFileTransferImpl>();

    private final Map<String, GroupFileTransferImpl> mGroupFileTransferCache = new HashMap<String, GroupFileTransferImpl>();

    /**
     * The sLogger
     */
    private static final Logger sLogger = Logger.getLogger(FileTransferServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private final Object mLock = new Object();

    private final Object mImsLock;

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param messagingLog MessagingLog
     * @param rcsSettings RcsSettings
     * @param contactManager ContactManager
     * @param core Core
     * @param mimoperationexecutor
     * @param mLocalContentResolver
     * @param oneToOneUndeliveredImManager
     */
    public FileTransferServiceImpl(InstantMessagingService imService, ChatServiceImpl chatService,
            MessagingLog messagingLog, RcsSettings rcsSettings, ContactManager contactManager,
            Core core, LocalContentResolver localContentResolver,
            ExecutorService imOperationExecutor, Object imsLock,
            OneToOneUndeliveredImManager oneToOneUndeliveredImManager) {
        if (sLogger.isActivated()) {
            sLogger.info("File transfer service API is loaded");
        }
        mImService = imService;
        mChatService = chatService;
        mMessagingLog = messagingLog;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mCore = core;
        mLocalContentResolver = localContentResolver;
        mImOperationExecutor = imOperationExecutor;
        mImsLock = imsLock;
        mOneToOneUndeliveredImManager = oneToOneUndeliveredImManager;
    }

    private ReasonCode imdnToFileTransferFailedReasonCode(ImdnDocument imdn) {
        String notificationType = imdn.getNotificationType();
        if (ImdnDocument.DELIVERY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DELIVERY;

        } else if (ImdnDocument.DISPLAY_NOTIFICATION.equals(notificationType)) {
            return ReasonCode.FAILED_DISPLAY;
        }

        throw new IllegalArgumentException(new StringBuilder(
                "Received invalid imdn notification type:'").append(notificationType).append("'")
                .toString());
    }

    public void ensureThumbnailIsDeleted(String transferId) {
        String icon = mMessagingLog.getFileTransferIcon(transferId);
        if (icon != null) {
            new File(icon).delete();
        }
    }

    /**
     * Close API
     */
    public void close() {
        /* Clear list of sessions */
        mOneToOneFileTransferCache.clear();
        mGroupFileTransferCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("File transfer service API is closed");
        }
    }

    /**
     * Add a 1-2-1 file transfer in the list
     * 
     * @param fileTransferId
     * @param oneToOneFileTransfer 1-2-1 File transfer
     */
    public void addOneToOneFileTransfer(String fileTransferId,
            OneToOneFileTransferImpl oneToOneFileTransfer) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a file transfer in the list (size="
                    + mOneToOneFileTransferCache.size() + ")");
        }
        mOneToOneFileTransferCache.put(fileTransferId, oneToOneFileTransfer);
    }

    /**
     * Remove a 1-2-1 file transfer from the list
     * 
     * @param fileTransferId File transfer ID
     */
    public void removeOneToOneFileTransfer(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a file transfer from the list (size="
                    + mOneToOneFileTransferCache.size() + ")");
        }

        mOneToOneFileTransferCache.remove(fileTransferId);
    }

    /**
     * Add a group file transfer in the list
     * 
     * @param fileTransferId
     * @param groupFileTransfer Group File transfer
     */
    public void addGroupFileTransfer(String fileTransferId, GroupFileTransferImpl groupFileTransfer) {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a file transfer in the list (size=" + mGroupFileTransferCache.size()
                    + ")");
        }
        mGroupFileTransferCache.put(fileTransferId, groupFileTransfer);
    }

    /**
     * Remove a group file transfer from the list
     * 
     * @param fileTransferId File transfer ID
     */
    public void removeGroupFileTransfer(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a file transfer from the list (size="
                    + mGroupFileTransferCache.size() + ")");
        }

        mGroupFileTransferCache.remove(fileTransferId);
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
     * Get or create group file transfer
     * 
     * @param chatId
     * @param transferId
     * @return GroupFileTransferImpl
     */
    public GroupFileTransferImpl getOrCreateGroupFileTransfer(String chatId, String transferId) {
        GroupFileTransferImpl groupChat = mGroupFileTransferCache.get(transferId);
        if (groupChat != null) {
            return groupChat;
        }
        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                transferId, mMessagingLog);
        return new GroupFileTransferImpl(transferId, mGroupFileTransferBroadcaster, mImService,
                storageAccessor, this, mRcsSettings, mCore, mMessagingLog, mContactManager);
    }

    /**
     * Get or create one-one file transfer
     * 
     * @param contact
     * @param transferId
     * @return OneToOneFileTransferImpl
     */
    public OneToOneFileTransferImpl getOrCreateOneToOneFileTransfer(ContactId contact,
            String transferId) {
        OneToOneFileTransferImpl oneToOneFileTransfer = mOneToOneFileTransferCache.get(transferId);
        if (oneToOneFileTransfer != null) {
            return oneToOneFileTransfer;
        }
        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                transferId, mMessagingLog);
        return new OneToOneFileTransferImpl(transferId, mOneToOneFileTransferBroadcaster,
                mImService, storageAccessor, this, mRcsSettings, mCore, mMessagingLog,
                mContactManager, mOneToOneUndeliveredImManager);
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
     * Receive a new file transfer invitation
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     * @param contact Contact ID
     * @param displayName the display name of the remote contact
     */
    public void receiveFileTransferInvitation(FileSharingSession session, boolean isGroup,
            ContactId contact, String displayName) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive FT invitation from " + contact + " file="
                    + session.getContent().getName() + " size=" + session.getContent().getSize()
                    + " displayName=" + displayName);
        }
        // Add session in the list
        String fileTransferId = session.getFileTransferId();
        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, mMessagingLog);
        if (isGroup) {
            GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
                    session.getContributionID(), mGroupFileTransferBroadcaster, mImService,
                    storageAccessor, this, mRcsSettings, mCore, mMessagingLog, mContactManager);
            session.addListener(groupFileTransfer);
            addGroupFileTransfer(fileTransferId, groupFileTransfer);
        } else {
            OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                    fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                    this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                    mOneToOneUndeliveredImManager);
            session.addListener(oneToOneFileTransfer);
            addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
        }
    }

    /**
     * Receive a new resend file transfer invitation
     * 
     * @param session File transfer session
     * @param remoteContact Contact ID of remote contact
     * @param displayName the display name of the remote contact
     */
    public void receiveResendFileTransferInvitation(FileSharingSession session, ContactId remoteContact,
            String displayName) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive resend FT invitation from " + remoteContact + " displayName="
                    + displayName);
        }
        String fileTransferId = session.getFileTransferId();
        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, mMessagingLog);
        OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                mOneToOneUndeliveredImManager);
        session.addListener(oneToOneFileTransfer);
        addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
    }

    /**
     * Returns the interface to the configuration of the file transfer service
     * 
     * @return IFileTransferServiceConfiguration instance
     * @throws RemoteException
     */
    public IFileTransferServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new IFileTransferServiceConfigurationImpl(mRcsSettings);

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
     * Add outgoing one to one file transfer to DB
     * 
     * @param fileTransferId File transfer ID
     * @param contact ContactId
     * @param content Content of file
     * @param fileicon Content of file icon
     * @param state State of the file transfer
     * @param timestamp Local timestamp of the file transfer
     * @param timestampSent Timestamp sent in payload of the file transfer
     */
    private void addOutgoingOneToOneFileTransfer(String fileTransferId, ContactId contact,
            MmContent content, MmContent fileicon, State state, long timestamp, long timestampSent) {
        mMessagingLog.addOneToOneFileTransfer(fileTransferId, contact, Direction.OUTGOING, content,
                fileicon, state, ReasonCode.UNSPECIFIED, timestamp, timestampSent,
                FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);
    }

    /**
     * Add outgoing group file transfer to DB
     * 
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID of group chat
     * @param content Content of file
     * @param fileicon Content of fileicon
     * @param state state of file transfer
     * @param timestamp Local timestamp of the file transfer
     * @param timestampSent Timestamp sent in payload of the file transfer
     */
    private void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent fileicon, State state, long timestamp, long timestampSent) {
        Set<ContactId> recipients = mChatService.getOrCreateGroupChat(chatId).getRecipients();
        if (recipients == null) {
            throw new ServerApiPersistentStorageException(
                    "Unable to determine recipients of the group chat " + chatId
                            + " to set as recipients for the the group file transfer "
                            + fileTransferId + "!");
        }
        mMessagingLog.addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileicon,
                recipients, state, FileTransfer.ReasonCode.UNSPECIFIED, timestamp, timestampSent);
    }

    public FileTransferProtocol getFileTransferProtocolForOneToOneFileTransfer(ContactId contact) {
        Capabilities myCapabilities = mRcsSettings.getMyCapabilities();
        Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
        if (remoteCapabilities == null) {
            return null;
        }
        boolean ftMsrpSupportedforSelf = myCapabilities.isFileTransferMsrpSupported();
        boolean ftHttpSupportedforSelf = myCapabilities.isFileTransferHttpSupported();
        boolean ftMsrpSupportedforRemote = remoteCapabilities.isFileTransferMsrpSupported();
        boolean ftHttpSupportedforRemote = remoteCapabilities.isFileTransferHttpSupported();
        if (ftMsrpSupportedforSelf && ftMsrpSupportedforRemote) {
            if (ftHttpSupportedforSelf && ftHttpSupportedforRemote) {
                return mRcsSettings.getFtProtocol();
            } else {
                return FileTransferProtocol.MSRP;
            }
        } else if (ftHttpSupportedforSelf && ftHttpSupportedforRemote) {
            return FileTransferProtocol.HTTP;
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder(
                        "There are are no available capabilities : FTMsrp(Self)")
                        .append(ftMsrpSupportedforSelf).append(" FTHttp(Self)")
                        .append(ftHttpSupportedforSelf).append(" FTMsrp(Remote)")
                        .append(ftMsrpSupportedforSelf).append(" FTHttp(Remote)")
                        .append(ftMsrpSupportedforRemote).toString());
            }
            return null;
        }
    }

    /**
     * 1-1 file send operation initiated
     * 
     * @param contact
     * @param file
     * @param fileIcon
     * @param fileTransferId return IFileTransfer OneToOneFileTransferImpl
     * @param timestamp Local timestamp of the file transfer
     * @param timestampSent Timestamp sent in payload of the file transfer
     */
    private IFileTransfer sendOneToOneFile(ContactId contact, MmContent file, MmContent fileIcon,
            String fileTransferId, long timestamp, long timestampSent) {
        mImService.assertFileSizeNotExceedingMaxLimit(file.getSize(), "File exceeds max size");

        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, contact, Direction.OUTGOING, contact.toString(), file, fileIcon,
                mMessagingLog);

        if (!mImService.isFileTransferSessionAvailable()
                || mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of file transfer sessions is achieved: queue the file transfer.");
            }
            addOutgoingOneToOneFileTransfer(fileTransferId, contact, file, fileIcon, State.QUEUED,
                    timestamp, timestampSent);
            return new OneToOneFileTransferImpl(fileTransferId, mOneToOneFileTransferBroadcaster,
                    mImService, storageAccessor, this, mRcsSettings, mCore, mMessagingLog,
                    mContactManager, mOneToOneUndeliveredImManager);
        }
        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
        if (ftProtocol == null) {
            /* Throw proper exception as part of CR037 */
            throw new ServerApiGenericException(
                    new StringBuilder(
                            "No valid file transfer protocol could be determined for sending file with fileTransferId '")
                            .append(fileTransferId).append("'!").toString());
        }

        addOutgoingOneToOneFileTransfer(fileTransferId, contact, file, fileIcon, State.INITIATING,
                timestamp, timestampSent);
        final FileSharingSession session = mImService.initiateFileTransferSession(fileTransferId,
                contact, file, fileIcon, timestamp, timestampSent, ftProtocol);

        OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                mOneToOneUndeliveredImManager);
        session.addListener(oneToOneFileTransfer);
        addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
        session.startSession();
        return oneToOneFileTransfer;
    }

    /**
     * Dequeue one-to-one file transfer
     * 
     * @param fileTransferId
     * @param contact
     * @param file
     * @param fileIcon
     */
    public void dequeueOneToOneFileTransfer(String fileTransferId, ContactId contact,
            MmContent file, MmContent fileIcon) {
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
        if (ftProtocol == null) {
            /* Throw proper exception as part of CR037 */
            throw new ServerApiGenericException(
                    new StringBuilder(
                            "No valid file transfer protocol could be determined for dequeueing file with fileTransferId '")
                            .append(fileTransferId).append("'!").toString());
        }
        mMessagingLog.dequeueFileTransfer(fileTransferId, timestamp, timestampSent);
        mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                State.INITIATING, ReasonCode.UNSPECIFIED);
        final FileSharingSession session = mImService.initiateFileTransferSession(fileTransferId,
                contact, file, fileIcon, timestamp, timestampSent, ftProtocol);

        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, contact, Direction.OUTGOING, contact.toString(), file, fileIcon,
                mMessagingLog);
        OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                mOneToOneUndeliveredImManager);
        session.addListener(oneToOneFileTransfer);
        addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
        session.startSession();
    }

    /**
     * 1-1 file re-send operation initiated
     * 
     * @param contact
     * @param file
     * @param fileIcon
     * @param fileTransferId
     */
    /* package private */void resendOneToOneFile(ContactId contact, MmContent file,
            MmContent fileIcon, String fileTransferId) {
        /* Set new timestamp for the resend file */
        long timestamp = System.currentTimeMillis();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        if (!ServerApiUtils.isImsConnected()) {
            /*
             * If the IMS is NOT connected at this time then re-queue transfer.
             */
            setOneToOneFileTransferStateAndReasonCode(fileTransferId, contact, State.QUEUED,
                    FileTransfer.ReasonCode.UNSPECIFIED);
            return;
        }
        if (!mImService.isFileTransferSessionAvailable()
                || mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of file transfer sessions is achieved: re-queue the file transfer with fileTransferId "
                        .concat(fileTransferId));
            }
            setOneToOneFileTransferStateAndReasonCode(fileTransferId, contact, State.QUEUED,
                    FileTransfer.ReasonCode.UNSPECIFIED);
            return;
        }

        mMessagingLog.setFileTransferStateAndTimestamps(fileTransferId, State.INITIATING,
                ReasonCode.UNSPECIFIED, timestamp, timestampSent);
        mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                State.INITIATING, ReasonCode.UNSPECIFIED);
        FileTransferProtocol ftProtocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
        if (ftProtocol == null) {
            /* Throw proper exception as part of CR037 */
            throw new ServerApiGenericException(
                    new StringBuilder(
                            "No valid file transfer protocol could be determined for resending file with fileTransferId '")
                            .append(fileTransferId).append("'!").toString());
        }

        final FileSharingSession session = mImService.initiateFileTransferSession(fileTransferId,
                contact, file, fileIcon, timestamp, timestampSent, ftProtocol);

        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, contact, Direction.OUTGOING, contact.toString(), file, fileIcon,
                mMessagingLog);
        OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                mOneToOneUndeliveredImManager);
        session.addListener(oneToOneFileTransfer);
        addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
        session.startSession();
    }

    /**
     * Returns true if it is possible to initiate file transfer to the contact specified by the
     * contact parameter, else returns false.
     * 
     * @param contact
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToTransferFile(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            Capabilities remoteCapabilities = mContactManager.getContactCapabilities(contact);
            if (remoteCapabilities == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot transfer file as the capabilities of contact ").append(contact)
                            .append(" are not known.").toString());
                }
                return false;
            }
            FileTransferProtocol protocol = getFileTransferProtocolForOneToOneFileTransfer(contact);
            if (protocol == null) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot transfer file as no valid file transfer protocol could be determined.");
                }
                return false;
            }
            MessagingMode mode = mRcsSettings.getMessagingMode();
            switch (mode) {
                case INTEGRATED:
                case SEAMLESS:
                    if ((FileTransferProtocol.MSRP == protocol && mRcsSettings.isFtAlwaysOn())
                            || (FileTransferProtocol.HTTP == protocol && mRcsSettings
                                    .isFtHttpCapAlwaysOn())) {
                        break;
                    }
                    if (!mImService.isCapabilitiesValid(remoteCapabilities)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder(
                                    "Cannot transfer file as the cached capabilities of contact ")
                                    .append(contact)
                                    .append(" are not valid anymore for one-to-one communication.")
                                    .toString());
                        }
                        return false;
                    }
                    break;
                default:
                    break;
            }
            return true;

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
     * Transfers a file to a contact. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the
     * format of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param file URI of file to transfer
     * @param attachfileIcon true if the stack must try to attach fileIcon
     * @return FileTransfer
     * @throws RemoteException
     */
    public IFileTransfer transferFile(ContactId contact, Uri file, boolean attachfileIcon)
            throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Transfer file " + file + " to " + contact + " (fileicon="
                    + attachfileIcon + ")");
        }
        try {
            FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
            MmContent fileIconContent = null;
            MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(),
                    fileDescription.getName());

            String fileTransferId = IdGenerator.generateMessageID();
            if (attachfileIcon && MimeManager.isImageType(content.getEncoding())) {
                fileIconContent = FileTransferUtils.createFileicon(file, fileTransferId,
                        mRcsSettings);
            }
            long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            long timestampSent = timestamp;
            /* If the IMS is connected at this time then send this one to one file. */
            if (ServerApiUtils.isImsConnected()) {
                return sendOneToOneFile(contact, content, fileIconContent, fileTransferId,
                        timestamp, timestampSent);
            }
            /* If the IMS is NOT connected at this time then queue this one to one file. */
            addOutgoingOneToOneFileTransfer(fileTransferId, contact, content, fileIconContent,
                    State.QUEUED, timestamp, timestampSent);
            FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                    fileTransferId, contact, Direction.OUTGOING, contact.toString(), content,
                    fileIconContent, mMessagingLog);
            return new OneToOneFileTransferImpl(fileTransferId, mOneToOneFileTransferBroadcaster,
                    mImService, storageAccessor, this, mRcsSettings, mCore, mMessagingLog,
                    mContactManager, mOneToOneUndeliveredImManager);

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
     * Returns true if it is possible to initiate file transfer to the group chat specified by the
     * chatId parameter, else returns false.
     * 
     * @param chatId
     * @return boolean
     * @throws RemoteException
     */
    public boolean isAllowedToTransferFileToGroupChat(String chatId) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        try {
            if (!mRcsSettings.isGroupChatActivated()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot transfer file to group chat with group chat Id '")
                            .append(chatId).append("' as group chat feature is not supported.")
                            .toString());
                }
                return false;
            }
            if (!mRcsSettings.getMyCapabilities().isFileTransferHttpSupported()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot transfer file to group chat with group chat Id '")
                            .append(chatId)
                            .append("' as FT over HTTP capabilities are not supported for self.")
                            .toString());
                }
                return false;
            }
            if (mChatService.isGroupChatAbandoned(chatId)) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder(
                            "Cannot transfer file to group chat with group chat Id '")
                            .append(chatId)
                            .append("' as the group chat is abandoned and can no more be used to send or receive messages.")
                            .toString());
                }
                return false;
            }
            GroupChatSession session = mImService.getGroupChatSession(chatId);
            if (session == null) {
                GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
                if (groupChat == null) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(new StringBuilder(
                                "Cannot transfer file to group chat with group chat Id '")
                                .append(chatId).append("' as the group chat does not exist in DB.")
                                .toString());
                    }
                    return false;
                }
                if (TextUtils.isEmpty(groupChat.getRejoinId())) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(new StringBuilder(
                                "Cannot transfer file to group chat with group chat Id '")
                                .append(chatId)
                                .append("' as there is no ongoing session with corresponding chatId and there exists no rejoinId to rejoin the group chat.")
                                .toString());
                    }
                    return false;
                }
            }
            return true;

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
     * Group file send operation initiated
     * 
     * @param content
     * @param fileIcon
     * @param chatId
     * @param fileTransferId
     */
    private IFileTransfer sendGroupFile(MmContent content, MmContent fileIcon, String chatId,
            String fileTransferId, long timestamp) {
        long fileSize = content.getSize();
        /* For outgoing file transfer, timestampSent = timestamp */
        long timestampSent = timestamp;
        mImService.assertFileSizeNotExceedingMaxLimit(fileSize, "File exceeds max size.");

        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, null, Direction.OUTGOING, chatId, content, fileIcon, mMessagingLog);

        if (!mImService.isFileTransferSessionAvailable()
                || mImService.isMaxConcurrentOutgoingFileTransfersReached()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of file transfer sessions is achieved: queue the file transfer.");
            }
            addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIcon, State.QUEUED,
                    timestamp, timestampSent);
            return new GroupFileTransferImpl(fileTransferId, chatId, mGroupFileTransferBroadcaster,
                    mImService, storageAccessor, this, mRcsSettings, mCore, mMessagingLog,
                    mContactManager);
        }
        final GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
        String chatSessionId = groupChatSession != null ? groupChatSession.getSessionID() : null;
        /* If groupChatSession is established send this group file transfer. */
        if (chatSessionId != null && groupChatSession.isMediaEstablished()) {

            addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIcon,
                    State.INITIATING, timestamp, timestampSent);
            final FileSharingSession session = mImService.initiateGroupFileTransferSession(
                    fileTransferId, content, fileIcon, chatId, chatSessionId, timestamp,
                    timestampSent);

            GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(
                    session.getFileTransferId(), chatId, mGroupFileTransferBroadcaster, mImService,
                    storageAccessor, this, mRcsSettings, mCore, mMessagingLog, mContactManager);
            session.addListener(groupFileTransfer);
            addGroupFileTransfer(fileTransferId, groupFileTransfer);
            session.startSession();
            return groupFileTransfer;
        }
        /*
         * If groupChatSession is NOT established then queue this file transfer and try to rejoin
         * group chat.
         */
        addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIcon, State.QUEUED,
                timestamp, timestampSent);
        if (groupChatSession != null) {
            if (groupChatSession.isInitiatedByRemote()) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Group chat session with chatId '")
                            .append(chatId).append("' is pending for acceptance, accept it.")
                            .toString());
                }
                groupChatSession.acceptSession();
            }
        } else {
            mCore.getListener().handleRejoinGroupChatAsPartOfSendOperation(chatId);
        }
        return new GroupFileTransferImpl(fileTransferId, chatId, mGroupFileTransferBroadcaster,
                mImService, storageAccessor, this, mRcsSettings, mCore, mMessagingLog,
                mContactManager);
    }

    /**
     * Dequeue group file transfer
     * 
     * @param fileTransferId
     * @param content
     * @param fileIcon
     * @param chatId
     * @throws MsrpException
     */
    public void dequeueGroupFileTransfer(String chatId, String fileTransferId, MmContent content,
            MmContent fileIcon) throws MsrpException {
        GroupChatSession groupChatSession = mImService.getGroupChatSession(chatId);
        if (groupChatSession == null) {
            mCore.getListener().handleRejoinGroupChatAsPartOfSendOperation(chatId);
        } else if (groupChatSession.isMediaEstablished()) {
            long timestamp = System.currentTimeMillis();
            /* For outgoing file transfer, timestampSent = timestamp */
            long timestampSent = timestamp;
            mMessagingLog.dequeueFileTransfer(fileTransferId, timestamp, timestampSent);
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                    FileTransfer.State.INITIATING, FileTransfer.ReasonCode.UNSPECIFIED);
            final FileSharingSession session = mImService.initiateGroupFileTransferSession(
                    fileTransferId, content, fileIcon, chatId, groupChatSession.getSessionID(),
                    timestamp, timestampSent);
            FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                    fileTransferId, null, Direction.OUTGOING, chatId, content, fileIcon,
                    mMessagingLog);
            GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
                    chatId, mGroupFileTransferBroadcaster, mImService, storageAccessor, this,
                    mRcsSettings, mCore, mMessagingLog, mContactManager);
            session.addListener(groupFileTransfer);
            addGroupFileTransfer(fileTransferId, groupFileTransfer);
            session.startSession();
        } else if (groupChatSession.isInitiatedByRemote()) {
            if (sLogger.isActivated()) {
                sLogger.debug(new StringBuilder("Group chat session with chatId '").append(chatId)
                        .append("' is pending for acceptance, accept it.").toString());
            }
            groupChatSession.acceptSession();
        } else {
            throw new MsrpException(new StringBuilder(
                    "The existing group chat session with chatId '").append(chatId)
                    .append("' is not established right now!").toString());
        }
    }

    /**
     * Transfers a file to participants. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file).
     * 
     * @param chatId ChatId of group chat
     * @param file Uri of file to transfer
     * @param attachfileIcon true if the stack must try to attach fileIcon
     * @return FileTransfer
     * @throws RemoteException
     */
    public IFileTransfer transferFileToGroupChat(String chatId, Uri file, boolean attachfileIcon)
            throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (!isAllowedToTransferFileToGroupChat(chatId)) {
            throw new ServerApiPermissionDeniedException(
                    "No sufficient capabilities to transfer file to group chat!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("sendFile (file=" + file + ") (fileicon=" + attachfileIcon + ")");
        }
        try {
            FileDescription fileDescription = FileFactory.getFactory().getFileDescription(file);
            MmContent content = ContentManager.createMmContent(file, fileDescription.getSize(),
                    fileDescription.getName());

            String fileTransferId = IdGenerator.generateMessageID();
            MmContent fileIconContent = null;
            if (attachfileIcon && MimeManager.isImageType(content.getEncoding())) {
                fileIconContent = FileTransferUtils.createFileicon(content.getUri(),
                        fileTransferId, mRcsSettings);
            }

            long timestamp = System.currentTimeMillis();
            /* If the IMS is connected at this time then send this group file. */
            if (ServerApiUtils.isImsConnected()) {
                return sendGroupFile(content, fileIconContent, chatId, fileTransferId, timestamp);
            }
            if (!mChatService.isGroupChatActive(chatId)) {
                /*
                 * Set inactive group chat as active as it now has a queued file that has to be
                 * dequeued after rejoining to the group chat on regaining IMS connection.
                 */
                mChatService.setGroupChatStateAndReasonCode(chatId, GroupChat.State.STARTED,
                        GroupChat.ReasonCode.UNSPECIFIED);
            }
            /* If the IMS is NOT connected at this time then queue this group file. */
            /* For outgoing file transfer, timestampSent = timestamp */
            addOutgoingGroupFileTransfer(fileTransferId, chatId, content, fileIconContent,
                    State.QUEUED, timestamp, timestamp);
            FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                    fileTransferId, null, Direction.OUTGOING, chatId, content, fileIconContent,
                    mMessagingLog);
            return new GroupFileTransferImpl(fileTransferId, chatId, mGroupFileTransferBroadcaster,
                    mImService, storageAccessor, this, mRcsSettings, mCore, mMessagingLog,
                    mContactManager);

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
     * Returns a current file transfer from its unique ID
     * 
     * @param transferId
     * @return File transfer
     * @throws RemoteException
     */
    public IFileTransfer getFileTransfer(String transferId) throws RemoteException {
        if (TextUtils.isEmpty(transferId)) {
            throw new ServerApiIllegalArgumentException("transferId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get file transfer session ".concat(transferId));
        }
        try {
            IFileTransfer fileTransfer = mOneToOneFileTransferCache.get(transferId);
            if (fileTransfer != null) {
                return fileTransfer;
            }

            fileTransfer = mGroupFileTransferCache.get(transferId);
            if (fileTransfer != null) {
                return fileTransfer;
            }

            FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                    transferId, mMessagingLog);

            if (mMessagingLog.isGroupFileTransfer(transferId)) {
                GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(transferId,
                        mGroupFileTransferBroadcaster, mImService, storageAccessor, this,
                        mRcsSettings, mCore, mMessagingLog, mContactManager);
                mGroupFileTransferCache.put(transferId, groupFileTransfer);
                return groupFileTransfer;
            }

            OneToOneFileTransferImpl oneToOnefileTransfer = new OneToOneFileTransferImpl(
                    transferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                    this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                    mOneToOneUndeliveredImManager);
            mOneToOneFileTransferCache.put(transferId, oneToOnefileTransfer);
            return oneToOnefileTransfer;

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
     * Adds a listener on file transfer events
     * 
     * @param listener OneToOne file transfer listener
     * @throws RemoteException
     */
    public void addEventListener2(IOneToOneFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a OneToOne file transfer invitation listener");
        }
        try {
            synchronized (mLock) {
                mOneToOneFileTransferBroadcaster.addOneToOneFileTransferListener(listener);
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
     * Removes a listener on file transfer events
     * 
     * @param listener OneToOne file transfer listener
     * @throws RemoteException
     */
    public void removeEventListener2(IOneToOneFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a OneToOne file transfer invitation listener");
        }
        try {
            synchronized (mLock) {
                mOneToOneFileTransferBroadcaster.removeOneToOneFileTransferListener(listener);
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
     * Adds a listener on group file transfer events
     * 
     * @param listener Group file transfer listener
     * @throws RemoteException
     */
    public void addEventListener3(IGroupFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a group file transfer invitation listener");
        }
        try {
            synchronized (mLock) {
                mGroupFileTransferBroadcaster.addGroupFileTransferListener(listener);
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
     * Removes a listener on group file transfer events
     * 
     * @param listener Group file transfer listener
     * @throws RemoteException
     */
    public void removeEventListener3(IGroupFileTransferListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a group file transfer invitation listener");
        }
        try {
            synchronized (mLock) {
                mGroupFileTransferBroadcaster.removeGroupFileTransferListener(listener);
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
     * File Transfer delivery status. In FToHTTP, Delivered status is done just after download
     * information are received by the terminating, and Displayed status is done when the file is
     * downloaded. In FToMSRP, the two status are directly done just after MSRP transfer complete.
     * 
     * @param imdn Imdn document
     * @param contact contact who received file
     */
    public void handleOneToOneFileDeliveryStatus(ImdnDocument imdn, ContactId contact) {
        String status = imdn.getStatus();
        long timestamp = imdn.getDateTime();

        /* Note: File transfer ID always corresponds to message ID in the imdn pay-load */
        String fileTransferId = imdn.getMsgId();
        if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            mOneToOneUndeliveredImManager.cancelDeliveryTimeoutAlarm(fileTransferId);
            mMessagingLog.setFileTransferDelivered(fileTransferId, timestamp);

            mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                    State.DELIVERED, ReasonCode.UNSPECIFIED);
        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            mOneToOneUndeliveredImManager.cancelDeliveryTimeoutAlarm(fileTransferId);
            mMessagingLog.setFileTransferDisplayed(fileTransferId, timestamp);

            mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                    State.DISPLAYED, ReasonCode.UNSPECIFIED);
        } else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            ReasonCode reasonCode = imdnToFileTransferFailedReasonCode(imdn);

            mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, State.FAILED,
                    reasonCode);

            mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId,
                    State.FAILED, reasonCode);
        }
    }

    private void handleGroupFileDeliveryStatusDelivered(String chatId, String fileTransferId,
            ContactId contact, long timestampDelivered) {
        // TODO: Potential race condition, the message may have been removed at this point which
        // means the database won't be updated, but we'll still do the broadcast.
        mMessagingLog.setGroupChatDeliveryInfoDelivered(chatId, contact, fileTransferId,
                timestampDelivered);
        mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact, fileTransferId,
                GroupDeliveryInfo.Status.DELIVERED, GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
        if (mMessagingLog.isDeliveredToAllRecipients(fileTransferId)) {
            // TODO: Potential race condition, the message may have been removed at this point which
            // means the database won't be updated, but we'll still do the broadcast.
            mMessagingLog.setFileTransferDelivered(fileTransferId, timestampDelivered);
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                    State.DELIVERED, ReasonCode.UNSPECIFIED);
        }
    }

    private void handleGroupFileDeliveryStatusDisplayed(String chatId, String fileTransferId,
            ContactId contact, long timestampDisplayed) {
        // TODO: Potential race condition, the file transfer may have been removed at this point
        // which means the database won't be updated, but we'll still do the broadcast.
        mMessagingLog.setGroupChatDeliveryInfoDisplayed(chatId, contact, fileTransferId,
                timestampDisplayed);
        mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact, fileTransferId,
                GroupDeliveryInfo.Status.DISPLAYED, GroupDeliveryInfo.ReasonCode.UNSPECIFIED);
        if (mMessagingLog.isDisplayedByAllRecipients(fileTransferId)) {
            // TODO: Potential race condition, the file transfer may have been removed at this point
            // which means the database won't be updated, but we'll still do the broadcast.
            mMessagingLog.setFileTransferDisplayed(fileTransferId, timestampDisplayed);
            mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId,
                    State.DISPLAYED, ReasonCode.UNSPECIFIED);
        }
    }

    private void handleGroupFileDeliveryStatusFailed(String chatId, String fileTransferId,
            ContactId contact, ReasonCode reasonCode) {
        if (ReasonCode.FAILED_DELIVERY == reasonCode) {
            if (!mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(chatId, contact,
                    fileTransferId, GroupDeliveryInfo.Status.FAILED,
                    GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY)) {
                /* Add entry with delivered and displayed timestamps set to 0. */
                mMessagingLog.addGroupChatDeliveryInfoEntry(chatId, contact, fileTransferId,
                        GroupDeliveryInfo.Status.FAILED,
                        GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY, 0, 0);
            }
            mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact,
                    fileTransferId, GroupDeliveryInfo.Status.FAILED,
                    GroupDeliveryInfo.ReasonCode.FAILED_DELIVERY);
            return;
        }
        if (!mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(chatId, contact,
                fileTransferId, GroupDeliveryInfo.Status.FAILED,
                GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY)) {
            /* Add entry with delivered and displayed timestamps set to 0. */
            mMessagingLog.addGroupChatDeliveryInfoEntry(chatId, contact, fileTransferId,
                    GroupDeliveryInfo.Status.FAILED, GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY,
                    0, 0);
        }
        mGroupFileTransferBroadcaster.broadcastDeliveryInfoChanged(chatId, contact, fileTransferId,
                GroupDeliveryInfo.Status.FAILED, GroupDeliveryInfo.ReasonCode.FAILED_DISPLAY);
    }

    /**
     * Handles group file transfer delivery status.
     * 
     * @param chatId Chat ID
     * @param imdn Imdn Document
     * @param contact Contact ID
     */
    public void handleGroupFileDeliveryStatus(String chatId, ImdnDocument imdn, ContactId contact) {
        String status = imdn.getStatus();
        String msgId = imdn.getMsgId();
        long timestamp = imdn.getDateTime();

        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Handling group file delivery status; contact=")
                    .append(contact).append(", msgId=").append(msgId).append(", status=")
                    .append(status).append(", notificationType=")
                    .append(imdn.getNotificationType()).toString());
        }
        if (ImdnDocument.DELIVERY_STATUS_DELIVERED.equals(status)) {
            handleGroupFileDeliveryStatusDelivered(chatId, msgId, contact, timestamp);
        } else if (ImdnDocument.DELIVERY_STATUS_DISPLAYED.equals(status)) {
            handleGroupFileDeliveryStatusDisplayed(chatId, msgId, contact, timestamp);
        } else if (ImdnDocument.DELIVERY_STATUS_ERROR.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FAILED.equals(status)
                || ImdnDocument.DELIVERY_STATUS_FORBIDDEN.equals(status)) {
            ReasonCode reasonCode = imdnToFileTransferFailedReasonCode(imdn);
            handleGroupFileDeliveryStatusFailed(chatId, msgId, contact, reasonCode);
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
     * Resume an outgoing HTTP file transfer
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
    public void resumeOutgoingFileTransfer(FileSharingSession session, boolean isGroup) {
        if (sLogger.isActivated()) {
            sLogger.info("Resume outgoing file transfer from " + session.getRemoteContact());
        }

        // Add session in the list
        String fileTransferId = session.getFileTransferId();
        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, mMessagingLog);
        if (isGroup) {
            GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
                    session.getContributionID(), mGroupFileTransferBroadcaster, mImService,
                    storageAccessor, this, mRcsSettings, mCore, mMessagingLog, mContactManager);
            session.addListener(groupFileTransfer);
            addGroupFileTransfer(fileTransferId, groupFileTransfer);
        } else {
            OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                    fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                    this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                    mOneToOneUndeliveredImManager);
            session.addListener(oneToOneFileTransfer);
            addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
        }
    }

    /**
     * Resume an incoming HTTP file transfer
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     * @param chatSessionId corresponding chatSessionId
     * @param chatId corresponding chatId
     */
    public void resumeIncomingFileTransfer(FileSharingSession session, boolean isGroup,
            String chatSessionId, String chatId) {
        if (sLogger.isActivated()) {
            sLogger.info("Resume incoming file transfer from " + session.getRemoteContact());
        }
        String fileTransferId = session.getFileTransferId();
        FileTransferPersistedStorageAccessor storageAccessor = new FileTransferPersistedStorageAccessor(
                fileTransferId, mMessagingLog);
        if (isGroup) {
            GroupFileTransferImpl groupFileTransfer = new GroupFileTransferImpl(fileTransferId,
                    session.getContributionID(), mGroupFileTransferBroadcaster, mImService,
                    storageAccessor, this, mRcsSettings, mCore, mMessagingLog, mContactManager);
            session.addListener(groupFileTransfer);
            addGroupFileTransfer(fileTransferId, groupFileTransfer);
        } else {
            OneToOneFileTransferImpl oneToOneFileTransfer = new OneToOneFileTransferImpl(
                    fileTransferId, mOneToOneFileTransferBroadcaster, mImService, storageAccessor,
                    this, mRcsSettings, mCore, mMessagingLog, mContactManager,
                    mOneToOneUndeliveredImManager);
            session.addListener(oneToOneFileTransfer);
            addOneToOneFileTransfer(fileTransferId, oneToOneFileTransfer);
        }
    }

    /**
     * Mark a received file transfer as read (i.e. the invitation or the file has been displayed in
     * the UI).
     * 
     * @param transferId File transfer ID
     * @throws RemoteException
     */
    @Override
    public void markFileTransferAsRead(String transferId) throws RemoteException {
        if (TextUtils.isEmpty(transferId)) {
            throw new ServerApiIllegalArgumentException("transferId must not be null or empty!");
        }
        try {
            /* No notification type corresponds currently to mark as read */
            mMessagingLog.markFileTransferAsRead(transferId);
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
     * Deletes all one to one file transfer history and abort/reject any associated ongoing session
     * if such exists.
     * 
     * @throws RemoteException
     */
    public void deleteOneToOneFileTransfers() throws RemoteException {
        try {
            mImOperationExecutor.execute(new OneToOneFileTransferDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock));
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
     * Deletes all group file transfer from history and abort/reject any associated ongoing session
     * if such exists.
     * 
     * @throws RemoteException
     */
    public void deleteGroupFileTransfers() throws RemoteException {
        try {
            mImOperationExecutor.execute(new GroupFileTransferDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock));
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
     * Deletes file transfer corresponding to a given one to one chat specified by contact from
     * history and abort/reject any associated ongoing session if such exists.
     * 
     * @param contact
     * @throws RemoteException
     */
    public void deleteOneToOneFileTransfers2(ContactId contact) throws RemoteException {
        if (contact == null) {
            throw new ServerApiIllegalArgumentException("contact must not be null!");
        }
        try {
            mImOperationExecutor.execute(new OneToOneFileTransferDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock, contact));
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
     * Deletes file transfer corresponding to a given group chat specified by chat id from history
     * and abort/reject any associated ongoing session if such exists.
     * 
     * @param chatId
     * @throws RemoteException
     */
    public void deleteGroupFileTransfers2(String chatId) throws RemoteException {
        if (TextUtils.isEmpty(chatId)) {
            throw new ServerApiIllegalArgumentException("chatId must not be null or empty!");
        }
        try {
            mImOperationExecutor.execute(new GroupFileTransferDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock, chatId));
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
     * Deletes a file transfer by its unique id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param transferId
     * @throws RemoteException
     */
    public void deleteFileTransfer(String transferId) throws RemoteException {
        if (TextUtils.isEmpty(transferId)) {
            throw new ServerApiIllegalArgumentException("transferId must not be null or empty!");
        }
        if (mMessagingLog.isGroupFileTransfer(transferId)) {
            mImOperationExecutor.execute(new GroupFileTransferDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock, mMessagingLog
                            .getFileTransferChatId(transferId), transferId));
        } else {
            mImOperationExecutor.execute(new OneToOneFileTransferDeleteTask(this, mImService,
                    mLocalContentResolver, mImsLock, transferId));
        }
    }

    /**
     * Disables and clears any delivery expiration for a set of file transfers regardless if the
     * delivery of them has expired already or not.
     * 
     * @param transferIds
     * @throws RemoteException
     */
    public void clearFileTransferDeliveryExpiration(List<String> transferIds)
            throws RemoteException {
        if (transferIds == null || transferIds.isEmpty()) {
            throw new ServerApiIllegalArgumentException(
                    "transferId list must not be null or empty!");
        }
        for (String transferId : transferIds) {
            mOneToOneUndeliveredImManager.cancelDeliveryTimeoutAlarm(transferId);
        }
        mMessagingLog.clearFileTransferDeliveryExpiration(transferIds);
    }

    /**
     * Add and broadcast file transfer invitation rejections
     * 
     * @param contact Contact
     * @param content File content
     * @param fileIcon File content
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     * @param timestampSent Timestamp sent in payload for the file transfer
     */
    public void addFileTransferInvitationRejected(ContactId contact, MmContent content,
            MmContent fileIcon, ReasonCode reasonCode, long timestamp, long timestampSent) {
        String fileTransferId = IdGenerator.generateMessageID();
        mMessagingLog.addOneToOneFileTransfer(fileTransferId, contact, Direction.INCOMING, content,
                fileIcon, State.REJECTED, reasonCode, timestamp, timestampSent,
                FileTransferData.UNKNOWN_EXPIRATION, FileTransferData.UNKNOWN_EXPIRATION);

        mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
    }

    /**
     * Set and broadcast resend file transfer invitation rejections
     * 
     * @param contact Contact
     * @param reasonCode Reason code
     * @param timestamp Local timestamp when got invitation
     * @param timestampSent Timestamp sent in payload for the file transfer
     */
    public void setResendFileTransferInvitationRejected(String fileTransferId, ContactId contact,
            ReasonCode reasonCode, long timestamp, long timestampSent) {
        mMessagingLog.setFileTransferStateAndTimestamps(fileTransferId, State.REJECTED, reasonCode,
                timestamp, timestampSent);

        mOneToOneFileTransferBroadcaster.broadcastInvitation(fileTransferId);
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
     * Set one-one file transfer state and reason code
     * 
     * @param fileTransferId
     * @param contact
     * @param state
     * @param reasonCode
     */
    public void setOneToOneFileTransferStateAndReasonCode(String fileTransferId, ContactId contact,
            State state, ReasonCode reasonCode) {
        mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
        mOneToOneFileTransferBroadcaster.broadcastStateChanged(contact, fileTransferId, state,
                reasonCode);
    }

    /**
     * Set group file transfer state and reason code
     * 
     * @param fileTransferId
     * @param chatId
     * @param state
     * @param reasonCode
     */
    public void setGroupFileTransferStateAndReasonCode(String fileTransferId, String chatId,
            State state, ReasonCode reasonCode) {
        mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
        mGroupFileTransferBroadcaster.broadcastStateChanged(chatId, fileTransferId, state,
                reasonCode);
    }

    public void broadcastOneToOneFileTransferDeleted(ContactId contact, Set<String> transferIds) {
        mOneToOneFileTransferBroadcaster.broadcastFileTransferDeleted(contact, transferIds);
    }

    public void broadcastGroupFileTransfersDeleted(String chatId, Set<String> transferIds) {
        mGroupFileTransferBroadcaster.broadcastFileTransfersDeleted(chatId, transferIds);
    }
}
