/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.api.connection;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.history.HistoryService;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;
import com.gsma.services.rcs.sharing.image.ImageSharingService;
import com.gsma.services.rcs.sharing.video.VideoSharingService;
import com.gsma.services.rcs.upload.FileUploadService;
import com.orangelabs.rcs.api.connection.utils.LogUtils;
import com.orangelabs.rcs.api.connection.utils.TimerUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class to manage RCS API connections
 *
 * @author YPLO6403
 */
public class ConnectionManager {

    private static final String LOGTAG = LogUtils.getTag(ConnectionManager.class.getSimpleName());

    private static final long API_DELAY_TO_CONNECT = 5000;

    private static volatile ConnectionManager sInstance;
    /**
     * Set of connected services
     */
    private final Set<RcsServiceName> mConnectedServices;

    /**
     * Map of Activity / Client Connection notifier
     */
    private final Map<Activity, ClientConnectionNotifier> mClientsToNotify;

    /**
     * Map of RCS services and listeners
     */
    private final Map<RcsServiceName, RcsService> mApis;

    private final Context mCtx;

    private final RcsServiceControl mRcsServiceControl;

    /**
     * The set of managed services
     */
    private final Set<RcsServiceName> mManagedServices;

    private final AlarmManager mAlarmManager;

    private PendingIntent mCnxIntent;

    private int mRetryCount;

    private static final int MAX_RETRY_API_CNX = 4;

    private static final String ACTION_CONNECT = "com.orangelabs.rcs.ri.api.ACTION_CONNECT";

