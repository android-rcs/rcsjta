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

package com.gsma.rcs.service;

import com.gsma.rcs.addressbook.AccountChangedReceiver;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreListener;
import com.gsma.rcs.core.TerminalInfo;
import com.gsma.rcs.core.content.AudioContent;
import com.gsma.rcs.core.content.GeolocContent;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.core.ims.ImsError;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatAutoRejoinTask;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FtHttpResumeManager;
import com.gsma.rcs.core.ims.service.ipcall.IPCallService;
import com.gsma.rcs.core.ims.service.ipcall.IPCallSession;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.geoloc.GeolocTransferSession;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.sip.messaging.GenericSipMsrpSession;
import com.gsma.rcs.core.ims.service.sip.streaming.GenericSipRtpSession;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.history.GroupChatDequeueTask;
import com.gsma.rcs.provider.history.GroupChatTerminalExceptionTask;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.history.OneToOneChatDequeueTask;
import com.gsma.rcs.provider.ipcall.IPCallHistory;
import com.gsma.rcs.provider.messaging.DelayedDisplayNotificationDispatcher;
import com.gsma.rcs.provider.messaging.FileTransferDequeueTask;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.messaging.OneToOneChatMessageDequeueTask;
import com.gsma.rcs.provider.messaging.RecreateDeliveryExpirationAlarms;
import com.gsma.rcs.provider.messaging.UpdateFileTransferStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.provider.sharing.UpdateGeolocSharingStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.sharing.UpdateImageSharingStateAfterUngracefulTerminationTask;
import com.gsma.rcs.provider.sharing.UpdateVideoSharingStateAfterUngracefulTerminationTask;
import com.gsma.rcs.service.api.CapabilityServiceImpl;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.ContactServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.FileUploadServiceImpl;
import com.gsma.rcs.service.api.GeolocSharingServiceImpl;
import com.gsma.rcs.service.api.HistoryServiceImpl;
import com.gsma.rcs.service.api.IPCallServiceImpl;
import com.gsma.rcs.service.api.ImageSharingServiceImpl;
import com.gsma.rcs.service.api.MultimediaSessionServiceImpl;
import com.gsma.rcs.service.api.OneToOneUndeliveredImManager;
import com.gsma.rcs.service.api.ServerApiException;
import com.gsma.rcs.service.api.VideoSharingServiceImpl;
import com.gsma.rcs.service.ipcalldraft.IIPCallService;
import com.gsma.rcs.service.ipcalldraft.IPCall;
import com.gsma.rcs.utils.AppUtils;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.capability.ICapabilityService;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.IContactService;
import com.gsma.services.rcs.extension.IMultimediaSessionService;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.IFileTransferService;
import com.gsma.services.rcs.history.IHistoryService;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharingService;
import com.gsma.services.rcs.sharing.image.IImageSharingService;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.video.IVideoSharingService;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.upload.IFileUploadService;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RCS core service. This service offers a flat API to any other process (activities) to access to
 * RCS features. This service is started automatically at device boot.
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsCoreService extends Service implements CoreListener {

    private final Object mOperationLock = new Object();

    private final ExecutorService mImOperationExecutor = Executors.newSingleThreadExecutor();

    private final ExecutorService mRcOperationExecutor = Executors.newSingleThreadExecutor();

    private OneToOneUndeliveredImManager mOneToOneUndeliveredImManager;

    /**
     * CPU manager
     */
    private CpuManager mCpuManager;

    /**
     * Account changed broadcast receiver
     */
    private AccountChangedReceiver mAccountChangedReceiver;

    // --------------------- RCSJTA API -------------------------

    /**
     * Contacts API
     */
    private ContactServiceImpl mContactApi;

    /**
     * Capability API
     */
    private CapabilityServiceImpl mCapabilityApi;

    /**
     * Chat API
     */
    private ChatServiceImpl mChatApi;

    /**
     * File transfer API
     */
    private FileTransferServiceImpl mFtApi;

    /**
     * Video sharing API
     */
    private VideoSharingServiceImpl mVshApi;

    /**
     * Image sharing API
     */
    private ImageSharingServiceImpl mIshApi;

    /**
     * Geoloc sharing API
     */
    private GeolocSharingServiceImpl mGshApi;

    /**
     * History Service API
     */
    private HistoryServiceImpl mHistoryApi;

    /**
     * IP call API
     */
    private IPCallServiceImpl mIpcallApi;

    /**
     * Multimedia session API
     */
    private MultimediaSessionServiceImpl mSessionApi;

    /**
     * File upload API
     */
    private FileUploadServiceImpl mUploadApi;

    /**
     * Need to start the core after stop if a StartService is called before the end of stopCore
     */
    private boolean mRestartCoreRequested = false;

    /**
     * Handler to restart core
     */
    private Handler mHandler;

    private Context mContext;

    private RcsSettings mRcsSettings;

    private ContentResolver mContentResolver;

    private LocalContentResolver mLocalContentResolver;

    private MessagingLog mMessagingLog;

    private RichCallHistory mRichCallHistory;

    private HistoryLog mHistoryLog;

    private ContactManager mContactManager;

    /**
     * The logger
     */
    private final static Logger sLogger = Logger.getLogger(RcsCoreService.class.getSimpleName());

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
        mContentResolver = mContext.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(mContext, mLocalContentResolver, mRcsSettings);
        RichCallHistory.createInstance(mLocalContentResolver);
        mRichCallHistory = RichCallHistory.getInstance();
        mContactManager = ContactManager.createInstance(mContext, mContentResolver,
                mLocalContentResolver, mRcsSettings);

        // Set application context
        AndroidFactory.setApplicationContext(mContext, mRcsSettings);

        // Set the terminal version
        TerminalInfo.setProductVersion(AppUtils.getApplicationVersion(this));

        mHandler = new Handler();

        // Start the core
        startCore();
    }

    @Override
    public void onDestroy() {
        // Unregister account changed broadcast receiver
        if (mAccountChangedReceiver != null) {
            try {
                unregisterReceiver(mAccountChangedReceiver);
            } catch (IllegalArgumentException e) {
                // Nothing to do
            }
        }

        // Stop the core
        new Thread() {
            /**
             * Processing
             */
            public void run() {
                stopCore();
            }
        }.start();
    }

    /**
     * Start core
     */
    private synchronized void startCore() {
        Core core = Core.getInstance();
        boolean logActivated = sLogger.isActivated();
        if (core != null) {
            if (core.isStopping()) {
                if (logActivated) {
                    sLogger.debug("The core is stopping, we will restart it when core is stopped");
                }
                core.setListener(this);
                mRestartCoreRequested = true;
            }
            if (core.isStarted()) {
                // Already started
                return;
            }
        }

        mRestartCoreRequested = false;
        try {
            /* Instantiate the contactUtils instance */
            com.gsma.services.rcs.contact.ContactUtil contactUtil = com.gsma.services.rcs.contact.ContactUtil
                    .getInstance(this);
            /* Read country code to check that we have permission to start the core stack */
            String myCC = contactUtil.getMyCountryCode();
            if (logActivated) {
                sLogger.debug("Start RCS core service (country code=" + myCC + ")");
            }

            IPCallHistory.createInstance(mLocalContentResolver);

            HistoryLog.createInstance(mLocalContentResolver);
            mHistoryLog = HistoryLog.getInstance();

            // Create the core
            Core.createCore(this, mRcsSettings, mContactManager, mMessagingLog);

            // Instantiate API
            mContactApi = new ContactServiceImpl(mContactManager, mRcsSettings);
            mCapabilityApi = new CapabilityServiceImpl(mContactManager, mRcsSettings);
            mOneToOneUndeliveredImManager = new OneToOneUndeliveredImManager(mContext,
                    mMessagingLog);
            core = Core.getInstance();
            InstantMessagingService imService = core.getImService();
            RichcallService richCallService = core.getRichcallService();
            IPCallService ipCallService = core.getIPCallService();
            SipService sipService = core.getSipService();

            mFtApi = new FileTransferServiceImpl(imService, mMessagingLog, mRcsSettings,
                    mContactManager, core, mLocalContentResolver, mImOperationExecutor,
                    mOperationLock, mOneToOneUndeliveredImManager);
            mChatApi = new ChatServiceImpl(imService, mMessagingLog, mRcsSettings, mContactManager,
                    core, mLocalContentResolver, mImOperationExecutor, mOperationLock, mFtApi,
                    mOneToOneUndeliveredImManager);
            mVshApi = new VideoSharingServiceImpl(richCallService, mRichCallHistory, mRcsSettings,
                    mContactManager, core, mLocalContentResolver, mRcOperationExecutor,
                    mOperationLock);
            mIshApi = new ImageSharingServiceImpl(richCallService, mRichCallHistory, mRcsSettings,
                    mContactManager, mLocalContentResolver, mRcOperationExecutor, mOperationLock);
            mGshApi = new GeolocSharingServiceImpl(richCallService, mContactManager,
                    mRichCallHistory, mRcsSettings, mLocalContentResolver, mRcOperationExecutor,
                    mOperationLock);
            mHistoryApi = new HistoryServiceImpl(getApplicationContext());
            mIpcallApi = new IPCallServiceImpl(ipCallService, IPCallHistory.getInstance(),
                    mContactManager, mRcsSettings);
            mSessionApi = new MultimediaSessionServiceImpl(sipService, mRcsSettings,
                    mContactManager);
            mUploadApi = new FileUploadServiceImpl(imService, mRcsSettings);

            // Set the logger properties
            Logger.activationFlag = mRcsSettings.isTraceActivated();
            Logger.traceLevel = mRcsSettings.getTraceLevel();

            // Terminal version
            if (logActivated) {
                sLogger.info("RCS stack release is ".concat(TerminalInfo.getProductVersion()));
            }

            // Start the core
            Core.getInstance().startCore();

            // Create multimedia directory on sdcard
            FileFactory.createDirectory(mRcsSettings.getPhotoRootDirectory());
            FileFactory.createDirectory(mRcsSettings.getVideoRootDirectory());
            FileFactory.createDirectory(mRcsSettings.getFileRootDirectory());

            // Init CPU manager
            mCpuManager = new CpuManager(mRcsSettings);
            mCpuManager.init();

            // Register account changed event receiver
            if (mAccountChangedReceiver == null) {
                mAccountChangedReceiver = new AccountChangedReceiver();

                // Register account changed broadcast receiver after a timeout of 2s (This is not
                // done immediately, as we do not want to catch
                // the removal of the account (creating and removing accounts is done
                // asynchronously). We can reasonably assume that no
                // RCS account deletion will be done by user during this amount of time, as he just
                // started his service.
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        registerReceiver(mAccountChangedReceiver, new IntentFilter(
                                "android.accounts.LOGIN_ACCOUNTS_CHANGED"));
                    }
                }, 2000);
            }

            if (logActivated) {
                sLogger.info("RCS core service started with success");
            }
        } catch (Exception e) {
            // TODO CR037 exception handling
            // Unexpected error
            if (logActivated) {
                sLogger.error("Can't instanciate the RCS core service", e);
            }

            // Exit service
            stopSelf();
        }
    }

    /**
     * Stop core
     */
    private synchronized void stopCore() {
        if (Core.getInstance() == null) {
            // Already stopped
            return;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Stop RCS core service");
        }

        // Close APIs
        if (mContactApi != null) {
            mContactApi.close();
            mContactApi = null;
        }
        if (mCapabilityApi != null) {
            mCapabilityApi.close();
            mCapabilityApi = null;
        }
        if (mFtApi != null) {
            mFtApi.close();
            mFtApi = null;
        }
        if (mChatApi != null) {
            mChatApi.close();
            mChatApi = null;
        }
        if (mIshApi != null) {
            mIshApi.close();
            mIshApi = null;
        }
        if (mGshApi != null) {
            mGshApi.close();
            mGshApi = null;
        }
        if (mIpcallApi != null) {
            mIpcallApi.close();
            mIpcallApi = null;
        }
        if (mVshApi != null) {
            mVshApi.close();
            mVshApi = null;
        }
        if (mHistoryApi != null) {
            mHistoryApi.close();
            mHistoryApi = null;
        }
        if (mOneToOneUndeliveredImManager != null) {
            mOneToOneUndeliveredImManager.cleanup();
        }

        // Terminate the core in background
        Core.terminateCore();

        // Close CPU manager
        if (mCpuManager != null) {
            mCpuManager.close();
            mCpuManager = null;
        }

        if (sLogger.isActivated()) {
            sLogger.info("RCS core service stopped with success");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (IContactService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Contact service API binding");
            }
            return mContactApi;
        } else if (ICapabilityService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Capability service API binding");
            }
            return mCapabilityApi;
        } else if (IFileTransferService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("File transfer service API binding");
            }
            return mFtApi;
        } else if (IChatService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Chat service API binding");
            }
            return mChatApi;
        } else if (IVideoSharingService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Video sharing service API binding");
            }
            return mVshApi;
        } else if (IImageSharingService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Image sharing service API binding");
            }
            return mIshApi;
        } else if (IGeolocSharingService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Geoloc sharing service API binding");
            }
            return mGshApi;
        } else if (IHistoryService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("History service API binding");
            }
            return mHistoryApi;
        } else if (IIPCallService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("IP call service API binding");
            }
            return mIpcallApi;
        } else if (IMultimediaSessionService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Multimedia session API binding");
            }
            return mSessionApi;
        } else if (IFileUploadService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("File upload service API binding");
            }
            return mUploadApi;
        } else {
            return null;
        }
    }

    /*---------------------------- CORE EVENTS ---------------------------*/

    /**
     * Notify registration to API
     */
    private void notifyRegistrationToApi() {
        if (mCapabilityApi != null) {
            mCapabilityApi.notifyRegistration();
        }
        if (mChatApi != null) {
            mChatApi.notifyRegistration();
        }
        if (mFtApi != null) {
            mFtApi.notifyRegistration();
        }
        if (mVshApi != null) {
            mVshApi.notifyRegistration();
        }
        if (mIshApi != null) {
            mIshApi.notifyRegistration();
        }
        if (mGshApi != null) {
            mGshApi.notifyRegistration();
        }
        if (mIpcallApi != null) {
            mIpcallApi.notifyRegistration();
        }
        if (mSessionApi != null) {
            mSessionApi.notifyRegistration();
        }
    }

    /**
     * Notify unregistration to API
     * 
     * @param reasonCode
     */
    private void notifyUnRegistrationToApi(RcsServiceRegistration.ReasonCode reason) {
        if (mCapabilityApi != null) {
            mCapabilityApi.notifyUnRegistration(reason);
        }
        if (mChatApi != null) {
            mChatApi.notifyUnRegistration(reason);
        }
        if (mFtApi != null) {
            mFtApi.notifyUnRegistration(reason);
        }
        if (mVshApi != null) {
            mVshApi.notifyUnRegistration(reason);
        }
        if (mIshApi != null) {
            mIshApi.notifyUnRegistration(reason);
        }
        if (mGshApi != null) {
            mGshApi.notifyUnRegistration(reason);
        }
        if (mIpcallApi != null) {
            mIpcallApi.notifyUnRegistration(reason);
        }
        if (mSessionApi != null) {
            mSessionApi.notifyUnRegistration(reason);
        }
    }

    @Override
    public void handleCoreLayerStarted() {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event core started");
        }
        /* Update interrupted file transfer status */
        mImOperationExecutor.execute(new UpdateFileTransferStateAfterUngracefulTerminationTask(
                mMessagingLog, mFtApi));
        mRcOperationExecutor.execute(new UpdateGeolocSharingStateAfterUngracefulTerminationTask(
                mRichCallHistory, mGshApi));
        mRcOperationExecutor.execute(new UpdateImageSharingStateAfterUngracefulTerminationTask(
                mRichCallHistory, mIshApi));
        mRcOperationExecutor.execute(new UpdateVideoSharingStateAfterUngracefulTerminationTask(
                mRichCallHistory, mVshApi));
        /*
         * Recreate delivery expiration alarm for one-one chat messages and one-one file transfers
         * after boot
         */
        mImOperationExecutor.execute(new RecreateDeliveryExpirationAlarms(mMessagingLog,
                mOneToOneUndeliveredImManager, mOperationLock));
        // Send service up intent
        Intent serviceUp = new Intent(RcsService.ACTION_SERVICE_UP);
        IntentUtils.tryToSetReceiverForegroundFlag(serviceUp);
        mContext.sendBroadcast(serviceUp);
    }

    @Override
    public void handleCoreLayerStopped() {
        boolean logActivated = sLogger.isActivated();
        // Display a notification
        if (logActivated) {
            sLogger.debug("Handle event core terminated");
        }
        if (!mRestartCoreRequested) {
            return;
        }
        if (logActivated) {
            sLogger.debug("Start the core after previous instance is stopped");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                startCore();
            }
        });
    }

    @Override
    public void handleRegistrationSuccessful() {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event registration ok");
        }

        // Notify APIs
        notifyRegistrationToApi();
    }

    @Override
    public void handleRegistrationFailed(ImsError error) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event registration failed");
        }

        // Notify APIs
        notifyUnRegistrationToApi(RcsServiceRegistration.ReasonCode.CONNECTION_LOST);
    }

    @Override
    public void handleRegistrationTerminated(RcsServiceRegistration.ReasonCode reason) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event registration terminated: ".concat(reason.name()));
        }

        // Notify APIs
        notifyUnRegistrationToApi(reason);
    }

    @Override
    public void handlePresenceSharingNotification(ContactId contact, String status, String reason) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event presence sharing notification for " + contact + " ("
                    + status + ":" + reason + ")");
        }
        // Not used
    }

    @Override
    public void handlePresenceInfoNotification(ContactId contact, PidfDocument presence) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event presence info notification for " + contact);
        }
        // Not used
    }

    @Override
    public void handleCapabilitiesNotification(ContactId contact, Capabilities capabilities) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle capabilities update notification for " + contact + " ("
                    + capabilities.toString() + ")");
        }

        // Notify API
        mCapabilityApi.receiveCapabilities(contact, capabilities);
    }

    @Override
    public void handlePresenceSharingInvitation(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event presence sharing invitation");
        }
        // Not used
    }

    @Override
    public void handleContentSharingTransferInvitation(ImageTransferSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event content sharing transfer invitation");
        }

        // Broadcast the invitation
        mIshApi.receiveImageSharingInvitation(session);
    }

    @Override
    public void handleContentSharingTransferInvitation(GeolocTransferSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event content sharing transfer invitation");
        }

        // Broadcast the invitation
        mGshApi.receiveGeolocSharingInvitation(session);
    }

    @Override
    public void handleContentSharingStreamingInvitation(VideoStreamingSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event content sharing streaming invitation");
        }

        // Broadcast the invitation
        mVshApi.receiveVideoSharingInvitation(session);
    }

    @Override
    public void handleFileTransferInvitation(FileSharingSession fileSharingSession,
            boolean isGroup, ContactId contact, String displayName, long fileExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event file transfer invitation");
        }

        // Broadcast the invitation
        mFtApi.receiveFileTransferInvitation(fileSharingSession, isGroup, contact, displayName);
    }

    @Override
    public void handleOneToOneFileTransferInvitation(FileSharingSession fileSharingSession,
            OneToOneChatSession oneToOneChatSession, long fileExpiration) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event file transfer invitation");
        }

        // Broadcast the invitation
        mFtApi.receiveFileTransferInvitation(fileSharingSession, false,
                oneToOneChatSession.getRemoteContact(), oneToOneChatSession.getRemoteDisplayName());
    }

    @Override
    public void handleIncomingFileTransferResuming(FileSharingSession session, boolean isGroup,
            String chatSessionId, String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event incoming file transfer resuming");
        }

        // Broadcast the invitation
        mFtApi.resumeIncomingFileTransfer(session, isGroup, chatSessionId, chatId);
    }

    @Override
    public void handleOutgoingFileTransferResuming(FileSharingSession session, boolean isGroup) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event outgoing file transfer resuming");
        }

        // Broadcast the invitation
        mFtApi.resumeOutgoingFileTransfer(session, isGroup);
    }

    @Override
    public void handleOneOneChatSessionInvitation(TerminatingOneToOneChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event receive 1-1 chat session invitation");
        }

        // Broadcast the invitation
        mChatApi.receiveOneOneChatInvitation(session);
    }

    @Override
    public void handleAdhocGroupChatSessionInvitation(TerminatingAdhocGroupChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event receive ad-hoc group chat session invitation");
        }

        // Broadcast the invitation
        mChatApi.receiveGroupChatInvitation(session);
    }

    @Override
    public void handleStoreAndForwardMsgSessionInvitation(
            TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event S&F messages session invitation");
        }

        // Broadcast the invitation
        mChatApi.receiveOneOneChatInvitation(session);
    }

    @Override
    public void handleOneToOneMessageDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle one to one message delivery status");
        }

        mChatApi.receiveOneToOneMessageDeliveryStatus(contact, imdn);
    }

    @Override
    public void handleGroupMessageDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn) {
        mChatApi.getOrCreateGroupChat(chatId).handleMessageDeliveryStatus(contact, imdn);
    }

    @Override
    public void handleOneToOneFileDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle file delivery status: fileTransferId=" + imdn.getMsgId()
                    + " notification_type=" + imdn.getNotificationType() + " status="
                    + imdn.getStatus() + " contact=" + contact);
        }

        mFtApi.handleOneToOneFileDeliveryStatus(imdn, contact);
    }

    @Override
    public void handleGroupFileDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle group file delivery status: fileTransferId=" + imdn.getMsgId()
                    + " notification_type=" + imdn.getNotificationType() + " status="
                    + imdn.getStatus() + " contact=" + contact);
        }

        mFtApi.handleGroupFileDeliveryStatus(chatId, imdn, contact);
    }

    @Override
    public void handleSipMsrpSessionInvitation(Intent intent, GenericSipMsrpSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event receive SIP MSRP session invitation");
        }

        // Broadcast the invitation
        mSessionApi.receiveSipMsrpSessionInvitation(intent, session);
    }

    @Override
    public void handleSipRtpSessionInvitation(Intent intent, GenericSipRtpSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event receive SIP RTP session invitation");
        }

        // Broadcast the invitation
        mSessionApi.receiveSipRtpSessionInvitation(intent, session);
    }

    @Override
    public void handleUserConfirmationRequest(ContactId remote, String id, String type,
            boolean pin, String subject, String text, String acceptButtonLabel,
            String rejectButtonLabel, long timeout) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event user terms confirmation request");
        }

        // Nothing to do here
    }

    @Override
    public void handleUserConfirmationAck(ContactId remote, String id, String status,
            String subject, String text) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event user terms confirmation ack");
        }

        // Nothing to do here
    }

    @Override
    public void handleUserNotification(ContactId remote, String id, String subject, String text,
            String okButtonLabel) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event user terms notification");
        }

        // Nothing to do here
    }

    @Override
    public void handleSimHasChanged() {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle SIM has changed");
        }

        // Restart the RCS service
        LauncherUtils.stopRcsService(mContext);
        LauncherUtils.launchRcsService(mContext, true, false, mRcsSettings);
    }

    @Override
    public void handleIPCallInvitation(IPCallSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event IP call invitation");
        }

        // Broadcast the invitation
        mIpcallApi.receiveIPCallInvitation(session);
    }

    @Override
    public void handleFileTransferInvitationRejected(ContactId contact, MmContent content,
            MmContent fileicon, FileTransfer.ReasonCode reasonCode, long timestamp,
            long timestampSent) {
        mFtApi.addFileTransferInvitationRejected(contact, content, fileicon, reasonCode, timestamp,
                timestampSent);
    }

    @Override
    public void handleGroupChatInvitationRejected(String chatId, ContactId contact, String subject,
            Map<ContactId, ParticipantStatus> participants, GroupChat.ReasonCode reasonCode,
            long timestamp) {
        mChatApi.addGroupChatInvitationRejected(chatId, contact, subject, participants, reasonCode,
                timestamp);
    }

    @Override
    public void handleImageSharingInvitationRejected(ContactId contact, MmContent content,
            ImageSharing.ReasonCode reasonCode, long timestamp) {
        mIshApi.addImageSharingInvitationRejected(contact, content, reasonCode, timestamp);
    }

    @Override
    public void handleVideoSharingInvitationRejected(ContactId contact, VideoContent content,
            VideoSharing.ReasonCode reasonCode, long timestamp) {
        mVshApi.addVideoSharingInvitationRejected(contact, content, reasonCode, timestamp);
    }

    @Override
    public void handleGeolocSharingInvitationRejected(ContactId contact, GeolocContent content,
            GeolocSharing.ReasonCode reasonCode, long timestamp) {
        mGshApi.addGeolocSharingInvitationRejected(contact, content, reasonCode, timestamp);
    }

    @Override
    public void handleIPCallInvitationRejected(ContactId contact, AudioContent audioContent,
            VideoContent videoContent, IPCall.ReasonCode reasonCode, long timestamp) {
        mIpcallApi.addIPCallInvitationRejected(contact, audioContent, videoContent, reasonCode,
                timestamp);
    }

    public void handleOneOneChatSessionInitiation(OneToOneChatSession session) {
        mChatApi.handleOneToOneChatSessionInitiation(session);
    }

    @Override
    public void handleRejoinGroupChatAsPartOfSendOperation(String chatId) throws ServerApiException {
        mChatApi.handleRejoinGroupChatAsPartOfSendOperation(chatId);
    }

    @Override
    public void handleRejoinGroupChat(String chatId) throws ServerApiException {
        mChatApi.handleRejoinGroupChat(chatId);
    }

    @Override
    public void tryToStartImServiceTasks(InstantMessagingService imService) {
        Core core = Core.getInstance();
        /* Try to auto-rejoin group chats that are still marked as active. */
        mImOperationExecutor.execute(new GroupChatAutoRejoinTask(mMessagingLog, core));
        /* Try to start auto resuming of HTTP file transfers marked as PAUSED_BY_SYSTEM */
        mImOperationExecutor.execute(new FtHttpResumeManager(imService, mRcsSettings,
                mMessagingLog, mContactManager));
        /* Try to dequeue one-to-one chat messages and one-to-one file transfers. */
        mImOperationExecutor.execute(new OneToOneChatDequeueTask(mOperationLock, imService,
                mChatApi, mFtApi, mHistoryLog, mMessagingLog, mContactManager, mRcsSettings));

        ImdnManager imdnManager = imService.getImdnManager();
        if (imdnManager.isSendOneToOneDeliveryDisplayedReportsEnabled()
                || imdnManager.isSendGroupDeliveryDisplayedReportsEnabled()) {
            /*
             * Try to send delayed displayed notifications for read messages if they were not sent
             * before already. This only attempts to send report and in case of failure the report
             * will be sent later as postponed delivery report
             */
            mImOperationExecutor.execute(new DelayedDisplayNotificationDispatcher(
                    mLocalContentResolver, mChatApi));
        }
    }

    @Override
    public void tryToInviteQueuedGroupChatParticipantInvitations(String chatId,
            InstantMessagingService imService) {
        mImOperationExecutor.execute(new GroupChatInviteQueuedParticipants(chatId, mChatApi,
                mMessagingLog, imService));
    }

    @Override
    public void tryToDispatchAllPendingDisplayNotifications() {
        mImOperationExecutor.execute(new DelayedDisplayNotificationDispatcher(
                mLocalContentResolver, mChatApi));
    }

    @Override
    public void tryToDequeueGroupChatMessagesAndGroupFileTransfers(String chatId,
            InstantMessagingService imService) {
        mImOperationExecutor.execute(new GroupChatDequeueTask(mOperationLock, chatId, imService,
                mMessagingLog, mChatApi, mFtApi, mRcsSettings, mHistoryLog, mContactManager));
    }

    @Override
    public void tryToDequeueOneToOneChatMessages(ContactId contact,
            InstantMessagingService imService) {
        mImOperationExecutor.execute(new OneToOneChatMessageDequeueTask(mOperationLock, contact,
                imService, mMessagingLog, mChatApi, mRcsSettings, mContactManager));
    }

    @Override
    public void tryToDequeueFileTransfers(InstantMessagingService imService) {
        mImOperationExecutor.execute(new FileTransferDequeueTask(mOperationLock, imService,
                mMessagingLog, mFtApi, mContactManager, mRcsSettings));
    }

    @Override
    public void tryToMarkQueuedGroupChatMessagesAndGroupFileTransfersAsFailed(String chatId) {
        mImOperationExecutor.execute(new GroupChatTerminalExceptionTask(chatId, mChatApi, mFtApi,
                mHistoryLog, mOperationLock));
    }

    @Override
    public void handleOneToOneChatMessageDeliveryExpiration(Intent intent) {
        mOneToOneUndeliveredImManager.handleChatMessageDeliveryExpiration(intent);
    }

    @Override
    public void handleOneToOneFileTransferDeliveryExpiration(Intent intent) {
        mOneToOneUndeliveredImManager.handleFileTransferDeliveryExpiration(intent);
    }
}
