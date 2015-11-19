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

package com.gsma.rcs.core.ims.service.im;

import static com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession.isFileCapacityAcceptable;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ContactInfo.RegistrationState;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatAutoRejoinTask;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OriginatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.imdn.DeliveryExpirationManager;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.StoreAndForwardManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatNotificationSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromInviteFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FtHttpResumeManager;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpGroupFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.msrp.OriginatingMsrpFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.msrp.TerminatingMsrpFileSharingSession;
import com.gsma.rcs.core.ims.service.upload.FileUploadSession;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.history.GroupChatDequeueTask;
import com.gsma.rcs.provider.history.GroupChatTerminalExceptionTask;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.history.OneToOneChatDequeueTask;
import com.gsma.rcs.provider.messaging.DelayedDisplayNotificationDispatcher;
import com.gsma.rcs.provider.messaging.FileTransferDequeueTask;
import com.gsma.rcs.provider.messaging.GroupChatDeleteTask;
import com.gsma.rcs.provider.messaging.GroupChatMessageDeleteTask;
import com.gsma.rcs.provider.messaging.GroupFileTransferDeleteTask;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneChatMessageDeleteTask;
import com.gsma.rcs.provider.messaging.OneToOneChatMessageDequeueTask;
import com.gsma.rcs.provider.messaging.OneToOneFileTransferDeleteTask;
import com.gsma.rcs.provider.messaging.RecreateDeliveryExpirationAlarms;
import com.gsma.rcs.provider.messaging.UpdateFileTransferStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImMsgTech;
import com.gsma.rcs.service.GroupChatInviteQueuedParticipantsTask;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.ServerApiMaxAllowedSessionLimitReachedException;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

/**
 * Instant messaging services (1-1 chat, group chat and file transfer)
 * 
 * @author Jean-Marc AUFFRET
 */
public class InstantMessagingService extends ImsService {

    private static final String IM_OPERATION_THREAD_NAME = "ImOperations";

    private static final String IM_DELETE_OPERATION_THREAD_NAME = "ImDeleteOperations";

    private final Core mCore;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final MessagingLog mMessagingLog;

    private final HistoryLog mHistoryLog;

    /**
     * OneToOneChatSessionCache with ContactId as key
     */
    private Map<ContactId, OneToOneChatSession> mOneToOneChatSessionCache = new HashMap<>();

    /**
     * StoreAndForwardMsgSessionCache with ContactId as key
     */
    private Map<ContactId, TerminatingStoreAndForwardOneToOneChatMessageSession> mStoreAndForwardMsgSessionCache = new HashMap<>();

    /**
     * StoreAndForwardNotifSessionCache with ContactId as key
     */
    private Map<ContactId, TerminatingStoreAndForwardOneToOneChatNotificationSession> mStoreAndForwardNotifSessionCache = new HashMap<>();

    /**
     * GroupChatSessionCache with ChatId as key
     */
    private Map<String, GroupChatSession> mGroupChatSessionCache = new HashMap<>();

    /**
     * FileSharingSessionCache with FileTransferId as key
     */
    private Map<String, FileSharingSession> mFileTransferSessionCache = new HashMap<>();

    /**
     * FileUploadSessionCache with UploadId as key
     */
    private Map<String, FileUploadSession> mFileUploadSessionCache = new HashMap<>();

    /**
     * GroupChatConferenceSubscriberCache with Conference subscriber's dialog path CallId as key
     */
    private Map<String, GroupChatSession> mGroupChatConferenceSubscriberCache = new HashMap<>();

    /**
     * Group Chat composing status to notify upon MSRP session restart
     */
    private final Map<String, Boolean> mGroupChatComposingStatusToNotify = new HashMap<>();

    /**
     * One-to-One Chat composing status to notify upon MSRP session restart
     */
    private final Map<ContactId, Boolean> mOneToOneChatComposingStatusToNotify = new HashMap<>();

    /**
     * Chat features tags
     */
    public final static String[] CHAT_FEATURE_TAGS = {
        FeatureTags.FEATURE_OMA_IM
    };

    /**
     * File transfer features tags
     */
    public final static String[] FT_FEATURE_TAGS = {
        FeatureTags.FEATURE_OMA_IM
    };

    private final ImdnManager mImdnManager;

    private final LocalContentResolver mLocalContentResolver;

    private final Context mCtx;

    private final StoreAndForwardManager mStoreAndFwdMgr;

    private final DeliveryExpirationManager mDeliveryExpirationManager;

    private static final Logger sLogger = Logger.getLogger(InstantMessagingService.class.getName());

    private static final String sSizeExceededMsg = "133 Size exceeded";

    private final Handler mImOperationHandler;

    private final Handler mImDeleteOperationHandler;

    private ChatServiceImpl mChatService;

    private FileTransferServiceImpl mFileTransferService;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactManager
     * @param messagingLog Messaging log accessor
     * @param historyLog History log accessor
     * @param localContentResolver local content resolver
     * @param core Core
     */
    public InstantMessagingService(ImsModule parent, RcsSettings rcsSettings,
            ContactManager contactsManager, MessagingLog messagingLog, HistoryLog historyLog,
            LocalContentResolver localContentResolver, Context ctx, Core core) {
        super(parent, true);
        mCtx = ctx;
        mCore = core;
        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
        mMessagingLog = messagingLog;
        mHistoryLog = historyLog;
        mLocalContentResolver = localContentResolver;

        mImOperationHandler = allocateBgHandler(IM_OPERATION_THREAD_NAME);
        mImDeleteOperationHandler = allocateBgHandler(IM_DELETE_OPERATION_THREAD_NAME);

        mStoreAndFwdMgr = new StoreAndForwardManager(this, mRcsSettings, mContactManager,
                mMessagingLog);
        mImdnManager = new ImdnManager(this, mRcsSettings);
        mDeliveryExpirationManager = new DeliveryExpirationManager(this, ctx, mMessagingLog);
    }

