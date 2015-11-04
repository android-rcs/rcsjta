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
import com.gsma.rcs.core.ims.ImsError;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.history.HistoryLog;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.sharing.RichCallHistory;
import com.gsma.rcs.service.api.CapabilityServiceImpl;
import com.gsma.rcs.service.api.ChatServiceImpl;
import com.gsma.rcs.service.api.ContactServiceImpl;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.service.api.FileUploadServiceImpl;
import com.gsma.rcs.service.api.GeolocSharingServiceImpl;
import com.gsma.rcs.service.api.HistoryServiceImpl;
import com.gsma.rcs.service.api.ImageSharingServiceImpl;
import com.gsma.rcs.service.api.MultimediaSessionServiceImpl;
import com.gsma.rcs.service.api.VideoSharingServiceImpl;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.capability.ICapabilityService;
import com.gsma.services.rcs.chat.IChatService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.IContactService;
import com.gsma.services.rcs.extension.IMultimediaSessionService;
import com.gsma.services.rcs.filetransfer.IFileTransferService;
import com.gsma.services.rcs.history.IHistoryService;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharingService;
import com.gsma.services.rcs.sharing.image.IImageSharingService;
import com.gsma.services.rcs.sharing.video.IVideoSharingService;
import com.gsma.services.rcs.upload.IFileUploadService;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.concurrent.CountDownLatch;

