/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.ri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;
import com.gsma.services.rcs.ipcall.IPCallService;
import com.gsma.services.rcs.sharing.image.ImageSharingService;
import com.gsma.services.rcs.upload.FileUploadService;
import com.gsma.services.rcs.sharing.video.VideoSharingService;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * A class which manages connection to APIs
 * 
 * @author YPLO6403
 */
public class ApiConnectionManager {

    private static volatile ApiConnectionManager sInstance;

    /**
     * Enumerated type for RCS service name
     */
    @SuppressWarnings("javadoc")
    public enum RcsServiceName {
        CAPABILITY, CONTACTS, CHAT, FILE_TRANSFER, IMAGE_SHARING, VIDEO_SHARING, GEOLOC_SHARING, FILE_UPLOAD, IP_CALL, MULTIMEDIA;
    };

    /**
     * Set of connected services
     */
    final private Set<RcsServiceName> mConnectedServices;

    /**
     * Map of Activity / Client Connection notifier
     */
    final private Map<Activity, ClientConnectionNotifier> mClientsToNotify;

    /**
     * Map of RCS services and listeners
     */
    final private Map<RcsServiceName, RcsService> mApis;

    private static final String LOGTAG = LogUtils
            .getTag(ApiConnectionManager.class.getSimpleName());

    /**
     * Client connection listener
     */
    private class ClientConnectionNotifier {
        /**
         * The set of monitored services
         */
        private Set<RcsServiceName> mMonitoredServices;

        /**
         * The activity to notify
         */
        private Activity mActivity;

        /**
         * A locker to notify only once
         */
        private LockAccess mTriggerOnlyOnce;

        /**
         * Constructor
         * 
         * @param activity the activity to notify
         * @param triggerOnlyOnce lock access to trigger only once
         * @param services the list of services to monitor
         */
        public ClientConnectionNotifier(Activity activity, LockAccess triggerOnlyOnce,
                RcsServiceName... services) {
            mActivity = activity;
            mTriggerOnlyOnce = triggerOnlyOnce;
            mMonitoredServices = new HashSet<RcsServiceName>();
            for (RcsServiceName service : services) {
                mMonitoredServices.add(service);
            }
        }

        public void notifyDisconnection() {
            Utils.showMessageAndExit(mActivity, mActivity.getString(R.string.label_api_disabled),
                    mTriggerOnlyOnce);
        }

        public Set<RcsServiceName> getMonitoredServices() {
            return mMonitoredServices;
        }

    }