    private Handler allocateBgHandler(String threadName) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        return new Handler(thread.getLooper());
    }

    public void register(ChatServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mChatService = service;
    }

    public void register(FileTransferServiceImpl service) {
        if (sLogger.isActivated()) {
            sLogger.debug(service.getClass().getName() + " registered ok.");
        }
        mFileTransferService = service;
    }

    /**
     * Initializes instant messaging service
     */
    public void initialize() {
        mImdnManager.start();
    }

    public void scheduleImOperation(Runnable runnable) {
        mImOperationHandler.post(runnable);
    }

    public void onCoreLayerStarted() {
        /* Update interrupted file transfer status */
        scheduleImOperation(new UpdateFileTransferStateAfterUngracefulTerminationTask(
                mMessagingLog, mFileTransferService));
        /*
         * Recreate delivery expiration alarm for one-one chat messages and one-one file transfers
         * after boot
         */
        scheduleImOperation(new RecreateDeliveryExpirationAlarms(mMessagingLog,
                mDeliveryExpirationManager));
    }

    private void send403Forbidden(SipRequest request, String warning) throws PayloadException,
            NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Send 403 Forbidden (warning=" + warning + ")");
        }
        getImsModule().getSipManager().sendSipResponse(
                SipMessageFactory.createResponse(request, null, Response.FORBIDDEN, warning));
    }

    @Override
    public synchronized void start() {
        if (isServiceStarted()) {
            return;
        }
        setServiceStarted(true);

        /* Try to auto-rejoin group chats that are still marked as active. */
        mImOperationHandler.post(new GroupChatAutoRejoinTask(this, mMessagingLog));
        /* Try to start auto resuming of HTTP file transfers marked as PAUSED_BY_SYSTEM */
        mImOperationHandler.post(new FtHttpResumeManager(this, mRcsSettings, mMessagingLog,
                mContactManager));
        /* Try to dequeue one-to-one chat messages and one-to-one file transfers. */
        mImOperationHandler.post(new OneToOneChatDequeueTask(mCtx, mCore, mMessagingLog,
                mRcsSettings, mChatService, mFileTransferService, mContactManager, mHistoryLog));

        if (mImdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()
                || mImdnManager.isSendGroupDeliveryDisplayedReportsEnabled()) {
            /*
             * Try to send delayed displayed notifications for read messages if they were not sent
             * before already. This only attempts to send report and in case of failure the report
             * will be sent later as postponed delivery report
             */
            mImOperationHandler.post(new DelayedDisplayNotificationDispatcher(
                    mLocalContentResolver, mChatService));
        }
    }

    @Override
    public synchronized void stop() {
        if (!isServiceStarted()) {
            return;
        }
        setServiceStarted(false);

        mImdnManager.terminate();
        mImdnManager.interrupt();
    }

    @Override
    public void check() {
    }

    /**
     * Returns the IMDN manager
     * 
     * @return IMDN manager
     */
    public ImdnManager getImdnManager() {
        return mImdnManager;
    }

    /**
     * Get Store & Forward manager
     */
    public StoreAndForwardManager getStoreAndForwardManager() {
        return mStoreAndFwdMgr;
    }

    /**
     * Get the delivery expiration manager
     */
    public DeliveryExpirationManager getDeliveryExpirationManager() {
        return mDeliveryExpirationManager;
    }

    public void addSession(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final OneToOneChatSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatSessionCache.remove(contact);
            removeImsServiceSession(session);
        }
    }

    public OneToOneChatSession getOneToOneChatSession(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mOneToOneChatSessionCache.get(contact);
        }
    }

    public void addSession(TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardMsgSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardMsgSessionCache.remove(contact);
            removeImsServiceSession(session);
        }
    }

    public TerminatingStoreAndForwardOneToOneChatMessageSession getStoreAndForwardMsgSession(
            ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mStoreAndForwardMsgSessionCache.get(contact);
        }
    }

    public void addSession(TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add StoreAndForwardNotifSessionCache with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardNotifSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(
            final TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder(
                    "Remove StoreAndForwardNotifSessionCache with contact '").append(contact)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardNotifSessionCache.remove(contact);
            removeImsServiceSession(session);
        }
    }

    public TerminatingStoreAndForwardOneToOneChatNotificationSession getStoreAndForwardNotifSession(
            ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get StoreAndForwardNotifSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mStoreAndForwardNotifSessionCache.get(contact);
        }
    }

    public void addSession(GroupChatSession session) {
        String chatId = session.getContributionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatSessionCache.put(chatId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GroupChatSession session) {
        final String chatId = session.getContributionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            SipDialogPath conferenceSubscriberDialogPath = session.getConferenceEventSubscriber()
                    .getDialogPath();
            if (conferenceSubscriberDialogPath != null) {
                mGroupChatConferenceSubscriberCache.remove(conferenceSubscriberDialogPath
                        .getCallId());
            }
            mGroupChatSessionCache.remove(chatId);
            removeImsServiceSession(session);
        }
    }

    public GroupChatSession getGroupChatSession(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatSessionCache.get(chatId);
        }
    }

    public void addGroupChatConferenceSubscriber(String callId, GroupChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GroupChatConferenceSubscriber with callId '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatConferenceSubscriberCache.put(callId, session);
        }
    }

    public void removeGroupChatConferenceSubscriber(final String callId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GroupChatConferenceSubscriber with callId '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatConferenceSubscriberCache.remove(callId);
        }
    }

    public GroupChatSession getGroupChatSessionOfConferenceSubscriber(String callId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GroupChatSession with ConferenceSunscriber '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatConferenceSubscriberCache.get(callId);
        }
    }

    public boolean isChatSessionAvailable() {
        synchronized (getImsServiceSessionOperationLock()) {
            /*
             * maxChatSessions == 0 means that the allowed number of chat sessions in use is
             * disabled
             */
            int maxChatSessions = mRcsSettings.getMaxChatSessions();
            return maxChatSessions == 0
                    || mOneToOneChatSessionCache.size() + mGroupChatSessionCache.size() < maxChatSessions;
        }
    }

    /**
     * Assert if it is allowed to initiate a new chat session right now or the allowed limit has
     * been reached.
     * 
     * @param errorMessage The error message
     */
    public void assertAvailableChatSession(String errorMessage) {
        if (!isChatSessionAvailable()) {
            throw new ServerApiMaxAllowedSessionLimitReachedException(errorMessage);
        }
    }

    public void addSession(FileSharingSession session) {
        String fileTransferId = session.getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileTransferSessionCache.put(fileTransferId, session);
            /*
             * Only FileSharingSessions of type ImsFileSharingSession has a dialog path. Hence add
             * only those type of sessions to the ImsServiceSession cache and add
             * HttpFileTransferSession to ImsServiceSessionWithoutDialogPath cache.
             */
            if (session instanceof ImsFileSharingSession) {
                addImsServiceSession(session);
            } else if (session instanceof HttpFileTransferSession) {
                addImsServiceSessionWithoutDialogPath(session);
            }
        }
    }

    public void removeSession(final FileSharingSession session) {
        final String fileTransferId = session.getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileTransferSessionCache.remove(fileTransferId);
            /*
             * Only FileSharingSessions of type ImsFileSharingSession has a dialog path. Hence it is
             * possible to remove only those type of sessions from the ImsServiceSession cache and
             * remove HttpFileTransferSession from ImsServiceSessionWithoutDialogPath cache.
             */
            if (session instanceof ImsFileSharingSession) {
                removeImsServiceSession(session);
            } else if (session instanceof HttpFileTransferSession) {
                removeImsServiceSessionWithoutDialogPath(session);
            }
        }
    }

    public FileSharingSession getFileSharingSession(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mFileTransferSessionCache.get(fileTransferId);
        }
    }

    public void addSession(FileUploadSession session) {
        String uploadId = session.getUploadID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileUploadSessionCache.put(uploadId, session);
        }
    }

    public void removeSession(final FileUploadSession session) {
        final String uploadId = session.getUploadID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileUploadSessionCache.remove(uploadId);
        }
    }

    public FileUploadSession getFileUploadSession(String uploadId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mFileUploadSessionCache.get(uploadId);
        }
    }

    public boolean isFileTransferSessionAvailable() {
        synchronized (getImsServiceSessionOperationLock()) {
            /*
             * maxFtSessions == 0 means that the checking of allowed number of file transfer
             * sessions in use is disabled
             */
            int maxFileTransferSessions = mRcsSettings.getMaxFileTransferSessions();
            return maxFileTransferSessions == 0
                    || mFileTransferSessionCache.size() + mFileUploadSessionCache.size() < maxFileTransferSessions;
        }
    }

    public void assertAvailableFileTransferSession(String errorMessage) {
        if (!isFileTransferSessionAvailable()) {
            throw new ServerApiMaxAllowedSessionLimitReachedException(errorMessage);
        }
    }

    public void assertFileSizeNotExceedingMaxLimit(long size, String errorMessage) {
        /*
         * maxFtSize == 0 means that the checking of allowed number of file transfer size in use is
         * disabled
         */
        long maxFileTransferSize = mRcsSettings.getMaxFileTransferSize();
        if (maxFileTransferSize > 0 && size > maxFileTransferSize) {
            throw new ServerApiPersistentStorageException(errorMessage);
        }
    }

    /**
     * Checks if max number of concurrent outgoing file transfer sessions reached
     * 
     * @return boolean
     */
    public boolean isMaxConcurrentOutgoingFileTransfersReached() {
        int nrOfConcurrentOutgoingFileTransferSessions = 0;
        synchronized (getImsServiceSessionOperationLock()) {
            for (FileSharingSession session : mFileTransferSessionCache.values()) {
                if (!session.isInitiatedByRemote()) {
                    nrOfConcurrentOutgoingFileTransferSessions++;
                }
            }
            /*
             * maxConcurrentOutgoingFilrTransferSessions == 0 means that the checking of allowed
             * concurrent number of outgoing file transfers in use is disabled
             */
            int maxConcurrentOutgoingFileTransferSessions = mRcsSettings
                    .getMaxConcurrentOutgoingFileTransferSessions();
            if (maxConcurrentOutgoingFileTransferSessions == 0) {
                return false;
            }
            if (nrOfConcurrentOutgoingFileTransferSessions >= maxConcurrentOutgoingFileTransferSessions) {
                return true;
            }
            nrOfConcurrentOutgoingFileTransferSessions += mFileUploadSessionCache.size();
            return nrOfConcurrentOutgoingFileTransferSessions >= maxConcurrentOutgoingFileTransferSessions;
        }
    }

    /**
     * Initiate a file transfer session
     * 
     * @param fileTransferId File transfer Id
     * @param contact Remote contact identifier
     * @param content Content of file to sent
     * @param fileIcon Content of fileicon
     * @param timestamp the local timestamp when initiating the file transfer
     * @param ftProtocol FileTransferProtocol
     * @return File transfer session
     */
    public FileSharingSession createFileTransferSession(String fileTransferId, ContactId contact,
            MmContent content, MmContent fileIcon, long timestamp, FileTransferProtocol ftProtocol) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a file transfer session with contact " + contact + ", file "
                    + content.toString());
        }
        switch (ftProtocol) {
            case HTTP:
                return new OriginatingHttpFileSharingSession(this, fileTransferId, content,
                        contact, fileIcon, UUID.randomUUID().toString(), mMessagingLog,
                        mRcsSettings, timestamp, mContactManager);
            case MSRP:
                /*
                 * Since in MSRP communication we do not have a timestampSent to be sent in payload,
                 * then we don't need to pass the timestampSent to OriginatingMsrpFileSharingSession
                 */
                return new OriginatingMsrpFileSharingSession(this, fileTransferId, content,
                        contact, fileIcon, mRcsSettings, timestamp, mContactManager);
            default:
                throw new IllegalArgumentException(new StringBuilder(
                        "Unknown FileTransferProtocol ").append(ftProtocol).toString());
        }
    }

    /**
     * Initiate a group file transfer session
     * 
     * @param fileTransferId File transfer Id
     * @param content The file content to be sent
     * @param fileIcon Content of fileicon
     * @param groupChatId Chat contribution ID
     * @param timestamp the local timestamp when initiating the file transfer
     * @return File transfer session
     */
    public FileSharingSession createGroupFileTransferSession(String fileTransferId,
            MmContent content, MmContent fileIcon, String groupChatId, long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.info("Send file " + content.toString() + " to " + groupChatId);
        }
        return new OriginatingHttpGroupFileSharingSession(this, fileTransferId, content, fileIcon,
                ImsModule.getImsUserProfile().getImConferenceUri(), groupChatId, UUID.randomUUID()
                        .toString(), mRcsSettings, mMessagingLog, timestamp, mContactManager);
    }

    /**
     * Receive a MSRP file transfer invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onMsrpFileTransferInvitationReceived(final SipRequest invite, final long timestamp) {
        final InstantMessagingService imService = this;
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    String assertedId = SipUtils.getAssertedIdentity(invite);
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(assertedId);
                    if (number == null) {
                        if (logActivated) {
                            sLogger.error("Discard MSRP FileTransfer Invitation: invalid remote ID '"
                                    + assertedId + "'");
                        }
                        sendErrorResponse(invite, Response.DECLINE);
                        return;

                    }
                    ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
                    if (logActivated) {
                        sLogger.debug("Receive a file transfer session invitation from "
                                .concat(remote.toString()));
                    }
                    String displayName = SipUtils.getDisplayNameFromUri(invite.getFrom());
                    /*
                     * Update the remote contact's capabilities to include at least MSRP FT
                     * capabilities as we have just received a MSRP file transfer session invitation
                     * from this contact so he/she must at least have this capability. We do not
                     * need any capability exchange response to determine that.
                     */
                    mContactManager.mergeContactCapabilities(remote, new CapabilitiesBuilder()
                            .setFileTransferMsrp(true).setTimestampOfLastResponse(timestamp)
                            .build(), RcsStatus.RCS_CAPABLE, RegistrationState.ONLINE, displayName);

                    /**
                     * Since in MSRP communication we do not have a timestampSent to be extracted
                     * from the payload then we need to fake that by using the local timestamp even
                     * if this is not the real proper timestamp from the remote side in this case.
                     */
                    long timestampSent = timestamp;
                    if (mContactManager.isBlockedForContact(remote)) {
                        if (logActivated) {
                            sLogger.debug("Contact "
                                    + remote
                                    + " is blocked: automatically reject the file transfer invitation");
                        }
                        MmContent content = ContentManager.createMmContentFromSdp(invite,
                                mRcsSettings);
                        MmContent fileIcon = FileTransferUtils
                                .extractFileIcon(invite, mRcsSettings);
                        addFileTransferInvitationRejected(remote, content, fileIcon,
                                FileTransfer.ReasonCode.REJECTED_SPAM, timestamp, timestampSent);
                        sendErrorResponse(invite, Response.DECLINE);
                        return;
                    }

                    if (!isFileTransferSessionAvailable()) {
                        if (logActivated) {
                            sLogger.debug("The max number of file transfer sessions is achieved: reject the invitation from "
                                    .concat(remote.toString()));
                        }
                        MmContent content = ContentManager.createMmContentFromSdp(invite,
                                mRcsSettings);
                        MmContent fileIcon = FileTransferUtils
                                .extractFileIcon(invite, mRcsSettings);
                        addFileTransferInvitationRejected(remote, content, fileIcon,
                                FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                timestampSent);
                        sendErrorResponse(invite, Response.DECLINE);
                        return;
                    }
                    /*
                     * Reject if file is too big or size exceeds device storage capacity. This
                     * control should be done on UI. It is done after end user accepts invitation to
                     * enable prior handling by the application.
                     */
                    MmContent content = ContentManager.createMmContentFromSdp(invite, mRcsSettings);
                    FileSharingError error = isFileCapacityAcceptable(content.getSize(),
                            mRcsSettings);
                    if (error != null) {
                        /*
                         * Extract of GSMA specification: If the file is bigger than FT MAX SIZE, a
                         * warning message is displayed when trying to send or receive a file larger
                         * than the mentioned limit and the transfer will be cancelled (that is at
                         * protocol level, the SIP INVITE request will never be sent or an automatic
                         * rejection response SIP 403 Forbidden with a Warning header set to 133
                         * Size exceeded will be sent by the entity that detects that the file size
                         * is too big to the other end depending on the scenario).
                         */
                        send403Forbidden(invite, sSizeExceededMsg);
                        int errorCode = error.getErrorCode();
                        MmContent fileIcon = FileTransferUtils
                                .extractFileIcon(invite, mRcsSettings);
                        switch (errorCode) {
                            case FileSharingError.MEDIA_SIZE_TOO_BIG:
                                addFileTransferInvitationRejected(remote, content, fileIcon,
                                        FileTransfer.ReasonCode.REJECTED_MAX_SIZE, timestamp,
                                        timestampSent);
                                break;
                            case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                                addFileTransferInvitationRejected(remote, content, fileIcon,
                                        FileTransfer.ReasonCode.REJECTED_LOW_SPACE, timestamp,
                                        timestampSent);
                                break;
                            default:
                                sLogger.error("Unexpected error while receiving MSRP file transfer invitation"
                                        .concat(Integer.toString(errorCode)));
                        }
                        return;
                    }

                    FileSharingSession session = new TerminatingMsrpFileSharingSession(imService,
                            invite, remote, mRcsSettings, timestamp, timestampSent, mContactManager);
                    mFileTransferService.receiveFileTransferInvitation(session, false, remote,
                            displayName);
                    session.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive msrp file transfer invitation! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (FileAccessException | ContactManagerException | PayloadException e) {
                    sLogger.error("Failed to receive msrp file transfer invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive msrp file transfer invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Initiate a one-to-one chat session
     * 
     * @param contact Remote contact identifier
     * @param firstMsg First message
     * @return IM session
     */
    public OneToOneChatSession createOneToOneChatSession(ContactId contact, ChatMessage firstMsg) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Initiate 1-1 chat session with ").append(contact)
                    .append(".").toString());
        }
        long timestamp = firstMsg.getTimestamp();
        return new OriginatingOneToOneChatSession(this, contact, firstMsg, mRcsSettings,
                mMessagingLog, timestamp, mContactManager);
    }

    /**
     * Receive a one-to-one chat session invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onOne2OneChatSessionReceived(final SipRequest invite, final long timestamp) {
        final InstantMessagingService imService = this;
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    /*
                     * Invitation will be rejected if it is OMA SIMPLE IM solution but it doesn't
                     * contains first message. Reference to spec: Rich Communication Suite 5.1
                     * Advanced Communications Services and Client Specification Version 4.0 Page
                     * 187 3.3.4.2 Technical Realization of 1-to-1 Chat features when using OMA
                     * SIMPLE IM At the technical level the 1-to-1 Chat service implemented using
                     * OMA SIMPLE IM extends the concepts described in section 3.3.4.1 with the
                     * following concepts: For OMA SIMPLE IM, first message is always included in a
                     * CPIM/IMDN wrapper carried in the SIP INVITE request. So the configuration
                     * parameter FIRST MSG IN INVITE defined in Table 80 is always set to 1.
                     */
                    if (!ChatUtils.isContainingFirstMessage(invite)) {
                        ImMsgTech mode = mRcsSettings.getImMsgTech();
                        switch (mode) {
                            case CPM:
                                /* Only reject the invitation when FirstMessageInInvite is true. */
                                if (mRcsSettings.isFirstMessageInInvite()) {
                                    if (logActivated) {
                                        sLogger.error("Currently in Cpm mode, Reject 1-1 chat invition due to it doesn't"
                                                .concat("carry first message."));
                                    }
                                    sendErrorResponse(invite, Response.DECLINE);
                                    return;
                                }
                                break;
                            case SIMPLE_IM:
                                if (logActivated) {
                                    sLogger.error("Currently in SIMPLE_IM mode, Reject 1-1 chat invition due to it doesn't"
                                            .concat("carry first message."));
                                }
                                sendErrorResponse(invite, Response.DECLINE);
                                return;
                            default:
                                if (sLogger.isActivated()) {
                                    sLogger.error("Unexpected ImMsgTech code:".concat(String
                                            .valueOf(mode)));
                                }
                                return;
                        }
                    }

                    String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
                    ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
                    String displayName = SipUtils.getDisplayNameFromUri(invite.getFrom());
                    if (remote == null) {
                        if (logActivated) {
                            sLogger.error("Discard One2OneChatSession: invalid remote ID '"
                                    + referredId + "'");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;

                    }
                    if (logActivated) {
                        sLogger.debug("Receive a 1-1 chat session invitation from ".concat(remote
                                .toString()));
                    }
                    /*
                     * Update the remote contact's capabilities to include at least IM session
                     * capabilities as we have just received a one-one chat session invitation from
                     * this contact so he/she must at least have this capability. We do not need any
                     * capability exchange response to determine that.
                     */
                    mContactManager.mergeContactCapabilities(remote, new CapabilitiesBuilder()
                            .setImSession(true).setTimestampOfLastResponse(timestamp).build(),
                            RcsStatus.RCS_CAPABLE, RegistrationState.ONLINE, displayName);

                    ChatMessage firstMsg = ChatUtils.getFirstMessage(invite, timestamp);
                    if (mContactManager.isBlockedForContact(remote)) {
                        if (logActivated) {
                            sLogger.debug("Contact " + remote
                                    + " is blocked: automatically reject the chat invitation");
                        }

                        if (firstMsg != null
                                && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
                            mMessagingLog.addOneToOneSpamMessage(firstMsg);
                        }

                        if (mImdnManager.isDeliveryDeliveredReportsEnabled()) {
                            String msgId = ChatUtils.getMessageId(invite);
                            if (msgId != null) {
                                String remoteInstanceId = null;
                                ContactHeader inviteContactHeader = (ContactHeader) invite
                                        .getHeader(ContactHeader.NAME);
                                if (inviteContactHeader != null) {
                                    remoteInstanceId = inviteContactHeader
                                            .getParameter(SipUtils.SIP_INSTANCE_PARAM);
                                }
                                mImdnManager.sendMessageDeliveryStatusImmediately(
                                        remote.toString(), remote, msgId,
                                        ImdnDocument.DELIVERY_STATUS_DELIVERED, remoteInstanceId,
                                        timestamp);
                            }
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }

                    /*
                     * Save the message if it was not already persisted in the DB. We don't have to
                     * reject the session if the message was a duplicate one as the session
                     * rejection/keeping will be handled in
                     * TerminatingOneToOneChatSession.startSession() in an uniform way as according
                     * to the defined race conditions in the specification document.
                     */
                    if (firstMsg != null
                            && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
                        boolean imdnDisplayRequested = mImdnManager
                                .isSendOneToOneDeliveryDisplayedReportsEnabled()
                                && ChatUtils.isImdnDisplayedRequested(invite);
                        mMessagingLog
                                .addIncomingOneToOneChatMessage(firstMsg, imdnDisplayRequested);
                    }

                    if (!isChatSessionAvailable()) {
                        if (logActivated) {
                            sLogger.debug("The max number of chat sessions is achieved: reject the invitation from "
                                    .concat(remote.toString()));
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }

                    TerminatingOneToOneChatSession session = new TerminatingOneToOneChatSession(
                            imService, invite, remote, mRcsSettings, mMessagingLog, timestamp,
                            mContactManager);
                    mChatService.receiveOneToOneChatInvitation(session);
                    session.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive o2o chat invitation! (" + e.getMessage()
                                + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (FileAccessException | ContactManagerException | PayloadException e) {
                    sLogger.error("Failed to receive o2o chat invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive o2o chat invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Create an ad-hoc group chat session
     * 
     * @param contacts List of contact identifiers
     * @param subject Subject
     * @param timestamp Local timestamp
     * @return GroupChatSession
     */
    public GroupChatSession createOriginatingAdHocGroupChatSession(Set<ContactId> contacts,
            String subject, long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an ad-hoc group chat session");
        }
        Map<ContactId, ParticipantStatus> participants = ChatUtils.getParticipants(contacts,
                ParticipantStatus.INVITING);
        return new OriginatingAdhocGroupChatSession(this, ImsModule.getImsUserProfile()
                .getImConferenceUri(), subject, participants, mRcsSettings, mMessagingLog,
                timestamp, mContactManager);
    }

    /**
     * Receive ad-hoc group chat session invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onAdHocGroupChatSessionReceived(final SipRequest invite, final long timestamp) {
        final InstantMessagingService imService = this;
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    ContactId contact = ChatUtils.getReferredIdentityAsContactId(invite);
                    if (logActivated) {
                        sLogger.debug("Receive an ad-hoc group chat session invitation from "
                                .concat(contact.toString()));
                    }
                    String displayName = SipUtils.getDisplayNameFromUri(invite.getFrom());
                    /*
                     * Update the remote contact's capabilities to include at least IM session
                     * capabilities as we have just received a group chat session invitation from
                     * this contact so he/she must at least have this capability. We do not need any
                     * capability exchange response to determine that.
                     */
                    mContactManager.mergeContactCapabilities(contact, new CapabilitiesBuilder()
                            .setImSession(true).setTimestampOfLastResponse(timestamp).build(),
                            RcsStatus.RCS_CAPABLE, RegistrationState.ONLINE, displayName);

                    String chatId = ChatUtils.getContributionId(invite);
                    String subject = ChatUtils.getSubject(invite);
                    Map<ContactId, ParticipantStatus> participants = ChatUtils.getParticipants(
                            invite, ParticipantStatus.FAILED);
                    if (mContactManager.isBlockedForContact(contact)) {
                        if (logActivated) {
                            sLogger.debug("Contact " + contact
                                    + " is blocked: automatically reject the chat invitation");
                        }
                        mChatService.addGroupChatInvitationRejected(chatId, contact, subject,
                                participants, GroupChat.ReasonCode.REJECTED_SPAM, timestamp);
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }
                    if (!isChatSessionAvailable()) {
                        if (logActivated) {
                            sLogger.debug("The max number of chat sessions is achieved: reject the invitation from "
                                    .concat(contact.toString()));
                        }
                        mChatService.addGroupChatInvitationRejected(chatId, contact, subject,
                                participants, GroupChat.ReasonCode.REJECTED_MAX_CHATS, timestamp);
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }
                    /*
                     * Get the list of participants from the invite, give them the initial status
                     * INVITED as the actual status was not included in the invite.
                     */
                    Map<ContactId, ParticipantStatus> inviteParticipants = ChatUtils
                            .getParticipants(invite, ParticipantStatus.INVITED);
                    // @FIXME: This method should return an URI
                    String remoteUri = ChatUtils.getReferredIdentityAsContactUri(invite);

                    TerminatingAdhocGroupChatSession session = new TerminatingAdhocGroupChatSession(
                            imService, invite, contact, inviteParticipants, Uri.parse(remoteUri),
                            mRcsSettings, mMessagingLog, timestamp, mContactManager);

                    /*--
                     * 6.3.3.1 Leaving a Group Chat that is idle
                     * In case the user expresses their desire to leave the Group Chat while it is inactive, the device will not offer the user
                     * the possibility any more to enter new messages and restart the chat and automatically decline the first incoming INVITE
                     * request for the chat with a SIP 603 DECLINE response. Subsequent INVITE requests should not be rejected as they may be
                     * received when the user is added again to the Chat by one of the participants.
                     */
                    boolean reject = mMessagingLog.isGroupChatNextInviteRejected(session
                            .getContributionID());
                    if (reject) {
                        if (logActivated) {
                            sLogger.debug("Chat Id "
                                    + session.getContributionID()
                                    + " is declined since previously terminated by user while disconnected");
                        }
                        sendErrorResponse(invite, Response.DECLINE);
                        mMessagingLog.acceptGroupChatNextInvitation(session.getContributionID());
                        return;
                    }
                    mChatService.receiveGroupChatInvitation(session);
                    session.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive group chat invitation! (" + e.getMessage()
                                + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (FileAccessException | ContactManagerException | PayloadException e) {
                    sLogger.error("Failed to receive group chat invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive group chat invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Rejoin a group chat session
     * 
     * @param chatId Chat ID
     * @return RejoinGroupChat session
     */
    public RejoinGroupChatSession rejoinGroupChatSession(String chatId) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Rejoin group chat session");
        }
        assertAvailableChatSession("Max chat sessions reached");

        /* Get the group chat info from database */
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
        if (groupChat == null) {
            if (logActivated) {
                sLogger.warn("Group chat " + chatId + " can't be rejoined: conversation not found");
            }
            throw new ServerApiPersistentStorageException(
                    "Group chat conversation not found in database");
        }
        if (groupChat.getRejoinId() == null) {
            if (logActivated) {
                sLogger.warn("Group chat " + chatId + " can't be rejoined: rejoin ID not found");
            }
            throw new ServerApiPersistentStorageException("Rejoin ID not found in database");
        }
        if (logActivated) {
            sLogger.debug("Rejoin group chat: " + groupChat.toString());
        }
        long timestamp = groupChat.getTimestamp();
        return new RejoinGroupChatSession(this, groupChat, mRcsSettings, mMessagingLog, timestamp,
                mContactManager);
    }

    /**
     * Restart a group chat session
     * 
     * @param chatId Chat ID
     * @return RestartGroupChat session
     */
    public RestartGroupChatSession restartGroupChatSession(String chatId) {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Restart group chat session");
        }
        assertAvailableChatSession("Max chat sessions reached");
        /* Get the group chat info from database */
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
        if (groupChat == null) {
            if (logActivated) {
                sLogger.warn("Group chat " + chatId + " can't be restarted: conversation not found");
            }
            throw new ServerApiPersistentStorageException(
                    "Group chat conversation not found in database");
        }
        if (logActivated) {
            sLogger.debug("Restart group chat: " + groupChat.toString());
        }
        Map<ContactId, ParticipantStatus> storedParticipants = groupChat.getParticipants();
        if (storedParticipants.isEmpty()) {
            if (logActivated) {
                sLogger.warn("Group chat " + chatId + " can't be restarted: participants not found");
            }
            throw new ServerApiPersistentStorageException(
                    "No connected group chat participants found in database");
        }
        long timestamp = groupChat.getTimestamp();
        return new RestartGroupChatSession(this,
                ImsModule.getImsUserProfile().getImConferenceUri(), groupChat.getSubject(), chatId,
                storedParticipants, mRcsSettings, mMessagingLog, timestamp, mContactManager);
    }

    /**
     * Receive a conference notification
     * 
     * @param notify Received notify
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onConferenceNotificationReceived(final SipRequest notify, final long timestamp) {
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    GroupChatSession session = getGroupChatSessionOfConferenceSubscriber(notify
                            .getCallId());
                    if (session != null) {
                        session.getConferenceEventSubscriber().receiveNotification(notify,
                                timestamp);
                    }

                } catch (PayloadException | RuntimeException e) {
                    sLogger.error("Failed to receive group conference notification!", e);
                }
            }
        });
    }

    /**
     * Receive a message delivery status
     * 
     * @param message Received message
     */
    public void onMessageDeliveryStatusReceived(final SipRequest message) {
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    /*
                     * Begin by sending 200 OK, a failure before doing that may cause the sender to
                     * re-send the report and if reception fails again we are stuck in a loop.
                     */
                    getImsModule().getSipManager().sendSipResponse(
                            SipMessageFactory.createResponse(message, IdGenerator.getIdentifier(),
                                    Response.OK));

                    ImdnDocument imdn = ChatUtils.parseCpimDeliveryReport(message.getContent());
                    String assertedId = SipUtils.getAssertedIdentity(message);
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(assertedId);

                    if (number == null) {
                        sLogger.error("Invalid remote ID " + assertedId);
                        return;
                    }

                    ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                    String msgId = imdn.getMsgId();

                    String chatId = mMessagingLog.getMessageChatId(msgId);
                    if (chatId != null) {
                        if (chatId.equals(contact.toString())) {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Handle one to one message delivery status");
                            }
                            mChatService.onOneToOneMessageDeliveryStatusReceived(contact, imdn);
                            return;
                        }
                        mChatService.getOrCreateGroupChat(chatId).onMessageDeliveryStatusReceived(
                                contact, imdn);
                        return;
                    }
                    chatId = mMessagingLog.getFileTransferChatId(msgId);
                    if (chatId != null) {
                        if (chatId.equals(contact.toString())) {
                            receiveOneToOneFileDeliveryStatus(contact, imdn);
                            return;
                        }
                        receiveGroupFileDeliveryStatus(chatId, contact, imdn);
                        return;
                    }
                    sLogger.warn(new StringBuilder(
                            "SIP imdn delivery report received referencing a message that was ")
                            .append("not found in our database. Message id ").append(msgId)
                            .append(", ignoring.").toString());

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive chat message delivery report! ("
                                + e.getMessage() + ")");
                    }
                } catch (ParserConfigurationException | ParseFailureException | SAXException
                        | PayloadException e) {
                    sLogger.error("Failed to receive chat message delivery report!", e);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive chat message delivery report!", e);
                }
            }
        });
    }

    /**
     * Receive S&F push messages
     * 
     * @param invite Received invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onStoreAndForwardPushMessagesReceived(final SipRequest invite, final long timestamp) {
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
                    ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
                    if (remote == null) {
                        if (logActivated) {
                            sLogger.error("Discard S&F PushMessages: invalid remote ID '"
                                    + referredId + "'");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }
                    if (logActivated) {
                        sLogger.debug("Receive S&F push messages invitation");
                    }
                    ChatMessage firstMsg = ChatUtils.getFirstMessage(invite, timestamp);

                    if (mContactManager.isBlockedForContact(remote)) {
                        if (logActivated) {
                            sLogger.debug("Contact " + remote
                                    + " is blocked: automatically reject the S&F invitation");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }
                    /*
                     * Save the message if it was not already persisted in the DB. We don't have to
                     * reject the session if the message was a duplicate one as the session
                     * rejection/keeping will be handled in
                     * TerminatingOneToOneChatSession.startSession() in an uniform way as according
                     * to the defined race conditions in the specification document.
                     */
                    if (firstMsg != null
                            && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
                        boolean imdnDisplayRequested = mImdnManager
                                .isSendOneToOneDeliveryDisplayedReportsEnabled()
                                && ChatUtils.isImdnDisplayedRequested(invite);
                        mMessagingLog
                                .addIncomingOneToOneChatMessage(firstMsg, imdnDisplayRequested);
                    }
                    getStoreAndForwardManager().receiveStoreAndForwardMessageInvitation(invite,
                            remote, timestamp);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive s&f chat messages! (" + e.getMessage()
                                + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);
                } catch (PayloadException e) {
                    sLogger.error("Failed to receive s&f chat messages!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive s&f chat messages!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Receive S&F push notifications
     * 
     * @param invite Received invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onStoredAndForwardPushNotificationReceived(final SipRequest invite,
            final long timestamp) {
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
                    ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
                    if (remote == null) {
                        if (sLogger.isActivated()) {
                            sLogger.error("Discard S&F PushNotifications: invalid remote ID '"
                                    + referredId + "'");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }
                    if (sLogger.isActivated()) {
                        sLogger.debug("Receive S&F push notifications invitation");
                    }
                    if (mContactManager.isBlockedForContact(remote)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Contact " + remote
                                    + " is blocked: automatically reject the S&F invitation");
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        return;
                    }
                    getStoreAndForwardManager().receiveStoreAndForwardNotificationInvitation(
                            invite, remote, timestamp);

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive s&f push notifications! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);
                } catch (PayloadException e) {
                    sLogger.error("Failed to receive s&f push notifications!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive s&f push notifications!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });

    }

    /**
     * Receive HTTP file transfer invitation
     * 
     * @param invite Received invite
     * @param ftinfo File transfer info document
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onOneToOneHttpFileTranferInvitationReceived(final SipRequest invite,
            final FileTransferHttpInfoDocument ftinfo, final long timestamp) {
        final InstantMessagingService imService = this;
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
                    ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
                    String displayName = SipUtils.getDisplayNameFromUri(invite.getFrom());
                    if (remote == null) {
                        if (logActivated) {
                            sLogger.error("Discard OneToOne HttpFileTranferInvitation: invalid remote ID '"
                                    + referredId + "'");
                        }
                        sendErrorResponse(invite, Response.DECLINE);
                        return;

                    }
                    if (logActivated) {
                        sLogger.debug("Receive a single HTTP file transfer invitation from "
                                .concat(remote.toString()));
                    }
                    /*
                     * Update the remote contact's capabilities to include at least HTTP FT and IM
                     * session capabilities as we have just received a HTTP file transfer invitation
                     * from this contact so he/she must at least have this capability. We do not
                     * need any capability exchange response to determine that.
                     */
                    mContactManager.mergeContactCapabilities(remote, new CapabilitiesBuilder()
                            .setImSession(true).setFileTransferHttp(true)
                            .setTimestampOfLastResponse(timestamp).build(), RcsStatus.RCS_CAPABLE,
                            RegistrationState.ONLINE, displayName);

                    String fileTransferId = ChatUtils.getMessageId(invite);
                    if (isFileTransferAlreadyOngoing(fileTransferId)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder("File transfer with fileTransferId '")
                                    .append(fileTransferId)
                                    .append("' already ongoing, so ignoring this one.").toString());
                        }
                        return;
                    }
                    CpimMessage cpimMessage = ChatUtils.extractCpimMessage(invite);
                    long timestampSent = cpimMessage.getTimestampSent();
                    boolean fileResent = isFileTransferResentAndNotAlreadyOngoing(fileTransferId);
                    if (mContactManager.isBlockedForContact(remote)) {
                        if (logActivated) {
                            sLogger.debug(new StringBuilder("Contact ")
                                    .append(remote)
                                    .append(" is blocked, automatically reject the HTTP File transfer")
                                    .toString());
                        }
                        sendErrorResponse(invite, Response.DECLINE);
                        if (fileResent) {
                            setResendFileTransferInvitationRejected(fileTransferId,                                     FileTransfer.ReasonCode.REJECTED_SPAM, timestamp, timestampSent);
                            return;
                        }
                        MmContent fileContent = ftinfo.getLocalMmContent();
                        FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
                        MmContent fileIconContent = thumbnail == null ? null : thumbnail
                                .getLocalMmContent(fileTransferId);
                        addFileTransferInvitationRejected(remote, fileContent, fileIconContent,
                                FileTransfer.ReasonCode.REJECTED_SPAM, timestamp, timestampSent);
                        return;
                    }

                    if (!isChatSessionAvailable()) {
                        if (logActivated) {
                            sLogger.debug("The max number of chat sessions is achieved: reject the invitation from "
                                    .concat(remote.toString()));
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        /*
                         * The more abstracted reason code REJECTED_MAX_FILE_TRANSFERS is used since
                         * the client need not be aware of chat session dependency here.
                         */
                        if (fileResent) {
                            setResendFileTransferInvitationRejected(fileTransferId,
                                    FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                    timestampSent);
                            return;
                        }
                        MmContent fileContent = ftinfo.getLocalMmContent();
                        FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
                        MmContent fileIconContent = thumbnail == null ? null : thumbnail
                                .getLocalMmContent(fileTransferId);
                        addFileTransferInvitationRejected(remote, fileContent, fileIconContent,
                                FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                timestampSent);
                        return;
                    }

                    TerminatingOneToOneChatSession oneToOneChatSession = new TerminatingOneToOneChatSession(
                            imService, invite, remote, mRcsSettings, mMessagingLog, timestamp,
                            mContactManager);
                    receiveOneOneChatSessionInitiation(oneToOneChatSession);
                    oneToOneChatSession.startSession();

                    if (!isFileTransferSessionAvailable()) {
                        if (logActivated) {
                            sLogger.debug("The max number of FT sessions is achieved, reject the HTTP File transfer from "
                                    .concat(remote.toString()));
                        }
                        sendErrorResponse(invite, Response.DECLINE);
                        if (fileResent) {
                            setResendFileTransferInvitationRejected(fileTransferId,
                                    FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                    timestampSent);
                            return;
                        }
                        setResendFileTransferInvitationRejected(fileTransferId,
                                FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                timestampSent);
                        return;
                    }
                    /*
                     * Reject if file is too big or size exceeds device storage capacity. This
                     * control should be done on UI. It is done after end user accepts invitation to
                     * enable prior handling by the application.
                     */
                    FileSharingError error = isFileCapacityAcceptable(ftinfo.getSize(),
                            mRcsSettings);
                    if (error != null) {
                        sendErrorResponse(invite, Response.DECLINE);
                        int errorCode = error.getErrorCode();
                        MmContent fileContent = ftinfo.getLocalMmContent();
                        FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
                        MmContent fileIconContent = thumbnail == null ? null : thumbnail
                                .getLocalMmContent(fileTransferId);
                        switch (errorCode) {
                            case FileSharingError.MEDIA_SIZE_TOO_BIG:
                                if (fileResent) {
                                    setResendFileTransferInvitationRejected(fileTransferId,
                                            FileTransfer.ReasonCode.REJECTED_MAX_SIZE, timestamp,
                                            timestampSent);
                                    break;
                                }
                                addFileTransferInvitationRejected(remote, fileContent,
                                        fileIconContent, FileTransfer.ReasonCode.REJECTED_MAX_SIZE,
                                        timestamp, timestampSent);
                                break;
                            case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                                if (fileResent) {
                                    setResendFileTransferInvitationRejected(fileTransferId,
                                            FileTransfer.ReasonCode.REJECTED_LOW_SPACE, timestamp,
                                            timestampSent);
                                    break;
                                }
                                addFileTransferInvitationRejected(remote, fileContent,
                                        fileIconContent,
                                        FileTransfer.ReasonCode.REJECTED_LOW_SPACE, timestamp,
                                        timestampSent);
                                break;
                            default:
                                sLogger.error(new StringBuilder(
                                        "Unexpected error while receiving HTTP file transfer invitation from ")
                                        .append(remote).append(" error : ")
                                        .append(Integer.toString(errorCode)).toString());
                        }
                        return;
                    }

                    DownloadFromInviteFileSharingSession fileSharingSession = new DownloadFromInviteFileSharingSession(
                            imService, oneToOneChatSession, ftinfo, fileTransferId,
                            oneToOneChatSession.getRemoteContact(),
                            oneToOneChatSession.getRemoteDisplayName(), mRcsSettings,
                            mMessagingLog, timestamp, timestampSent, mContactManager);
                    if (fileSharingSession.getFileicon() != null) {
                        try {
                            fileSharingSession.downloadFileIcon();
                        } catch (NetworkException e) {
                            if (sLogger.isActivated()) {
                                sLogger.debug(new StringBuilder("Failed to download file icon! (")
                                        .append(e.getMessage()).append(")").toString());
                            }
                            sendErrorResponse(invite, Response.DECLINE);
                            if (fileResent) {
                                setResendFileTransferInvitationRejected(fileTransferId,
                                        FileTransfer.ReasonCode.REJECTED_MEDIA_FAILED, timestamp,
                                        timestampSent);
                                return;
                            }
                            MmContent fileContent = ftinfo.getLocalMmContent();
                            FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
                            MmContent fileIconContent = thumbnail == null ? null : thumbnail
                                    .getLocalMmContent(fileTransferId);
                            addFileTransferInvitationRejected(remote, fileContent, fileIconContent,
                                    FileTransfer.ReasonCode.REJECTED_MEDIA_FAILED, timestamp,
                                    timestampSent);
                            return;
                        } catch (FileAccessException e) {
                            sLogger.error("Failed to download file icon", e);
                            sendErrorResponse(invite, Response.DECLINE);
                            if (fileResent) {
                                setResendFileTransferInvitationRejected(fileTransferId,
                                        FileTransfer.ReasonCode.REJECTED_MEDIA_FAILED, timestamp,
                                        timestampSent);
                                return;
                            }
                            MmContent fileContent = ftinfo.getLocalMmContent();
                            FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
                            MmContent fileIconContent = thumbnail == null ? null : thumbnail
                                    .getLocalMmContent(fileTransferId);
                            addFileTransferInvitationRejected(remote, fileContent, fileIconContent,
                                    FileTransfer.ReasonCode.REJECTED_MEDIA_FAILED, timestamp,
                                    timestampSent);
                            return;
                        }
                    }
                    if (fileResent) {
                        receiveResendFileTransferInvitation(fileSharingSession, remote, displayName);
                    } else {
                        mFileTransferService.receiveFileTransferInvitation(fileSharingSession,
                                false, oneToOneChatSession.getRemoteContact(),
                                oneToOneChatSession.getRemoteDisplayName());
                    }
                    fileSharingSession.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive http o2o file transfer invitation! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);

                } catch (FileAccessException | PayloadException | ContactManagerException e) {
                    sLogger.error("Failed to receive http o2o file transfer invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);

                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive http o2o file transfer invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Receive S&F HTTP file transfer invitation
     * 
     * @param invite Received invite
     * @param ftinfo File transfer info document
     * @param timestamp Local timestamp when got SipRequest
     */
    public void onStoreAndForwardOneToOneHttpFileTranferInvitationReceived(final SipRequest invite,
            final FileTransferHttpInfoDocument ftinfo, final long timestamp) {
        final InstantMessagingService imService = this;
        scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean logActivated = sLogger.isActivated();
                    String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
                    ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
                    String displayName = SipUtils.getDisplayNameFromUri(invite.getFrom());
                    if (remote == null) {
                        sLogger.error("Discard S&F OneToOne HttpFileTranfer Invitation. Invalid remote ID "
                                .concat(referredId));
                        /* We cannot refuse a S&F File transfer invitation */
                        // TODO normally send a deliver to enable transmission of awaiting messages
                        return;
                    }
                    if (logActivated) {
                        sLogger.debug("Receive a single S&F HTTP file transfer invitation from "
                                .concat(remote.toString()));
                    }
                    CpimMessage cpimMessage = ChatUtils.extractCpimMessage(invite);
                    long timestampSent = cpimMessage.getTimestampSent();
                    String fileTransferId = ChatUtils.getMessageId(invite);
                    boolean fileResent = isFileTransferResentAndNotAlreadyOngoing(fileTransferId);
                    if (!isChatSessionAvailable()) {
                        if (logActivated) {
                            sLogger.debug("The max number of chat sessions is achieved: reject the invitation from "
                                    .concat(remote.toString()));
                        }
                        sendErrorResponse(invite, Response.BUSY_HERE);
                        /*
                         * The more abstracted reason code REJECTED_MAX_FILE_TRANSFERS is used since
                         * the client need not be aware of chat session dependency here.
                         */
                        if (fileResent) {
                            setResendFileTransferInvitationRejected(fileTransferId,
                                    FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                    timestampSent);
                            return;
                        }
                        MmContent fileContent = ftinfo.getLocalMmContent();
                        FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
                        MmContent fileIconContent = thumbnail == null ? null : thumbnail
                                .getLocalMmContent(fileTransferId);
                        addFileTransferInvitationRejected(remote, fileContent, fileIconContent,
                                FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp,
                                timestampSent);
                        return;
                    }

                    TerminatingStoreAndForwardOneToOneChatMessageSession oneToOneChatSession = new TerminatingStoreAndForwardOneToOneChatMessageSession(
                            imService, invite, remote, mRcsSettings, mMessagingLog, timestamp,
                            mContactManager);
                    receiveOneOneChatSessionInitiation(oneToOneChatSession);
                    oneToOneChatSession.startSession();

                    if (isFileTransferAlreadyOngoing(fileTransferId)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder("File transfer with fileTransferId '")
                                    .append(fileTransferId)
                                    .append("' already ongoing, so ignoring this one from ")
                                    .append(remote).toString());
                        }
                        return;
                    }

                    if (isFileSizeExceeded(ftinfo.getSize())) {
                        if (logActivated) {
                            sLogger.debug("File is too big, reject file transfer invitation from "
                                    .concat(remote.toString()));
                        }
                        // TODO add warning header "xxx Size exceeded"
                        oneToOneChatSession.sendErrorResponse(invite, oneToOneChatSession
                                .getDialogPath().getLocalTag(),
                                InvitationStatus.INVITATION_REJECTED_FORBIDDEN);
                        // Close session
                        oneToOneChatSession.handleError(new FileSharingError(
                                FileSharingError.MEDIA_SIZE_TOO_BIG));
                        return;
                    }

                    DownloadFromInviteFileSharingSession filetransferSession = new DownloadFromInviteFileSharingSession(
                            imService, oneToOneChatSession, ftinfo, fileTransferId,
                            oneToOneChatSession.getRemoteContact(),
                            oneToOneChatSession.getRemoteDisplayName(), mRcsSettings,
                            mMessagingLog, timestamp, timestampSent, mContactManager);
                    if (filetransferSession.getFileicon() != null) {
                        try {
                            filetransferSession.downloadFileIcon();
                        } catch (FileAccessException e) {
                            sLogger.error("Failed to download file icon", e);
                            oneToOneChatSession.sendErrorResponse(invite, oneToOneChatSession
                                    .getDialogPath().getLocalTag(),
                                    InvitationStatus.INVITATION_REJECTED_DECLINE);

                            /* Close session */
                            oneToOneChatSession.handleError(new FileSharingError(
                                    FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                            return;
                        }

                    }
                    if (fileResent) {
                        receiveResendFileTransferInvitation(filetransferSession, remote,
                                displayName);
                    } else {
                        mFileTransferService.receiveFileTransferInvitation(filetransferSession,
                                false, oneToOneChatSession.getRemoteContact(),
                                oneToOneChatSession.getRemoteDisplayName());
                    }
                    filetransferSession.startSession();

                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Failed to receive s&f o2o http file transfer invitation! ("
                                + e.getMessage() + ")");
                    }
                    tryToSendErrorResponse(invite, Response.BUSY_HERE);
                } catch (PayloadException e) {
                    sLogger.error("Failed to receive s&f o2o file http transfer invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Failed to receive s&f o2o file http transfer invitation!", e);
                    tryToSendErrorResponse(invite, Response.DECLINE);
                }
            }
        });
    }

    /**
     * Check whether file size exceeds the limit
     * 
     * @param size of file
     * @return {@code true} if file size limit is exceeded, otherwise {@code false}
     */
    public boolean isFileSizeExceeded(long size) {
        // Auto reject if file too big
        long maxSize = mRcsSettings.getMaxFileTransferSize();
        return maxSize > 0 && size > maxSize;
    }

    /**
     * Check if the capabilities are valid based on msgCapValidity paramter
     * 
     * @param capabilities The capabilities
     * @return {@code true} if valid, otherwise {@code false}
     */
    public boolean isCapabilitiesValid(Capabilities capabilities) {
        long msgCapValidityPeriod = mRcsSettings.getMsgCapValidityPeriod();
        return System.currentTimeMillis() <= capabilities.getTimestampOfLastResponse()
                + msgCapValidityPeriod;
    }

    /**
     * Removes the group chat composing status from the map
     * 
     * @param chatId The chat ID
     */
    public void removeGroupChatComposingStatus(final String chatId) {
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatComposingStatusToNotify.remove(chatId);
        }
    }

    /**
     * Adds the group chat composing status to the map to enable re-sending upon media session
     * restart
     * 
     * @param chatId the group chat identifier
     * @param status the composing status which failed to be notified
     */
    public void addGroupChatComposingStatus(String chatId, boolean status) {
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatComposingStatusToNotify.put(chatId, status);
        }
    }

    /**
     * Gets the group chat composing status
     * 
     * @param chatId the chat ID
     * @return the group chat composing status if previous sending failed or null if network is
     *         aligned with client composing status
     */
    public Boolean getGroupChatComposingStatus(String chatId) {
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatComposingStatusToNotify.get(chatId);
        }
    }

    /**
     * Removes the one-to-one chat composing status from the map
     * 
     * @param contact the remote contact
     */
    public void removeOneToOneChatComposingStatus(final ContactId contact) {
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatComposingStatusToNotify.remove(contact);
        }
    }

    /**
     * Adds the one-to-one chat composing status to the map to enable re-sending upon media session
     * restart
     * 
     * @param contact the remote contact
     * @param status the composing status which failed to be notified
     */
    public void addOneToOneChatComposingStatus(ContactId contact, boolean status) {
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatComposingStatusToNotify.put(contact, status);
        }
    }

    /**
     * Gets the one-to-one chat composing status
     * 
     * @param contact the remote contact
     * @return the one-to-one chat composing status if previous sending failed or null if network is
     *         aligned with client composing status
     */
    public Boolean getOneToOneChatComposingStatus(ContactId contact) {
        synchronized (getImsServiceSessionOperationLock()) {
            return mOneToOneChatComposingStatusToNotify.get(contact);
        }
    }

    /**
     * Accept store and forward message session with this remoteContact that is not yet accepted.
     * 
     * @param remoteContact The remote contact
     */
    public void acceptStoreAndForwardMessageSessionIfSuchExists(ContactId remoteContact) {
        TerminatingStoreAndForwardOneToOneChatMessageSession session = getStoreAndForwardMsgSession(remoteContact);
        if (session == null) {
            return;
        }
        if (session.getDialogPath().isSessionEstablished()) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Accept store and forward message session with contact "
                    .concat(remoteContact.toString()));
        }
        session.acceptSession();
    }

    /**
     * Is file transfer already ongoing with specific fileTransferId.
     * 
     * @param fileTransferId the file transfer ID
     * @return boolean
     */
    public boolean isFileTransferAlreadyOngoing(String fileTransferId) {
        return getFileSharingSession(fileTransferId) != null;
    }

    /**
     * Is file transfer resent and not already ongoing.
     * 
     * @param fileTransferId the file transfer ID
     * @return boolean
     */
    public boolean isFileTransferResentAndNotAlreadyOngoing(String fileTransferId) {
        return !isFileTransferAlreadyOngoing(fileTransferId)
                && mMessagingLog.isFileTransfer(fileTransferId);
    }

    /**
     * Try to dequeue of one-to-one chat messages for specific contact
     * 
     * @param contact the contact ID
     */
    public void tryToDequeueOneToOneChatMessages(ContactId contact) {
        mImOperationHandler.post(new OneToOneChatMessageDequeueTask(mCtx, mCore, contact,
                mMessagingLog, mChatService, mRcsSettings, mContactManager, mFileTransferService));
    }

    /**
     * Try to dequeue group chat messages and group file transfers
     * 
     * @param chatId the chat ID
     */
    public void tryToDequeueGroupChatMessagesAndGroupFileTransfers(String chatId) {
        mImOperationHandler.post(new GroupChatDequeueTask(mCtx, mCore, mMessagingLog, mChatService,
                mFileTransferService, mRcsSettings, mContactManager, mHistoryLog, chatId));
    }

    /**
     * Try to dequeue all one-to-one chat messages and one-one file transfers
     */
    public void tryToDequeueAllOneToOneChatMessagesAndOneToOneFileTransfers() {
        mImOperationHandler.post(new OneToOneChatDequeueTask(mCtx, mCore, mMessagingLog,
                mRcsSettings, mChatService, mFileTransferService, mContactManager, mHistoryLog));
    }

    /**
     * Try to dequeue one-to-one and group file transfers
     */
    public void tryToDequeueFileTransfers() {
        mImOperationHandler.post(new FileTransferDequeueTask(mCtx, mCore, mMessagingLog,
                mChatService, mFileTransferService, mContactManager, mRcsSettings));
    }

    /**
     * Try to mark all queued group chat messages and group file transfers corresponding to contact
     * as failed
     * 
     * @param chatId the chat ID
     */
    public void tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(String chatId) {
        mImOperationHandler.post(new GroupChatTerminalExceptionTask(chatId, mChatService,
                mFileTransferService, mHistoryLog));
    }

    /**
     * Try to send delayed displayed notification after service reconnection
     */
    public void tryToDispatchAllPendingDisplayNotifications() {
        mImOperationHandler.post(new DelayedDisplayNotificationDispatcher(mLocalContentResolver,
                mChatService));
    }

    /**
     * Try to invite queued group chat participants
     * 
     * @param chatId the chat ID
     */
    public void tryToInviteQueuedGroupChatParticipantInvitations(String chatId) {
        mImOperationHandler.post(new GroupChatInviteQueuedParticipantsTask(chatId, mChatService,
                this));
    }

    /**
     * Handle auto rejoin group chat
     * 
     * @param chatId the chat ID
     * @throws NetworkException
     * @throws PayloadException
     */
    public void rejoinGroupChat(String chatId) throws PayloadException, NetworkException {
        mChatService.rejoinGroupChat(chatId);
    }

    /**
     * Handle rejoin group chat as part of send operation
     * 
     * @param chatId the chat ID
     * @throws NetworkException
     * @throws PayloadException
     */
    public void rejoinGroupChatAsPartOfSendOperation(String chatId) throws PayloadException,
            NetworkException {
        mChatService.rejoinGroupChatAsPartOfSendOperation(chatId);
    }

    /**
     * Handle one-one file transfer failure
     * 
     * @param fileTransferId the file transfer ID
     * @param contact the contact
     * @param reasonCode the reason code
     */
    public void setOneToOneFileTransferFailureReasonCode(String fileTransferId, ContactId contact,
            FileTransfer.ReasonCode reasonCode) {
        mFileTransferService.setOneToOneFileTransferStateAndReasonCode(fileTransferId, contact,
                FileTransfer.State.FAILED, reasonCode);
    }

    /**
     * Deletes all one to one chat from history and abort/reject any associated ongoing session if
     * such exists.
     */
    public void tryToDeleteOneToOneChats() {
        mImDeleteOperationHandler.post(new OneToOneFileTransferDeleteTask(mFileTransferService,
                this, mLocalContentResolver));
        mImDeleteOperationHandler.post(new OneToOneChatMessageDeleteTask(mChatService, this,
                mLocalContentResolver));
    }

    /**
     * Deletes all group chat from history and abort/reject any associated ongoing session if such
     * exists.
     */
    public void tryToDeleteGroupChats() {
        mImDeleteOperationHandler.post(new GroupFileTransferDeleteTask(mFileTransferService, this,
                mLocalContentResolver));
        mImDeleteOperationHandler.post(new GroupChatMessageDeleteTask(mChatService, this,
                mLocalContentResolver));
        mImDeleteOperationHandler.post(new GroupChatDeleteTask(mChatService, this,
                mLocalContentResolver));
    }

    /**
     * Deletes a one to one chat with a given contact from history and abort/reject any associated
     * ongoing session if such exists.
     * 
     * @param contact the contact ID
     */
    public void tryToDeleteOneToOneChat(ContactId contact) {
        mImDeleteOperationHandler.post(new OneToOneFileTransferDeleteTask(mFileTransferService,
                this, mLocalContentResolver, contact));
        mImDeleteOperationHandler.post(new OneToOneChatMessageDeleteTask(mChatService, this,
                mLocalContentResolver, contact));
    }

    /**
     * Delete a group chat by its chat id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param chatId the chat ID
     */
    public void tryToDeleteGroupChat(String chatId) {
        mImDeleteOperationHandler.post(new GroupChatMessageDeleteTask(mChatService, this,
                mLocalContentResolver, chatId));
        mImDeleteOperationHandler.post(new GroupFileTransferDeleteTask(mFileTransferService, this,
                mLocalContentResolver, chatId));
        mImDeleteOperationHandler.post(new GroupChatDeleteTask(mChatService, this,
                mLocalContentResolver, chatId));
    }

    /**
     * Delete a message from its message id from history. Will resolve if the message is one to one
     * or from a group chat.
     * 
     * @param msgId the message ID
     */
    public void tryToDeleteChatMessage(String msgId) {
        if (mMessagingLog.isOneToOneChatMessage(msgId)) {
            mImDeleteOperationHandler.post(new OneToOneChatMessageDeleteTask(mChatService, this,
                    mLocalContentResolver, msgId));
        } else {
            mImDeleteOperationHandler.post(new GroupChatMessageDeleteTask(mChatService, this,
                    mLocalContentResolver, null, msgId));
        }
    }

    /**
     * Delete a file transfer from its transfer id from history. Will resolve if the message is one
     * to one or from a group chat.
     * 
     * @param transferId the file transfer ID
     */
    public void tryToDeleteFileTransfer(String transferId) {
        if (mMessagingLog.isGroupFileTransfer(transferId)) {
            mImDeleteOperationHandler.post(new GroupFileTransferDeleteTask(mFileTransferService,
                    this, mLocalContentResolver, mMessagingLog.getFileTransferChatId(transferId),
                    transferId));
        } else {
            mImDeleteOperationHandler.post(new OneToOneFileTransferDeleteTask(mFileTransferService,
                    this, mLocalContentResolver, transferId));
        }
    }

    /**
     * Try to delete file transfer corresponding to a given one to one chat specified by contact
     * from history and abort/reject any associated ongoing session if such exists.
     * 
     * @param contact the contact ID
     */
    public void tryToDeleteFileTransfers(ContactId contact) {
        mImDeleteOperationHandler.post(new OneToOneFileTransferDeleteTask(mFileTransferService,
                this, mLocalContentResolver, contact));
    }

    /**
     * Try to delete all one to one file transfer from history and abort/reject any associated
     * ongoing session if such exists.
     */
    public void tryToDeleteOneToOneFileTransfers() {
        mImDeleteOperationHandler.post(new OneToOneFileTransferDeleteTask(mFileTransferService,
                this, mLocalContentResolver));
    }

    /**
     * Try to delete all group file transfer from history and abort/reject any associated ongoing
     * session if such exists.
     */
    public void tryToDeleteGroupFileTransfers() {
        mImDeleteOperationHandler.post(new GroupFileTransferDeleteTask(mFileTransferService, this,
                mLocalContentResolver));
    }

    /**
     * A new file transfer invitation has been received when already in a chat session
     * 
     * @param fileSharingSession File transfer session
     * @param isGroup is Group file transfer
     * @param contact Contact ID
     * @param displayName the display name of the remote contact
     * @param fileExpiration file transfer validity in milliseconds (or 0 if not applicable)
     */
    public void receiveFileTransferInvitation(FileSharingSession fileSharingSession,
            boolean isGroup, ContactId contact, String displayName, long fileExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event file transfer invitation");
        }
        mFileTransferService.receiveFileTransferInvitation(fileSharingSession, isGroup, contact,
                displayName);
    }

    /**
     * Handle resend file transfer invitation
     * 
     * @param session the session
     * @param contact the contact ID
     * @param displayName the display name of the remote contact
     */
    public void receiveResendFileTransferInvitation(FileSharingSession session, ContactId contact,
            String displayName) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event file transfer resend invitation");
        }
        mFileTransferService.receiveResendFileTransferInvitation(session, contact, displayName);
    }

    /**
     * An incoming file transfer has been resumed
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
    public void resumeIncomingFileTransfer(FileSharingSession session, boolean isGroup) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event incoming file transfer resuming");
        }
        mFileTransferService.resumeIncomingFileTransfer(session, isGroup);
    }

    /**
     * An outgoing file transfer has been resumed
     * 
     * @param session File transfer session
     * @param isGroup is group file transfer
     */
    public void resumeOutgoingFileTransfer(FileSharingSession session, boolean isGroup) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event outgoing file transfer resuming");
        }
        mFileTransferService.resumeOutgoingFileTransfer(session, isGroup);
    }

    /**
     * Store and Forward messages session invitation
     * 
     * @param session Chat session
     */
    public void receiveStoreAndForwardMsgSessionInvitation(
            TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event S&F messages session invitation");
        }
        mChatService.receiveOneToOneChatInvitation(session);
    }

    /**
     * Handle store and forward notification session invitation
     * 
     * @param session the session
     */
    public void receiveStoreAndForwardNotificationSessionInvitation(
            TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event S&F notification session invitation");
        }
        mChatService.receiveOneToOneChatInvitation(session);
    }

    /**
     * New file delivery status
     * 
     * @param contact who notified status
     * @param imdn IMDN document
     */
    public void receiveOneToOneFileDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle file delivery status: fileTransferId=" + imdn.getMsgId()
                    + " notification_type=" + imdn.getNotificationType() + " status="
                    + imdn.getStatus() + " contact=" + contact);
        }
        mFileTransferService.receiveOneToOneFileDeliveryStatus(imdn, contact);
    }

    /**
     * New group file delivery status
     * 
     * @param chatId Chat Id
     * @param contact who notified status
     * @param imdn IMDN document
     */
    public void receiveGroupFileDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle group file delivery status: fileTransferId=" + imdn.getMsgId()
                    + " notification_type=" + imdn.getNotificationType() + " status="
                    + imdn.getStatus() + " contact=" + contact);
        }
        mFileTransferService.receiveGroupFileDeliveryStatus(chatId, imdn, contact);
    }

    /**
     * Handle imdn DISPLAY report sent for message
     * 
     * @param chatId the chat ID
     * @param remote the remote contact
     * @param msgId the message ID
     */
    public void onChatMessageDisplayReportSent(String chatId, ContactId remote, String msgId) {
        mChatService.onDisplayReportSent(chatId, remote, msgId);
    }

    /**
     * Handle one-to-one chat session initiation
     * 
     * @param session Chat session
     */
    public void receiveOneOneChatSessionInitiation(OneToOneChatSession session) {
        mChatService.receiveOneToOneChatSessionInitiation(session);
    }

    /**
     * Handle the case of rejected resend file transfer
     * 
     * @param fileTransferId the file transfer ID
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got file transfer invitation
     * @param timestampSent Remote timestamp sent in payload for the file transfer
     */
    public void setResendFileTransferInvitationRejected(String fileTransferId,
            FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        mFileTransferService.setResendFileTransferInvitationRejected(fileTransferId,
                reasonCode, timestamp, timestampSent);
    }

    /**
     * Handle the case of rejected file transfer
     * 
     * @param remoteContact Remote contact
     * @param content File content
     * @param fileIcon File icon content
     * @param reasonCode Rejected reason code
     * @param timestamp Local timestamp when got file transfer invitation
     * @param timestampSent Remote timestamp sent in payload for the file transfer
     */
    public void addFileTransferInvitationRejected(ContactId remoteContact, MmContent content,
            MmContent fileIcon, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        mFileTransferService.addFileTransferInvitationRejected(remoteContact, content, fileIcon,
                reasonCode, timestamp, timestampSent);
    }

}
