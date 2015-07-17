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

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.RcsServiceRegistrationListener;
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

import com.orangelabs.rcs.ri.settings.SettingsDisplay;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.TimerUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class which manages connection to APIs and IMS platform
 * 
 * @author YPLO6403
 */
public class ConnectionManager {

    private final PendingIntent mPollCnxIntent;

    private static volatile ConnectionManager sInstance;

    /**
     * Notification ID
     */
    private final static int SERVICE_NOTIFICATION = 1000;

    private static final long API_CNX_POOLING_PERIOD = 30000;
    private static final long API_CNX_POOLING_START = 5000;

    /**
     * Enumerated type for RCS service name
     */
    @SuppressWarnings("javadoc")
    public enum RcsServiceName {
        CAPABILITY, CONTACT, CHAT, FILE_TRANSFER, IMAGE_SHARING, VIDEO_SHARING, GEOLOC_SHARING, FILE_UPLOAD, MULTIMEDIA, HISTORY;
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

    final private RcsServiceRegistrationListener mRcsServiceRegistrationListener = new RcsServiceRegistrationListener() {

        @Override
        public void onServiceUnregistered(RcsServiceRegistration.ReasonCode reason) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "IMS Service Registration connection lost");
            }
            notifyImsDisconnection(reason);
        }

        @Override
        public void onServiceRegistered() {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "IMS Service Registered");
            }
            notifyImsConnection();
        }
    };

    private final Context mContext;

    private final Handler mHandler = new Handler();

    private RcsServiceControl mRcsServiceControl;

    /**
     * The set of managed services
     */
    private final Set<RcsServiceName> mManagedServices;

    /**
     * A service is elected to monitor the IMS registration events.
     */
    private final RcsServiceName mElectedService;

    private static final String LOGTAG = LogUtils.getTag(ConnectionManager.class.getSimpleName());

    private static final String ACTION_POOL_CONNECTION = "com.orangelabs.rcs.ri.ACTION_POOL_CONNECTION";

    /**
     * Client connection listener
     */
    private class ClientConnectionNotifier {
        /**
         * The set of monitored services
         */
        private final Set<RcsServiceName> mMonitoredServices;

        /**
         * The activity to notify
         */
        private Activity mActivity;

        /**
         * A locker to notify only once
         */
        private LockAccess mTriggerOnlyOnce;

        private RcsServiceListener mListener;

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

        /**
         * Constructor
         * 
         * @param listener Listener to execute if a connection event occurs
         * @param services services to notify
         */
        public ClientConnectionNotifier(RcsServiceListener listener, RcsServiceName... services) {
            mListener = listener;
            mMonitoredServices = new HashSet<RcsServiceName>();
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
            }
        }

        public void notifyDisconnection(ReasonCode error) {
            if (mActivity != null) {
                String msg = null;
                if (ReasonCode.SERVICE_DISABLED == error) {
                    msg = mActivity.getString(R.string.label_api_disabled);
                } else {
                    msg = mActivity.getString(R.string.label_api_disconnected);
                }
                Utils.showMessageAndExit(mActivity, msg, mTriggerOnlyOnce);
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
            /* Connect all APIs */
            ConnectionManager.getInstance().connectApis();
        }
    }

    /**
     * Interface to listen for the response for RCS service starting state query
     */
    public interface IRcsServiceStartedListener {
        /**
         * Callback response
         * 
         * @param serviceStarted True if RCS services are started
         */
        public void handleResponse(boolean serviceStarted);
    }

    /**
     * Get an instance of ConnectionManager.
     * 
     * @param context the context
     * @param managedServices Set of managed services
     * @return the singleton instance.
     */
    public static ConnectionManager createInstance(Context context,
            Set<RcsServiceName> managedServices) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ConnectionManager.class) {
            if (sInstance == null) {
                if (context == null) {
                    throw new IllegalArgumentException("Context is null");
                }
                sInstance = new ConnectionManager(context, managedServices);
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
     * Create a RCS service listener to monitor API connection
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
                if (mElectedService == service) {
                    if (LogUtils.isActive) {
                        Log.w(LOGTAG, "API ".concat(error.name()));
                    }
                    notifyImsDisconnection(RcsServiceRegistration.ReasonCode.CONNECTION_LOST);
                }
            }

            @Override
            public void onServiceConnected() {
                mConnectedServices.add(service);
                notifyConnection(service);
                if (mElectedService != service) {
                    return;
                }
                /* Add a listener to be notified of IMS registration events */
                final RcsService service = mApis.get(mElectedService);
                mHandler.post(new Runnable() {
                    public void run() {
                        try {
                            service.addEventListener(mRcsServiceRegistrationListener);
                            if (service.isServiceRegistered()) {
                                notifyImsConnection();
                            } else {
                                RcsServiceRegistration.ReasonCode reason = service
                                        .getServiceRegistrationReasonCode();
                                notifyImsDisconnection(reason);
                            }
                        } catch (RcsServiceException e) {
                            if (LogUtils.isActive) {
                                Log.w(LOGTAG, "Cannot add RCS Service Registration Listener", e);
                            }
                        }
                    }
                });

            }
        };

    }

    /**
     * Constructor
     * 
     * @param context
     */
    private ConnectionManager(Context context, Set<RcsServiceName> managedServices) {
        mContext = context;
        mManagedServices = managedServices;
        mRcsServiceControl = RiApplication.getRcsServiceControl();
        /* Construct list of connected services */
        mConnectedServices = new HashSet<RcsServiceName>();
        /* Construct list of clients to notify */
        mClientsToNotify = new HashMap<Activity, ClientConnectionNotifier>();
        /* Construct list of APIs */
        mApis = new HashMap<RcsServiceName, RcsService>();

        mPollCnxIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_POOL_CONNECTION),
                0);

        if (managedServices == null || managedServices.isEmpty()) {
            throw new RuntimeException("Incorrect parameter managedService!");
        }
        mElectedService = managedServices.iterator().next();
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

    private boolean isRcsServiceStarted() {
        try {
            return mRcsServiceControl.isServiceStarted();
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Failed to get RCS service starting state!", e);
            }
            return false;
        }
    }

    /**
     * Start connection manager
     */
    public void start() {
        /* Register the broadcast receiver to catch ACTION_SERVICE_UP */
        mContext.registerReceiver(new RcsServiceReceiver(), new IntentFilter(
                RcsService.ACTION_SERVICE_UP));

        /* Register the broadcast receiver to pool periodically the API connections */
        mContext.registerReceiver(new TimerPoolRcsServiceStartingState(), new IntentFilter(
                ACTION_POOL_CONNECTION));

        /* By default we consider that IMS connection is lost */
        notifyImsDisconnection(RcsServiceRegistration.ReasonCode.CONNECTION_LOST);

        /* Start pooling connection */
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        TimerUtils.setExactTimer(am, System.currentTimeMillis() + API_CNX_POOLING_START,
                mPollCnxIntent);

        connectApis();
    }

    /**
     * Connect APIs
     */
    private void connectApis() {
        if (isRcsServiceStarted()) {
            /* Connect all APIs */
            for (RcsServiceName service : mApis.keySet()) {
                /* Check if not already connected */
                if (!isServiceConnected(service)) {
                    try {
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "Connect service ".concat(service.name()));
                        }
                        RcsService rcsService = mApis.get(service);
                        rcsService.connect();
                    } catch (Exception e) {
                        if (LogUtils.isActive) {
                            Log.e(LOGTAG, "Cannot connect service ".concat(service.name()), e);
                        }
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
        if (LogUtils.isActive) {
            Log.w(LOGTAG, new StringBuilder(service.name()).append(" ").append(error).toString());
        }
        for (ClientConnectionNotifier clienttoNotify : mClientsToNotify.values()) {
            Set<RcsServiceName> monitoredServices = clienttoNotify.getMonitoredServices();
            if (monitoredServices == null || monitoredServices.contains(service)) {
                clienttoNotify.notifyDisconnection(error);
            }
        }
    }

    private void notifyConnection(RcsServiceName service) {
        if (LogUtils.isActive) {
            Log.w(LOGTAG, new StringBuilder(service.name()).append(" ").append(" is connected")
                    .toString());
        }
        for (ClientConnectionNotifier clienttoNotify : mClientsToNotify.values()) {
            clienttoNotify.notifyConnection();
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
     * Get the instance of HistoryService
     * 
     * @return the instance
     */
    public HistoryService getHistoryApi() {
        return (HistoryService) mApis.get(RcsServiceName.HISTORY);
    }

    /**
     * Get the instance of MultimediaSessionService
     * 
     * @return the instance
     */
    public MultimediaSessionService getMultimediaSessionApi() {
        return (MultimediaSessionService) mApis.get(RcsServiceName.MULTIMEDIA);
    }

    private void notifyImsConnection() {
        addImsConnectionNotification(true, null);
    }

    private void notifyImsDisconnection(RcsServiceRegistration.ReasonCode reason) {
        addImsConnectionNotification(false, reason);
    }

    private void addImsConnectionNotification(boolean connected,
            RcsServiceRegistration.ReasonCode reason) {
        /* Create notification */
        Intent intent = new Intent(mContext, SettingsDisplay.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        int iconId;
        String label;
        if (connected) {
            iconId = R.drawable.ri_notif_on_icon;
            label = mContext.getString(R.string.ims_connected);
        } else {
            iconId = R.drawable.ri_notif_off_icon;
            if (RcsServiceRegistration.ReasonCode.BATTERY_LOW == reason) {
                label = mContext.getString(R.string.ims_battery_disconnected);
            } else {
                label = mContext.getString(R.string.ims_disconnected);
            }
        }
        String title = mContext.getString(R.string.notification_title_rcs_service);
        /* Create notification */
        Notification notif = buildImsConnectionNotification(contentIntent, title, label, iconId);
        notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_FOREGROUND_SERVICE;
        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(SERVICE_NOTIFICATION, notif);
    }

    /**
     * Generate a notification
     * 
     * @param intent
     * @param title
     * @param message
     * @param iconId
     * @return the notification
     */
    private Notification buildImsConnectionNotification(PendingIntent intent, String title,
            String message, int iconId) {
        /* Create notification */
        NotificationCompat.Builder notif = new NotificationCompat.Builder(mContext);
        notif.setContentIntent(intent);
        notif.setSmallIcon(iconId);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(false);
        notif.setOnlyAlertOnce(true);
        notif.setContentTitle(title);
        notif.setContentText(message);
        return notif.build();
    }

    private class TimerPoolRcsServiceStartingState extends BroadcastReceiver {
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
                        Log.e(LOGTAG, "Failed to pool conection to RCS service!", e);
                    } finally {
                        AlarmManager am = (AlarmManager) mContext
                                .getSystemService(Context.ALARM_SERVICE);
                        TimerUtils.setExactTimer(am, System.currentTimeMillis()
                                + API_CNX_POOLING_PERIOD, mPollCnxIntent);
                    }
                }
            }.start();
        }
    }

}
