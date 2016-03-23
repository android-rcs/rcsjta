/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.notif;

import com.gsma.rcs.registry.R;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceRegistration;
import com.gsma.services.rcs.RcsServiceRegistrationListener;
import com.gsma.services.rcs.capability.CapabilityService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Service manager that monitors the service availability and that displays the RCS status in the
 * notification bar.
 * 
 * @author Jean-Marc AUFFRET
 */
public class RcsServiceNotifManager extends Service {

    /**
     * Flag to disable or enable log
     */
    private static final boolean isActive = true;

    private static final String LOGTAG = new StringBuilder("[IMS]").append(
            RcsServiceNotifManager.class.getSimpleName()).toString();

    private final static int NOTIF_ID = 1000;

    private RcsServiceStartupListener mStartupEventReceiver;

    private RcsService mServiceApi;

    @Override
    public void onCreate() {
        if (isActive) {
            Log.d(LOGTAG, "Service started");
        }

        notifyImsUnregistered(RcsServiceRegistration.ReasonCode.UNSPECIFIED);

        RcsServiceStartupListener mStartupEventReceiver = new RcsServiceStartupListener();
        registerReceiver(mStartupEventReceiver, new IntentFilter(RcsService.ACTION_SERVICE_UP));

        connectToService(this);
    }

    @Override
    public void onDestroy() {
        if (isActive) {
            Log.d(LOGTAG, "Service stopped");
        }

        unregisterReceiver(mStartupEventReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connectToService(Context ctx) {
        if (isActive) {
            Log.d(LOGTAG, "Try to connect to service API");
        }
        try {
            if (!RcsServiceControl.getInstance(ctx).isServiceStarted()) {
                if (isActive) {
                    Log.d(LOGTAG, "RCS service not yet started");
                }
                return;
            }
            mServiceApi = new CapabilityService(ctx, newRcsServiceListener());
            mServiceApi.connect();
        } catch (RcsServiceException e) {
            if (isActive) {
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

            if (isActive) {
                Log.d(LOGTAG, "Service UP");
            }
            connectToService(context);
        }
    }

    private RcsServiceListener newRcsServiceListener() {
        return new RcsServiceListener() {
            @Override
            public void onServiceDisconnected(ReasonCode error) {
                if (isActive) {
                    Log.d(LOGTAG, "Service API disconnected");
                }
                notifyImsUnregistered(RcsServiceRegistration.ReasonCode.CONNECTION_LOST);
            }

            @Override
            public void onServiceConnected() {
                if (isActive) {
                    Log.d(LOGTAG, "Service API connected");
                }
                try {
                    mServiceApi.addEventListener(mRcsRegistrationListener);
                    if (mServiceApi.isServiceRegistered()) {
                        if (isActive) {
                            Log.d(LOGTAG, "IMS is registered");
                        }
                        notifyImsRegistered();
                    } else {
                        if (isActive) {
                            Log.d(LOGTAG, "IMS is unregistered");
                        }
                        RcsServiceRegistration.ReasonCode reason = mServiceApi
                                .getServiceRegistrationReasonCode();
                        notifyImsUnregistered(reason);
                    }
                } catch (RcsServiceException e) {
                    if (isActive) {
                        Log.w(LOGTAG, "Cannot add RCS Service Registration Listener", e);
                    }
                }
            }
        };
    }

    private RcsServiceRegistrationListener mRcsRegistrationListener = new RcsServiceRegistrationListener() {
        @Override
        public void onServiceUnregistered(RcsServiceRegistration.ReasonCode reason) {
            if (isActive) {
                Log.d(LOGTAG, "IMS has been unregistered");
            }
            notifyImsUnregistered(reason);
        }

        @Override
        public void onServiceRegistered() {
            if (isActive) {
                Log.d(LOGTAG, "IMS has been registered");
            }
            notifyImsRegistered();
        }
    };

    private void notifyImsRegistered() {
        addImsConnectionNotification(true, getString(R.string.ims_connected));
    }

    private void notifyImsUnregistered(RcsServiceRegistration.ReasonCode reason) {
        String label;
        if (RcsServiceRegistration.ReasonCode.BATTERY_LOW == reason) {
            label = getString(R.string.ims_battery_disconnected);
        } else {
            label = getString(R.string.ims_disconnected);
        }
        addImsConnectionNotification(false, label);
    }

    private void addImsConnectionNotification(boolean connected, String label) {
        String title = this.getString(R.string.notification_title_rcs_service);
        Notification notif = buildImsConnectionNotification(null, title, label, connected);
        notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_FOREGROUND_SERVICE;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID, notif);
    }

    private Notification buildImsConnectionNotification(PendingIntent intent, String title,
            String message, boolean connected) {
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(intent);
        // With Android 5.0 Lollipop it is no longer possible to use colored icons in the
        // notification area.
        // Only large icon supports colors but only small icon can be shown in notification bar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (connected) {
                notif.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ri_notif_on_icon_color));
                notif.setSmallIcon(R.drawable.ri_notif_on_icon_white);
            } else {
                notif.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ri_notif_off_icon_color));
                notif.setSmallIcon(R.drawable.ri_notif_off_icon_white);
            }
        } else {
            if (connected) {
                notif.setSmallIcon(R.drawable.ri_notif_on_icon_color);
            } else {
                notif.setSmallIcon(R.drawable.ri_notif_off_icon_color);
            }
        }
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(false);
        notif.setOnlyAlertOnce(true);
        notif.setContentTitle(title);
        notif.setContentText(message);
        return notif.build();
    }
}