    /**
     * A broadcast receiver to catch ACTION_SERVICE_UP from the RCS stack
     */
    private class RcsServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null
                    && intent.getAction().equals(RcsService.ACTION_SERVICE_UP)) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "RCS service is UP");
                }
                // Connect all APIs
                ApiConnectionManager.getInstance(context).connectApis();
            }
        }
    }

    /**
     * Get an instance of ApiConnectionManager.
     * 
     * @param context the context
     * @return the singleton instance.
     */
    public static ApiConnectionManager getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ApiConnectionManager.class) {
            if (sInstance == null) {
                if (context == null) {
                    throw new IllegalArgumentException("Context is null");
                }
                sInstance = new ApiConnectionManager(context);
            }
        }
        return sInstance;
    }

    /**
     * Create a RCS service listener to monitor connection
     * 
     * @param service the service to monitor
     * @return the listener
     */
    private RcsServiceListener newRcsServiceListener(final RcsServiceName service) {
        return new RcsServiceListener() {
            @Override
            public void onServiceDisconnected(ReasonCode error) {
                mConnectedServices.remove(service);
                notifyDisconnection(service, error);
            }

            @Override
            public void onServiceConnected() {
                mConnectedServices.add(service);
            }
        };

    }

    /**
     * Constructor
     * 
     * @param context
     */
    private ApiConnectionManager(Context context) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "ApiConnectionManager");
        }
        // Construct list of connected services
        mConnectedServices = new HashSet<RcsServiceName>();
        // Construct list of clients to notify
        mClientsToNotify = new HashMap<Activity, ClientConnectionNotifier>();
        // Construct list of APIs
        mApis = new HashMap<RcsServiceName, RcsService>();
        // Instantiate APIs
        mApis.put(RcsServiceName.CAPABILITY, new CapabilityService(context,
                newRcsServiceListener(RcsServiceName.CAPABILITY)));
        mApis.put(RcsServiceName.CHAT, new ChatService(context,
                newRcsServiceListener(RcsServiceName.CHAT)));
        mApis.put(RcsServiceName.CONTACTS, new ContactsService(context,
                newRcsServiceListener(RcsServiceName.CONTACTS)));
        mApis.put(RcsServiceName.FILE_TRANSFER, new FileTransferService(context,
                newRcsServiceListener(RcsServiceName.FILE_TRANSFER)));
        mApis.put(RcsServiceName.IMAGE_SHARING, new ImageSharingService(context,
                newRcsServiceListener(RcsServiceName.IMAGE_SHARING)));
        mApis.put(RcsServiceName.VIDEO_SHARING, new VideoSharingService(context,
                newRcsServiceListener(RcsServiceName.VIDEO_SHARING)));
        mApis.put(RcsServiceName.FILE_UPLOAD, new FileUploadService(context,
                newRcsServiceListener(RcsServiceName.FILE_UPLOAD)));
        mApis.put(RcsServiceName.GEOLOC_SHARING, new GeolocSharingService(context,
                newRcsServiceListener(RcsServiceName.GEOLOC_SHARING)));
        mApis.put(RcsServiceName.IP_CALL, new IPCallService(context,
                newRcsServiceListener(RcsServiceName.IP_CALL)));
        mApis.put(RcsServiceName.MULTIMEDIA, new MultimediaSessionService(context,
                newRcsServiceListener(RcsServiceName.MULTIMEDIA)));
        // Register the broadcast receiver to catch ACTION_SERVICE_UP
        IntentFilter filter = new IntentFilter();
        filter.addAction(RcsService.ACTION_SERVICE_UP);
        context.registerReceiver(new RcsServiceReceiver(), filter);
    }

    /**
     * Connect APIs
     */
    public void connectApis() {
        // Connect all APIs
        for (RcsServiceName service : mApis.keySet()) {
            // Check if not already connected
            if (!isServiceConnected(service)) {
                try {
                    mApis.get(service).connect();
                } catch (Exception e) {
                    if (LogUtils.isActive) {
                        Log.e(LOGTAG, "Cannot connect service ".concat(service.name()), e);
                    }
                }
            }
        }
    }

    /**
     * Notify API disconnection to client
     * 
     * @param service the disconnected service
     * @param error the error
     */
    private void notifyDisconnection(RcsServiceName service, ReasonCode error) {
        for (ClientConnectionNotifier clienttoNotify : mClientsToNotify.values()) {
            if (clienttoNotify.getMonitoredServices().contains(service)) {
                clienttoNotify.notifyDisconnection();
            }
        }
    }

    /**
     * Check if services are connected
     * 
     * @param services list of services
     * @return true if all services of the list are connected
     */
    public boolean isServiceConnected(RcsServiceName... services) {
        for (RcsServiceName service : services) {
            if (!mConnectedServices.contains(service)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Start monitoring the services
     * 
     * @param activity the activity requesting to start monitoring the services
     * @param exitOnce a locker
     * @param services the list of services to monitor
     */
    public void startMonitorServices(Activity activity, LockAccess exitOnce,
            RcsServiceName... services) {
        mClientsToNotify.put(activity, new ClientConnectionNotifier(activity, exitOnce, services));
    }

    /**
     * Stop monitoring the services
     * 
     * @param activity the activity requesting to stop monitoring the services
     */
    public void stopMonitorServices(Activity activity) {
        mClientsToNotify.remove(activity);
    }

    /**
     * Get the instance of CapabilityService
     * 
     * @return the instance
     */
    public CapabilityService getCapabilityApi() {
        return (CapabilityService) mApis.get(RcsServiceName.CAPABILITY);
    }

    /**
     * Get the instance of ChatService
     * 
     * @return the instance
     */
    public ChatService getChatApi() {
        return (ChatService) mApis.get(RcsServiceName.CHAT);
    }

    /**
     * Get the instance of ContactsService
     * 
     * @return the instance
     */
    public ContactsService getContactsApi() {
        return (ContactsService) mApis.get(RcsServiceName.CONTACTS);
    }

    /**
     * Get the instance of FileTransferService
     * 
     * @return the instance
     */
    public FileTransferService getFileTransferApi() {
        return (FileTransferService) mApis.get(RcsServiceName.FILE_TRANSFER);
    }

    /**
     * Get the instance of VideoSharingService
     * 
     * @return the instance
     */
    public VideoSharingService getVideoSharingApi() {
        return (VideoSharingService) mApis.get(RcsServiceName.VIDEO_SHARING);
    }

    /**
     * Get the instance of ImageSharingService
     * 
     * @return the instance
     */
    public ImageSharingService getImageSharingApi() {
        return (ImageSharingService) mApis.get(RcsServiceName.IMAGE_SHARING);
    }

    /**
     * Get the instance of GeolocSharingService
     * 
     * @return the instance
     */
    public GeolocSharingService getGeolocSharingApi() {
        return (GeolocSharingService) mApis.get(RcsServiceName.GEOLOC_SHARING);
    }

    /**
     * Get the instance of FileUploadService
     * 
     * @return the instance
     */
    public FileUploadService getFileUploadApi() {
        return (FileUploadService) mApis.get(RcsServiceName.FILE_UPLOAD);
    }

    /**
     * Get the instance of IPCallService
     * 
     * @return the instance
     */
    public IPCallService getIPCallApi() {
        return (IPCallService) mApis.get(RcsServiceName.IP_CALL);
    }

    /**
     * Get the instance of MultimediaSessionService
     * 
     * @return the instance
     */
    public MultimediaSessionService getMultimediaSessionApi() {
        return (MultimediaSessionService) mApis.get(RcsServiceName.MULTIMEDIA);
    }

}
