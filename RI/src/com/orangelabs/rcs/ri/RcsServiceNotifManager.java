
package com.orangelabs.rcs.ri;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.RcsServiceRegistrationListener;
import com.gsma.services.rcs.capability.CapabilityService;

import com.orangelabs.rcs.ri.utils.LogUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Service manager that monitors the service availability and that displays the RCS status in the
 * notification bar
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsServiceNotifManager extends Service {
    private static final String LOGTAG = LogUtils.getTag(RcsServiceNotifManager.class
            .getSimpleName());

    private final static int NOTIF_ID = 1000;

    private RcsServiceStartupListener mStartupEventReceiver;

    private RcsService mServiceApi;

    private static final String ACTION_VIEW_SETTINGS = "com.gsma.services.rcs.action.VIEW_SETTINGS";

    @Override
    public void onCreate() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Service started");
        }

        notifyImsUnregistered(RcsServiceRegistration.ReasonCode.UNSPECIFIED);

        RcsServiceStartupListener mStartupEventReceiver = new RcsServiceStartupListener();
        registerReceiver(mStartupEventReceiver, new IntentFilter(RcsService.ACTION_SERVICE_UP));

        connectToService(this);
    }

    @Override
    public void onDestroy() {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Service stopped");
        }

        unregisterReceiver(mStartupEventReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connectToService(Context ctx) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "Try to connect to service API");
        }
        try {
            if (!RcsServiceControl.getInstance(ctx).isServiceStarted()) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "RCS service not yet started");
                }
                return;
            }
            mServiceApi = new CapabilityService(ctx, newRcsServiceListener());
            mServiceApi.connect();
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot connect service API", e);
            }
        }
    }

    private class RcsServiceStartupListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!RcsService.ACTION_SERVICE_UP.equals(intent.getAction())) {
                return;
            }

            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Service UP");
            }
            connectToService(context);
        }
    }

    private RcsServiceListener newRcsServiceListener() {
        return new RcsServiceListener() {
            @Override
            public void onServiceDisconnected(ReasonCode error) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Service API disconnected");
                }
                notifyImsUnregistered(RcsServiceRegistration.ReasonCode.CONNECTION_LOST);
            }

            @Override
            public void onServiceConnected() {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Service API connected");
                }
                try {
                    mServiceApi.addEventListener(mRcsRegistrationListener);
                    if (mServiceApi.isServiceRegistered()) {
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "IMS is registered");
                        }
                        notifyImsRegistered();
                    } else {
                        if (LogUtils.isActive) {
                            Log.d(LOGTAG, "IMS is unregistered");
                        }
                        RcsServiceRegistration.ReasonCode reason = mServiceApi
                                .getServiceRegistrationReasonCode();
                        notifyImsUnregistered(reason);
                    }
                } catch (RcsServiceException e) {
                    if (LogUtils.isActive) {
                        Log.w(LOGTAG, "Cannot add RCS Service Registration Listener", e);
                    }
                }
            }
        };
    }

    private RcsServiceRegistrationListener mRcsRegistrationListener = new RcsServiceRegistrationListener() {
        @Override
        public void onServiceUnregistered(RcsServiceRegistration.ReasonCode reason) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "IMS has been unregistered");
            }
            notifyImsUnregistered(reason);
        }

        @Override
        public void onServiceRegistered() {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "IMS has been registered");
            }
            notifyImsRegistered();
        }
    };

    private void notifyImsRegistered() {
        addImsConnectionNotification(R.drawable.ri_notif_on_icon, getString(R.string.ims_connected));
    }

    private void notifyImsUnregistered(RcsServiceRegistration.ReasonCode reason) {
        String label;
        if (RcsServiceRegistration.ReasonCode.BATTERY_LOW == reason) {
            label = getString(R.string.ims_battery_disconnected);
        } else {
            label = getString(R.string.ims_disconnected);
        }
        addImsConnectionNotification(R.drawable.ri_notif_off_icon, label);
    }

    private void addImsConnectionNotification(int iconId, String label) {
        Intent intent = new Intent(ACTION_VIEW_SETTINGS);
        PendingIntent contentIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                intent, 0);
        String title = this.getString(R.string.notification_title_rcs_service);
        Notification notif = buildImsConnectionNotification(contentIntent, title, label, iconId);
        notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_FOREGROUND_SERVICE;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID, notif);
    }

    private Notification buildImsConnectionNotification(PendingIntent intent, String title,
            String message, int iconId) {
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(intent);
        notif.setSmallIcon(iconId);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(false);
        notif.setOnlyAlertOnce(true);
        notif.setContentTitle(title);
        notif.setContentText(message);
        return notif.build();
    }
}