    /**
     * Constructor
     *
     * @param context           The context
     * @param managedServices   Set of managed services
     * @param rcsServiceControl instance of RcsServiceControl
     */
    private ConnectionManager(Context context, Set<RcsServiceName> managedServices,
                              RcsServiceControl rcsServiceControl) {
        mCtx = context;
        mCnxIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_CONNECT), 0);
        mAlarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);

        mManagedServices = managedServices;
        mRcsServiceControl = rcsServiceControl;
        /* Construct list of connected services */
        mConnectedServices = new HashSet<>();
        /* Construct list of clients to notify */
        mClientsToNotify = new HashMap<>();
        /* Construct list of APIs */
        mApis = new HashMap<>();

        if (managedServices == null || managedServices.isEmpty()) {
            throw new RuntimeException("Incorrect parameter managedService!");
        }
        /* Instantiate APIs */
        for (RcsServiceName service : mManagedServices) {
            switch (service) {
                case CAPABILITY:
                    mApis.put(RcsServiceName.CAPABILITY, new CapabilityService(context,
                            newRcsServiceListener(RcsServiceName.CAPABILITY)));
                    break;
                case CHAT:
                    mApis.put(RcsServiceName.CHAT, new ChatService(context,
                            newRcsServiceListener(RcsServiceName.CHAT)));
                    break;
                case CONTACT:
                    mApis.put(RcsServiceName.CONTACT, new ContactService(context,
                            newRcsServiceListener(RcsServiceName.CONTACT)));
                    break;
                case FILE_TRANSFER:
                    mApis.put(RcsServiceName.FILE_TRANSFER, new FileTransferService(context,
                            newRcsServiceListener(RcsServiceName.FILE_TRANSFER)));
                    break;
                case FILE_UPLOAD:
                    mApis.put(RcsServiceName.FILE_UPLOAD, new FileUploadService(context,
                            newRcsServiceListener(RcsServiceName.FILE_UPLOAD)));
                    break;
                case GEOLOC_SHARING:
                    mApis.put(RcsServiceName.GEOLOC_SHARING, new GeolocSharingService(context,
                            newRcsServiceListener(RcsServiceName.GEOLOC_SHARING)));
                    break;
                case HISTORY:
                    mApis.put(RcsServiceName.HISTORY, new HistoryService(context,
                            newRcsServiceListener(RcsServiceName.HISTORY)));
                    break;
                case IMAGE_SHARING:
                    mApis.put(RcsServiceName.IMAGE_SHARING, new ImageSharingService(context,
                            newRcsServiceListener(RcsServiceName.IMAGE_SHARING)));
                    break;
                case MULTIMEDIA:
                    mApis.put(RcsServiceName.MULTIMEDIA, new MultimediaSessionService(context,
                            newRcsServiceListener(RcsServiceName.MULTIMEDIA)));
                    break;
                case VIDEO_SHARING:
                    mApis.put(RcsServiceName.VIDEO_SHARING, new VideoSharingService(context,
                            newRcsServiceListener(RcsServiceName.VIDEO_SHARING)));
                    break;
            }
        }
    }

    /**
     * Create a RCS service listener to monitor API connection
     *
     * @param service the service to monitor
     * @return the listener
     */
    private RcsServiceListener newRcsServiceListener(final RcsServiceName service) {
        return new RcsServiceListener() {
            @Override
            public void onServiceConnected() {
                mConnectedServices.add(service);
                notifyConnection(service);
            }

            @Override
            public void onServiceDisconnected(ReasonCode error) {
                mConnectedServices.remove(service);
                notifyDisconnection(service, error);
            }
        };

    }

    /**
     * Notify API disconnection to client
     *
     * @param service the disconnected service
     * @param error   the error
     */
    private void notifyDisconnection(RcsServiceName service, ReasonCode error) {
        if (LogUtils.isActive) {
            Log.w(LOGTAG, service.name() + " " + error);
        }
        for (ClientConnectionNotifier clientToNotify : mClientsToNotify.values()) {
            Set<RcsServiceName> monitoredServices = clientToNotify.getMonitoredServices();
            if (monitoredServices == null || monitoredServices.contains(service)) {
                clientToNotify.notifyDisconnection(error);
            }
        }
    }

    private void notifyConnection(RcsServiceName service) {
        if (LogUtils.isActive) {
            Log.w(LOGTAG, service.name() + " " + " is connected");
        }
        for (ClientConnectionNotifier clienttoNotify : mClientsToNotify.values()) {
            clienttoNotify.notifyConnection();
        }
    }

    /**
     * Get an instance of ConnectionManager.
     *
     * @param context           the context
     * @param rcsServiceControl instance of RcsServiceControl
     * @param services          list of managed services
     * @return the singleton instance.
     */
    public static ConnectionManager createInstance(Context context,
                                                   RcsServiceControl rcsServiceControl, RcsServiceName... services) {
        Set<RcsServiceName> managedServices = new HashSet<>();
        Collections.addAll(managedServices, services);
        return createInstance(context, rcsServiceControl, managedServices);
    }

    /**
     * Get an instance of ConnectionManager.
     *
     * @param ctx               the context
     * @param rcsServiceControl instance of RcsServiceControl
     * @param managedServices   Set of managed services
     * @return the singleton instance.
     */
    public static ConnectionManager createInstance(Context ctx,
                                                   RcsServiceControl rcsServiceControl, Set<RcsServiceName> managedServices) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ConnectionManager.class) {
            if (sInstance == null) {
                if (ctx == null) {
                    throw new IllegalArgumentException("Context is null");
                }
                sInstance = new ConnectionManager(ctx, managedServices, rcsServiceControl);
            }
        }
        return sInstance;
    }

    /**
     * Gets the singleton instance of ConnectionManager.
     *
     * @return the singleton instance.
     */
    public static ConnectionManager getInstance() {
        return sInstance;
    }

    /**
     * Start connection manager
     */
    public void start() {
        /* Register the broadcast receiver to catch ACTION_SERVICE_UP */
        mCtx.registerReceiver(new RcsServiceReceiver(), new IntentFilter(
                RcsService.ACTION_SERVICE_UP));
        /* Register the broadcast receiver to pool periodically the API connections */
        mCtx.registerReceiver(new ReceiveTimerToReConnectApi(), new IntentFilter(
                ACTION_CONNECT));
        mRetryCount = 0;
        connectApis();
    }

    /**
     * Connect APIs
     */
    private void connectApis() {
        try {
            if (mRcsServiceControl.isServiceStarted()) {
            /* Connect all APIs */
                for (RcsServiceName service : mApis.keySet()) {
                /* Check if not already connected */
                    if (!isServiceConnected(service)) {
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "Connect service ".concat(service.name()));
                        }
                        RcsService rcsService = mApis.get(service);
                        rcsService.connect();
                    }
                }
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, "Cannot connect service API: ".concat(e.getMessage()));
            mRetryCount++;
            if (mRetryCount < MAX_RETRY_API_CNX) {
                TimerUtils.setExactTimer(mAlarmManager, System.currentTimeMillis()
                        + API_DELAY_TO_CONNECT, mCnxIntent);
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Set timer to retry API connection");
                }
            } else {
                Log.e(LOGTAG, "Maximum attempts to connect API is reached");
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
            if (!mManagedServices.contains(service)) {
                throw new IllegalArgumentException("Service " + service
                        + " does not belong to set of managed services!");
            }
            if (!mConnectedServices.contains(service)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Start monitoring the API connection by using a callback listener
     *
     * @param activity the activity requesting to start monitoring the API connection
     * @param listener Listener to execute in case of connection event
     * @param services services to monitor
     */
    public void startMonitorApiCnx(Activity activity, RcsServiceListener listener,
                                   RcsServiceName... services) {
        mClientsToNotify.put(activity, new ClientConnectionNotifier(listener, services));
    }

    /**
     * Start monitoring the services and finish activities if service is disconnected
     *
     * @param activity    the activity requesting to start monitoring the services
     * @param iFinishable the interface to finish activity
     * @param services    the list of services to monitor
     */
    public void startMonitorServices(Activity activity, IRcsActivityFinishable iFinishable,
                                     RcsServiceName... services) {
        mClientsToNotify.put(activity, new ClientConnectionNotifier(iFinishable, services));
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
     * Stop monitoring the API connection
     *
     * @param activity the activity requesting to stop monitoring the API connection
     */
    public void stopMonitorApiCnx(Activity activity) {
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
    public ContactService getContactApi() {
        return (ContactService) mApis.get(RcsServiceName.CONTACT);
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
     * Get the instance of MultimediaSessionService
     *
     * @return the instance
     */
    public MultimediaSessionService getMultimediaSessionApi() {
        return (MultimediaSessionService) mApis.get(RcsServiceName.MULTIMEDIA);
    }

    /**
     * Enumerated type for RCS service name
     */
    @SuppressWarnings("javadoc")
    public enum RcsServiceName {
        CAPABILITY, CONTACT, CHAT, FILE_TRANSFER, IMAGE_SHARING, VIDEO_SHARING, GEOLOC_SHARING, FILE_UPLOAD, MULTIMEDIA, HISTORY
    }

    /**
     * Client connection listener
     */
    private class ClientConnectionNotifier {
        /**
         * The set of monitored services
         */
        private final Set<RcsServiceName> mMonitoredServices;

        private RcsServiceListener mListener;

        private IRcsActivityFinishable mIFinishable;

        /**
         * Constructor
         *
         * @param iFinishable Callback called when exception is thrown
         * @param services    the list of services to monitor
         */
        public ClientConnectionNotifier(IRcsActivityFinishable iFinishable,
                                        RcsServiceName... services) {
            mIFinishable = iFinishable;
            mMonitoredServices = new HashSet<>();
            Collections.addAll(mMonitoredServices, services);
        }

        /**
         * Constructor
         *
         * @param listener Listener to execute if a connection event occurs
         * @param services services to notify
         */
        public ClientConnectionNotifier(RcsServiceListener listener, RcsServiceName... services) {
            mListener = listener;
            mMonitoredServices = new HashSet<>();
            for (RcsServiceName service : services) {
                if (!mManagedServices.contains(service)) {
                    throw new IllegalArgumentException("Service " + service
                            + " does not belong to set of managed services!");
                }
                mMonitoredServices.add(service);
            }
        }

        public void notifyConnection() {
            if (mListener == null) {
                return;
            }
            if (mConnectedServices.containsAll(mMonitoredServices)) {
                /* All monitored services are connected -> notify connection */
                mListener.onServiceConnected();
                mRetryCount = 0;
            }
        }

        public void notifyDisconnection(ReasonCode error) {
            if (mIFinishable != null) {
                String msg;
                if (ReasonCode.SERVICE_DISABLED == error) {
                    msg = "RCS service disabled";
                } else {
                    msg = "RCS service disconnected";
                }
                mIFinishable.showMessageThenExit(msg);
                return;
            }
            if (mListener != null) {
                mListener.onServiceDisconnected(error);
            }
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
            if (!RcsService.ACTION_SERVICE_UP.equals(intent.getAction())) {
                return;
            }
            if (LogUtils.isActive) {
                Log.i(LOGTAG, "RCS service is UP");
            }
            /* Connect all APIs with delay */
            mRetryCount = 0;
            TimerUtils.setExactTimer(mAlarmManager, System.currentTimeMillis()
                    + API_DELAY_TO_CONNECT, mCnxIntent);
        }
    }

    private class ReceiveTimerToReConnectApi extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread() {
                public void run() {
                    try {
                        connectApis();

                    } catch (RuntimeException e) {
                        /*
                         * Intentionally catch runtime exceptions as else it will abruptly end the
                         * thread and eventually bring the whole system down, which is not intended.
                         */
                        Log.e(LOGTAG, "Failed to pool connection to RCS service!", e);
                    }
                }
            }.start();
        }
    }

}
