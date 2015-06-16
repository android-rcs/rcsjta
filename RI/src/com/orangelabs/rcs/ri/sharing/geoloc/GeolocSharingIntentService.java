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

package com.orangelabs.rcs.ri.sharing.geoloc;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingIntent;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Geoloc sharing intent service
 * 
 * @author YPLO6403
 */
public class GeolocSharingIntentService extends IntentService {

    private static final String LOGTAG = LogUtils.getTag(GeolocSharingIntentService.class
            .getSimpleName());

    static final String BUNDLE_GSH_ID = "bundle_gsh";

    /**
     * Creates an IntentService.
     */
    public GeolocSharingIntentService() {
        super("GeolocSharingIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // We want this service to stop running if forced stop
        // so return not sticky.
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }
        // Check action from incoming intent
        if (!GeolocSharingIntent.ACTION_NEW_INVITATION.equals(action)) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Unknown action ".concat(action));
            }
            return;
        }
        // Gets data from the incoming Intent
        String sharingId = intent.getStringExtra(GeolocSharingIntent.EXTRA_SHARING_ID);
        if (sharingId == null) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read sharing ID");
            }
            return;
        }

        try {
            ConnectionManager connectionManager = ConnectionManager.getInstance();
            if (!connectionManager.isServiceConnected(RcsServiceName.GEOLOC_SHARING)) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Cannot bind to GSH service");
                }
                return;
            }
            GeolocSharingService api = connectionManager.getGeolocSharingApi();
            if (api == null) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Cannot connect to GSH service");
                }
                return;
            }
            GeolocSharing gshSharing = api.getGeolocSharing(sharingId);
            if (gshSharing == null) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Cannot get geoloc sharing for ".concat(sharingId));
                }
                return;
            }
            ContactId contact = gshSharing.getRemoteContact();
            if (contact == null) {
                if (LogUtils.isActive) {
                    Log.e(LOGTAG, "Cannot get contact sharing for ".concat(sharingId));
                }
                return;
            }
            // Save contact into intent
            Bundle bundle = new Bundle();
            bundle.putParcelable(BUNDLE_GSH_ID, contact);
            intent.putExtras(bundle);
            // Display invitation notification
            addGeolocSharingInvitationNotification(intent, sharingId, contact);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot read GSH data from provider", e);
            }
        }
    }

    /**
     * Add geoloc share notification
     * 
     * @param intent Intent invitation
     * @param gshSharing the geoloc sharing
     */
    private void addGeolocSharingInvitationNotification(Intent invitation, String sharingId,
            ContactId contact) {
        /* Create pending intent */
        Intent intent = new Intent(invitation);
        intent.setClass(this, ReceiveGeolocSharing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        int uniqueId = Utils.getUniqueIdForPendingIntent();
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);
        String title = getString(R.string.title_recv_geoloc_sharing);

        /* Create notification */
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(contentIntent);
        notif.setSmallIcon(R.drawable.ri_notif_csh_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notif.setDefaults(Notification.DEFAULT_VIBRATE);
        notif.setContentTitle(title);
        notif.setContentText(getString(R.string.label_from_args, displayName));

        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(uniqueId, notif.build());
    }
}