/**
 * RCS core service. This service offers a flat API to any other process (activities) to access to
 * RCS features. This service is started automatically at device boot.
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsCoreService extends Service implements CoreListener {

    private static final String BACKGROUND_THREAD_NAME = RcsCoreService.class.getSimpleName();

    private CpuManager mCpuManager;

    private AccountChangedReceiver mAccountChangedReceiver;

    // --------------------- RCSJTA API -------------------------

    private ContactServiceImpl mContactApi;

    private CapabilityServiceImpl mCapabilityApi;

    private ChatServiceImpl mChatApi;

    private FileTransferServiceImpl mFtApi;

    private VideoSharingServiceImpl mVshApi;

    private ImageSharingServiceImpl mIshApi;

    private GeolocSharingServiceImpl mGshApi;

    private HistoryServiceImpl mHistoryApi;

    private MultimediaSessionServiceImpl mMmSessionApi;

    private FileUploadServiceImpl mUploadApi;

    /**
     * Need to start the core after stop if a StartService is called before the end of stopCore
     */
    private boolean mRestartCoreRequested = false;

    private Context mCtx;

    private RcsSettings mRcsSettings;

    private ContentResolver mContentResolver;

    private LocalContentResolver mLocalContentResolver;

    private MessagingLog mMessagingLog;

    private RichCallHistory mRichCallHistory;

    private HistoryLog mHistoryLog;

    private ContactManager mContactManager;

    private CountDownLatch mLatch;

    /**
     * Handler to process messages & runnable associated with background thread.
     */
    private Handler mBackgroundHandler;

    private final static Logger sLogger = Logger.getLogger(RcsCoreService.class.getSimpleName());

    @Override
    public void onCreate() {
        mCtx = getApplicationContext();
        mContentResolver = mCtx.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mHistoryLog = HistoryLog.createInstance(mLocalContentResolver);
        mRichCallHistory = RichCallHistory.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(mLocalContentResolver, mRcsSettings);
        mContactManager = ContactManager.createInstance(mCtx, mContentResolver,
                mLocalContentResolver, mRcsSettings);
        AndroidFactory.setApplicationContext(mCtx, mRcsSettings);
        final HandlerThread backgroundThread = new HandlerThread(BACKGROUND_THREAD_NAME);
        backgroundThread.start();

        mBackgroundHandler = new Handler(backgroundThread.getLooper());

        mLatch = new CountDownLatch(1);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    startCore();
                    mLatch.countDown();
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Unable to start IMS core!", e);
                    mLatch.countDown();
                }
            }
        });
        try {
            /*
             * After moving startcore() to a side thread, onCreate will finish before startcore is
             * done. This will lead to return null binder object when clients are trying to bind.
             * And android will cache the binder object which means it will always return null
             * afterward. Block main thread and wait for completion of startcore will prevent the
             * issue mentioned above.
             */
            mLatch.await();
        } catch (InterruptedException e) {
            /* Do nothing */
        }
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

        // @FIXME: This is not the final implementation, this is certainly an improvement over the
        // previous Handler implementation as for now stop core will run on worker thread instead of
        // main thread. However there is a need to properly refactor the whole start & stop core
        // functionality to properly handle simultaneous start/stop request's.
        mBackgroundHandler.post(new Runnable() {
            /**
             * Processing
             */
            public void run() {
                // TODO : This logic of stopping core during onDestroy() needs to be refactored
                // as it's not recommended to do such tasks in onDestroy() and as this method
                // eventually will also perform a de-register to IMS so this needs to be moved to a
                // much appropriate level.
                try {
                    stopCore();
                } catch (NetworkException e) {
                    if (sLogger.isActivated()) {
                        sLogger.debug(e.getMessage());
                    }

                } catch (ContactManagerException e) {
                    sLogger.error("Unable to stop IMS core!", e);

                } catch (PayloadException e) {
                    sLogger.error("Unable to stop IMS core!", e);

                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Unable to stop IMS core!", e);
                }
            }
        });
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
        com.gsma.services.rcs.contact.ContactUtil contactUtil = com.gsma.services.rcs.contact.ContactUtil
                .getInstance(this);
        if (!contactUtil.isMyCountryCodeDefined()) {
            if (logActivated) {
                sLogger.debug("Can't instanciate RCS core service, Reason : Country code not defined!");
            }
            stopSelf();
            return;
        }
        try {
            core = Core.createCore(mCtx, this, mRcsSettings, mContentResolver,
                    mLocalContentResolver, mContactManager, mMessagingLog, mHistoryLog,
                    mRichCallHistory);

            InstantMessagingService imService = core.getImService();
            RichcallService richCallService = core.getRichcallService();
            SipService sipService = core.getSipService();
            CapabilityService capabilityService = core.getCapabilityService();

            mContactApi = new ContactServiceImpl(mContactManager, mRcsSettings);
            mCapabilityApi = new CapabilityServiceImpl(capabilityService, mContactManager,
                    mRcsSettings);
            mChatApi = new ChatServiceImpl(imService, mMessagingLog, mHistoryLog, mRcsSettings,
                    mContactManager);
            mFtApi = new FileTransferServiceImpl(imService, mChatApi, mMessagingLog, mRcsSettings,
                    mContactManager, mCtx);
            mVshApi = new VideoSharingServiceImpl(richCallService, mRichCallHistory, mRcsSettings);
            mIshApi = new ImageSharingServiceImpl(richCallService, mRichCallHistory, mRcsSettings);
            mGshApi = new GeolocSharingServiceImpl(richCallService, mRichCallHistory, mRcsSettings);
            mHistoryApi = new HistoryServiceImpl(mCtx);
            mMmSessionApi = new MultimediaSessionServiceImpl(sipService, mRcsSettings,
                    mContactManager);
            mUploadApi = new FileUploadServiceImpl(imService, mRcsSettings);

            Logger.activationFlag = mRcsSettings.isTraceActivated();
            Logger.traceLevel = mRcsSettings.getTraceLevel();

            if (logActivated) {
                sLogger.info("RCS stack release is ".concat(TerminalInfo.getProductVersion(mCtx)));
            }

            core.initialize();

            core.startCore();

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
        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        } catch (KeyStoreException e) {
            sLogger.error("Can't instanciate the RCS core service", e);
            stopSelf();
        }
    }

    /**
     * Stop core
     * 
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    private synchronized void stopCore() throws PayloadException, NetworkException,
            ContactManagerException {
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
        if (mVshApi != null) {
            mVshApi.close();
            mVshApi = null;
        }
        if (mHistoryApi != null) {
            mHistoryApi.close();
            mHistoryApi = null;
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
        } else if (IMultimediaSessionService.class.getName().equals(intent.getAction())) {
            if (sLogger.isActivated()) {
                sLogger.debug("Multimedia session API binding");
            }
            return mMmSessionApi;
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
        if (mContactApi != null) {
            mContactApi.notifyRegistration();
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
        if (mMmSessionApi != null) {
            mMmSessionApi.notifyRegistration();
        }
    }

    /**
     * Notify unregistration to API
     * 
     * @param reason reason Code
     */
    private void notifyUnRegistrationToApi(RcsServiceRegistration.ReasonCode reason) {
        if (mCapabilityApi != null) {
            mCapabilityApi.notifyUnRegistration(reason);
        }
        if (mChatApi != null) {
            mChatApi.notifyUnRegistration(reason);
        }
        if (mContactApi != null) {
            mContactApi.notifyUnRegistration(reason);
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
        if (mMmSessionApi != null) {
            mMmSessionApi.notifyUnRegistration(reason);
        }
    }

    @Override
    public void onCoreLayerStarted() {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event core started");
        }

        Core core = Core.getInstance();
        core.getImService().onCoreLayerStarted();
        core.getRichcallService().onCoreLayerStarted();

        IntentUtils.sendBroadcastEvent(mCtx, RcsService.ACTION_SERVICE_UP);
    }

    @Override
    public void onCoreLayerStopped() {
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

        // @FIXME: This is not the final implementation, this is certainly an improvement over the
        // previous implementation as for now start core will run on worker thread instead of
        // main thread. However there is a need to properly refactor the whole start & stop core
        // functionality to properly handle simultaneous start/stop request's.
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    startCore();
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Unable to start IMS core!", e);
                }

            }
        });
    }

    @Override
    public void onRegistrationSuccessful() {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event registration ok");
        }
        notifyRegistrationToApi();
    }

    @Override
    public void onRegistrationFailed(ImsError error) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event registration failed");
        }
        notifyUnRegistrationToApi(RcsServiceRegistration.ReasonCode.CONNECTION_LOST);
    }

    @Override
    public void onRegistrationTerminated(RcsServiceRegistration.ReasonCode reason) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event registration terminated: ".concat(reason.name()));
        }
        notifyUnRegistrationToApi(reason);
    }

    @Override
    public void onUserConfirmationRequest(ContactId remote, String id, String type, boolean pin,
            String subject, String text, String acceptButtonLabel, String rejectButtonLabel,
            long timeout) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event user terms confirmation request");
        }
        // Nothing to do here
    }

    @Override
    public void onUserConfirmationAck(ContactId remote, String id, String status, String subject,
            String text) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event user terms confirmation ack");
        }
        // Nothing to do here
    }

    @Override
    public void onUserNotification(ContactId remote, String id, String subject, String text,
            String okButtonLabel) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event user terms notification");
        }
        // Nothing to do here
    }

    @Override
    public void onSimChangeDetected() {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle SIM has changed");
        }
        // Restart the RCS service
        LauncherUtils.stopRcsService(mCtx);
        LauncherUtils.launchRcsService(mCtx, true, false, mRcsSettings);
    }

}
